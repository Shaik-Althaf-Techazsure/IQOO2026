package com.techazsure.leanflow

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

/**
 * Structural container tracking grouped message arrays under a localized identification index.
 */
data class ChatThread(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String = "New Conversation",
    val messages: MutableList<ChatMessage> = mutableListOf()
)

class ChatHistoryManager {

    // Observable list of all historical threads for Akthar's sidebar navigation drawer
    val allThreads = mutableStateListOf<ChatThread>()

    // Tracks the currently active thread session layout
    val currentActiveThread = mutableStateOf<ChatThread?>(null)

    /**
     * 🔥 MODIFIED & FIXED: Bridges VoiceCommandEngine directly into the active Thread architecture
     */
    fun addChatEntry(prompt: String, response: String) {
        // 1. Log the user's input question to the current running thread loop
        saveMessageToCurrentThread(sender = "USER", rawText = prompt)

        // 2. Log Gemma's local AI response directly behind it under the same session context
        saveMessageToCurrentThread(sender = "AI", rawText = response)

        // Terminal verification logger for presentations
        println("[HISTORY DB SUCCESS] Saved sequence pair cleanly -> Q: '${prompt.take(15)}...' | A: '${response.take(15)}...'")
    }

    /**
     * Initializes a brand new conversational tracking thread
     */
    fun createNewThread(): ChatThread {
        val newThread = ChatThread()
        allThreads.add(0, newThread) // Add to the absolute top of history list stack
        currentActiveThread.value = newThread
        return newThread
    }

    /**
     * Switches the app context to continue an older conversation thread
     */
    fun selectThread(threadId: String) {
        val target = allThreads.find { it.id == threadId }
        if (target != null) {
            currentActiveThread.value = target
        }
    }

    /**
     * Saves a message packet to the active tracking thread and auto-updates titles contextually
     */
    fun saveMessageToCurrentThread(sender: String, rawText: String) {
        // Strip markdown symbols inside database strings to prevent indexing corruption
        val cleanText = rawText.replace("\"", "").trim()

        var thread = currentActiveThread.value
        if (thread == null) {
            thread = createNewThread()
        }

        // Inject the fresh data model packet directly into the thread message tracker list
        thread.messages.add(ChatMessage(sender, cleanText))

        // Auto-generate a clean sidebar title from the first user question if it's still default
        if (thread.title == "New Conversation" && sender == "USER") {
            val words = cleanText.split(" ")
            thread.title = words.take(4).joinToString(" ") + if (words.size > 4) "..." else ""
        }

        // Trigger a shallow structural state refresh for Jetpack Compose layout tracking observers
        val temp = currentActiveThread.value
        currentActiveThread.value = null
        currentActiveThread.value = temp
    }

    fun clearAllHistory() {
        allThreads.clear()
        currentActiveThread.value = null
    }
}