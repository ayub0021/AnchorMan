package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Memory
import com.example.ui.AnchorViewModel
import com.example.ui.MemoryFilter
import com.example.ui.components.AnchorLogoSymbol
import com.example.ui.components.CategoryPill
import com.example.ui.components.ComicBurstCaughtBadge
import com.example.ui.components.ComicHalftoneOverlay
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.theme.AnchorBg
import com.example.ui.theme.AnchorBorder
import com.example.ui.theme.AnchorCardBg
import com.example.ui.theme.AnchorCardCompleted
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorCyanDark
import com.example.ui.theme.AnchorCyanLight
import com.example.ui.theme.AnchorDark
import com.example.ui.theme.AnchorMuted
import com.example.ui.theme.AnchorRed
import com.example.ui.theme.AnchorRedLight
import com.example.ui.theme.AnchorYellow
import com.example.ui.theme.AnchorYellowLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorHomeScreen(
    viewModel: AnchorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val captureState by viewModel.captureState.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var speechPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        speechPermissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "Microphone enabled for interception", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone denied. Use quick-type capture!", Toast.LENGTH_SHORT).show()
        }
    }

    // Initialize SpeechRecognizer
    DisposableEffect(context) {
        val recognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
        speechRecognizer = recognizer

        onDispose {
            recognizer?.destroy()
        }
    }

    fun startListeningFlow() {
        if (!speechPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (speechRecognizer != null) {
            viewModel.startListening()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    viewModel.stopListeningAndIntercept(null)
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    viewModel.stopListeningAndIntercept(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { partial ->
                        viewModel.updateSpeechTranscript(partial)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                viewModel.stopListeningAndIntercept(null)
            }
        } else {
            // SpeechRecognizer unavailable on emulator/device: open quick input automatically
            viewModel.setQuickInputExpanded(true)
            Toast.makeText(context, "Voice input unavailable on this device. Type your thought!", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopListeningFlow() {
        if (captureState.isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // ignore
            }
            viewModel.stopListeningAndIntercept()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onKeyEvent { keyEvent ->
                // Support keyboard shortcut: Shift + A or Ctrl + A to trigger capture
                if (keyEvent.isShiftPressed && keyEvent.key == Key.A) {
                    viewModel.setQuickInputExpanded(true)
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Subtle comic halftone background pattern
        ComicHalftoneOverlay(
            modifier = Modifier.fillMaxSize(),
            spacing = 16.dp,
            dotRadius = 1.2.dp
        )

        // Main content constrained for tablet/desktop ergonomics
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // HEADER: Modern Geometric Anchor + ANCHOR MAN + Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnchorLogoSymbol(
                    modifier = Modifier.size(32.dp),
                    accentColor = AnchorCyan,
                    strokeColor = AnchorDark
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ANCHOR MAN",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = AnchorDark
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Catch it before it slips.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                color = AnchorDark.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // HERO CAPTURE AREA
            HeroCaptureControl(
                isListening = captureState.isListening,
                transcript = captureState.listeningTranscript,
                isInputExpanded = captureState.isQuickInputExpanded,
                onStartHolding = { startListeningFlow() },
                onStopHolding = { stopListeningFlow() },
                onToggleQuickInput = {
                    viewModel.setQuickInputExpanded(!captureState.isQuickInputExpanded)
                }
            )

            // EXPANDABLE QUICK-CAPTURE DRAWER / BOX
            AnimatedVisibility(
                visible = captureState.isQuickInputExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                QuickCaptureCard(
                    inputText = captureState.inputText,
                    onTextChanged = { viewModel.updateInputText(it) },
                    selectedCategory = captureState.selectedCategory,
                    onSelectCategory = { viewModel.setCategory(it) },
                    selectedDeadline = captureState.selectedDeadline,
                    onSelectDeadline = { viewModel.setDeadline(it) },
                    onIntercept = {
                        viewModel.interceptRawThought(
                            rawInput = captureState.inputText,
                            explicitCategory = captureState.selectedCategory,
                            explicitDeadline = captureState.selectedDeadline
                        )
                        focusManager.clearFocus()
                    },
                    onClose = { viewModel.setQuickInputExpanded(false) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION HEADER: "TODAY" & FILTER CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TODAY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = AnchorDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val activeCount = memories.count { !it.isCompleted }
                    val completedCount = memories.count { it.isCompleted }

                    Box(
                        modifier = Modifier
                            .background(AnchorCyan, RoundedCornerShape(6.dp))
                            .border(1.5.dp, AnchorDark, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$activeCount ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AnchorDark
                        )
                    }
                }

                // Filter tabs: ALL / ACTIVE / CLEARED
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterPill(
                        label = "ALL",
                        isSelected = currentFilter == MemoryFilter.ALL,
                        onClick = { viewModel.setFilter(MemoryFilter.ALL) }
                    )
                    FilterPill(
                        label = "ACTIVE",
                        isSelected = currentFilter == MemoryFilter.ACTIVE,
                        onClick = { viewModel.setFilter(MemoryFilter.ACTIVE) }
                    )
                    FilterPill(
                        label = "CLEARED",
                        isSelected = currentFilter == MemoryFilter.COMPLETED,
                        onClick = { viewModel.setFilter(MemoryFilter.COMPLETED) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MEMORY LIST
            if (memories.isEmpty()) {
                EmptyInterceptState(
                    onTapToCapture = { viewModel.setQuickInputExpanded(true) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(memories, key = { it.id }) { memory ->
                        MemoryItemCard(
                            memory = memory,
                            onToggleComplete = { viewModel.toggleComplete(memory) },
                            onDelete = { viewModel.deleteMemory(memory) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // COMIC BURST "CAUGHT." POPUP ANIMATION
        if (captureState.showCaughtStamp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 180.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                ComicBurstCaughtBadge(
                    onDismiss = { viewModel.dismissCaughtStamp() }
                )
            }
        }
    }
}

/**
 * Large Central Interceptor Button with tactile comic hold/press feedback
 */
@Composable
private fun HeroCaptureControl(
    isListening: Boolean,
    transcript: String,
    isInputExpanded: Boolean,
    onStartHolding: () -> Unit,
    onStopHolding: () -> Unit,
    onToggleQuickInput: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    var isPressedDown by remember { mutableStateOf(false) }

    val buttonScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressedDown || isListening) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "btn_scale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = if (isListening) AnchorYellow else AnchorCyan,
        label = "btn_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            // Expanding cyan ripples when listening / holding
            if (isListening || isPressedDown) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(rippleScale)
                        .background(
                            color = AnchorCyan.copy(alpha = rippleAlpha),
                            shape = CircleShape
                        )
                        .border(
                            2.dp,
                            AnchorDark.copy(alpha = rippleAlpha),
                            CircleShape
                        )
                )
            }

            // Hard shadow layer underneath the big button (4px 4px 0 #181818)
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .offset(x = 5.dp, y = 5.dp)
                    .background(AnchorDark, CircleShape)
            )

            // The main interceptor capture button
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .scale(buttonScale)
                    .background(buttonBgColor, CircleShape)
                    .border(3.dp, AnchorDark, CircleShape)
                    .clip(CircleShape)
                    .testTag("intercept_hero_button")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressedDown = true
                                onStartHolding()
                                val released = tryAwaitRelease()
                                isPressedDown = false
                                onStopHolding()
                            },
                            onTap = {
                                // Tap triggers quick text drawer if not holding
                                onToggleQuickInput()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isListening) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Listening microphone",
                            tint = AnchorDark,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        // Inner geometric radar interceptor reticle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(2.5.dp, AnchorDark, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(AnchorDark, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // STATUS LABEL
        if (isListening) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "INTERCEPTING...",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = AnchorRed
                )
                if (transcript.isNotBlank()) {
                    Text(
                        text = "\"$transcript\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AnchorDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Release to catch memory",
                        fontSize = 12.sp,
                        color = AnchorDark.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "HOLD TO CAPTURE",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = AnchorDark
                )
                Text(
                    text = " • ",
                    fontWeight = FontWeight.Bold,
                    color = AnchorDark.copy(alpha = 0.4f)
                )
                Text(
                    text = if (isInputExpanded) "HIDE INPUT" else "TAP TO TYPE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = AnchorDark.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { onToggleQuickInput() }
                        .padding(4.dp)
                )
            }
        }
    }
}

/**
 * Fast typed capture interface
 */
@Composable
private fun QuickCaptureCard(
    inputText: String,
    onTextChanged: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    selectedDeadline: String,
    onSelectDeadline: (String) -> Unit,
    onIntercept: () -> Unit,
    onClose: () -> Unit
) {
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        backgroundColor = AnchorCardBg,
        borderColor = AnchorDark,
        shadowOffset = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FAST INTERCEPT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = AnchorDark
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close input",
                        tint = AnchorDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        "e.g. Submit DBMS record on Monday...",
                        color = AnchorMuted,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("intercept_text_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnchorDark,
                    unfocusedBorderColor = AnchorDark,
                    focusedContainerColor = AnchorBg,
                    unfocusedContainerColor = AnchorBg,
                    cursorColor = AnchorDark
                ),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntercept() }),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            // CATEGORY CHIPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categories = listOf("🎓 College", "⚡ Urgent", "📦 Personal", "💡 Idea")
                categories.forEach { cat ->
                    CategoryPill(
                        label = cat,
                        isSelected = selectedCategory == cat,
                        onClick = { onSelectCategory(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // DEADLINE QUICK CHIPS & ACTION BUTTON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val deadlines = listOf("Monday", "Tomorrow", "Today", "Friday", "None")
                    items(deadlines) { dl ->
                        val isSelected = selectedDeadline == dl
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) AnchorDark else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(1.dp, AnchorDark, RoundedCornerShape(6.dp))
                                .clickable { onSelectDeadline(dl) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = dl,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else AnchorDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                NeoButton(
                    onClick = onIntercept,
                    backgroundColor = AnchorYellow,
                    shape = RoundedCornerShape(8.dp),
                    borderWidth = 1.5.dp,
                    shadowOffset = 2.dp,
                    testTag = "intercept_submit_button"
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTERCEPT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = AnchorDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Intercept thought",
                            tint = AnchorDark,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter Pill for Today's memories
 */
@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) AnchorDark else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .border(1.5.dp, AnchorDark, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            color = if (isSelected) Color.White else AnchorDark
        )
    }
}

/**
 * Memory Card Component adhering to comic neo-brutalist styling:
 * Thick 2px border, 4px hard shadow, title, original thought, category, deadline, completion state.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoryItemCard(
    memory: Memory,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (memory.isCompleted) AnchorCardCompleted else AnchorCardBg
    val textColor = if (memory.isCompleted) AnchorMuted else AnchorDark

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_card_${memory.id}"),
        backgroundColor = cardBg,
        borderColor = AnchorDark,
        shadowOffset = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // TOP ROW: Title + Category Pill + Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = memory.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp,
                            color = textColor,
                            textDecoration = if (memory.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = memory.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = textColor.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_memory_${memory.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete memory",
                            tint = AnchorDark.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BOTTOM ROW: Category Badge, Deadline Chip, and Custom Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryPill(
                            label = memory.category,
                            backgroundColor = when {
                                memory.category.contains("Urgent") -> AnchorRedLight
                                memory.category.contains("College") -> AnchorCyanLight
                                memory.category.contains("Idea") -> AnchorYellowLight
                                else -> AnchorBg
                            }
                        )

                        if (!memory.deadline.isNullOrBlank() && memory.deadline != "None") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(AnchorBg, RoundedCornerShape(6.dp))
                                    .border(1.dp, AnchorDark, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Deadline",
                                    tint = AnchorDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = memory.deadline,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AnchorDark
                                )
                            }
                        }
                    }

                    // Tactile Completion Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onToggleComplete() }
                            .padding(4.dp)
                            .testTag("checkbox_${memory.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    if (memory.isCompleted) AnchorCyan else Color.White,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(2.dp, AnchorDark, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (memory.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = AnchorDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Diagonal "CLEARED" comic stamp when completed
            if (memory.isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 40.dp)
                        .rotate(-10f)
                        .border(2.dp, AnchorDark, RoundedCornerShape(4.dp))
                        .background(AnchorYellow.copy(alpha = 0.95f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CLEARED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = AnchorDark
                    )
                }
            }
        }
    }
}

/**
 * Empty state when no memories are present
 */
@Composable
private fun EmptyInterceptState(
    onTapToCapture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(AnchorCyanLight, CircleShape)
                .border(2.dp, AnchorDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnchorLogoSymbol(
                modifier = Modifier.size(32.dp),
                accentColor = AnchorCyan,
                strokeColor = AnchorDark
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "NO THOUGHTS IN RADAR",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = AnchorDark
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Hold the button or tap below to intercept your next assignment, thought, or task.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = AnchorDark.copy(alpha = 0.65f),
            modifier = Modifier.padding(horizontal = 30.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        NeoButton(
            onClick = onTapToCapture,
            backgroundColor = AnchorYellow,
            shape = RoundedCornerShape(8.dp),
            shadowOffset = 3.dp
        ) {
            Text(
                text = "INTERCEPT A THOUGHT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = AnchorDark,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
