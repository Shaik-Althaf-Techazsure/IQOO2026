package com.techazsure.leanflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.techazsure.leanflow.ui.theme.LeanFlowTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechEngine: SpeechToTextEngine
    private lateinit var cameraEngine: CameraFlowEngine
    private lateinit var brainEngine: BrainEngine
    private var ttsEngine: TextToSpeech? = null

    private var isSpeechEngineInitialized = false
    private val liveSpeechState = mutableStateOf("Press 'START LISTENING' to capture context...")
    private val aiResponseState = mutableStateOf("Waiting for local AI engine initialization...")
    private val isCameraPermissionGranted = mutableStateOf(false)

    // CONTROL SWITCH: Tracks whether the microphone loop is actively processing speech parameters
    private val isCurrentlyListening = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Android's Native Offline Text To Speech Engine
        ttsEngine = TextToSpeech(this, this)

        // 2. Initialize the Local Brain Engine First
        brainEngine = BrainEngine(this) { brainReady, statusMessage ->
            aiResponseState.value = statusMessage
            if (brainReady) {
                speechEngine = SpeechToTextEngine(this) { ready ->
                    if (ready) {
                        isSpeechEngineInitialized = true
                        liveSpeechState.value = "System Standby. Press button below to record query."
                    } else {
                        liveSpeechState.value = "Offline speech asset verification dropped."
                    }
                }
            }
        }

        cameraEngine = CameraFlowEngine(this)

        // 3. Permissions Registration Array
        val requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
            val camGranted = permissions[Manifest.permission.CAMERA] ?: false
            if (camGranted) isCameraPermissionGranted.value = true
        }

        requestPermissionsLauncher.launch(
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        )

        setContent {
            LeanFlowTheme {
                Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {

                    // Live Camera Frame Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCameraPermissionGranted.value) {
                            val lifecycleOwner = LocalLifecycleOwner.current
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        cameraEngine.startCameraPreview(lifecycleOwner, this)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Awaiting hardware camera permissions...", color = Color.White)
                        }
                    }

                    // Agentic Console UI Output Container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Status: LearnFlow Engine Active", color = Color.Gray, fontSize = 12.sp)

                        Text(text = "🎤 Voice Transcription:", color = Color.Cyan, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        Text(text = liveSpeechState.value, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

                        Text(text = "🧠 Socratic AI Response & Study Planner:", color = Color.Green, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(text = aiResponseState.value, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))

                        // BUTTON 1: THE VOICE TRIGGER INTERRUPT SWITCH
                        Button(
                            onClick = {
                                if (!isSpeechEngineInitialized) return@Button

                                if (!isCurrentlyListening.value) {
                                    // Start listening safely
                                    isCurrentlyListening.value = true
                                    liveSpeechState.value = "Listening closely... Speak your doubt clearly now."
                                    startVoiceRecordingLoop()
                                } else {
                                    // Stop listening and lock the captured phrase context string
                                    isCurrentlyListening.value = false
                                    // FIX: Removed the unresolved stopAudioStream() call entirely
                                    liveSpeechState.value = "Voice sample locked: " + liveSpeechState.value.replace("Streaming: ", "")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrentlyListening.value) Color.Red else Color(0xFF007AFF)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(if (isCurrentlyListening.value) "🛑 LOCK VOICE INFERENCE INPUT" else "🎙️ START LISTENING")
                        }

                        // BUTTON 2: THE MULTI-MODAL REASONING TRIGGER
                        Button(
                            onClick = {
                                val currentStatus = aiResponseState.value
                                if (currentStatus == "Local AI Brain Engine Connected and Ready!") {
                                    aiResponseState.value = "Analyzing video/photo stream and compiling agent schedule..."
                                    cameraEngine.takeMentorSnapshot(
                                        onPhotoSaved = { file ->
                                            lifecycleScope.launch {
                                                val finalCapturedSpeechPrompt = liveSpeechState.value
                                                val agentStructuredQuery = """
                                                    User Query context string: $finalCapturedSpeechPrompt. 
                                                    Analyze the visual material schema metadata labeled [${file.name}]. 
                                                    Execute multi-sensory cross-referencing and output a comprehensive structured study outline now.
                                                """.trimIndent()

                                                val aiResult = brainEngine.generateMentorResponse(agentStructuredQuery)
                                                aiResponseState.value = aiResult
                                                speakOut(aiResult)
                                            }
                                        },
                                        onError = { error ->
                                            aiResponseState.value = "Camera frame extraction drop: ${error.message}"
                                        }
                                    )
                                }
                            },
                            enabled = !isCurrentlyListening.value,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ANALYZE CAMERA + AUDIO CONTEXT")
                        }
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsEngine?.language = Locale.US
        }
    }

    private fun speakOut(text: String) {
        ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startVoiceRecordingLoop() {
        lifecycleScope.launch {
            speechEngine.recordAudioStream(
                onPartialResult = { partialText ->
                    if (isCurrentlyListening.value) {
                        liveSpeechState.value = "Streaming: $partialText"
                    }
                },
                onFinalResult = { finalSpeechText ->
                    if (finalSpeechText.trim().isNotEmpty()) {
                        liveSpeechState.value = finalSpeechText
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraEngine.shutdownExecutor()
        ttsEngine?.stop()
        ttsEngine?.shutdown()
    }
}