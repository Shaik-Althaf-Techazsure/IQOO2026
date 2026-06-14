package com.techazsure.leanflow.ui

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
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.techazsure.leanflow.ChatMessage
import com.techazsure.leanflow.ChatRole
import com.techazsure.leanflow.speech.VoiceCommandEngine
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.techazsure.leanflow.CameraFlowEngine
import com.techazsure.leanflow.SpeechToTextEngine
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

// --- Theme Colors from DESIGN.md ---
val SurfaceColor = Color(0xFFF8F9FF)
val PrimaryColor = Color(0xFF4648D4)
val SecondaryContainer = Color(0xFFDAE2FD)
val PrimaryContainer = Color(0xFF6063EE)
val OnSurface = Color(0xFF0B1C30)
val OnSurfaceVariant = Color(0xFF464554)

enum class InteractionState { IDLE, TEXT, VOICE, VIDEO, LIVE_CHAT, TESTING_HUB }

data class MediaAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Preview(showBackground = true)
@Composable
fun LearnflowlyScreenPreview() {
    LearnflowlyScreen()
}

@Composable
fun LearnflowlyScreen(
    viewModel: LearnFlowViewModel? = null,
    cameraEngine: CameraFlowEngine? = null,
    voiceCommandEngine: VoiceCommandEngine? = null,
    sttEngine: SpeechToTextEngine? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var interactionState by rememberSaveable { mutableStateOf(InteractionState.IDLE) }

    // --- Launchers for Media Ingestion ---
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel?.ingestContextFromUri(context, it, "Media") }
    }

    val textUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel?.ingestContextFromUri(context, it, "Text Document") }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel?.ingestContextFromBitmap(it, isOcrTarget = false) }
    }

    val scanTextOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel?.ingestContextFromBitmap(it, isOcrTarget = true) }
    }

    val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()
    val scanner = GmsDocumentScanning.getClient(scannerOptions)

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uri = scanResult?.pdf?.uri
            if (uri != null) {
                viewModel?.ingestContextFromUri(context, uri, "Scanned PDF")
            }
        }
    }
    
    // Collect the memory state
    val chatHistory by (viewModel?.chatHistory?.collectAsState() ?: remember { mutableStateOf(emptyList<ChatMessage>()) })
    val isProcessing by (viewModel?.isProcessing?.collectAsState() ?: remember { mutableStateOf(false) })
    val engineStatus by (viewModel?.engineStatus?.collectAsState() ?: remember { mutableStateOf<String?>(null) })
    
    // Controls auto-scrolling to the latest message
    val listState = rememberLazyListState()

    // Auto-scroll logic
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    // Handle Voice Mode Transcription
    LaunchedEffect(interactionState) {
        if (interactionState == InteractionState.VOICE) {
            sttEngine?.recordAudioStream(
                onPartialResult = { /* could display partial in UI if needed */ }
            ) { final ->
                viewModel?.submitPrompt(final)
                interactionState = InteractionState.IDLE
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
            }
        }

        // 6. Overlays (Drawn on top)
        if (interactionState == InteractionState.VIDEO) {
            VideoOverlay(
                viewModel = viewModel,
                onClose = { interactionState = InteractionState.IDLE },
                onStartLiveStream = {
                    interactionState = InteractionState.LIVE_CHAT
                },
                mediaPickerLauncher = mediaPickerLauncher,
                textUploadLauncher = textUploadLauncher,
                takePhotoLauncher = takePhotoLauncher,
                scanner = scanner,
                scanLauncher = scanLauncher,
                scanTextOcrLauncher = scanTextOcrLauncher
            )
        }

        if (interactionState == InteractionState.TESTING_HUB) {
            TestingZoneOverlay(
                onClose = { interactionState = InteractionState.IDLE },
                onSubmitPrompt = { prompt -> viewModel?.submitPrompt(prompt) }
            )
        }
        
        // 7. Live Chat Mode Viewport
        if (interactionState == InteractionState.LIVE_CHAT) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { context ->
                        PreviewView(context).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        cameraEngine?.startCameraPreview(
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView
                        )
                    }
                )
                
                IconButton(
                    onClick = { interactionState = InteractionState.IDLE },
                    modifier = Modifier.align(Alignment.TopEnd).padding(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Live Mode", tint = Color.White)
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
fun TopAppBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                spotColor = Color.Black.copy(alpha = 0.15f),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            ),
        color = SurfaceColor.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
}

