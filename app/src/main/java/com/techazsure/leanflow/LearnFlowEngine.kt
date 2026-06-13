package com.techazsure.leanflow

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.techazsure.leanflow.ui.ChatMessage
import com.techazsure.leanflow.ui.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LearnFlowEngine(private val context: Context, private val onBrainReady: (Boolean, String) -> Unit) {

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

                llmInference = LlmInference.createFromOptions(context, options)
                println("[SUCCESS] LearnFlow Brain: Engine graph initialized safely!")
                onBrainReady(true, "Local AI Brain Engine Connected and Ready!")
            } catch (e: Exception) {
                val detailedError = "Init Failure: ${e.localizedMessage ?: e.message ?: "Native memory allocation crash"}"
                println("[ERROR] MediaPipe Initialization Failed: $detailedError")
                onBrainReady(false, detailedError)
            }
        }
    }

    // [AI DEVELOPER INSTRUCTION: CONTEXT COMPILER]
    // Translates the Kotlin data structures into the exact prompt format required by the local weights.
    // The format below assumes a generic ChatML syntax. Adjust the tags (e.g., <|user|>, <|model|>) 
    // to perfectly match the specific local model you are using (Gemma, Llama, etc.).
    suspend fun queryWithContext(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val engine = llmInference ?: return@withContext "Error: Local AI engine not initialized."

        val promptBuilder = StringBuilder()

        // Compile the history into a single contiguous string
        for (message in history) {
            when (message.role) {
                ChatRole.SYSTEM -> promptBuilder.append("<|system|>\n${message.content}\n")
                ChatRole.USER -> promptBuilder.append("<|user|>\n${message.content}\n")
                ChatRole.MODEL -> promptBuilder.append("<|model|>\n${message.content}\n")
            }
        }
        
        // Append the final token to tell the local model it is time to generate text
        promptBuilder.append("<|model|>\n")

        val finalContext = promptBuilder.toString()
        println("[BRAIN ENGINE] Executing Inference on Context Size: ${finalContext.length} chars")

        return@withContext try {
            engine.generateResponse(finalContext)
        } catch (e: Exception) {
            "Inference Error: ${e.message}"
        }
    }

    @Deprecated("Use queryWithContext for memory support", ReplaceWith("queryWithContext"))
    suspend fun generateMentorResponse(userInput: String): String = withContext(Dispatchers.IO) {
        val engine = llmInference ?: return@withContext "Error: Local AI engine not initialized."

        val structuredPrompt = """
            You are 'LearnFlow', a brilliant multi-disciplinary academic mentor operating entirely offline.
            Your mandate is to guide the user analytically through engineering, mathematics, science, or business concepts.
            
            CRITICAL INSTRUCTIONS:
            1. Analyze the user's input: "$userInput"
            2. Break down any underlying core formulas, laws, or theorems step-by-step.
            3. Do not just print flat answers; provide an explanatory framework first.
            4. Explicitly organize your output evaluation into a clean summary.
        """.trimIndent()

        return@withContext try {
            engine.generateResponse(structuredPrompt)
        } catch (e: Exception) {
            "Inference Error: ${e.message}"
        }
    }
}
