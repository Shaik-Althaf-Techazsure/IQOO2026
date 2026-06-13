package com.techazsure.leanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.techazsure.leanflow.ui.theme.LeanFlowTheme
import kotlinx.coroutines.launch

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.techazsure.leanflow.ui.LearnflowlyScreen

class MainActivity : ComponentActivity() {

    private lateinit var aiEngine: LearnFlowEngine
    private lateinit var cameraEngine: CameraFlowEngine
    private lateinit var brainEngine: BrainEngine
    private lateinit var sttEngine: SpeechToTextEngine

    private var isBrainReady by mutableStateOf(false)
    private var isSttReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        aiEngine = LearnFlowEngine(applicationContext)
        cameraEngine = CameraFlowEngine(applicationContext)
        
        brainEngine = BrainEngine(applicationContext) { ready, message ->
            isBrainReady = ready
            println("[MAIN] Brain Engine Status: $ready - $message")
        }

        sttEngine = SpeechToTextEngine(applicationContext) { ready ->
            isSttReady = ready
            println("[MAIN] STT Engine Status: $ready")
        }

        setContent {
            LeanFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LearnflowlyScreen(
                        cameraEngine = cameraEngine,
                        aiEngine = aiEngine,
                        brainEngine = brainEngine,
                        sttEngine = sttEngine
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
