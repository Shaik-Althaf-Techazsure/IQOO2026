package com.techazsure.leanflow

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChatHistoryManager(private val context: Context) {

    // Observable list of all historical threads for Akthar's sidebar navigation drawer
    val allThreads = mutableStateListOf<ChatThread>()

    // Tracks the currently active thread session layout
    val currentActiveThread = mutableStateOf<ChatThread?>(null)

    private val historyFile = File(context.filesDir, "chat_history_v2.json")

    init {
        loadHistory()
    }

    /**
     * 🔥 MODIFIED & FIXED: Bridges VoiceCommandEngine directly into the active Thread architecture
     */
    fun addChatEntry(prompt: String, response: String) {
        saveMessageToCurrentThread(sender = "USER", rawText = prompt)
        saveMessageToCurrentThread(sender = "AI", rawText = response)
        saveHistory()
    }

    private fun saveHistory() {
        try {
            val rootArray = JSONArray()
            allThreads.forEach { thread ->
                val threadObj = JSONObject()
                threadObj.put("id", thread.id)
                threadObj.put("title", thread.title)
                
                val messagesArray = JSONArray()
                thread.messages.forEach { msg ->
                    val msgObj = JSONObject()
                    msgObj.put("sender", msg.sender)
                    msgObj.put("text", msg.text)
                    msgObj.put("timestamp", msg.timestamp)
                    messagesArray.put(msgObj)
                }
                threadObj.put("messages", messagesArray)
                rootArray.put(threadObj)
            }
            historyFile.writeText(rootArray.toString())
        } catch (e: Exception) {
            println("[ERROR] Failed to persist history: ${e.message}")
        }
    }

    private fun loadHistory() {
        if (!historyFile.exists()) return
        try {
            val content = historyFile.readText()
            val rootArray = JSONArray(content)
            allThreads.clear()
            for (i in 0 until rootArray.length()) {
                val threadObj = rootArray.getJSONObject(i)
                val thread = ChatThread(
                    id = threadObj.getString("id"),
                    title = threadObj.getString("title"),
                    messages = mutableStateListOf()
                )
                val messagesArray = threadObj.getJSONArray("messages")
                for (j in 0 until messagesArray.length()) {
                    val msgObj = messagesArray.getJSONObject(j)
                    thread.messages.add(ChatMessage(
                        sender = msgObj.getString("sender"),
                        text = msgObj.getString("text"),
                        timestamp = msgObj.getLong("timestamp")
                    ))
                }
                allThreads.add(thread)
            }
            // Resume the most recent thread if available
            if (allThreads.isNotEmpty()) {
                currentActiveThread.value = allThreads.first()
            }
        } catch (e: Exception) {
            println("[ERROR] Failed to load history: ${e.message}")
        }
    }

    /**
     * Initializes a brand new conversational tracking thread
     */
    fun createNewThread(): ChatThread {
        val newThread = ChatThread()
        allThreads.add(0, newThread) // Add to the absolute top of history list stack
        currentActiveThread.value = newThread
        saveHistory()
        return newThread
    }

    /**
     * Switches the app context to continue an older conversation thread
     */
    fun selectThread(threadId: String) {
        val target = allThreads.find { it.id == threadId }
        if (target != null) {
            currentActiveThread.value = target
            // Move to top of the list for "Recent" sorting
            allThreads.remove(target)
            allThreads.add(0, target)
            saveHistory()
        }
    }

    /**
     * Saves a message packet to the active tracking thread and auto-updates titles contextually
     */
    fun saveMessageToCurrentThread(sender: String, rawText: String) {
        val cleanText = rawText.trim()

        var thread = currentActiveThread.value
        if (thread == null) {
            thread = createNewThread()
        }

        thread.messages.add(ChatMessage(sender, cleanText))

        if (thread.title == "New Conversation" && sender == "USER") {
            val words = cleanText.split(" ")
            thread.title = words.take(4).joinToString(" ") + if (words.size > 4) "..." else ""
        }

        saveHistory()
        
        // Refresh UI state
        val current = currentActiveThread.value
        currentActiveThread.value = null
        currentActiveThread.value = current
    }

    fun clearAllHistory() {
        allThreads.clear()
        currentActiveThread.value = null
        if (historyFile.exists()) historyFile.delete()
    }
}
