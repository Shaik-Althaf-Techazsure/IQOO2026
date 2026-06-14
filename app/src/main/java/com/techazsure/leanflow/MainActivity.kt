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
import androidx.lifecycle.ViewModelProvider
import com.techazsure.leanflow.speech.VoiceCommandEngine
import com.techazsure.leanflow.ui.LearnflowlyScreen
import com.techazsure.leanflow.ui.LearnFlowViewModel
import com.techazsure.leanflow.ui.theme.LeanFlowTheme
import com.techazsure.leanflow.visual.PhotoParser
import com.techazsure.leanflow.visual.PhotoUploadParser
import com.techazsure.leanflow.visual.VideoParser

class MainActivity : ComponentActivity() {

    private lateinit var cameraEngine: CameraFlowEngine
    private lateinit var learnFlowEngine: LearnFlowEngine
    private lateinit var brainEngine: BrainEngine
    private lateinit var sttEngine: SpeechToTextEngine
    private lateinit var voiceCommandEngine: VoiceCommandEngine
    private lateinit var photoParser: PhotoParser
    private lateinit var photoUploadParser: PhotoUploadParser
    private lateinit var videoParser: VideoParser
    private lateinit var viewModel: LearnFlowViewModel

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
        println("[DEBUG_LIFECYCLE] MainActivity onCreate triggered!")
        enableEdgeToEdge()

        // Explicitly check or request permissions right on app launch
        checkAndRequestSystemPermissions()

        setContent {
            LeanFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (arePermissionsGranted && ::voiceCommandEngine.isInitialized) {
                        // Pass exactly what Akthar's frontend layout signature expects
                        LearnflowlyScreen(
                            viewModel = viewModel,
                            cameraEngine = cameraEngine,
                            voiceCommandEngine = voiceCommandEngine,
                            photoParser = photoParser,
                            photoUploadParser = photoUploadParser,
                            videoParser = videoParser
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

        learnFlowEngine = LearnFlowEngine(this) { ready, message ->
            isBrainReady = ready
            println("[MAIN] LearnFlow Engine Status: $ready - $message")
        }

        brainEngine = BrainEngine(learnFlowEngine)

        sttEngine = SpeechToTextEngine(this) { ready ->
            isSttReady = ready
            println("[MAIN] STT Engine Status: $ready")
        }

        photoParser = PhotoParser(this, brainEngine)
        photoUploadParser = PhotoUploadParser(this, brainEngine)
        videoParser = VideoParser(this, brainEngine)

        // 🧠 FIXED: Initialized Akthar's history tracking module with zero arguments to match its constructor
        val chatHistoryManager = ChatHistoryManager(this)

        // 🔥 FIXED: Parameters cleanly isolated and mapped using named arguments
        voiceCommandEngine = VoiceCommandEngine(
            context = this,
            brainEngine = brainEngine,
            sttEngine = sttEngine,
            chatHistoryManager = chatHistoryManager,
            scope = lifecycleScope
        )

        val factory = LearnFlowViewModel.Factory(learnFlowEngine)
        viewModel = ViewModelProvider(this, factory)[LearnFlowViewModel::class.java]
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraEngine.isInitialized) {
            cameraEngine.shutdownExecutor()
        }
    }
}