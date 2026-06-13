package com.techazsure.leanflow

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LearnFlowEngine(private val context: Context) {

    private var llmInference: LlmInference? = null

    init {
        // Step 1: Map configuration options to target our offline model file asset folder path
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/assets/gemma.task") // Points directly to your local assets block
            .setMaxTokens(1024)
            .build()

        // Step 2: Instantiate the localized engine straight onto the smartphone processor
        try {
            llmInference = LlmInference.createFromOptions(context, options)
            println("[SUCCESS LOG] Native Mobile AI Task Engine Initialized Successfully.")
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