package com.techazsure.leanflow

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SpeechToTextEngine(private val context: Context, private val onModelLoaded: (Boolean) -> Unit) {

    private var voskModel: Model? = null
    private var isListening = false

    fun stopListening() {
        isListening = false
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targetDir = File(context.filesDir, "vosk-model")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                copyAssetsFolder("vosk-model", targetDir.absolutePath)
                voskModel = Model(targetDir.absolutePath)

                withContext(Dispatchers.Main) {
                    println("[SUCCESS] LearnFlow Ears: Engine Mounted background successfully!")
                    onModelLoaded(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    println("[ERROR] Failed background mount: ${e.message}")
                    onModelLoaded(false)
                }
            }
        }
    }

    private fun copyAssetsFolder(assetDir: String, targetDir: String) {
        val assetManager = context.assets
        var files: Array<String>? = null
        try {
            files = assetManager.list(assetDir)
        } catch (e: IOException) {
            println("[ERROR] Failed to get asset file list: ${e.message}")
        }

        if (files != null) {
            for (filename in files) {
                val assetPath = "$assetDir/$filename"
                val targetPath = "$targetDir/$filename"

                if (assetManager.list(assetPath)?.isNotEmpty() == true) {
                    File(targetPath).mkdirs()
                    copyAssetsFolder(assetPath, targetPath)
                } else {
                    try {
                        assetManager.open(assetPath).use { inputStream ->
                            FileOutputStream(targetPath).use { outputStream ->
                                val buffer = ByteArray(1024)
                                var read: Int
                                while (inputStream.read(buffer).also { read = it } != -1) {
                                    outputStream.write(buffer, 0, read)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Safely bypass auxiliary system entries
                    }
                }
            }
        }
    }

    /**
     * Streams microphone hardware vibrations and flags real-time text outputs.
     */
    suspend fun recordAudioStream(onPartialResult: (String) -> Unit, onFinalResult: (String) -> Unit) = withContext(Dispatchers.IO) {
        val model = voskModel ?: return@withContext println("[WARN] Core model not mounted yet.")

        // 1. Explicitly check if the RECORD_AUDIO permission is currently active in the OS context
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("[ERROR] Record Audio hardware permission is missing! Aborting voice stream track.")
            withContext(Dispatchers.Main) {
                onFinalResult("Hardware Error: Microphone permission not granted. Please enable it in Settings.")
            }
            return@withContext
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

        // 2. Wrap the initialization and recording invocation block in a defensive hardware try-catch wrapper
        try {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                println("[ERROR] Direct microphone hardware link failed to initialize.")
                return@withContext
            }

            val recognizer = Recognizer(model, sampleRate.toFloat())
            recognizer.setWords(true) // Keeps the parsing speed fast
            val buffer = ShortArray(bufferSize)

            audioRecord.startRecording()
            isListening = true
            println("[STREAM ACTIVE] Microphones live.")

            while (isListening) {
                val readBytes = audioRecord.read(buffer, 0, buffer.size)
                if (readBytes > 0) {
                    if (recognizer.acceptWaveForm(buffer, readBytes)) {
                        val text = JSONObject(recognizer.result).optString("text", "")
                        if (text.isNotEmpty()) {
                            isListening = false
                            withContext(Dispatchers.Main) { onFinalResult(text) }
                        }
                    } else {
                        val partialText =
                            JSONObject(recognizer.partialResult).optString("partial", "")
                        if (partialText.isNotEmpty()) {
                            withContext(Dispatchers.Main) { onPartialResult(partialText) }
                        }
                    }
                }
            }

            audioRecord.stop()
            audioRecord.release()
            println("[STREAM INACTIVE] Microphone stream stopped cleanly.")

        } catch (e: SecurityException) {
            // 🔥 FIXED: Explicitly captures the exception if permission checks slip past the system layout
            println("[FATAL PERMISSION EXCEPTION] User blocked hardware access stream: ${e.message}")
            isListening = false
            withContext(Dispatchers.Main) {
                onFinalResult("Permission Error: Enable microphone hardware rights.")
            }
        } catch (e: Exception) {
            println("[ERROR] Generic hardware capture failure: ${e.message}")
            isListening = false
        }
    }
}