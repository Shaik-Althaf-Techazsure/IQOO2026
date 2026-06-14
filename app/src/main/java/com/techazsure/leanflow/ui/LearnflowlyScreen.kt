package com.techazsure.leanflow.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.techazsure.leanflow.*
import com.techazsure.leanflow.speech.VoiceCommandEngine
import com.techazsure.leanflow.visual.PhotoParser
import com.techazsure.leanflow.visual.PhotoUploadParser
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// --- Mapped Theme Color Palettes ---
val SurfaceColor = Color(0xFFF8F9FF)
val PrimaryColor = Color(0xFF4648D4)
val SecondaryContainer = Color(0xFFDAE2FD)
val PrimaryContainer = Color(0xFF6063EE)
val OnSurface = Color(0xFF0B1C30)
val OnSurfaceVariant = Color(0xFF464554)

enum class InteractionState { IDLE, TEXT, VOICE, VIDEO, LIVE_CHAT }
enum class ChatRole { USER, ASSISTANT }

@Composable
fun LearnflowlyScreen(
    cameraEngine: CameraFlowEngine? = null,
    voiceCommandEngine: VoiceCommandEngine? = null,
    photoParser: PhotoParser? = null,
    photoUploadParser: PhotoUploadParser? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var interactionState by rememberSaveable { mutableStateOf(InteractionState.IDLE) }

    // 🔥 FIXED: Safe null-chaining and explicit type definition
    val activeThread = voiceCommandEngine?.chatHistoryManager?.currentActiveThread?.value
    val messageList: List<ChatMessage> = activeThread?.messages?.map {
        ChatMessage(if (it.sender == "USER") ChatRole.USER else ChatRole.ASSISTANT, it.text)
    } ?: emptyList()

    val listState = rememberLazyListState()

    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) listState.animateScrollToItem(messageList.size - 1)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Chat History Threads", modifier = Modifier.padding(16.dp))
                voiceCommandEngine?.chatHistoryManager?.let { ChatHistorySidebar(it) }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(SurfaceColor)) {
            CentralGlowOrb(interactionState)

<<<<<<< HEAD
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(top = 80.dp, bottom = 140.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messageList) { ChatBubble(it) }
=======
        // SAFE ZONE WRAPPER: Handles System Bars AND Landscape Camera Notches
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .displayCutoutPadding()
        ) {
            // RESPONSIVE CONTENT CONTAINER: The "Landscape Constraint"
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 800.dp)
                    .align(Alignment.Center)
            ) {
                // 2. The Memory Stream (Chat History)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 80.dp,
                        bottom = 160.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Filter out the SYSTEM prompt so the user doesn't see it
                    items(chatHistory.filter { it.role != ChatRole.SYSTEM }, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }
                    
                    if (isProcessing) {
                        item {
                            Text(
                                text = "Learnflowly is thinking...", 
                                color = OnSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                // 3. Top App Bar
                TopAppBar(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                // 4. Center Guidance Text
                AnimatedVisibility(
                    visible = interactionState == InteractionState.IDLE && chatHistory.filter { it.role != ChatRole.SYSTEM }.isEmpty(),
                    enter = fadeIn(tween(500)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TAP OR SWIPE TO INTERACT",
                            color = OnSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        engineStatus?.let {
                            Text(
                                text = it,
                                color = Color.Red.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 5. Bottom Interaction Zone
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    BottomInteractionZone(
                        state = interactionState,
                        onStateChange = { interactionState = it },
                        onSubmitPrompt = { userText ->
                            viewModel?.submitPrompt(userText)
                        }
                    )
                }
>>>>>>> 61197a2021ccce920eda9a56be18d2b9177b6761
            }

<<<<<<< HEAD
            BottomInteractionZone(
                state = interactionState,
                onStateChange = { interactionState = it },
                onSubmitPrompt = { voiceCommandEngine?.handleTypedPrompt(it) }
=======
        // 6. Video Mode Overlay (Swipe Right)
        if (interactionState == InteractionState.VIDEO) {
            VideoOverlay(
                onClose = { interactionState = InteractionState.IDLE },
                onStartLiveStream = {
                    interactionState = InteractionState.LIVE_CHAT
                }
>>>>>>> 61197a2021ccce920eda9a56be18d2b9177b6761
            )

            if (interactionState == InteractionState.VIDEO) {
                VideoOverlay(photoParser, photoUploadParser, cameraEngine, voiceCommandEngine, { interactionState = InteractionState.IDLE }, { interactionState = InteractionState.LIVE_CHAT })
            }

            if (interactionState == InteractionState.LIVE_CHAT) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx -> PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } },
                        modifier = Modifier.fillMaxSize(),
                        update = { cameraEngine?.startCameraPreview(lifecycleOwner, it) }
                    )
                    IconButton(onClick = { interactionState = InteractionState.IDLE }, modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) PrimaryColor else SecondaryContainer
    val textColor = if (isUser) Color.White else OnSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ))
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ChatHistorySidebar(chatHistoryManager: ChatHistoryManager) {
    val threads = chatHistoryManager.allThreads

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(threads) { thread ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SecondaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { chatHistoryManager.selectThread(thread.id) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${thread.messages.size} points discussed",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CentralGlowOrb(state: InteractionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "orb_scale"
    )

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
<<<<<<< HEAD
fun TopAppBar(onMenuClick: () -> Unit = {}) {
=======
fun TopAppBar(modifier: Modifier = Modifier) {
>>>>>>> 61197a2021ccce920eda9a56be18d2b9177b6761
    Row(
        modifier = modifier
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
                contentDescription = "Menu Sidebar Trigger",
                tint = PrimaryColor,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onMenuClick() }
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

    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, animationSpec = spring(), label = "offset_x")
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, animationSpec = spring(), label = "offset_y")

    val pulseTransition = rememberInfiniteTransition(label = "button_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 48.dp),
        contentAlignment = Alignment.BottomCenter
    ) {

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

        AnimatedVisibility(
            visible = state == InteractionState.TEXT,
            enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 50 }) + fadeOut(),
            modifier = Modifier.padding(bottom = 100.dp)
        ) {
            TextInputBar(
                onClose = { onStateChange(InteractionState.IDLE) },
                onSubmit = { textInput ->
                    onSubmitPrompt(textInput)
                    onStateChange(InteractionState.IDLE)
                }
            )
        }

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
                            if (-offsetY > 150f && abs(offsetY) > abs(offsetX)) {
                                onStateChange(InteractionState.VOICE)
                            } else if (offsetX > 150f && abs(offsetX) > abs(offsetY)) {
                                onStateChange(InteractionState.VIDEO)
                            } else if (offsetX < -150f && abs(offsetX) > abs(offsetY)) {
                                onStateChange(InteractionState.TESTING_HUB)
                            }
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(pulseScale)
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

        AnimatedVisibility(visible = text.isNotBlank()) {
            IconButton(
                onClick = {
                    onSubmit(text)
                    text = ""
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Entry Packet",
                    tint = PrimaryColor
                )
            }
        }

        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close Bar", tint = OnSurfaceVariant)
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
fun VideoOverlay(
    photoParser: PhotoParser?,
    photoUploadParser: PhotoUploadParser?,
    cameraEngine: CameraFlowEngine?,
    voiceCommandEngine: VoiceCommandEngine?,
    onClose: () -> Unit,
    onStartLiveStream: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val localContext = LocalContext.current

    // 🔥 Native Picker Launcher: Upload Photo
    val imageUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                voiceCommandEngine?.aiResponseText?.value = "Extracting upload context matrix..."
                val response = photoUploadParser?.parseUploadedPhoto(
                    imageUri = selectedUri,
                    userPrompt = "Deconstruct formulas and abstract concepts inside this asset text layout."
                ) ?: "Offline Photo Upload Parser unavailable."

                // 🔥 FIXED (Errors 1 & 2): Linked to 'saveMessageToCurrentThread'
                voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("USER", "Uploaded a Photo Asset")
                voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("AI", response)
                onClose()
            }
        }
    }

    // 🔥 Native Picker Launcher: Upload Video
    val videoUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { _ ->
            // 🔥 FIXED (Errors 3 & 4): Linked to 'saveMessageToCurrentThread'
            voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("USER", "Uploaded a Local Video Sequence")
            voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("AI", "Video context ingestion initialized.")
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Visual Integration Hub",
                tint = PrimaryContainer,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "VISUAL CONTEXT",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Provide visual data to Learnflowly",
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MediaCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Videocam,
                    title = "Live Stream",
                    onClick = { onStartLiveStream() }
                )
                MediaCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Image,
                    title = "Scan Text",
                    onClick = {
                        // 🔥 FIXED: Call the existing onStartLiveStream function pointer to lock the camera open
                        onStartLiveStream()

                        cameraEngine?.takeMentorSnapshot(
                            onPhotoSaved = { file ->
                                coroutineScope.launch {
                                    val result = photoParser?.parseImageAndSummarize(file, "Extract text elements and summarize explicitly.")
                                        ?: "Parsing engine connection broken."
                                    voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("USER", "Scanned Text Page")
                                    voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("AI", result)
                                    onClose() // 🔥 FIXED: Returns layout back to IDLE home state cleanly
                                }
                            },
                            onError = { println("[ERROR] Lens Capture Failed") }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MediaCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CameraAlt,
                    title = "Scan Image",
                    onClick = {
                        // 🔥 FIXED: Call onStartLiveStream to open your active viewport layout container
                        onStartLiveStream()

                        cameraEngine?.takeMentorSnapshot(
                            onPhotoSaved = { file ->
                                coroutineScope.launch {
                                    val result = photoParser?.parseImageAndSummarize(file, "Deconstruct the visual engineering schematics inside this layout.")
                                        ?: "Parsing engine connection broken."
                                    voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("USER", "Captured Hardware Matrix Image")
                                    voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("AI", result)
                                    onClose() // 🔥 FIXED: Returns back home smoothly
                                }
                            },
                            onError = { println("[ERROR] Lens Frame Extraction Aborted") }
                        )
                    }
                )
                MediaCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.VideoFile,
                    title = "Upload\nVideo",
                    onClick = { videoUploadLauncher.launch("video/*") }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Text("Close", color = Color.White)
            }
        }
    }
}

@Composable
fun MediaCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

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
    val infiniteTransition = rememberInfiniteTransition(label = "swarm_timer")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_float"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "swarm_rotation"
    )

    val particles = remember {
        List(100) {
            SwarmParticle(
                orbitRadius = Random.nextFloat() * 40f + 5f,
                speed = (Random.nextFloat() * 0.08f) + 0.02f,
                angleOffset = Random.nextFloat() * (2 * Math.PI.toFloat()),
                wobbleSpeed = Random.nextFloat() * 0.2f,
                wobbleAmplitude = Random.nextFloat() * 10f,
                size = Random.nextFloat() * 2.5f + 1f
            )
        }
    }

    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        val center = Offset(size.width / 2, size.height / 2)

        particles.forEach { p ->
            val currentAngle = p.angleOffset + (time * p.speed)
            val currentRadius = p.orbitRadius + (sin(time * p.wobbleSpeed) * p.wobbleAmplitude)
            val x = center.x + cos(currentAngle) * currentRadius
            val y = center.y + sin(currentAngle) * currentRadius

            drawCircle(
                color = particleColor.copy(alpha = 0.8f),
                radius = p.size,
                center = Offset(x, y)
            )
        }
    }
}