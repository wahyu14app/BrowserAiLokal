package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(navController: NavController, viewModel: BrowserViewModel = viewModel()) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil AI Web & Aturan Aksi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Profil")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (profiles.isEmpty()) {
                Text(
                    text = "Belum ada profil khusus. Tekan + untuk membuat profil robot AI.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profiles) { profile ->
                        ProfileItemCard(
                            profile = profile,
                            onDeleteProfile = { viewModel.removeProfile(profile.id) },
                            onAddAction = { condition, action ->
                                viewModel.addActionToProfile(profile.id, condition, action)
                            },
                            onRemoveAction = { actionId ->
                                viewModel.removeActionFromProfile(profile.id, actionId)
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddProfileDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, urlMatch, instructions ->
                    viewModel.addProfile(
                        AiProfile(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            urlMatch = urlMatch,
                            customInstructions = instructions
                        )
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ProfileItemCard(
    profile: AiProfile,
    onDeleteProfile: () -> Unit,
    onAddAction: (String, String) -> Unit,
    onRemoveAction: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var newCondition by remember { mutableStateOf("") }
    var newAction by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "URL Target: ${profile.urlMatch}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Toggle")
                }
                IconButton(onClick = onDeleteProfile) {
                    Icon(Icons.Default.Delete, "Hapus Profil", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "📌 Peran & Instruksi Utama:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = profile.customInstructions, style = MaterialTheme.typography.bodySmall)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "⚡ Daftar Aksi Terprogram (${profile.actions.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (profile.actions.isEmpty()) {
                        Text(
                            text = "Belum ada aksi. AI akan berhenti jika menemukan halaman tanpa aksi yang cocok.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        profile.actions.forEach { act ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Jika: ${act.condition}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "Aksi: ${act.action}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                    IconButton(onClick = { onRemoveAction(act.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Aksi", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "➕ Tambah Aksi Baru:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = newCondition,
                        onValueChange = { newCondition = it },
                        label = { Text("Kondisi (misal: 'nonaktif', 'kriteria A')") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newAction,
                        onValueChange = { newAction = it },
                        label = { Text("Aksi (JavaScript / Link URL)") },
                        placeholder = { Text("misal: alert('Aktifkan!'); document.querySelector('button').click();") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newCondition.isNotBlank() && newAction.isNotBlank()) {
                                onAddAction(newCondition, newAction)
                                newCondition = ""
                                newAction = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = newCondition.isNotBlank() && newAction.isNotBlank()
                    ) {
                        Text("Simpan Aksi")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var urlMatch by remember { mutableStateOf("*") }
    var instructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Profil Robot AI Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            name = "Penulis Quotes Facebook"
                            urlMatch = "facebook.com"
                            instructions = "Kamu adalah seorang penulis quotes di Facebook. Setiap kali halaman dimuat, kamu akan berpikir dan membuat postingan quote inspiratif baru secara terus menerus."
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✍️ Template FB Quotes", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            name = "Management Produk"
                            urlMatch = "*"
                            instructions = "Kamu adalah seorang manager yang bekerja sebagai pengelola produk."
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🛒 Management Produk", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Profil") },
                    placeholder = { Text("misal: Management Produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = urlMatch,
                    onValueChange = { urlMatch = it },
                    label = { Text("URL Target (Gunakan '*' untuk semua)") },
                    placeholder = { Text("misal: * atau admin/produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Peran / Instruksi Khusus Robot") },
                    placeholder = { Text("Kamu adalah seorang manager yang bekerja sebagai pengelola produk...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, urlMatch, instructions) },
                enabled = name.isNotBlank() && urlMatch.isNotBlank() && instructions.isNotBlank()
            ) {
                Text("Simpan Profil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
