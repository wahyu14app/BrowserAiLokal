package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiManagementScreen(viewModel: BrowserViewModel, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val baseDir = File(Environment.getExternalStorageDirectory(), ".browser-ai-lokal/models")
    var models by remember { mutableStateOf(listOf<File>()) }
    var isImporting by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val isLocalAiActive by viewModel.isLocalAiActive.collectAsStateWithLifecycle()

    fun refreshModels() {
        if (baseDir.exists()) {
            models = baseDir.listFiles()?.toList() ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        refreshModels()
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isImporting = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        if (!baseDir.exists()) {
                            baseDir.mkdirs()
                        }
                        
                        // Get file name from URI (simplified, usually you'd query MediaStore)
                        val fileName = uri.path?.substringAfterLast('/')?.plus(".literlm") ?: "imported_model_${System.currentTimeMillis()}.literlm"
                        val destFile = File(baseDir, fileName)
                        
                        context.contentResolver.openInputStream(it)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    snackbarMessage = "Model berhasil diimpor!"
                    refreshModels()
                } catch (e: Exception) {
                    snackbarMessage = "Gagal mengimpor model: ${e.message}"
                } finally {
                    isImporting = false
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            importLauncher.launch(arrayOf("*/*"))
        } else {
            snackbarMessage = "Izin penyimpanan diperlukan."
        }
    }

    fun handleImportClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                importLauncher.launch(arrayOf("*/*"))
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
                snackbarMessage = "Izinkan akses semua file, lalu coba lagi."
            }
        } else {
            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun deleteModel(file: File) {
        if (file.delete()) {
            snackbarMessage = "Model dihapus."
            refreshModels()
        } else {
            snackbarMessage = "Gagal menghapus model."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen AI") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { handleImportClick() }) {
                Icon(Icons.Default.Add, contentDescription = "Import Model")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Aktifkan AI Lokal",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = isLocalAiActive,
                    onCheckedChange = { viewModel.setLocalAiActive(it) }
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = "Format file model yang didukung: .literlm, .gguf, .bin, dll. Model yang diimpor akan diproses secara lokal.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (models.isEmpty()) {
                    Text(
                        text = "Tidak ada model AI lokal. Ketuk tombol + untuk mengimpor file.",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(models) { file ->
                            ListItem(
                                headlineContent = { Text(file.name) },
                                supportingContent = { Text("Ukuran: ${file.length() / (1024 * 1024)} MB") },
                                trailingContent = {
                                    IconButton(onClick = { deleteModel(file) }) {
                                        Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
