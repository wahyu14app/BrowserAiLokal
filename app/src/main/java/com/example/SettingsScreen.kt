package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var jsEnabled by remember { mutableStateOf(true) }
    var saveHistory by remember { mutableStateOf(true) }
    var aiAutoScroll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Browser") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    text = "Umum",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Aktifkan JavaScript") },
                    supportingContent = { Text("Diperlukan agar sebagian besar situs web dan agen AI berfungsi.") },
                    trailingContent = {
                        Switch(checked = jsEnabled, onCheckedChange = { jsEnabled = it })
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Simpan Riwayat") },
                    supportingContent = { Text("Simpan riwayat penelusuran secara lokal.") },
                    trailingContent = {
                        Switch(checked = saveHistory, onCheckedChange = { saveHistory = it })
                    }
                )
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Agen AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Gulir Otomatis (Auto-Scroll)") },
                    supportingContent = { Text("Izinkan agen AI untuk menggulir halaman secara otomatis saat membaca.") },
                    trailingContent = {
                        Switch(checked = aiAutoScroll, onCheckedChange = { aiAutoScroll = it })
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Model AI Default") },
                    supportingContent = { Text("Gemini Pro (Cloud)") },
                    modifier = Modifier.clickable { /* Show dialog */ }
                )
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ListItem(
                    headlineContent = { Text("Hapus Data Browsing") },
                    supportingContent = { Text("Hapus cache, cookie, dan riwayat penelusuran.") },
                    colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.clickable { /* Clear data logic */ }
                )
            }
        }
    }
}
