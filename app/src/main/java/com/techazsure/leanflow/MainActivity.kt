package com.techazsure.leanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

    private val aiEngine by lazy {
        LearnFlowEngine(applicationContext) { ready, message ->
            println("[MAIN] Brain Engine Status: $ready - $message")
            viewModel.setEngineStatus(message)
        }
    }
    
    private val cameraEngine by lazy { CameraFlowEngine(applicationContext) }
    private val sttEngine by lazy {
        SpeechToTextEngine(applicationContext) { ready ->
            println("[MAIN] STT Engine Status: $ready")
        }
    }
    
    private val viewModel: LearnFlowViewModel by viewModels {
        LearnFlowViewModel.Factory(aiEngine)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("[DEBUG_LIFECYCLE] MainActivity onCreate triggered!")
        enableEdgeToEdge()

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
