package com.techazsure.leanflow

// This is the ONE definition for your entire app
data class ChatMessage(
    val sender: String, // "USER", "SYSTEM", or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,         // Uri string for image/video thumbnail preview
    val attachmentType: String? = null    // "image", "video", "document", or null
)

/**
 * Structural container tracking grouped message arrays under a localized identification index.
 */
data class ChatThread(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String = "New Conversation",
    val messages: MutableList<ChatMessage> = mutableListOf()
)
