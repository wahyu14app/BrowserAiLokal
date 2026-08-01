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

    private val _isLocalAiActive = MutableStateFlow(true)
    val isLocalAiActive = _isLocalAiActive.asStateFlow()

    private val _isInteractiveAiActive = MutableStateFlow(true)
    val isInteractiveAiActive = _isInteractiveAiActive.asStateFlow()

    private val _isAutonomousLoopRunning = MutableStateFlow(false)
    val isAutonomousLoopRunning = _isAutonomousLoopRunning.asStateFlow()

    private val _currentAiThought = MutableStateFlow<String?>(null)
    val currentAiThought = _currentAiThought.asStateFlow()

    private val _actionLoopCount = MutableStateFlow(0)
    val actionLoopCount = _actionLoopCount.asStateFlow()

    private val _isAutoLoopEnabled = MutableStateFlow(true)
    val isAutoLoopEnabled = _isAutoLoopEnabled.asStateFlow()

    private var currentTaskPrompt: String? = null

    private val _profiles = MutableStateFlow<List<AiProfile>>(emptyList())
    val profiles = _profiles.asStateFlow()

    var webView: WebView? = null

    private val sampleQuotes = listOf(
        "Hati yang bersyukur adalah magnet bagi keajaiban kehidupan. ✨ #QuoteHariIni",
        "Jangan pernah menyerah pada mimpi yang belum selesai, setiap langkah kecil membawamu lebih dekat. 🌟",
        "Kerja keras mengalahkan bakat ketika bakat tidak bekerja keras. 💪 #Inspirasi",
        "Setiap hari adalah kesempatan baru untuk menjadi versi terbaik dari dirimu. 🌱",
        "Bahagia itu sederhana: bersyukur dengan apa yang ada saat ini dan tetap berjuang. ❤️",
        "Kesuksesan berawal dari keberanian untuk memulai langkah pertama. 🚀 #Motivasi",
        "Kegagalan adalah batu loncatan menuju keberhasilan yang lebih besar. 🏆",
        "Fokus pada prosesnya, hasil terbaik akan mengikuti usaha kerasmu. 🔥"
    )

    init {
        _profiles.value = listOf(
            AiProfile(
                id = "facebook_quotes_writer",
                name = "Penulis Quotes Facebook",
                urlMatch = "facebook.com",
                customInstructions = "Kamu adalah seorang penulis quotes di Facebook. Setiap kali halaman dimuat, kamu akan berpikir dan membuat postingan quote inspiratif baru secara terus menerus.",
                actions = listOf(
                    AiAction(
                        id = "fb_act_1",
                        condition = "facebook.com",
                        action = "Tulis & posting quote inspiratif baru ke Facebook"
                    )
                ),
                isInteractiveMode = true,
                autoLoopIntervalSeconds = 6
            ),
            AiProfile(
                id = "default_management_produk",
                name = "Management Produk",
                urlMatch = "*",
                customInstructions = "Kamu adalah seorang manager yang bekerja sebagai pengelola produk.",
                actions = listOf(
                    AiAction(
                        id = "act_1",
                        condition = "nonaktif",
                        action = "alert('Mengaktifkan produk dengan status nonaktif!'); document.querySelectorAll('button').forEach(b => { if(b.innerText.toLowerCase().includes('aktifkan')) b.click(); });"
                    ),
                    AiAction(
                        id = "act_2",
                        condition = "kriteria a",
                        action = "alert('Proses produk dengan Kriteria A selesai!');"
                    ),
                    AiAction(
                        id = "act_3",
                        condition = "klik tombol login",
                        action = "document.querySelectorAll('button, input[type=submit], input[type=button], a').forEach(b => { if(b.innerText.toLowerCase().includes('login') || b.value?.toLowerCase().includes('login')) b.click(); }); alert('Mencoba mengeklik tombol login');"
                    )
                )
            )
        )
    }

    fun setLocalAiActive(active: Boolean) {
        _isLocalAiActive.value = active
    }

    fun setInteractiveAiActive(active: Boolean) {
        _isInteractiveAiActive.value = active
        if (!active) {
            stopAutonomousLoop()
        }
    }

    fun addProfile(profile: AiProfile) {
        _profiles.update { it + profile }
    }

    fun removeProfile(profileId: String) {
        _profiles.update { list -> list.filter { it.id != profileId } }
    }

    fun addActionToProfile(profileId: String, condition: String, action: String) {
        _profiles.update { list ->
            list.map {
                if (it.id == profileId) {
                    it.copy(actions = it.actions + AiAction(condition = condition, action = action))
                } else it
            }
        }
    }

    fun removeActionFromProfile(profileId: String, actionId: String) {
        _profiles.update { list ->
            list.map {
                if (it.id == profileId) {
                    it.copy(actions = it.actions.filter { action -> action.id != actionId })
                } else it
            }
        }
    }

    fun setAutoLoopEnabled(enabled: Boolean) {
        _isAutoLoopEnabled.value = enabled
    }

    fun updateUrl(url: String) {
        _currentUrl.value = url
    }

    fun updateNavState(canBack: Boolean, canForward: Boolean) {
        _canGoBack.value = canBack
        _canGoForward.value = canForward
    }

    fun submitAgentTask(task: String) {
        currentTaskPrompt = task
        if (_isAgentRunning.value) return
        _chatHistory.update { it + ChatMessage("user", task) }
        runAgentLoop(task)
    }
    
    fun stopAgent() {
        _isAgentRunning.value = false
        _isAutonomousLoopRunning.value = false
        _currentAiThought.value = "Dihentikan oleh pengguna."
        _chatHistory.update { it + ChatMessage("system", "🛑 Task & AI Interaktif dihentikan oleh pengguna.") }
    }

    fun stopAutonomousLoop() {
        _isAutonomousLoopRunning.value = false
        _isAgentRunning.value = false
        _currentAiThought.value = "Mode AI Interaktif dihentikan."
        _chatHistory.update { it + ChatMessage("system", "🛑 Mode AI Interaktif dihentikan.") }
    }

    fun onPageFinished(url: String) {
        _currentUrl.value = url
        if (_isInteractiveAiActive.value && _isLocalAiActive.value) {
            startAutonomousLoop(url)
        } else if (_isAutoLoopEnabled.value && _isLocalAiActive.value && !_isAgentRunning.value) {
            val taskToRun = currentTaskPrompt ?: ""
            runPageLoadAutoInspect(url, taskToRun)
        }
    }

    fun startAutonomousLoop(url: String = _currentUrl.value) {
        if (!_isInteractiveAiActive.value || !_isLocalAiActive.value) return
        _isAutonomousLoopRunning.value = true
        
        viewModelScope.launch {
            delay(1000) // beri waktu DOM stabil
            var loopCount = 0
            
            while (_isAutonomousLoopRunning.value && _isInteractiveAiActive.value) {
                loopCount++
                _actionLoopCount.value = loopCount
                
                val activeProfile = _profiles.value.find { 
                    it.urlMatch != "*" && url.contains(it.urlMatch, ignoreCase = true) 
                } ?: _profiles.value.find { it.urlMatch == "*" } ?: _profiles.value.firstOrNull()

                val roleName = activeProfile?.name ?: "Asisten AI"
                val instructions = activeProfile?.customInstructions ?: "Otomasi halaman web secara cerdas"

                _currentAiThought.value = "🧠 [Langkah #$loopCount] AI Interaktif ($roleName) sedang menganalisis halaman..."
                _chatHistory.update { 
                    it + ChatMessage("system", "⚡ [Fitur AI Interaktif #$loopCount]: Berpikiran sebagai '$roleName' pada URL: $url") 
                }

                val html = extractHtml() ?: "<html><body>Not loaded</body></html>"
                
                val lowerInstructions = instructions.lowercase()
                if (lowerInstructions.contains("quote") || lowerInstructions.contains("penulis") || url.contains("facebook.com", ignoreCase = true)) {
                        val chosenQuote = sampleQuotes[(loopCount - 1) % sampleQuotes.size]
                        val thoughtMsg = "Saya adalah seorang penulis quotes Facebook. Saya telah membuat quote inspiratif baru: \"$chosenQuote\" dan mengetiknya ke dalam kolom postingan Facebook."
                        
                        _currentAiThought.value = thoughtMsg
                        _chatHistory.update { it + ChatMessage("ai", "🧠 Pemikiran AI (Otonom): $thoughtMsg") }

                        val quoteScript = """
                            (function() {
                                let textToPost = ${JSONObject.quote(chosenQuote)};
                                let inputs = document.querySelectorAll('textarea, [contenteditable="true"], input[type="text"], [role="textbox"]');
                                let filled = false;
                                for (let i = 0; i < inputs.length; i++) {
                                    let el = inputs[i];
                                    if (el.offsetWidth > 0 || el.offsetHeight > 0) {
                                        if (el.isContentEditable) {
                                            el.innerText = textToPost;
                                            el.dispatchEvent(new Event('input', { bubbles: true }));
                                        } else {
                                            el.value = textToPost;
                                            el.dispatchEvent(new Event('input', { bubbles: true }));
                                            el.dispatchEvent(new Event('change', { bubbles: true }));
                                        }
                                        filled = true;
                                        break;
                                    }
                                }
                                
                                setTimeout(function() {
                                    let buttons = document.querySelectorAll('button, input[type="submit"], [role="button"]');
                                    for (let j = 0; j < buttons.length; j++) {
                                        let btn = buttons[j];
                                        let txt = (btn.innerText || btn.value || '').toLowerCase();
                                        if (txt.includes('post') || txt.includes('kirim') || txt.includes('bagikan') || txt.includes('publish') || txt.includes('terbitkan')) {
                                            btn.click();
                                            break;
                                        }
                                    }
                                }, 1500);
                                
                                if (!filled) {
                                    alert('⚡ [AI Interaktif]: Mencari kolom postingan Facebook/Sosial Media...\nQuote Siap: ' + textToPost);
                                }
                                return filled ? 'Quote terpasang!' : 'Kolom tidak ditemukan';
                            })();
                        """.trimIndent()

                        executeJavascript(quoteScript)
                    } else if (activeProfile != null && activeProfile.actions.isNotEmpty()) {
                        val act = activeProfile.actions.first()
                        val thoughtMsg = "Menjalankan aturan profil '${activeProfile.name}': Jika ${act.condition} -> ${act.action}"
                        _currentAiThought.value = thoughtMsg
                        _chatHistory.update { it + ChatMessage("ai", "🧠 Pemikiran AI: $thoughtMsg") }
                        
                        if (act.action.startsWith("http")) {
                            withContext(Dispatchers.Main) { webView?.loadUrl(act.action) }
                        } else {
                            executeJavascript(act.action)
                        }
                    } else {
                        val defaultThought = "Halaman telah dianalisis. Menunggu input atau aksi selanjutnya..."
                        _currentAiThought.value = defaultThought
                        _chatHistory.update { it + ChatMessage("ai", "🧠 Pemikiran AI: $defaultThought") }
                    }

                val intervalSec = activeProfile?.autoLoopIntervalSeconds ?: 6
                _chatHistory.update { it + ChatMessage("system", "⏳ Menunggu $intervalSec detik sebelum berpikir & posting quote berikutnya...") }
                
                // Jeda waktu antar loop otonom
                delay((intervalSec * 1000).toLong())
            }
        }
    }

    private fun runPageLoadAutoInspect(url: String, prompt: String) {
        viewModelScope.launch {
            delay(1200) // allow DOM to settle after page finish
            if (_isAgentRunning.value || !_isAutoLoopEnabled.value) return@launch

            _chatHistory.update { 
                it + ChatMessage("system", "🔄 [Auto-Loop AI]: Halaman selesai dimuat. Memeriksa kriteria & tindakan otomatis...") 
            }
            runAgentLoop(if (prompt.isNotBlank()) prompt else "periksa halaman")
        }
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
                } else if (agentResponse.action != "done") {
                    _chatHistory.update { it + ChatMessage("system", "Aksi tidak dikenali. Melanjutkan tugas.") }
                }

                if (agentResponse.isDone) {
                    _chatHistory.update { it + ChatMessage("system", "Tugas selesai!") }
                    isDone = true
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
        if (!_isLocalAiActive.value) {
            _chatHistory.update { it + ChatMessage("system", "AI belum diaktifkan. Harap aktifkan di Manajemen AI terlebih dahulu.") }
            return@withContext null
        }

        // Cari profil aktif berdasarkan URL saat ini atau wildcard '*'
        val current = _currentUrl.value
        val activeProfile = _profiles.value.find { 
            it.urlMatch != "*" && current.contains(it.urlMatch, ignoreCase = true) 
        } ?: _profiles.value.find { it.urlMatch == "*" } ?: _profiles.value.firstOrNull()

        if (activeProfile != null) {
            _chatHistory.update { 
                it + ChatMessage("system", "🤖 Role Robot: [${activeProfile.name}] - \"${activeProfile.customInstructions}\"") 
            }

            // Cari aksi yang sesuai dengan prompt atau konten HTML halaman
            val matchedAction = activeProfile.actions.find { action ->
                task.contains(action.condition, ignoreCase = true) || 
                html.contains(action.condition, ignoreCase = true)
            }

            if (matchedAction != null) {
                _chatHistory.update { 
                    it + ChatMessage("system", "✅ Kondisi Terpenuhi: '${matchedAction.condition}'. Menjalankan aksi terprogram.") 
                }
                
                val isNav = matchedAction.action.startsWith("http://") || matchedAction.action.startsWith("https://") || matchedAction.action.startsWith("www.")
                val isJs = matchedAction.action.startsWith("document.") || matchedAction.action.startsWith("window.") || matchedAction.action.startsWith("alert(") || matchedAction.action.startsWith("console.")
                
                return@withContext AgentDecision(
                    thought = "Menjalankan instruksi '${matchedAction.condition}': ${matchedAction.action}",
                    action = if (isNav) "navigate" else "execute_js",
                    javascript = if (!isNav) matchedAction.action else null,
                    url = if (isNav) matchedAction.action else null,
                    isDone = true
                )
            } else {
                // Evaluasi aksi dinamis berbasis prompt jika tidak ada aturan statis yang cocok
                val cleanTask = task.lowercase().trim()
                if (cleanTask.contains("klik tombol") || cleanTask.contains("klik") || cleanTask.contains("tekan")) {
                    val targetName = cleanTask
                        .replace("klik tombol", "")
                        .replace("tekan tombol", "")
                        .replace("klik", "")
                        .replace("tekan", "")
                        .trim()
                    
                    val dynamicJs = """
                        (function() {
                            let clicked = false;
                            let target = '$targetName'.toLowerCase();
                            let elements = document.querySelectorAll('button, input[type=submit], input[type=button], a, [role=button]');
                            elements.forEach(el => {
                                if (!clicked && (el.innerText.toLowerCase().includes(target) || (el.value && el.value.toLowerCase().includes(target)))) {
                                    el.click();
                                    clicked = true;
                                }
                            });
                            if (clicked) {
                                console.log('Berhasil mengeklik ' + target);
                            } else {
                                alert('Tombol "' + target + '" tidak ditemukan pada halaman ini.');
                            }
                        })();
                    """.trimIndent()

                    _chatHistory.update { 
                        it + ChatMessage("system", "⚡ [Aksi Prompt]: Mengidentifikasi instruksi klik tombol '$targetName'. Executing DOM action...") 
                    }
                    return@withContext AgentDecision(
                        thought = "Mengeksekusi perintah klik tombol '$targetName'",
                        action = "execute_js",
                        javascript = dynamicJs,
                        url = null,
                        isDone = true
                    )
                } else if (cleanTask.contains("scroll") || cleanTask.contains("gulir")) {
                    val isUp = cleanTask.contains("atas") || cleanTask.contains("up")
                    val scrollJs = if (isUp) "window.scrollBy(0, -500);" else "window.scrollBy(0, 500);"
                    _chatHistory.update { 
                        it + ChatMessage("system", "⚡ [Aksi Prompt]: Mengeksekusi perintah gulir ${if (isUp) "ke atas" else "ke bawah"}.") 
                    }
                    return@withContext AgentDecision(
                        thought = "Mengeksekusi perintah gulir layar",
                        action = "execute_js",
                        javascript = scrollJs,
                        url = null,
                        isDone = true
                    )
                }

                _chatHistory.update { 
                    it + ChatMessage("system", "🛑 [Proteksi AI]: Berdasarkan instruksi '${activeProfile.customInstructions}', tidak ditemukan kriteria atau aksi yang cocok pada halaman ini. AI berhenti bertindak.") 
                }
                return@withContext AgentDecision(
                    thought = "Halaman tidak memiliki kriteria yang cocok. Robot AI berhenti sesuai aturan.",
                    action = "done",
                    javascript = null,
                    url = null,
                    isDone = true
                )
            }
        }

        _chatHistory.update { it + ChatMessage("system", "Tidak ada profil robot yang terdaftar. Menunggu instruksi atau profil baru.") }
        return@withContext AgentDecision(
            thought = "Saya tidak memiliki profil instruksi robot.",
            action = "done",
            javascript = null,
            url = null,
            isDone = true
        )
    }
}
