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
                    text = "Cara Menggunakan Browser AI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                InstructionItem(
                    title = "1. Navigasi Dasar",
                    description = "Gunakan bilah URL di bagian atas untuk memasukkan alamat situs web. Tekan tombol Go pada keyboard untuk memuat halaman. Gunakan tombol navigasi di bawah untuk kembali, maju, atau kembali ke beranda."
                )
            }
            item {
                InstructionItem(
                    title = "2. Memerintah Agen AI",
                    description = "Di bagian bawah layar, terdapat kolom teks khusus untuk asisten AI. Masukkan instruksi Anda (misal: \"Cari artikel tentang AI\", \"Klik tombol login\", \"Scroll ke bawah\") lalu kirim. AI akan menganalisis halaman dan mengambil tindakan yang sesuai."
                )
            }
            item {
                InstructionItem(
                    title = "3. Riwayat Tugas AI",
                    description = "Tekan tombol ikon bintang (✨) mengambang di pojok kanan bawah untuk membuka panel riwayat. Di sana, Anda bisa melihat apa yang sedang dipikirkan AI, aksi apa yang dijalankannya (seperti klik atau mengetik), dan log sistem."
                )
            }
            item {
                InstructionItem(
                    title = "4. Menghentikan AI",
                    description = "Jika AI sedang berjalan, tombol kirim akan berubah menjadi tombol stop. Tekan tombol tersebut untuk menghentikan proses agen AI seketika."
                )
            }
            item {
                InstructionItem(
                    title = "5. API Key",
                    description = "Agar fitur agen otomatis ini berfungsi, pastikan GEMINI_API_KEY telah diatur di Environment Variables / Secrets."
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
