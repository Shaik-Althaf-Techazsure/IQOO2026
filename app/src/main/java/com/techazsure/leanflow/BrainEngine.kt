package com.techazsure.leanflow

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BrainEngine(private val context: Context, private val onBrainReady: (Boolean, String) -> Unit) {

    private var llmInference: LlmInference? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val safeFolder = context.getExternalFilesDir(null)
            // 🔥 UPDATED: Match the filename you placed in assets
            val modelFile = File(safeFolder, "gemma-2n-quantized.task")

            if (safeFolder == null) {
                withContext(Dispatchers.Main) { onBrainReady(false, "Internal Storage Error") }
                return@launch
            }

            // 1. Check if model exists in internal storage, if not, try to copy from assets
            if (!modelFile.exists()) {
                withContext(Dispatchers.Main) { onBrainReady(false, "Preparing AI Model (Copying from Assets)...") }
                val success = copyModelFromAssets(modelFile)
                if (!success) {
                    withContext(Dispatchers.Main) { 
                        onBrainReady(false, "Error: 'gemma-2n-quantized.task' not found in assets folder.")
                    }
                    return@launch
                }
            }

            // 2. Initialize MediaPipe
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                withContext(Dispatchers.Main) {
                    println("[SUCCESS] LearnFlow Brain Ready!")
                    onBrainReady(true, "Local GPU AI Brain Engine Connected!")
                }
            } catch (e: Exception) {
                val errorDetails = e.localizedMessage ?: "Memory boundary error"
                withContext(Dispatchers.Main) { onBrainReady(false, "Init Failure: $errorDetails") }
            }
        }
    }

    private fun copyModelFromAssets(targetFile: File): Boolean {
        return try {
            // 🔥 UPDATED: Match the filename you placed in assets
            context.assets.open("gemma-2n-quantized.task").use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(1024 * 8)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                }
            }
            true
        } catch (e: Exception) {
            println("[ERROR] Failed to copy model: ${e.message}")
            false
        }
    }

    suspend fun generateMentorResponse(userInput: String): String = withContext(Dispatchers.IO) {
        val engine = llmInference ?: return@withContext "Error: Local AI engine not initialized."

        val systemAgentPrompt = """
        You are 'LearnFlow', an autonomous Agentic Academic Companion operating completely offline.
        Be extremely concise and professional. 
        IMPORTANT: Do NOT use markdown symbols like '**' or '###'. 
        Do NOT use bullet points. 
        Use natural, clear paragraphs. Provide a beautiful, flowing response.
    """.trimIndent()

        return@withContext try {
            // Generates the output cleanly using the hardware layout track
            engine.generateResponse(systemAgentPrompt + "\nUser Input: " + userInput)
        } catch (e: Exception) {
            "Inference Execution Error: ${e.message}"
        }
    }
}