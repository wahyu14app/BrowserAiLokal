package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alat (Tools)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
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
                ListItem(
                    headlineContent = { Text("Lihat Kode Sumber (View Source)") },
                    supportingContent = { Text("Tampilkan HTML mentah dari halaman saat ini.") },
                    modifier = Modifier.clickable { /* action */ }
                )
            }
            item {
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Ambil Tangkapan Layar") },
                    supportingContent = { Text("Simpan seluruh halaman sebagai gambar.") },
                    modifier = Modifier.clickable { /* action */ }
                )
            }
            item {
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Mode Desktop") },
                    supportingContent = { Text("Muat ulang halaman sebagai peramban desktop.") },
                    trailingContent = {
                        Switch(checked = false, onCheckedChange = { /* action */ })
                    }
                )
            }
            item {
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Mode Baca") },
                    supportingContent = { Text("Hilangkan gangguan dan fokus pada teks artikel.") },
                    modifier = Modifier.clickable { /* action */ }
                )
            }
        }
    }
}
