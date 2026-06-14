package com.techazsure.leanflow

// This is the ONE definition for your entire app
data class ChatMessage(
    val sender: String, // "USER", "SYSTEM", or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)