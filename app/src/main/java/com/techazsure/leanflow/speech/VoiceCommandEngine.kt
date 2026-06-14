package com.techazsure.leanflow.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.mutableStateOf
import com.techazsure.leanflow.BrainEngine
import com.techazsure.leanflow.SpeechToTextEngine
import com.techazsure.leanflow.ChatHistoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceCommandEngine(
    private val context: Context,
    private val brainEngine: BrainEngine,
    private val sttEngine: SpeechToTextEngine,
    val chatHistoryManager: ChatHistoryManager,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    // Dynamic UI states
    val inputTranscript = mutableStateOf("")
    val aiResponseText = mutableStateOf("Awaiting your input...")
    val isMicActive = mutableStateOf(false)

    // On-device TTS Speaker Engine
    private var textToSpeech: TextToSpeech = TextToSpeech(context, this)
    private var isTtsReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    /**
     * Fix 1 & 2: Triggered when user swipes up. Clears old layout states instantly,
     * processes raw audio stream, runs model inference, then speaks out sequentially.
     */
    fun handleSwipeUpVoiceCommand() {
        if (isMicActive.value) return

        // Clear previous state layout history instantly so old text doesn't show up
        stopSpeaking()
        inputTranscript.value = ""
        aiResponseText.value = "Listening..."
        isMicActive.value = true

        scope.launch(Dispatchers.IO) {
            sttEngine.recordAudioStream(
                onPartialResult = { partialText ->
                    if (isMicActive.value && partialText.isNotEmpty()) {
                        scope.launch(Dispatchers.Main) {
                            inputTranscript.value = partialText
                        }
                    }
                },
                onFinalResult = { finalSpeechText ->
                    scope.launch(Dispatchers.Main) {
                        isMicActive.value = false
                        if (finalSpeechText.trim().isNotEmpty()) {
                            inputTranscript.value = finalSpeechText
                            executeBrainInference(finalSpeechText)
                        } else {
                            aiResponseText.value = "No speech detected. Please swipe up and try again."
                        }
                    }
                }
            )
        }
    }

    /**
     * Handles keyboard typing inputs cleanly, wipes previous response, and fires inference.
     */
    fun handleTypedPrompt(customPrompt: String) {
        if (customPrompt.trim().isEmpty()) return

        stopSpeaking()
        inputTranscript.value = customPrompt
        aiResponseText.value = "Thinking..."

        scope.launch(Dispatchers.Main) {
            executeBrainInference(customPrompt)
        }
    }

    /**
     * Sequentially handles the on-device AI generation loop, updates UI text,
     * saves session logs to local storage, and speaks the output aloud.
     */
    private suspend fun executeBrainInference(prompt: String) {
        // 1. Fetch response from offline model engine
        val response = brainEngine.generateMentorResponse(prompt)

        // 2. Push text to screen layout before speaking out loud
        aiResponseText.value = response

        // 3. Commit session into chatHistoryManager automatically
        try {
            chatHistoryManager.addChatEntry(prompt, response)
            println("[SUCCESS] Session committed to Chat History database.")
        } catch (e: Exception) {
            println("[ERROR] Failed to store conversation: ${e.message}")
        }

        // 4. Sequential TTS execution: Only speaks after text is fully visible on screen
        speakOut(response)
    }

    private fun speakOut(text: String) {
        if (isTtsReady && text.isNotEmpty()) {
            // Strips markdown symbols internally so the engine doesn't stutter on syntax characters
            val cleanSpokenText = text.replace("**", "").replace("*", "").replace("#", "")
            textToSpeech.speak(cleanSpokenText, TextToSpeech.QUEUE_FLUSH, null, "LeanFlow_TTS_Callback")
        }
    }

    fun stopSpeaking() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
    }

    fun cancelActiveVoiceStream() {
        isMicActive.value = false
        stopSpeaking()
        inputTranscript.value = ""
        aiResponseText.value = "Interaction canceled."
    }
}