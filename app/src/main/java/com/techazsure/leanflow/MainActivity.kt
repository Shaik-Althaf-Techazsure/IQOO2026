package com.techazsure.leanflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.techazsure.leanflow.ui.theme.LeanFlowTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var speechEngine: SpeechToTextEngine
    private lateinit var cameraEngine: CameraFlowEngine
    private lateinit var brainEngine: BrainEngine

    private var isSpeechEngineInitialized = false
    private val liveSpeechState = mutableStateOf("Waiting for voice input...")
    private val aiResponseState = mutableStateOf("Waiting for local AI engine initialization...")
    private val isCameraPermissionGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize the Local Brain Engine First
        brainEngine = BrainEngine(this) { brainReady, statusMessage ->
            aiResponseState.value = statusMessage
            if (brainReady) {
                initializeSpeechEngine()
            }
        }

        cameraEngine = CameraFlowEngine(this)

        // 2. Asynchronous Permissions Registration Array
        val requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
            val camGranted = permissions[Manifest.permission.CAMERA] ?: false

            if (camGranted) {
                isCameraPermissionGranted.value = true
            } else {
                liveSpeechState.value = "Camera hardware permission required!"
            }

            if (micGranted && checkMicPermission()) {
                startVoiceRecording()
            }
        }

        requestPermissionsLauncher.launch(
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        )

        setContent {
            LeanFlowTheme {
                Column(modifier = Modifier.fillMaxSize()) {

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
                            Text("Awaiting hardware camera streaming links...")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Status: LeanFlow Multi-Engine Mesh Active!", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))

                        Text(text = "🎤 User Input Text:", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(text = liveSpeechState.value, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

                        Text(text = "🧠 Local AI Mentor Evaluation:", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(text = aiResponseState.value, fontSize = 15.sp, modifier = Modifier.padding(bottom = 16.dp))

                        Button(
                            onClick = {
                                val currentStatus = aiResponseState.value
                                if (currentStatus == "Local AI Brain Engine Connected and Ready!") {
                                    aiResponseState.value = "Capturing frame and executing on-device local inference..."
                                    cameraEngine.takeMentorSnapshot(
                                        onPhotoSaved = { file ->
                                            lifecycleScope.launch {
                                                val currentPromptText = liveSpeechState.value
                                                val combinedQuery = "Analyzing document context [Saved as: ${file.name}]. Concept prompt: $currentPromptText"

                                                val aiResult = brainEngine.generateMentorResponse(combinedQuery)
                                                aiResponseState.value = aiResult
                                            }
                                        },
                                        onError = { error ->
                                            aiResponseState.value = "Camera execution failure: ${error.message}"
                                        }
                                    )
                                } else {
                                    liveSpeechState.value = "Cannot capture: Fix engine initialization first."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ANALYZE SCREEN & AUDIO CONTEXT")
                        }
                    }
                }
            }
        }
    }

    private fun initializeSpeechEngine() {
        speechEngine = SpeechToTextEngine(this) { ready ->
            if (ready) {
                isSpeechEngineInitialized = true
                liveSpeechState.value = "Voice Engine Operational. Begin speaking concept..."
                startVoiceRecording()
            } else {
                liveSpeechState.value = "Offline asset file read dropped."
            }
        }
    }

    private fun checkMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun startVoiceRecording() {
        // DEFENSIVE CHECK: Safe guard check to avoid UninitializedPropertyAccessException crashes
        if (!isSpeechEngineInitialized) return

        lifecycleScope.launch {
            speechEngine.recordAudioStream(
                onPartialResult = { partialText ->
                    liveSpeechState.value = "Streaming: $partialText"
                },
                onFinalResult = { finalSpeechText ->
                    liveSpeechState.value = finalSpeechText

                    if (aiResponseState.value == "Local AI Brain Engine Connected and Ready!") {
                        aiResponseState.value = "Running local execution graph inference... Please wait."
                        lifecycleScope.launch {
                            val aiResult = brainEngine.generateMentorResponse(finalSpeechText)
                            aiResponseState.value = aiResult
                        }
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraEngine.shutdownExecutor()
    }
}