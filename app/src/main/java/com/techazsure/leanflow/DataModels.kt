package com.techazsure.leanflow

import java.util.UUID

object ChatRole {
    const val USER = "USER"
    const val AI = "AI"
    const val SYSTEM = "SYSTEM"
}

// This is the ONE definition for your entire app
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER", "SYSTEM", or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Compatibility properties for different code styles in the project
    val role: String get() = sender
    val content: String get() = text
}
