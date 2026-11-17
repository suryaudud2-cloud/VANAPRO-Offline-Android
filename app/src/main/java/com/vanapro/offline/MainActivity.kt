package com.vanapro.offline

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
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
            setPadding(0, 18, 0, 30)
        }
        val pickButton = Button(this).apply {
            text = "PILIH VIDEO"
            setOnClickListener { openVideoPicker() }
        }
        val processButton = Button(this).apply {
            text = "PROSES / EKSPOR MP4"
            isEnabled = false
            tag = "process"
            setOnClickListener { startExport() }
        }
        statusView = TextView(this).apply {
            text = "Pilih video untuk memulai."
            textSize = 15f
            setPadding(0, 24, 0, 0)
        }
        root.addView(title, match())
        root.addView(subtitle, match())
        root.addView(pickButton, match())
        root.addView(processButton, match())
        root.addView(statusView, match())
        setContentView(root)
    }

    private fun match(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 12
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
            statusView.text = "Video dipilih. Tekan PROSES / EKSPOR MP4."
            (findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as? LinearLayout)
                ?.let { layout -> (layout.getChildAt(3) as? Button)?.isEnabled = true }
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
