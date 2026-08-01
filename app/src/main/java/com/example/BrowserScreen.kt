package com.example

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel = viewModel(), navController: NavController) {
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val isAgentRunning by viewModel.isAgentRunning.collectAsStateWithLifecycle()
    val isInteractiveAiActive by viewModel.isInteractiveAiActive.collectAsStateWithLifecycle()
    val isAutonomousLoopRunning by viewModel.isAutonomousLoopRunning.collectAsStateWithLifecycle()
    val currentAiThought by viewModel.currentAiThought.collectAsStateWithLifecycle()
    val actionLoopCount by viewModel.actionLoopCount.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf(currentUrl) }
    var showAiSheet by remember { mutableStateOf(false) }
    var aiInput by remember { mutableStateOf("") }
    
    // Sync text input with actual URL when it changes
    LaunchedEffect(currentUrl) {
        urlInput = currentUrl
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Browser Header: URL Bar
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                var target = urlInput
                                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                    target = "https://$target"
                                }
                                viewModel.webView?.loadUrl(target)
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { viewModel.webView?.reload() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }

        // Active Autonomous HUD Banner
        if (isInteractiveAiActive) {
            Surface(
                color = if (isAutonomousLoopRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAutonomousLoopRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAutonomousLoopRunning) "🧠 AI Interaktif Berjalan (#$actionLoopCount)" else "⚡ Fitur AI Interaktif Siap",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isAutonomousLoopRunning) {
                        Button(
                            onClick = { viewModel.stopAutonomousLoop() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("HENTIKAN AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.startAutonomousLoop() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Mulai AI Interaktif", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Main Browser Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            WebViewContainer(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel
            )

            // Floating Thought Overlay Badge on top of WebView
            if (isAutonomousLoopRunning && !currentAiThought.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentAiThought ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.stopAutonomousLoop() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            // Floating Action Button overlay for AI history
            FloatingActionButton(
                onClick = { showAiSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("ai_fab"),
                shape = CircleShape
            ) {
                if (isAgentRunning || isAutonomousLoopRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
                }
            }
        }

        // AI Task Bar (Input & Context)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = aiInput,
                            onValueChange = { aiInput = it },
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (aiInput.isNotBlank()) {
                                        viewModel.submitAgentTask(aiInput)
                                        aiInput = ""
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (aiInput.isEmpty()) {
                                    Text(
                                        text = "Masukkan prompt robot (misal: 'kelola produk')...",
                                        style = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (isAgentRunning) {
                        IconButton(
                            onClick = { viewModel.stopAgent() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Agent",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (aiInput.isNotBlank()) {
                                    viewModel.submitAgentTask(aiInput)
                                    aiInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Browser Controls (Standard Browser UI)
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.webView?.goBack() }, enabled = canGoBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.webView?.goForward() }, enabled = canGoForward) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    // Home Button (Pushed up)
                    Box(
                        modifier = Modifier
                            .offset(y = (-16).dp)
                            .size(56.dp)
                            .shadow(4.dp, CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .clickable { viewModel.webView?.loadUrl("https://www.google.com") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = { /* Tabs */ }) {
                            Icon(Icons.Default.Tab, contentDescription = "Tabs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 8.dp)
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Profil AI Web") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("profiles")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Alat") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("tools")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manajemen AI") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("ai_management")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pengaturan Browser") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("settings")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Panduan Penggunaan") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("instructions")
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
            }
        }
    }

    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AiAssistantSheetContent(viewModel = viewModel)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel
) {
    BackHandler(enabled = viewModel.canGoBack.collectAsStateWithLifecycle().value) {
        viewModel.webView?.goBack()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { viewModel.updateUrl(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { viewModel.onPageFinished(it) }
                    }
                    
                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        view?.let {
                            viewModel.updateNavState(it.canGoBack(), it.canGoForward())
                        }
                    }
                }
                
                loadUrl("https://www.google.com")
                viewModel.webView = this
            }
        },
        update = {
            viewModel.webView = it
        }
    )
}

@Composable
fun AiAssistantSheetContent(viewModel: BrowserViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isRunning by viewModel.isAgentRunning.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val isAutoLoopEnabled by viewModel.isAutoLoopEnabled.collectAsStateWithLifecycle()
    val isInteractiveAiActive by viewModel.isInteractiveAiActive.collectAsStateWithLifecycle()
    val isAutonomousLoopRunning by viewModel.isAutonomousLoopRunning.collectAsStateWithLifecycle()
    
    val activeProfile = profiles.find { 
        it.urlMatch != "*" && currentUrl.contains(it.urlMatch, ignoreCase = true) 
    } ?: profiles.find { it.urlMatch == "*" } ?: profiles.firstOrNull()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Chat Robot", "Kelola Aturan Aksi")
    var sheetPromptInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(top = 8.dp)
    ) {
        // Active Profile Role Banner & Interactive AI Switch
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            if (activeProfile != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profil Robot: ${activeProfile.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Instruksi: ${activeProfile.customInstructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Fitur AI Interaktif (Otonom) Toggle Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isInteractiveAiActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🧠 Fitur AI Interaktif (Pemikiran Otonom)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isInteractiveAiActive) "AI mendapatkan hak penuh untuk berpikir & membuat postingan/aksi secara terus menerus." else "Fitur AI Interaktif dinonaktifkan.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = isInteractiveAiActive,
                        onCheckedChange = { viewModel.setInteractiveAiActive(it) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTabIndex == 0) {
            // Chat Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    reverseLayout = false
                ) {
                    items(chatHistory) { msg ->
                        val (alignment, bgColor, textColor) = when (msg.role) {
                            "user" -> Triple(Alignment.CenterEnd, MaterialTheme.colorScheme.primaryContainer, Color.Black)
                            "ai" -> Triple(Alignment.CenterStart, MaterialTheme.colorScheme.secondaryContainer, Color.Black)
                            else -> Triple(Alignment.Center, Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = alignment
                        ) {
                            if (msg.role == "system") {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgColor,
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Prompt Shortcuts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.webView?.loadUrl("https://www.facebook.com") },
                        label = { Text("✍️ Buka Facebook", fontSize = 12.sp) }
                    )
                    if (isAutonomousLoopRunning) {
                        AssistChip(
                            onClick = { viewModel.stopAutonomousLoop() },
                            label = { Text("🛑 Hentikan AI", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                        )
                    } else {
                        AssistChip(
                            onClick = { viewModel.startAutonomousLoop() },
                            label = { Text("🧠 Mulai Loop AI", fontSize = 12.sp) }
                        )
                    }
                    AssistChip(
                        onClick = { viewModel.submitAgentTask("klik tombol login") },
                        label = { Text("⚡ Klik Login", fontSize = 12.sp) }
                    )
                }

                // Chat Input Row Inside Sheet
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = sheetPromptInput,
                            onValueChange = { sheetPromptInput = it },
                            placeholder = { Text("Ketik prompt instruksi robot...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isRunning) {
                            IconButton(onClick = { viewModel.stopAgent() }) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (sheetPromptInput.isNotBlank()) {
                                        viewModel.submitAgentTask(sheetPromptInput)
                                        sheetPromptInput = ""
                                    }
                                },
                                enabled = sheetPromptInput.isNotBlank()
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Kirim", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        } else {
            // Action Management Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (activeProfile == null) {
                    Text(
                        text = "Belum ada profil robot khusus. Silakan buat profil baru di menu Profil AI Web.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = "Daftar Aturan Aksi Terprogram:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (activeProfile.actions.isEmpty()) {
                        Text(
                            text = "Belum ada aksi yang didefinisikan untuk profil ini.\nJika tidak ada aksi yang cocok, robot akan berhenti bertindak secara aman.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(activeProfile.actions) { action ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "Jika menemukan/prompt: ${action.condition}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = "Maka eksekusi: ${action.action}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.removeActionFromProfile(activeProfile.id, action.id) }) {
                                            Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Hapus Aksi", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var newCondition by remember { mutableStateOf("") }
                    var newAction by remember { mutableStateOf("") }
                    
                    Text(text = "Tambah Kondisi & Aksi Baru:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = newCondition,
                        onValueChange = { newCondition = it },
                        label = { Text("Kondisi (misal: 'nonaktif', 'kriteria a')") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newAction,
                        onValueChange = { newAction = it },
                        label = { Text("Aksi (JavaScript / Link URL)") },
                        placeholder = { Text("document.querySelector('button').click();") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newCondition.isNotBlank() && newAction.isNotBlank()) {
                                viewModel.addActionToProfile(activeProfile.id, newCondition, newAction)
                                newCondition = ""
                                newAction = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newCondition.isNotBlank() && newAction.isNotBlank()
                    ) {
                        Text("Tambah Aksi ke Profil")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
