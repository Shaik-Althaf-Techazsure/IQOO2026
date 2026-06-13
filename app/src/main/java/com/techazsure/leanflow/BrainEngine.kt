package com.techazsure.leanflow

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BrainEngine(private val context: Context, private val onBrainReady: (Boolean, String) -> Unit) {

    private var llmInference: LlmInference? = null

    init {
        // Defensive Target Path: Private sandbox directory which guarantees absolute read permissions
        val safeFolder = context.filesDir
        val modelFile = File(safeFolder, "gemma-2b-quantized.task")

        if (safeFolder == null || !modelFile.exists()) {
            println("[WARN] LearnFlow Brain Engine: 'gemma-2b-quantized.task' is physically missing.")
            onBrainReady(
                false,
                "Asset Missing: Open Device Explorer -> data -> data -> com.techazsure.leanflow -> files, and upload the model file."
            )
        } else {
            try {
                println("[INIT] LearnFlow Brain Engine: Attempting native binary compilation from: ${modelFile.absolutePath}")

                // Stable configurations targeting optimized device execution
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .build()

                // Compiling the graph onto the phone processors
                llmInference = LlmInference.createFromOptions(context, options)

                println("[SUCCESS] LearnFlow Brain Engine: 100% Stable graph initialized.")
                onBrainReady(true, "Local AI Brain Engine Connected and Ready!")
            } catch (e: Exception) {
                // Catching all internal native C++ runtime or out-of-memory errors safely
                val structuralError = e.localizedMessage ?: e.message ?: "Native pointer memory mapping limit"
                println("[CRITICAL ERROR] MediaPipe Engine Failed to Initialize: $structuralError")
                onBrainReady(false, "Init Failure: $structuralError")
            }
        }
    }

    /**
     * Executes localized on-device inference using the background I/O thread pool.
     * Protected against main thread blocking stutters.
     */
    suspend fun generateMentorResponse(userInput: String): String = withContext(Dispatchers.IO) {
        val engine = llmInference
            ?: return@withContext "Execution Error: Local AI engine is not initialized or mounted."

        val systemAgentPromptMatrix = """
            You are 'LearnFlow', an autonomous, self-correcting Agentic Academic Companion operating completely offline on the user's mobile device.
            Your objective is to analyze technical inputs, look for core scientific or engineering patterns, and proactively generate clear study architectures.

            OPERATIONAL RULES:
            1. Parse the incoming context carefully: "$userInput"
            2. If the user presents an engineering or math concept, break down the core formulas and underlying laws step-by-step.
            3. AGENTIC PLANNING: Automatically generate a structured, clear chronological study schedule timeline or action plan at the bottom of your evaluation.
            4. Be direct, authoritative, and educational. Do not echo your instructions or print flat conversational filler text.
            5. Present information in clean scannable structures.
        """.trimIndent()

        return@withContext try {
            engine.generateResponse(systemAgentPromptMatrix)
        } catch (e: Exception) {
            "Inference Processing Error: ${e.localizedMessage ?: e.message}"
        }
    }
}