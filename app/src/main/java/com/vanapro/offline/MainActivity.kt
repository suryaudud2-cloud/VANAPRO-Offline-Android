package com.vanapro.offline

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var inputUri by remember { mutableStateOf<Uri?>(null) }
            var outputFile by remember { mutableStateOf<File?>(null) }
            var status by remember { mutableStateOf("Pilih video. Semua proses berjalan lokal di perangkat.") }

            val chooseVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                inputUri = uri
                status = if (uri == null) "Pemilihan dibatalkan." else "Video dipilih. Tekan Proses untuk ekspor MP4."
            }
            val saveOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
                val file = outputFile
                if (uri != null && file != null) {
                    try {
                        contentResolver.openOutputStream(uri)?.use { destination ->
                            file.inputStream().use { source -> source.copyTo(destination) }
                        }
                        status = "Selesai. Video tersimpan."
                    } catch (e: Exception) {
                        status = "Gagal menyimpan: ${e.localizedMessage}"
                    }
                }
            }

            MaterialTheme {
                Scaffold(topBar = { TopAppBar(title = { Text("VANAPRO OFFLINE") }) }) { inset ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(inset).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Pemrosesan video lokal untuk Android.")
                        Text("Target: POCO M5s • RAM 6 GB • video sumber hingga 1080p")
                        Button(onClick = { chooseVideo.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Pilih video")
                        }
                        Button(
                            enabled = inputUri != null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val selected = inputUri ?: return@Button
                                val destination = File(cacheDir, "vanapro_${System.currentTimeMillis()}.mp4")
                                outputFile = destination
                                status = "Sedang memproses video…"
                                val listener = object : Transformer.Listener {
                                    override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                                        runOnUiThread {
                                            status = "Pemrosesan selesai. Pilih lokasi penyimpanan."
                                            saveOutput.launch("VANAPRO_${System.currentTimeMillis()}.mp4")
                                        }
                                    }
                                    override fun onError(composition: androidx.media3.transformer.Composition, exportResult: ExportResult, exportException: ExportException) {
                                        runOnUiThread { status = "Ekspor gagal: ${exportException.localizedMessage}" }
                                    }
                                }
                                try {
                                    Transformer.Builder(this@MainActivity).addListener(listener).build()
                                        .start(EditedMediaItem.Builder(MediaItem.fromUri(selected)).build(), destination.absolutePath)
                                } catch (e: Exception) {
                                    status = "Tidak dapat memulai ekspor: ${e.localizedMessage}"
                                }
                            }
                        ) { Text("Proses / ekspor MP4") }
                        Text(status)
                        Text("Tidak menggunakan server, akun, lisensi, atau izin internet. Hasil mengikuti resolusi sumber dan dukungan codec perangkat.")
                    }
                }
            }
        }
    }
}
