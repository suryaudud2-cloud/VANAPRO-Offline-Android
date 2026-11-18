package com.vanapro.offline

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

class MainActivity : Activity() {
    private val requestVideo = 1001
    private val requestSave = 1002
    private var selectedVideo: Uri? = null
    private var outputFile: File? = null
    private lateinit var statusView: TextView
    private lateinit var processButton: Button
    private lateinit var metadataButton: Button
    private lateinit var narrationButton: Button
    private lateinit var narrationInputs: List<EditText>
    private var narrationEngine: NarrationEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
        narrationEngine = NarrationEngine(this) { message -> runOnUiThread { statusView.text = message; narrationButton.isEnabled = narrationEngine?.isReady() == true } }
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 48, 36, 32)
        }
        val title = TextView(this).apply {
            text = "VANAPRO OFFLINE"
            textSize = 26f
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Pemrosesan video lokal • POCO M5s • hingga 1080p"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 22)
        }
        val section = TextView(this).apply {
            text = "METADATA STUDIO"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        val pickButton = Button(this).apply {
            text = "PILIH VIDEO"
            setOnClickListener { openVideoPicker() }
        }
        metadataButton = Button(this).apply {
            text = "PERIKSA METADATA"
            isEnabled = false
            setOnClickListener { inspectMetadata() }
        }
        processButton = Button(this).apply {
            text = "PROSES / EKSPOR MP4"
            isEnabled = false
            setOnClickListener { startExport() }
        }
        val narrationTitle = TextView(this).apply {
            text = "AUTO CREATIVE VERSIONING • 5 NARASI"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        val narrationHint = TextView(this).apply {
            text = "Isi lima naskah komentar yang berbeda dan sesuai isi video. VANAPRO membuat lima file audio WAV dengan suara Indonesia offline."
            textSize = 13f
            setPadding(0, 4, 0, 8)
        }
        val profileNames = listOf("01 CINEMATIC", "02 ENGAGEMENT", "03 STORYTELLING", "04 COMMENTARY", "05 CREATIVE CUT")
        narrationInputs = profileNames.mapIndexed { index, profile ->
            EditText(this).apply {
                hint = "$profile — tulis narasi versi ${index + 1}"
                minLines = 2
                maxLines = 4
                gravity = Gravity.TOP
                textSize = 14f
            }
        }
        narrationButton = Button(this).apply {
            text = "BUAT 5 AUDIO NARASI OFFLINE"
            isEnabled = false
            setOnClickListener {
                narrationButton.isEnabled = false
                narrationEngine?.createFive(
                    narrationInputs.map { it.text.toString() },
                    onComplete = {
                        runOnUiThread {
                            narrationButton.isEnabled = true
                            statusView.text = "Selesai: lima WAV tersimpan di Downloads/VANAPRO."
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            narrationButton.isEnabled = narrationEngine?.isReady() == true
                            statusView.text = error
                        }
                    }
                )
            }
        }
        val note = TextView(this).apply {
            text = "Profil iPhone 17 Pro hanya ditampilkan sebagai referensi. Identitas kamera tidak akan dipalsukan. Pemeriksaan SynthID/C2PA otomatis belum tersedia pada versi ini."
            textSize = 13f
            setPadding(0, 12, 0, 8)
        }
        statusView = TextView(this).apply {
            text = "Pilih video untuk memulai."
            textSize = 15f
            setPadding(0, 12, 0, 0)
        }
        root.addView(title, match())
        root.addView(subtitle, match())
        root.addView(section, match())
        root.addView(pickButton, match())
        root.addView(metadataButton, match())
        root.addView(processButton, match())
        root.addView(narrationTitle, match())
        root.addView(narrationHint, match())
        narrationInputs.forEach { root.addView(it, match()) }
        root.addView(narrationButton, match())
        root.addView(note, match())
        root.addView(statusView, match())
        setContentView(root)
    }

    private fun match(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 10
        }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, requestVideo)
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        if (requestCode == requestVideo) {
            selectedVideo = data.data
            try {
                contentResolver.takePersistableUriPermission(data.data!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            statusView.text = "Video dipilih. Metadata siap diperiksa."
            metadataButton.isEnabled = true
            processButton.isEnabled = true
        } else if (requestCode == requestSave) {
            val target = data.data ?: return
            val source = outputFile ?: return
            try {
                contentResolver.openOutputStream(target)?.use { out ->
                    source.inputStream().use { input -> input.copyTo(out) }
                }
                statusView.text = "Selesai. Video tersimpan."
            } catch (e: Exception) {
                statusView.text = "Gagal menyimpan: ${e.localizedMessage}"
            }
        }
    }

    private fun inspectMetadata() {
        val uri = selectedVideo ?: return
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            fun value(key: Int, fallback: String = "Tidak tersedia"): String =
                retriever.extractMetadata(key)?.takeIf { it.isNotBlank() } ?: fallback

            val width = value(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = value(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val durationMs = value(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationMs.toLongOrNull()?.let {
                val seconds = it / 1000
                "${seconds / 60} menit ${seconds % 60} detik"
            } ?: "Tidak tersedia"
            val report = """
                METADATA SUMBER

                Judul: ${value(MediaMetadataRetriever.METADATA_KEY_TITLE)}
                Pembuat/artist: ${value(MediaMetadataRetriever.METADATA_KEY_ARTIST)}
                Tanggal: ${value(MediaMetadataRetriever.METADATA_KEY_DATE)}
                Penulis: ${value(MediaMetadataRetriever.METADATA_KEY_AUTHOR)}
                Format/MIME: ${value(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)}
                Resolusi: $width × $height
                Durasi: $duration
                Bitrate: ${value(MediaMetadataRetriever.METADATA_KEY_BITRATE)}
                Rotasi: ${value(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)}°
                Koordinat GPS: ${value(MediaMetadataRetriever.METADATA_KEY_LOCATION)}

                PROFIL IPHONE 17 PRO
                Profil referensi saja. Model kamera hanya boleh dicatat sebagai perangkat sumber jika rekaman memang dibuat dengan perangkat tersebut.

                C2PA / SYNTHID
                Status belum dianalisis oleh pemeriksa khusus. Metadata standar yang tampil di sini tidak membuktikan ada atau tidaknya watermark/provenance.
            """.trimIndent()
            AlertDialog.Builder(this)
                .setTitle("VANAPRO • Metadata Studio")
                .setMessage(report)
                .setPositiveButton("TUTUP", null)
                .show()
            statusView.text = "Metadata sumber berhasil dibaca."
        } catch (e: Exception) {
            statusView.text = "Gagal membaca metadata: ${e.localizedMessage}"
            AlertDialog.Builder(this).setTitle("Metadata tidak terbaca")
                .setMessage(e.localizedMessage ?: "Format file tidak didukung.")
                .setPositiveButton("OK", null).show()
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        narrationEngine?.release()
        narrationEngine = null
        super.onDestroy()
    }

    private fun startExport() {
        val uri = selectedVideo ?: return
        val destination = File(cacheDir, "vanapro_${System.currentTimeMillis()}.mp4")
        outputFile = destination
        statusView.text = "Sedang memproses di perangkat…"
        try {
            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                    runOnUiThread {
                        statusView.text = "Pemrosesan selesai. Pilih lokasi penyimpanan."
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_TITLE, "VANAPRO_${System.currentTimeMillis()}.mp4")
                        }
                        startActivityForResult(intent, requestSave)
                    }
                }
                override fun onError(composition: androidx.media3.transformer.Composition, exportResult: ExportResult, exportException: ExportException) {
                    runOnUiThread { statusView.text = "Ekspor gagal: ${exportException.localizedMessage}" }
                }
            }
            Transformer.Builder(this).addListener(listener).build()
                .start(EditedMediaItem.Builder(MediaItem.fromUri(uri)).build(), destination.absolutePath)
        } catch (e: Exception) {
            statusView.text = "Tidak dapat memulai ekspor: ${e.localizedMessage}"
        }
    }
}
