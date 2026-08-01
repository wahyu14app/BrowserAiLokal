package com.example

import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.json.JSONObject

data class ChatMessage(
    val role: String,
    val text: String
)

class BrowserViewModel : ViewModel() {
    private val _currentUrl = MutableStateFlow("https://www.google.com")
    val currentUrl = _currentUrl.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward = _canGoForward.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning = _isAgentRunning.asStateFlow()

    var webView: WebView? = null

    fun updateUrl(url: String) {
        _currentUrl.value = url
    }

    fun updateNavState(canBack: Boolean, canForward: Boolean) {
        _canGoBack.value = canBack
        _canGoForward.value = canForward
    }

    fun submitAgentTask(task: String) {
        if (_isAgentRunning.value) return
        _chatHistory.update { it + ChatMessage("user", task) }
        runAgentLoop(task)
    }
    
    fun stopAgent() {
        _isAgentRunning.value = false
        _chatHistory.update { it + ChatMessage("system", "Task dihentikan oleh pengguna.") }
    }

    private fun runAgentLoop(task: String) {
        viewModelScope.launch {
            _isAgentRunning.value = true
            var isDone = false
            var stepCount = 0
            val maxSteps = 15

            while (_isAgentRunning.value && !isDone && stepCount < maxSteps) {
                stepCount++
                _chatHistory.update { it + ChatMessage("system", "Langkah $stepCount: Mengambil HTML dari halaman...") }
                
                val html = extractHtml()
                if (html == null) {
                    _chatHistory.update { it + ChatMessage("system", "Gagal mengambil HTML. Menghentikan tugas.") }
                    break
                }
                
                val simplifiedHtml = if (html.length > 50000) html.substring(0, 50000) + "...(terpotong)" else html

                _chatHistory.update { it + ChatMessage("system", "Menganalisis halaman dan merencanakan tindakan...") }
                val agentResponse = callGeminiAgent(task, simplifiedHtml)
                
                if (agentResponse == null) {
                    _chatHistory.update { it + ChatMessage("system", "Gagal mendapatkan respons dari AI. Menghentikan tugas.") }
                    break
                }
                
                _chatHistory.update { it + ChatMessage("ai", agentResponse.thought) }
                
                if (agentResponse.isDone) {
                    _chatHistory.update { it + ChatMessage("system", "Tugas selesai!") }
                    isDone = true
                    break
                }
                
                if (agentResponse.action == "execute_js" && !agentResponse.javascript.isNullOrEmpty()) {
                    _chatHistory.update { it + ChatMessage("system", "Mengeksekusi JavaScript: ${agentResponse.javascript}") }
                    executeJavascript(agentResponse.javascript)
                    // Wait a bit for the page to react
                    delay(3000)
                } else if (agentResponse.action == "navigate" && !agentResponse.url.isNullOrEmpty()) {
                    _chatHistory.update { it + ChatMessage("system", "Navigasi ke: ${agentResponse.url}") }
                    withContext(Dispatchers.Main) {
                        webView?.loadUrl(agentResponse.url)
                    }
                    delay(3000)
                } else {
                    _chatHistory.update { it + ChatMessage("system", "Aksi tidak dikenali atau kosong. Menghentikan tugas.") }
                    break
                }
            }
            
            if (stepCount >= maxSteps) {
                 _chatHistory.update { it + ChatMessage("system", "Mencapai batas maksimum langkah. Tugas dihentikan.") }
            }
            
            _isAgentRunning.value = false
        }
    }

    private suspend fun extractHtml(): String? = withContext(Dispatchers.Main) {
        return@withContext kotlin.coroutines.suspendCoroutine { continuation ->
            var resumed = false
            webView?.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })();",
                ValueCallback { html ->
                    if (!resumed) {
                        resumed = true
                        if (html != null && html != "null") {
                            // JS string comes wrapped in quotes, we need to unescape it roughly
                            val unescaped = html.removePrefix("\"").removeSuffix("\"")
                                .replace("\\u003C", "<")
                                .replace("\\\"", "\"")
                                .replace("\\n", "\n")
                                .replace("\\t", "\t")
                            continuation.resumeWith(Result.success(unescaped))
                        } else {
                            continuation.resumeWith(Result.success(null))
                        }
                    }
                }
            )
            // Timeout safety
            viewModelScope.launch {
                delay(2000)
                if (!resumed) {
                    resumed = true
                    continuation.resumeWith(Result.success(null))
                }
            }
        }
    }

    private suspend fun executeJavascript(js: String) = withContext(Dispatchers.Main) {
        webView?.evaluateJavascript(js, null)
    }

    data class AgentDecision(
        val thought: String,
        val action: String,
        val javascript: String?,
        val url: String?,
        val isDone: Boolean
    )

    private suspend fun callGeminiAgent(task: String, html: String): AgentDecision? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            _chatHistory.update { it + ChatMessage("system", "API Key tidak valid. Silakan atur di Secrets AI Studio.") }
            return@withContext null
        }

        val prompt = """
            Anda adalah Agent Browser AI yang bertugas menjalankan perintah pengguna.
            Tugas: "$task"
            
            Berikut adalah cuplikan HTML dari halaman saat ini:
            ```html
            $html
            ```
            
            Tentukan tindakan selanjutnya. Anda harus merespons DALAM FORMAT JSON sesuai dengan skema yang diberikan.
        """.trimIndent()

        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("thought") {
                    put("type", "STRING")
                    put("description", "Pemikiran Anda tentang apa yang ada di halaman dan apa yang harus dilakukan selanjutnya (Bahasa Indonesia).")
                }
                putJsonObject("action") {
                    put("type", "STRING")
                    put("description", "Tindakan yang akan dilakukan: 'execute_js', 'navigate', atau 'none'.")
                }
                putJsonObject("javascript") {
                    put("type", "STRING")
                    put("description", "Kode JavaScript untuk dieksekusi jika action='execute_js'. Gunakan ini untuk klik tombol, isi form, dll.")
                }
                putJsonObject("url") {
                    put("type", "STRING")
                    put("description", "URL tujuan jika action='navigate'.")
                }
                putJsonObject("isDone") {
                    put("type", "BOOLEAN")
                    put("description", "True jika tugas sudah selesai, false jika belum.")
                }
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = prompt))
                )
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "Anda adalah AI web automation agent. Analisis HTML, lalu hasilkan skrip untuk berinteraksi dengan DOM sesuai tugas."))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schema,
                temperature = 0.2f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val json = Json.parseToJsonElement(jsonText).jsonObject
                return@withContext AgentDecision(
                    thought = json["thought"]?.jsonPrimitive?.content ?: "",
                    action = json["action"]?.jsonPrimitive?.content ?: "none",
                    javascript = json["javascript"]?.jsonPrimitive?.content,
                    url = json["url"]?.jsonPrimitive?.content,
                    isDone = json["isDone"]?.jsonPrimitive?.booleanOrNull ?: false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
