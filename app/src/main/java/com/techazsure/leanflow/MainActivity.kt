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
import com.techazsure.leanflow.ui.LearnFlowViewModel
import com.techazsure.leanflow.ui.LearnflowlyScreen
import com.techazsure.leanflow.ui.theme.LeanFlowTheme

class MainActivity : ComponentActivity() {

    private lateinit var aiEngine: LearnFlowEngine
    private lateinit var cameraEngine: CameraFlowEngine
    private lateinit var sttEngine: SpeechToTextEngine
    
    private lateinit var viewModel: LearnFlowViewModel

    private var isBrainReady by mutableStateOf(false)
    private var isSttReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        aiEngine = LearnFlowEngine(applicationContext) { ready, message ->
            isBrainReady = ready
            println("[MAIN] Brain Engine Status: $ready - $message")
        }

        cameraEngine = CameraFlowEngine(applicationContext)

        sttEngine = SpeechToTextEngine(applicationContext) { ready ->
            isSttReady = ready
            println("[MAIN] STT Engine Status: $ready")
        }
        
        viewModel = LearnFlowViewModel(aiEngine)

        setContent {
            LeanFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LearnflowlyScreen(
                        viewModel = viewModel,
                        cameraEngine = cameraEngine,
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
