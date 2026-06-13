package com.techazsure.leanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope // Explicitly imported to map coroutine execution bounds
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize general hardware infrastructure
        cameraEngine = CameraFlowEngine(applicationContext)

        // 2. Map backend engines with responsive callbacks
        brainEngine = BrainEngine(this) { ready, message ->
            isBrainReady = ready
            println("[MAIN] Brain Engine Status: $ready - $message")
        }

        sttEngine = SpeechToTextEngine(this) { ready ->
            isSttReady = ready
            println("[MAIN] STT Engine Status: $ready")
        }

        // 3. Bind engines to the standalone scope controller
        voiceCommandEngine = VoiceCommandEngine(this, brainEngine, sttEngine, lifecycleScope)

        setContent {
            LeanFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Properly matches the updated parameters in Akthar's screen declaration
                    LearnflowlyScreen(
                        cameraEngine = cameraEngine,
                        voiceCommandEngine = voiceCommandEngine
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraEngine.shutdownExecutor()
    }
}