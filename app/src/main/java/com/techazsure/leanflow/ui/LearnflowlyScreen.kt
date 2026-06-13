package com.techazsure.leanflow.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import com.techazsure.leanflow.CameraFlowEngine
import com.techazsure.leanflow.LearnFlowEngine
import com.techazsure.leanflow.BrainEngine
import com.techazsure.leanflow.SpeechToTextEngine
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// --- Theme Colors from DESIGN.md ---
val SurfaceColor = Color(0xFFF8F9FF)
val PrimaryColor = Color(0xFF4648D4)
val SecondaryContainer = Color(0xFFDAE2FD)
val PrimaryContainer = Color(0xFF6063EE)
val OnSurface = Color(0xFF0B1C30)
val OnSurfaceVariant = Color(0xFF464554)

enum class InteractionState { IDLE, TEXT, VOICE, VIDEO }

@Preview(showBackground = true)
@Composable
fun LearnflowlyScreenPreview() {
    LearnflowlyScreen()
}

@Composable
fun LearnflowlyScreen(
    cameraEngine: CameraFlowEngine? = null,
    aiEngine: LearnFlowEngine? = null,
    brainEngine: BrainEngine? = null,
    sttEngine: SpeechToTextEngine? = null,
) {
    var interactionState by remember { mutableStateOf(InteractionState.IDLE) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var transcription by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(value = false) }
    val coroutineScope = rememberCoroutineScope()

    // Handle Voice Mode Transcription and Brain Engine Processing
    LaunchedEffect(interactionState) {
        if (interactionState == InteractionState.VOICE) {
            transcription = "Listening..."
            sttEngine?.recordAudioStream(
                onPartialResult = { partial ->
                    transcription = partial
                }
            ) { final ->
                transcription = final
                coroutineScope.launch {
                    isThinking = true
                    val response = brainEngine?.generateMentorResponse(final)
                    aiResponse = response
                    isThinking = false
                    interactionState = InteractionState.IDLE
                }
            }
        } else {
            sttEngine?.stopListening()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceColor)
    ) {
        // 1. Ambient Background & Central Orb
        CentralGlowOrb(interactionState)

        // 2. Top App Bar
        TopAppBar()

        // 3. Center Guidance Text or Transcription
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (interactionState == InteractionState.IDLE) {
                Text(
                    text = "TAP OR SWIPE TO INTERACT",
                    color = OnSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            } else if ((interactionState == InteractionState.VOICE) || isThinking) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isThinking) "THINKING..." else transcription.uppercase(),
                        color = PrimaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }

        // 4. Bottom Interaction Zone
        BottomInteractionZone(
            state = interactionState,
            onStateChange = { interactionState = it },
            onSubmitPrompt = { userText ->
                coroutineScope.launch {
                    isThinking = true
                    println("Sending to Brain Engine: $userText")
                    val response = brainEngine?.generateMentorResponse(userText)
                    aiResponse = response
                    isThinking = false
                    println("Brain Engine Response: $response")
                }
            }
        )

        // 5. Video Mode Overlay (Swipe Right)
        if (interactionState == InteractionState.VIDEO) {
            VideoOverlay(
                cameraEngine = cameraEngine,
                onClose = { interactionState = InteractionState.IDLE }
            )
        }

        // Display AI Response if any
        aiResponse?.let { response ->
            AlertDialog(
                onDismissRequest = { aiResponse = null },
                confirmButton = {
                    TextButton(onClick = { aiResponse = null }) {
                        Text("OK")
                    }
                },
                title = { Text("AI Analysis") },
                text = { Text(response) }
            )
        }
    }
}

@Composable
fun CentralGlowOrb(state: InteractionState) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "orb_scale"
    )

    // Increase scale slightly if in voice mode
    val activeScale by animateFloatAsState(
        targetValue = if (state == InteractionState.VOICE) 1.25f else 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "active_scale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(400.dp)
                .scale(scale * activeScale)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryContainer.copy(alpha = 0.6f),
                            PrimaryColor.copy(alpha = 0.4f),
                            SecondaryContainer.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun TopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .statusBarsPadding()
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = PrimaryColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "LEARNFLOWLY",
                color = OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
        
        // Profile Placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, PrimaryColor.copy(alpha = 0.2f), CircleShape)
                .background(PrimaryColor.copy(alpha = 0.1f))
        )
    }
}

