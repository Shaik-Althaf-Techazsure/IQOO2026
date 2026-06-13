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

class MainActivity : ComponentActivity() {

    // 1. Declare our native on-device AI engine variable
    private lateinit var aiEngine: LearnFlowEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. Initialize the local engine using the application context
        aiEngine = LearnFlowEngine(applicationContext)

        // 3. Launch a background execution pipeline test frame
        lifecycleScope.launch {
            val sampleSubject = "EEE_Induction_Motors"
            val sampleLecture = "A three-phase induction motor runs at synchronous speed Ns = 120f / P."

            println("[TEST RUN] Triggering baseline mobile local inference check...")

            // Execute the processing call cleanly inside the coroutine worker scope
            val resultJson = aiEngine.synthesizeActiveRecall(sampleSubject, sampleLecture)
            println("[TEST RESULT MATRIX] $resultJson")
        }

        setContent {
            LeanFlowTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "LearnFlow Engine Active",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Status: $name!",
        modifier = modifier
    )
}