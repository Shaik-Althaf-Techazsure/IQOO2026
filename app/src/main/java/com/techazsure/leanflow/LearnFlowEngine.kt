package com.techazsure.leanflow

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LearnFlowEngine(private val context: Context) {

    private var llmInference: LlmInference? = null

    init {
        val modelFileName = "gemma.task"
        val modelFile = File(context.filesDir, modelFileName)

        if (!modelFile.exists()) {
            try {
                context.assets.open(modelFileName).use { inputStream ->
                    FileOutputStream(modelFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                println("[SUCCESS LOG] Model copied to internal storage: ${modelFile.absolutePath}")
            } catch (e: Exception) {
                println("[ERROR EXCEPTION] Failed to copy model from assets: ${e.message}")
            }
        }

        // Step 1: Map configuration options to target our offline model file path
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(1024)
            .build()

        // Step 2: Instantiate the localized engine straight onto the smartphone processor
        try {
            if (modelFile.exists()) {
                llmInference = LlmInference.createFromOptions(context, options)
                println("[SUCCESS LOG] Native Mobile AI Task Engine Initialized Successfully.")
            } else {
                println("[ERROR] Model file not found even after copy attempt.")
            }
        } catch (e: Exception) {
            println("[ERROR EXCEPTION] Native compilation failure: ${e.message}")
        }
    }

    /**
     * Executes localized asynchronous background inference tasks on device cores.
     */
    suspend fun synthesizeActiveRecall(subject: String, lectureBody: String): String? = withContext(Dispatchers.IO) {
        // Formulating a hard system instruction framework forcing the local 1B model to drop structured JSON arrays
        val strictSystemPrompt = """
            You are a local academic assistant. Analyze the input text about '$subject'.
            Generate exactly one summary paragraph, one flashcard question and answer, and one multiple choice quiz question.
            Format the entire response as a single, raw JSON block. Do NOT use markdown code blocks or triple backticks.
            
            Format Match: {"subject": "$subject", "summary": "...", "flashcards": [{"question": "...", "answer": "..."}], "quizzes": [{"question": "...", "options": ["A","B","C","D"], "correct_index": 0, "explanation": "..."}]}
            
            Input Material:
            $lectureBody
        """.trimIndent()

        // Execute inference synchronously inside our safe IO thread allocation pool
        return@withContext llmInference?.generateResponse(strictSystemPrompt)
    }
}