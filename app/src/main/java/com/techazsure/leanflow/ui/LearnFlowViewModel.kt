package com.techazsure.leanflow.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.techazsure.leanflow.ContextExtractor
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

    // Handles standard files, documents, and videos
    fun ingestContextFromUri(context: Context, uri: Uri, fileType: String) {
        viewModelScope.launch {
            // 1. Give the user visual feedback that the file was attached
            val attachmentMessage = ChatMessage(
                sender = "USER",
                text = "[Attached $fileType]"
            )
            _chatHistory.value = _chatHistory.value + attachmentMessage

            // 2. Extract data (This happens in the background)
            try {
                // [AI DEVELOPER NOTE] ContextExtractor must be implemented to handle URI data extraction
                val extractedData = ContextExtractor.extractTextFromUri(context, uri)
                
                // 3. Silently feed this to the Brain Engine as system context
                val contextInjection = ChatMessage(
                    sender = "SYSTEM",
                    text = "Context Update from user's attached file: $extractedData"
                )
                _chatHistory.value = _chatHistory.value + contextInjection
                
            } catch (e: Exception) {
                println("[INGESTION ERROR] Failed to read URI: ${e.message}")
            }
        }
    }

    // Handles immediate Camera captures and OCR
    fun ingestContextFromBitmap(bitmap: Bitmap, isOcrTarget: Boolean = false) {
        viewModelScope.launch {
            val attachmentMessage = ChatMessage(
                sender = "USER",
                text = if (isOcrTarget) "[Scanning Text from Image...]" else "[Captured Photo]"
            )
            _chatHistory.value = _chatHistory.value + attachmentMessage

            if (isOcrTarget) {
                // [AI DEVELOPER NOTE] ContextExtractor must be implemented to handle ML Kit OCR
                ContextExtractor.extractTextWithMLKit(bitmap) { extractedText ->
                    val ocrResult = ChatMessage(
                        sender = "SYSTEM",
                        text = "Extracted Text from Image: $extractedText"
                    )
                    _chatHistory.value = _chatHistory.value + ocrResult
                }
            } else {
                // For multimodal local LLMs, you would pass the raw bitmap here.
                println("[INGESTION] Raw image saved to memory for multimodal processing.")
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