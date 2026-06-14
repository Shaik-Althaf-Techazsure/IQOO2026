package com.techazsure.leanflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.techazsure.leanflow.LearnFlowEngine
import com.techazsure.leanflow.ChatMessage // IMPORT THE SHARED MODEL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearnFlowViewModel(private val aiEngine: LearnFlowEngine) : ViewModel() {

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _engineStatus = MutableStateFlow<String?>(null)
    val engineStatus: StateFlow<String?> = _engineStatus.asStateFlow()

    init {
        // Inject the initial system persona using the shared ChatMessage model
        _chatHistory.value = listOf(
            ChatMessage(
                sender = "SYSTEM",
                text = "You are Learnflowly, a highly advanced, concise, and helpful AI educational assistant running locally on a mobile device."
            )
        )
    }

    fun setEngineStatus(message: String) {
        _engineStatus.value = message
    }

    fun submitPrompt(userText: String) {
        if (userText.isBlank() || _isProcessing.value) return

        viewModelScope.launch {
            _isProcessing.value = true

            // 1. Append the User's message
            val userMessage = ChatMessage(sender = "USER", text = userText)
            _chatHistory.value = _chatHistory.value + userMessage

            // 2. Query the Engine
            try {
                // queryWithContext expects List<ChatMessage>
                val responseText = aiEngine.queryWithContext(_chatHistory.value)

                // 3. Append the AI's response
                val aiMessage = ChatMessage(sender = "AI", text = responseText)
                _chatHistory.value = _chatHistory.value + aiMessage
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    sender = "AI",
                    text = "Inference failed: ${e.message}",
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    class Factory(private val aiEngine: LearnFlowEngine) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LearnFlowViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LearnFlowViewModel(aiEngine) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}