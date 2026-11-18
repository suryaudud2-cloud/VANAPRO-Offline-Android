package com.vanapro.offline

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale

/**
 * Creates five genuinely different Indonesian voice-over files with an installed
 * on-device voice. Network-dependent voices are deliberately rejected.
 */
class NarrationEngine(
    private val context: Context,
    private val onStatus: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var scripts: List<String> = emptyList()
    private var index = 0
    private var files: List<File> = emptyList()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            onStatus("Mesin Text-to-Speech tidak tersedia.")
            return
        }
        val engine = tts ?: return
        val voice = engine.voices.firstOrNull {
            it.locale.language == "id" && !it.isNetworkConnectionRequired
        }
        if (voice == null) {
            onStatus("Suara Indonesia offline tidak ditemukan. Pasang data suara Indonesia offline pada pengaturan Android.")
            return
        }
        engine.voice = voice
        engine.language = Locale("id", "ID")
        ready = true
        onStatus("TTS Indonesia offline siap.")
    }

    fun isReady(): Boolean = ready

    fun createFive(
        inputScripts: List<String>,
        onComplete: (List<File>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!ready || inputScripts.size != 5 || inputScripts.any { it.trim().length < 12 }) {
            onFailure("TTS belum siap atau lima naskah belum lengkap (minimal 12 karakter per naskah).")
            return
        }
        scripts = inputScripts.map { it.trim() }
        files = scripts.indices.map { File(context.cacheDir, "vanapro_voice_v${it + 1}.wav").apply { delete() } }
        index = 0
        val engine = tts ?: run {
            onFailure("Mesin TTS tidak tersedia.")
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onError(utteranceId: String?) {
                onFailure("TTS gagal pada versi ${index + 1}. Pastikan suara Indonesia offline terpasang.")
            }
            override fun onDone(utteranceId: String?) {
                val completed = index
                val file = files[completed]
                if (!file.exists() || file.length() == 0L) {
                    onFailure("File audio versi ${completed + 1} kosong.")
                    return
                }
                try {
                    saveToDownloads(file, "VANAPRO_Narration_V${completed + 1}.wav")
                } catch (error: Exception) {
                    onFailure("Gagal menyimpan audio: ${error.localizedMessage}")
                    return
                }
                index++
                if (index == scripts.size) {
                    onComplete(files)
                } else {
                    onStatus("Membuat audio ${index + 1}/5…")
                    synthesizeCurrent()
                }
            }
        })
        onStatus("Membuat audio 1/5 dengan TTS lokal…")
        synthesizeCurrent()
    }

    private fun synthesizeCurrent() {
        val result = tts?.synthesizeToFile(
            scripts[index],
            Bundle(),
            files[index],
            "vanapro_${index + 1}"
        )
        if (result != TextToSpeech.SUCCESS) {
            onStatus("Mesin TTS menolak naskah versi ${index + 1}.")
        }
    }

    private fun saveToDownloads(source: File, name: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/VANAPRO")
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values
        ) ?: error("Tidak dapat membuat file di Downloads.")
        context.contentResolver.openOutputStream(uri)?.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Tidak dapat menulis file audio.")
    }

    fun release() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) { }
        tts = null
        ready = false
    }
}
