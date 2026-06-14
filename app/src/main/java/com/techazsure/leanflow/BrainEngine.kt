package com.techazsure.leanflow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrainEngine(private val learnFlowEngine: LearnFlowEngine) {

    suspend fun generateMentorResponse(userInput: String): String = withContext(Dispatchers.IO) {
        val systemAgentPrompt = """
        You are 'LearnFlow', an autonomous Agentic Academic Companion operating completely offline.
        Be extremely concise and professional. 
        IMPORTANT: Do NOT use markdown symbols like '**' or '###'. 
        Do NOT use bullet points. 
        Use natural, clear paragraphs. Provide a beautiful, flowing response.
    """.trimIndent()

        return@withContext try {
            val history = listOf(
                ChatMessage(sender = "SYSTEM", text = systemAgentPrompt),
                ChatMessage(sender = "USER", text = userInput)
            )
            learnFlowEngine.queryWithContext(history)
        } catch (e: Exception) {
            "Inference Execution Error: ${e.message}"
        }
    }
}