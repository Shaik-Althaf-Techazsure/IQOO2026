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
import com.techazsure.leanflow.ui.LearnflowlyScreen

class MainActivity : ComponentActivity() {

    private lateinit var aiEngine: LearnFlowEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        aiEngine = LearnFlowEngine(applicationContext)

        lifecycleScope.launch {
            val sampleSubject = "EEE_Induction_Motors"
            val sampleLecture = "A three-phase induction motor runs at synchronous speed Ns = 120f / P."

            println("[TEST RUN] Triggering baseline mobile local inference check...")

            try {
                val resultJson = aiEngine.synthesizeActiveRecall(sampleSubject, sampleLecture)
                println("[TEST RESULT MATRIX] $resultJson")
            } catch (e: Exception) {
                println("[TEST ERROR] ${e.message}")
            }
        }

        setContent {
            LeanFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LearnflowlyScreen()
                }
            }
        }
    }
}
