package com.techazsure.leanflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.techazsure.leanflow.speech.VoiceCommandEngine
import com.techazsure.leanflow.ui.LearnflowlyScreen
import com.techazsure.leanflow.ui.theme.LeanFlowTheme

class MainActivity : ComponentActivity() {

    private lateinit var cameraEngine: CameraFlowEngine
    private lateinit var brainEngine: BrainEngine
    private lateinit var sttEngine: SpeechToTextEngine
    private lateinit var voiceCommandEngine: VoiceCommandEngine

    private var isBrainReady by mutableStateOf(false)
    private var isSttReady by mutableStateOf(false)

    // Tracks permission status dynamically before launching hardware layers
    private var arePermissionsGranted by mutableStateOf(false)

    // Hardware Request Launcher Registry Array to trigger permission pop-ups safely
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val camGranted = permissions[Manifest.permission.CAMERA] ?: false

        if (micGranted && camGranted) {
            println("[SUCCESS] System permissions approved by user.")
            arePermissionsGranted = true
            initializeHardwareComponents()
        } else {
            println("[WARN] Critical system permissions denied by user.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Explicitly check or request permissions right on app launch
        checkAndRequestSystemPermissions()

        setContent {
            LeanFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (arePermissionsGranted) {
                        // Pass exactly what Akthar's frontend layout signature expects
                        LearnflowlyScreen(
                            cameraEngine = cameraEngine,
                            voiceCommandEngine = voiceCommandEngine
                        )
                    } else {
                        // Graceful loading fallback until permissions are explicitly granted
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            // Awaiting permissions initialization
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestSystemPermissions() {
        val hasCam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (hasCam && hasMic) {
            arePermissionsGranted = true
            initializeHardwareComponents()
        } else {
            // Force prompt the hardware dialog request array windows dynamically
            requestPermissionsLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    private fun initializeHardwareComponents() {
        // Maps the current Activity instance to bypass overlay window canvas rendering bugs
        cameraEngine = CameraFlowEngine(this)

        brainEngine = BrainEngine(this) { ready, message ->
            isBrainReady = ready
            println("[MAIN] Brain Engine Status: $ready - $message")
        }

        sttEngine = SpeechToTextEngine(this) { ready ->
            isSttReady = ready
            println("[MAIN] STT Engine Status: $ready")
        }

        // 🧠 FIXED: Initialized Akthar's history tracking module with zero arguments to match its constructor
        val chatHistoryManager = ChatHistoryManager()

        // 🔥 FIXED: Parameters cleanly isolated and mapped using named arguments
        voiceCommandEngine = VoiceCommandEngine(
            context = this,
            brainEngine = brainEngine,
            sttEngine = sttEngine,
            chatHistoryManager = chatHistoryManager,
            scope = lifecycleScope
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraEngine.isInitialized) {
            cameraEngine.shutdownExecutor()
        }
    }
}