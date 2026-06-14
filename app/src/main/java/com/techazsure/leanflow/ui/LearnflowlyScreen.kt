package com.techazsure.leanflow.ui

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.techazsure.leanflow.*
import com.techazsure.leanflow.speech.VoiceCommandEngine
import com.techazsure.leanflow.visual.PhotoParser
import com.techazsure.leanflow.visual.PhotoUploadParser
import com.techazsure.leanflow.visual.VideoParser
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
enum class CameraMode { VIDEO_RECORD, SCAN_TEXT, SCAN_IMAGE }
enum class ChatRole { USER, ASSISTANT }

@Composable
fun LearnflowlyScreen(
    cameraEngine: CameraFlowEngine? = null,
    voiceCommandEngine: VoiceCommandEngine? = null,
    photoParser: PhotoParser? = null,
    photoUploadParser: PhotoUploadParser? = null,
    videoParser: VideoParser? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var interactionState by rememberSaveable { mutableStateOf(InteractionState.IDLE) }
    var cameraMode by remember { mutableStateOf(CameraMode.VIDEO_RECORD) }

    // 🔥 FIXED: Launcher moved to main scope to prevent CoroutineScope lifecycle bugs
    val fileUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                voiceCommandEngine?.aiResponseText?.value = "Extracting multi-modal context..."
                val response = photoUploadParser?.parseUploadedFile(
                    fileUri = selectedUri,
                    userPrompt = "Deconstruct and paraphrase the content of this file asset."
                ) ?: "Offline Upload Parser unavailable."

                voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("USER", "Uploaded Asset: ${selectedUri.lastPathSegment}")
                voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("AI", response)
                voiceCommandEngine?.speakOut(response)
                interactionState = InteractionState.IDLE
            }
        }
    }

    // 🔥 FIXED: Safe null-chaining and explicit type definition
    val activeThread = voiceCommandEngine?.chatHistoryManager?.currentActiveThread?.value
    val messageList: List<ChatMessage> = activeThread?.messages ?: emptyList()

    val listState = rememberLazyListState()

    // 🔥 FIXED: Trigger voice recording when state changes to VOICE
    LaunchedEffect(interactionState) {
        if (interactionState == InteractionState.VOICE) {
            voiceCommandEngine?.handleSwipeUpVoiceCommand()
        }
    }

    // 🔥 FIXED: Auto-revert state to IDLE when mic is deactivated
    val isMicActive by remember { derivedStateOf { voiceCommandEngine?.isMicActive?.value == true } }
    LaunchedEffect(isMicActive) {
        if (!isMicActive && interactionState == InteractionState.VOICE) {
            interactionState = InteractionState.IDLE
        }
    }

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
                        items(messageList.filter { it.sender != "SYSTEM" }) { message ->
                            ChatBubble(message = message)
                        }
                        
                        val aiStatus = voiceCommandEngine?.aiResponseText?.value ?: ""
                        val transcript = voiceCommandEngine?.inputTranscript?.value ?: ""

                        if (transcript.isNotEmpty() && interactionState == InteractionState.VOICE) {
                            item {
                                Text(
                                    text = "You said: \"$transcript\"",
                                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                        }

                        if (aiStatus.isNotEmpty() && aiStatus != "Awaiting your input...") {
                            item {
                                Text(
                                    text = if (aiStatus == "Thinking..." || aiStatus == "Listening...") 
                                        "Learnflowly is $aiStatus" else aiStatus,
                                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    // 3. Top App Bar
                    TopAppBar(onMenuClick = { coroutineScope.launch { drawerState.open() } })

                    // 4. Center Guidance Text
                    AnimatedVisibility(
                        visible = interactionState == InteractionState.IDLE && messageList.isEmpty(),
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
                            voiceCommandEngine = voiceCommandEngine,
                            onStateChange = { interactionState = it },
                            onSubmitPrompt = { voiceCommandEngine?.handleTypedPrompt(it) }
                        )
                    }
                }
            }

            // 6. Video Mode Overlay
            if (interactionState == InteractionState.VIDEO) {
                VideoOverlay(
                    photoParser = photoParser,
                    photoUploadParser = photoUploadParser,
                    cameraEngine = cameraEngine,
                    voiceCommandEngine = voiceCommandEngine,
                    fileUploadLauncher = fileUploadLauncher,
                    onClose = { interactionState = InteractionState.IDLE },
                    onStartLiveStream = { interactionState = InteractionState.LIVE_CHAT },
                    onModeSelect = { cameraMode = it }
                )
            }

            // 7. Live Chat Viewport
            if (interactionState == InteractionState.LIVE_CHAT) {
                var isRecording by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx -> 
                            PreviewView(ctx).apply { 
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE 
                            } 
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { cameraEngine?.startCameraPreview(lifecycleOwner, it) }
                    )

                    // 🔥 MODE-SPECIFIC BUTTON
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 64.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Red.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f))
                            .border(4.dp, Color.White, CircleShape)
                            .clickable {
                                when (cameraMode) {
                                    CameraMode.VIDEO_RECORD -> {
                                        if (!isRecording) {
                                            isRecording = true
                                            cameraEngine?.startVideoRecording(
                                                onVideoSaved = { file ->
                                                    coroutineScope.launch {
                                                        voiceCommandEngine?.aiResponseText?.value = "Analyzing video stream..."
                                                        val summary = videoParser?.summarizeVideoClip(file) 
                                                            ?: "Video processing engine offline."
                                                        
                                                        voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("USER", "Recorded Live Session")
                                                        voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("AI", summary)
                                                        voiceCommandEngine?.speakOut(summary)
                                                        interactionState = InteractionState.IDLE
                                                    }
                                                },
                                                onError = { isRecording = false }
                                            )
                                        } else {
                                            isRecording = false
                                            cameraEngine?.stopVideoRecording()
                                        }
                                    }
                                    CameraMode.SCAN_TEXT -> {
                                        cameraEngine?.takeMentorSnapshot(
                                            onPhotoSaved = { file ->
                                                coroutineScope.launch {
                                                    val result = photoParser?.parseImageAndSummarize(file, "Identify all text layers and focus on key definitions.")
                                                        ?: "Parsing engine connection broken."
                                                    voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("USER", "Captured Text Analysis")
                                                    voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("AI", result)
                                                    voiceCommandEngine?.speakOut(result)
                                                    interactionState = InteractionState.IDLE
                                                }
                                            },
                                            onError = { interactionState = InteractionState.IDLE }
                                        )
                                    }
                                    CameraMode.SCAN_IMAGE -> {
                                        cameraEngine?.takeMentorSnapshot(
                                            onPhotoSaved = { file ->
                                                coroutineScope.launch {
                                                    val result = photoParser?.parseImageAndSummarize(file, "Deconstruct the visual engineering schematics inside this layout.")
                                                        ?: "Parsing engine connection broken."
                                                    voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("USER", "Captured Hardware Matrix Image")
                                                    voiceCommandEngine?.chatHistoryManager?.saveMessageToCurrentThread("AI", result)
                                                    voiceCommandEngine?.speakOut(result)
                                                    interactionState = InteractionState.IDLE
                                                }
                                            },
                                            onError = { interactionState = InteractionState.IDLE }
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) {
                            Box(modifier = Modifier.size(28.dp).background(Color.White, RoundedCornerShape(4.dp)))
                        } else {
                            val icon = if (cameraMode == CameraMode.VIDEO_RECORD) Icons.Default.Videocam else Icons.Default.CameraAlt
                            Icon(icon, contentDescription = "Action", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    IconButton(
                        onClick = { 
                            if (isRecording) cameraEngine?.stopVideoRecording()
                            interactionState = InteractionState.IDLE 
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "USER"
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
                text = if (isUser) AnnotatedString(message.text) else parseMarkdownToAnnotatedString(message.text),
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * 🔥 FIXED: Parses markdown bold tags (**text**) into Compose AnnotatedStrings
 * for a cleaner academic UI presentation.
 */
private fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) { // It's inside ** **
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
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
fun TopAppBar(modifier: Modifier = Modifier, onMenuClick: () -> Unit = {}) {
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
    voiceCommandEngine: VoiceCommandEngine?,
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
                            } else if (offsetY > 150f && abs(offsetY) > abs(offsetX)) {
                                // 🔥 SWIPE DOWN: Stop Processing & Commands
                                voiceCommandEngine?.cancelActiveVoiceStream()
                                onStateChange(InteractionState.IDLE)
                            } else if (offsetX > 150f && abs(offsetX) > abs(offsetY)) {
                                onStateChange(InteractionState.VIDEO)
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
    fileUploadLauncher: ManagedActivityResultLauncher<String, Uri?>,
    onClose: () -> Unit,
    onStartLiveStream: () -> Unit = {},
    onModeSelect: (CameraMode) -> Unit
) {
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
                    onClick = { 
                        onModeSelect(CameraMode.VIDEO_RECORD)
                        onStartLiveStream() 
                    }
                )
                MediaCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Image,
                    title = "Scan Text",
                    onClick = {
                        onModeSelect(CameraMode.SCAN_TEXT)
                        onStartLiveStream()
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
                        onModeSelect(CameraMode.SCAN_IMAGE)
                        onStartLiveStream()
                    }
                )
                MediaCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.VideoFile,
                    title = "Upload\nFile",
                    onClick = { fileUploadLauncher.launch("*/*") }
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
