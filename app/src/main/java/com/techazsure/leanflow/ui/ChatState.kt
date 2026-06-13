package com.techazsure.leanflow.ui

import java.util.UUID

enum class ChatRole {
    SYSTEM, // Core instructions for the Brain Engine
    USER,   // The human
    MODEL   // Learnflowly (The AI)
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val isError: Boolean = false
)
