package com.techazsure.leanflow

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class LearnFlowEngine(private val context: Context, private val onBrainReady: (Boolean, String) -> Unit) {

    private var llmInference: LlmInference? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch { initializeEngine() }
    }

    private suspend fun initializeEngine() {
        val modelName = "gemma-2n-quantized.task"
        val modelFile = File(context.filesDir, modelName)

        if (!modelFile.exists() || modelFile.length() < 100 * 1024 * 1024) {
            if (!copyModelFromAssets(modelName, modelFile)) {
                withContext(Dispatchers.Main) { onBrainReady(false, "Model file error.") }
                return
            }
        }

        try {
            // Stateless Initialization
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)

            withContext(Dispatchers.Main) { onBrainReady(true, "AI Engine Ready!") }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onBrainReady(false, "Init Failure: ${e.message}") }
        }
    }

    /**
     * STREAMING INFERENCE: Sends the full history context as a single prompt
     * and streams tokens back to the UI.
     */
    fun streamResponse(history: List<ChatMessage>, onTokenGenerated: (String, Boolean) -> Unit) {
        scope.launch {
            try {
                val prompt = buildPrompt(history)
                // Generate response asynchronously
                llmInference?.generateResponseAsync(prompt) { partialResult, done ->
                    onTokenGenerated(partialResult, done)
                }
            } catch (e: Exception) {
                onTokenGenerated("Inference Error: ${e.message}", true)
            }
        }
    }

    /**
     * BLOCKING INFERENCE: Returns the full response as a single string.
     */
    suspend fun queryWithContext(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(history)
            llmInference?.generateResponse(prompt) ?: "Engine not ready."
        } catch (e: Exception) {
            "Inference Error: ${e.message}"
        }
    }

    private fun buildPrompt(history: List<ChatMessage>): String {
        val promptBuilder = StringBuilder()
        for (message in history) {
            val tag = when (message.sender) {
                "USER" -> "<|user|>"
                "SYSTEM" -> "<|system|>"
                else -> "<|model|>"
            }
            promptBuilder.append("$tag\n${message.text}\n")
        }
        promptBuilder.append("<|model|>\n")
        return promptBuilder.toString()
    }

    private fun copyModelFromAssets(assetName: String, targetFile: File): Boolean {
        return try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(targetFile).use { output -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) { false }
    }

    fun close() {
        scope.cancel()
        llmInference?.close()
    }
}