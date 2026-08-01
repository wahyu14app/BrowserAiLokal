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

    private val _profiles = MutableStateFlow<List<AiProfile>>(emptyList())
    val profiles = _profiles.asStateFlow()

    var webView: WebView? = null

    fun addProfile(profile: AiProfile) {
        _profiles.update { it + profile }
    }

    fun removeProfile(profileId: String) {
        _profiles.update { list -> list.filter { it.id != profileId } }
    }

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
                val agentResponse = callLocalAgent(task, simplifiedHtml)
                
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

    private suspend fun callLocalAgent(task: String, html: String): AgentDecision? = withContext(Dispatchers.IO) {
        val isLocalAiActive = false // TODO: Integrasi dengan model AI lokal (misal: MediaPipe LLM atau server lokal)

        if (!isLocalAiActive) {
            _chatHistory.update { it + ChatMessage("system", "AI belum di aktifkan. Harap bangun atau jalankan model AI lokal.") }
            return@withContext null
        }

        // Placeholder untuk respons AI lokal di masa mendatang
        return@withContext null
    }
}