@Composable
fun BottomInteractionZone(
    state: InteractionState,
    onStateChange: (InteractionState) -> Unit,
    onSubmitPrompt: (String) -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Smooth return to center
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, animationSpec = spring(), label = "offset_x")
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, animationSpec = spring(), label = "offset_y")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 48.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        
        // Voice Mode Indicators (Swipe Up)
        AnimatedVisibility(
            visible = state == InteractionState.VOICE,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.padding(bottom = 120.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    VoiceDot(delayMillis = index * 150)
                }
            }
        }

        // Text Mode Input (Tap)
        AnimatedVisibility(
            visible = state == InteractionState.TEXT,
            enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 50 }) + fadeOut(),
            modifier = Modifier.padding(bottom = 100.dp)
        ) {
            TextInputBar(
                onClose = { onStateChange(InteractionState.IDLE) },
                onSubmit = onSubmitPrompt
            )
        }

        // Super Circular Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                .size(72.dp)
                .scale(if (state == InteractionState.TEXT) 0.7f else 1f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.65f))
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (state == InteractionState.IDLE) {
                                onStateChange(InteractionState.TEXT)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Logic mapping to prompt specifications:
                            // Swipe Up (Negative Y) -> Voice
                            // Swipe Right (Positive X) -> Video
                            if (-offsetY > 150f && abs(offsetY) > abs(offsetX)) {
                                onStateChange(InteractionState.VOICE)
                                // Auto-reset voice after a few seconds for demo purposes
                                coroutineScope.launch {
                                    delay(4000.milliseconds)
                                    onStateChange(InteractionState.IDLE)
                                }
                            } else if (offsetX > 150f && abs(offsetX) > abs(offsetY)) {
                                onStateChange(InteractionState.VIDEO)
                            }
                            
                            // Snap back to center
                            offsetX = 0f
                            offsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (state == InteractionState.IDLE) {
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                    )
                }
        ) {
            // Inner Core of the Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor.copy(alpha = 0.1f))
                    .border(1.dp, PrimaryColor.copy(alpha = 0.3f), CircleShape)
            ) {
                ParticleSwarm(
                    modifier = Modifier.fillMaxSize(),
                    particleColor = PrimaryColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputBar(
    onClose: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White, RoundedCornerShape(24.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Type your thought...", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = PrimaryColor
            ),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        
        // Dynamically show the Send/Upload button only when there is text
        AnimatedVisibility(visible = text.isNotBlank()) {
            IconButton(
                onClick = { 
                    onSubmit(text)
                    text = "" // Clear the input after sending
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send, 
                    contentDescription = "Send to Brain Engine",
                    tint = PrimaryColor
                )
            }
        }

        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = OnSurfaceVariant)
        }
    }
}

@Composable
fun VoiceDot(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_dot_transition")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing, delayMillis = delayMillis),
            repeatMode = RepeatMode.Reverse
        ), label = "voice_dot_offset"
    )

    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(12.dp)
            .clip(CircleShape)
            .background(PrimaryColor)
    )
}

@Composable
fun VideoOverlay(cameraEngine: CameraFlowEngine?, onClose: () -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(value = false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        launcher.launch(android.Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission && cameraEngine != null) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        cameraEngine.startCameraPreview(lifecycleOwner, this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            if (!hasCameraPermission) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Mode",
                    tint = PrimaryContainer,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "VIDEO MODE",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Camera permission required",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "LEARNFLOW VIDEO FEED",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Text("End Stream", color = Color.White)
            }
        }
    }
}

// Data class to hold the unique physics traits of each particle
private data class SwarmParticle(
    val orbitRadius: Float,
    val speed: Float,
    val angleOffset: Float,
    val wobbleSpeed: Float,
    val wobbleAmplitude: Float,
    val size: Float
)

@Composable
fun ParticleSwarm(modifier: Modifier = Modifier, particleColor: Color) {
    // Drives the continuous time loop for the animation
    val infiniteTransition = rememberInfiniteTransition(label = "swarm_timer")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_float"
    )

    // Generate the particle "DNA" once and remember it
    val particles = remember {
        List(150) {
            SwarmParticle(
                orbitRadius = Random.nextFloat() * 50f + 10f, // Distance from center
                speed = (Random.nextFloat() * 0.04f) + 0.01f,   // Orbital speed
                angleOffset = Random.nextFloat() * (2 * Math.PI.toFloat()), // Starting position
                wobbleSpeed = Random.nextFloat() * 0.1f,      // How fast it moves in/out
                wobbleAmplitude = Random.nextFloat() * 15f,   // How far it moves in/out
                size = Random.nextFloat() * 3f + 1f           // Size of the particle dot
            )
        }
    }

    // High-performance drawing canvas
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)

        particles.forEach { p ->
            // 1. Calculate the current angle on the circle
            val currentAngle = p.angleOffset + (time * p.speed)
            
            // 2. Add organic in/out movement (the "breathing" or "wobbling" effect)
            val currentRadius = p.orbitRadius + (sin(time * p.wobbleSpeed) * p.wobbleAmplitude)

            // 3. Convert polar coordinates (angle/radius) to X/Y screen coordinates
            val x = center.x + cos(currentAngle) * currentRadius
            val y = center.y + sin(currentAngle) * currentRadius

            // 4. Draw the particle
            drawCircle(
                color = particleColor.copy(alpha = 0.8f),
                radius = p.size,
                center = Offset(x, y)
            )
        }
    }
}