@Composable
fun BottomInteractionZone(
    state: InteractionState,
    onStateChange: (InteractionState) -> Unit,
    onSubmitPrompt: (String) -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Smooth return to center
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, animationSpec = spring(), label = "offset_x")
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, animationSpec = spring(), label = "offset_y")

    // Pulsing animation for the button core
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
                onSubmit = {
                    onSubmitPrompt(it)
                    onStateChange(InteractionState.IDLE)
                }
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
                            if (-offsetY > 150f && abs(offsetY) > abs(offsetX)) {
                                onStateChange(InteractionState.VOICE)
                            } else if (offsetX > 150f && abs(offsetX) > abs(offsetY)) {
                                onStateChange(InteractionState.VIDEO)
                            } else if (offsetX < -150f && abs(offsetX) > abs(offsetY)) {
                                onStateChange(InteractionState.TESTING_HUB)
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
        
        AnimatedVisibility(visible = text.isNotBlank()) {
            IconButton(
                onClick = { 
                    onSubmit(text)
                    text = ""
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send, 
                    contentDescription = "Send",
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
fun VideoOverlay(
    viewModel: LearnFlowViewModel?,
    onClose: () -> Unit,
    onStartLiveStream: () -> Unit = {},
    mediaPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    textUploadLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    takePhotoLauncher: ManagedActivityResultLauncher<Void?, Bitmap?>,
    scanner: GmsDocumentScanner,
    scanLauncher: ManagedActivityResultLauncher<IntentSenderRequest, androidx.activity.result.ActivityResult>,
    scanTextOcrLauncher: ManagedActivityResultLauncher<Void?, Bitmap?>
) {
    val context = LocalContext.current
    
    val actions = listOf(
        MediaAction("Upload\nPhoto", Icons.Default.Image) {
            mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        MediaAction("Upload\nVideo", Icons.Default.VideoFile) {
            mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        },
        MediaAction("Upload\nText", Icons.Default.Description) {
            textUploadLauncher.launch(arrayOf("text/plain"))
        },
        MediaAction("Capture\nPhoto", Icons.Default.CameraAlt) {
            takePhotoLauncher.launch(null)
        },
        MediaAction("Scan\nImage (PDF)", Icons.Default.DocumentScanner) {
            scanner.getStartScanIntent((context as Activity))
                .addOnSuccessListener { 
                    scanLauncher.launch(IntentSenderRequest.Builder(it).build()) 
                }
        },
        MediaAction("Scan\nText (OCR)", Icons.AutoMirrored.Filled.TextSnippet) {
            scanTextOcrLauncher.launch(null)
        },
        MediaAction("Live Video\nChat", Icons.Default.VideoCall) {
            onStartLiveStream()
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Visual Mode",
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(actions) { action ->
                    MediaCard(
                        icon = action.icon,
                        title = action.title,
                        onClick = action.onClick
                    )
                }
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
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
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

@Composable
fun TestingZoneOverlay(
    onClose: () -> Unit,
    onSubmitPrompt: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = "Test & Practice",
                tint = PrimaryContainer,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TEST YOUR CONCEPTS",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Challenge your understanding with tailored content from your chat",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TestingConceptCard(
                    icon = Icons.Rounded.Psychology,
                    title = "Quiz from the Chat",
                    description = "Take a dynamic quiz compiled directly from your recent conversation.",
                    accentColor = Color(0xFF4F46E5),
                    onClick = { 
                        onSubmitPrompt("Please generate a short quiz based on the concepts we've discussed so far.")
                        onClose()
                    }
                )
                TestingConceptCard(
                    icon = Icons.Rounded.SportsEsports,
                    title = "Educational Games",
                    description = "Play matching games and puzzles based on learned terms.",
                    accentColor = Color(0xFF10B981),
                    onClick = { 
                        onSubmitPrompt("Let's play an educational text-based game to test my knowledge of the recent topics.")
                        onClose()
                    }
                )
                TestingConceptCard(
                    icon = Icons.Rounded.Terminal,
                    title = "Code Playground",
                    description = "Solve code exercises and compile snippets on chat topics.",
                    accentColor = Color(0xFFF59E0B),
                    onClick = { 
                        onSubmitPrompt("Give me a practical coding exercise to apply the concepts we just talked about.")
                        onClose()
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Back to Chat", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TestingConceptCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
