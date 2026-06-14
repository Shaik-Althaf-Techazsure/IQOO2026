package com.techazsure.leanflow

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BrainEngine(private val context: Context, private val onBrainReady: (Boolean, String) -> Unit) {

    private var llmInference: LlmInference? = null

    init {
        // PERMANENT STORAGE HIGHWAY: Accesses a persistent app folder that survives cleans/rebuilds
        val safeFolder = context.getExternalFilesDir(null)
        val modelFile = File(safeFolder, "gemma-2b-quantized.task")

        if (safeFolder == null || !modelFile.exists()) {
            println("[WARN] Model file not found in permanent storage folder.")
            onBrainReady(
                false,
                "Notice: Drop 'gemma-2b-quantized.task' inside the path: Device Explorer -> sdcard -> Android -> data -> com.techazsure.leanflow -> files"
            )
        } else {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .build()

                // Create the instance forcing native hardware optimization channels
                llmInference = LlmInference.createFromOptions(context, options)
                println("[SUCCESS] LearnFlow Brain: Fast Local GPU AI engine initialized successfully!")
                onBrainReady(true, "Local GPU AI Brain Engine Connected and Ready!")
            } catch (e: Exception) {
                // Fallback logic if the hardware context drops
                val errorDetails = e.localizedMessage ?: e.message ?: "Memory boundary error"
                println("[ERROR] MediaPipe GPU Initialization Failed: $errorDetails")
                onBrainReady(false, "Init Failure: $errorDetails")
            }
        }
    }

    suspend fun generateMentorResponse(userInput: String): String = withContext(Dispatchers.IO) {
        val engine = llmInference ?: return@withContext "Error: Local AI engine not initialized."

        val systemAgentPrompt = """
        You are 'LearnFlow', an autonomous Agentic Academic Companion operating completely offline.
        Be extremely concise, brief, and provide short, scannable structures to save processor cycles.
    """.trimIndent()

        return@withContext try {
            // Generates the output cleanly using the hardware layout track
            engine.generateResponse(systemAgentPrompt + "\nUser Input: " + userInput)
        } catch (e: Exception) {
            "Inference Execution Error: ${e.message}"
        }
    }
}