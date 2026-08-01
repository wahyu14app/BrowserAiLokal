package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panduan Penggunaan") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Cara Menggunakan Browser Robot AI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                InstructionItem(
                    title = "1. Navigasi Dasar",
                    description = "Gunakan bilah URL di bagian atas untuk memasukkan alamat situs web. Tekan enter pada keyboard untuk memuat halaman. Gunakan tombol navigasi di bawah untuk kembali, maju, atau kembali ke beranda."
                )
            }
            item {
                InstructionItem(
                    title = "2. Manajemen Profil Robot AI",
                    description = "Buka menu 'Profil AI Web' untuk membuat persona robot. Anda dapat menentukan nama profil, URL target spesifik (atau '*' untuk semua web), dan instruksi tugas robot (misal: 'Kamu adalah pengelola produk')."
                )
            }
            item {
                InstructionItem(
                    title = "3. Membuat Aturan Aksi Terprogram",
                    description = "Di dalam setiap profil, Anda dapat membuat aturan otomatis (Jika-Maka). Contoh: Jika kondisi prompt atau halaman mengandung 'nonaktif', maka jalankan aksi berupa JavaScript atau URL."
                )
            }
            item {
                InstructionItem(
                    title = "4. Panel Interaksi Robot",
                    description = "Tekan tombol ikon robot mengambang di pojok kanan bawah untuk membuka panel Robot AI. Di sana, Anda bisa melihat log sistem, memicu aksi otomatis via chat, dan mengelola aturan aksi dengan cepat."
                )
            }
            item {
                InstructionItem(
                    title = "5. Keamanan Eksekusi Robot",
                    description = "Robot hanya akan bertindak dan mengeksekusi JavaScript jika kriteria kondisi terpenuhi berdasarkan profil aktif. Jika tidak ada aturan yang cocok, robot otomatis berhenti demi keamanan."
                )
            }
        }
    }
}

@Composable
fun InstructionItem(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
