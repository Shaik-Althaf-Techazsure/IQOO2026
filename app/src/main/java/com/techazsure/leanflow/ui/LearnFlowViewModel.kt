package com.techazsure.leanflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techazsure.leanflow.LearnFlowEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [AI DEVELOPER INSTRUCTION: MEMORY MANAGER]
// This ViewModel maintains the conversation state.
// It initializes with a SYSTEM prompt to define Learnflowly's persona.

class LearnFlowViewModel(private val aiEngine: LearnFlowEngine) : ViewModel() {

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _engineStatus = MutableStateFlow<String?>(null)
    val engineStatus: StateFlow<String?> = _engineStatus.asStateFlow()

    init {
        // Inject the initial system persona into the memory
        _chatHistory.value = listOf(
            ChatMessage(
                role = ChatRole.SYSTEM,
                content = "You are Learnflowly, a highly advanced, concise, and helpful AI educational assistant running locally on a mobile device."
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
            
            // 1. Append the User's message to the history
            val userMessage = ChatMessage(role = ChatRole.USER, content = userText)
            _chatHistory.value = _chatHistory.value + userMessage

            // 2. Query the Brain Engine WITH the full history context
            try {
                val responseText = aiEngine.queryWithContext(_chatHistory.value)
                
                // 3. Append the AI's response to the history
                val aiMessage = ChatMessage(role = ChatRole.MODEL, content = responseText)
                _chatHistory.value = _chatHistory.value + aiMessage
            } catch (e: Exception) {
                // Handle local inference failures (e.g., Out of Memory)
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    role = ChatRole.MODEL,
                    content = "Inference failed: ${e.message}",
                    isError = true
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
