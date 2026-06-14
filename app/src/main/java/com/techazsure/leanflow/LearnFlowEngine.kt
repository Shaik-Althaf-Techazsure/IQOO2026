package com.techazsure.leanflow

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.techazsure.leanflow.ui.ChatMessage
import com.techazsure.leanflow.ui.ChatRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LearnFlowEngine(private val context: Context, private val onBrainReady: (Boolean, String) -> Unit) {

    private var llmInference: LlmInference? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            initializeEngine()
        }
    }

    private suspend fun initializeEngine() {
        // Updated name to match the logs and common MediaPipe naming
        val modelName = "gemma-2b-quantized.task"
        val modelFile = File(context.filesDir, modelName)

        // 1. AUTO-DEPLOYMENT: Check if the model exists in internal storage
        if (!modelFile.exists()) {
            println("[INFO] Model not found in internal storage. Attempting to copy from assets...")
            val success = copyModelFromAssets(modelName, modelFile)
            if (!success) {
                // Fallback attempt: The user might have named it gemma.task in assets
                val retrySuccess = copyModelFromAssets("gemma.task", modelFile)
                if (!retrySuccess) {
                    withContext(Dispatchers.Main) {
                        onBrainReady(false, "Failed to copy model from assets. Please ensure $modelName is in the assets folder.")
                    }
                    return
                }
            }
        }

        // 2. INITIALIZATION: Create the MediaPipe Inference engine
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512) // Slightly reduced for better mobile responsiveness
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            println("[SUCCESS] LearnFlow Brain: Engine graph initialized safely!")
            withContext(Dispatchers.Main) {
                onBrainReady(true, "Local AI Brain Engine Connected and Ready!")
            }
        } catch (e: Exception) {
            val detailedError = "Init Failure: ${e.localizedMessage ?: e.message ?: "Native memory allocation crash"}"
            println("[ERROR] MediaPipe Initialization Failed: $detailedError")
            withContext(Dispatchers.Main) {
                onBrainReady(false, detailedError)
            }
        }
    }

    private fun copyModelFromAssets(assetName: String, targetFile: File): Boolean {
        return try {
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            println("[SUCCESS] Model copied from assets ($assetName) to: ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            println("[ERROR] Failed to copy model ($assetName) from assets: ${e.message}")
            false
        }
    }

    suspend fun queryWithContext(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val engine = llmInference ?: return@withContext "Error: Local AI engine not initialized. Please ensure the model is copied and the engine is ready."

        val promptBuilder = StringBuilder()

        // Compile history into ChatML format
        for (message in history) {
            when (message.role) {
                ChatRole.SYSTEM -> promptBuilder.append("<|system|>\n${message.content}\n")
                ChatRole.USER -> promptBuilder.append("<|user|>\n${message.content}\n")
                ChatRole.MODEL -> promptBuilder.append("<|model|>\n${message.content}\n")
            }
        }
        
        promptBuilder.append("<|model|>\n")

        val finalContext = promptBuilder.toString()
        println("[BRAIN ENGINE] Executing Inference. Context length: ${finalContext.length}")

        return@withContext try {
            engine.generateResponse(finalContext)
        } catch (e: Exception) {
            "Inference Error: ${e.message}"
        }
    }
}
