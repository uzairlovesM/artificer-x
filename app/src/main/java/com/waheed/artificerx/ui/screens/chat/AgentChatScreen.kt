package com.waheed.artificerx.ui.screens.chat

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.domain.model.ChatMessage
import com.waheed.artificerx.domain.model.ChatMessageRole
import com.waheed.artificerx.domain.model.ToolCallEntry
import com.waheed.artificerx.domain.model.ToolCallStatus
import com.waheed.artificerx.ui.theme.AgentLogTextStyle
import com.waheed.artificerx.ui.theme.ChatBubbleAgentShape
import com.waheed.artificerx.ui.theme.ChatBubbleUserShape
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.PurpleAccent
import com.waheed.artificerx.ui.theme.QualityFail
import com.waheed.artificerx.ui.theme.QualityPass
import com.waheed.artificerx.ui.theme.QualityWarn
import com.waheed.artificerx.ui.theme.ToolCallChipShape
import com.waheed.artificerx.ui.theme.glassSurface
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Conversational agent interface (Section 90-92). Renders user/agent
 * chat bubbles plus, inline within an agent message, a chip row of
 * every tool call it made to produce that reply — each chip shows
 * status (pending/running/success/failed) so the "how did it draw
 * this" question (Section 92 Live Agent Log's spirit) is answered
 * right in the conversation, not buried in a separate debug screen.
 */
@Composable
fun AgentChatScreen(
    onBack: () -> Unit,
    studioViewModel: com.waheed.artificerx.ui.screens.canvas.StudioViewModel,
    viewModel: AgentChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(studioViewModel) {
        viewModel.bindStudioViewModel(studioViewModel)
    }

    val imagePickerLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract =
                androidx.activity.result.contract.ActivityResultContracts
                    .GetContent(),
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val base64 =
                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val bytes = inputStream?.use { it.readBytes() }
                                bytes?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
                            }.getOrNull()
                        }
                    if (base64 != null) {
                        viewModel.onImageAttached(uri.toString(), base64)
                    }
                }
            }
        }

    // Voice input via Android's built-in on-device/cloud SpeechRecognizer
    // dialog (RECORD_AUDIO is already requested lazily by ArtificerXRoot on
    // first use — Section 116). No extra dependency needed: RecognizerIntent
    // launches the system speech UI and returns the best transcription
    // result, which is appended into the existing text field exactly like
    // typed input so the agent pipeline downstream is unaffected.
    var voiceInputError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val speechRecognizerLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract =
                androidx.activity.result.contract.ActivityResultContracts
                    .StartActivityForResult(),
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val spokenText =
                    result.data
                        ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    val merged = if (state.inputText.isBlank()) spokenText else "${state.inputText} $spokenText"
                    viewModel.onInputChanged(merged)
                }
            }
        }

    fun launchVoiceInput() {
        if (android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent =
                android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Describe what to create or change…")
                }
            runCatching { speechRecognizerLauncher.launch(intent) }
                .onFailure { voiceInputError = "Couldn't open voice input" }
        } else {
            voiceInputError = "No speech recognition service available on this device"
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ChatTopBar(onBack = onBack, onClear = viewModel::clearConversation)

        if (!state.hasConfiguredProvider) {
            NoProviderBanner()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.messages, key = { it.id }) { message ->
                ChatBubble(message)
            }
            if (state.isAgentResponding) {
                item { TypingIndicatorBubble() }
            }
        }

        voiceInputError?.let { errorText ->
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = QualityFail,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        ChatInputBar(
            inputText = state.inputText,
            attachedImageUri = state.attachedImageUri,
            isSending = state.isAgentResponding,
            onInputChanged = viewModel::onInputChanged,
            onSend = viewModel::sendMessage,
            onStop = viewModel::stopCurrentTurn,
            onAttachImage = { imagePickerLauncher.launch("image/*") },
            onClearAttachment = { viewModel.onImageAttached(null, null) },
            onVoiceInput = {
                voiceInputError = null
                launchVoiceInput()
            },
        )
    }
}

@Composable
private fun ChatTopBar(
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = "Agent Chat",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClear) {
            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear conversation", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoProviderBanner() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .glassSurface()
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Error, contentDescription = null, tint = QualityWarn, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Text(
            text = "No AI provider configured — connect one in Settings to use the agent.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StreamingCursor() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "streaming_cursor")
    val alpha by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.15f,
            animationSpec =
                androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(durationMillis = 500),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ),
            label = "streaming_cursor_alpha",
        )
    Text(
        text = "▍",
        style = MaterialTheme.typography.bodyMedium,
        color = GoldPrimary.copy(alpha = alpha),
        modifier = Modifier.padding(start = 2.dp),
    )
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .clip(if (isUser) ChatBubbleUserShape else ChatBubbleAgentShape)
                    .background(
                        if (isUser) {
                            PurpleAccent.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ).padding(12.dp),
        ) {
            // v0.4.30: real per-token streaming (AgentOrchestrator.stream
            // CloudProvider) means text can visibly still be arriving —
            // a blinking cursor after the last character is the standard
            // "still typing" affordance (ChatGPT/Claude apps use the
            // same idea) and was meaningless before this fix, since the
            // old fake streaming delivered the whole reply in one shot
            // with nothing left to indicate.
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onBackground,
                )
                if (message.isStreaming) {
                    StreamingCursor()
                }
            }

            if (message.toolCalls.isNotEmpty()) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                message.toolCalls.forEach { toolCall ->
                    ToolCallChip(toolCall)
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                }
            }

            // v0.4.30: every AI turn that touches the canvas auto-saves to
            // Pictures/ARTIFICER-X (see AgentChatViewModel.applyAgentEvent).
            // This row is the visible confirmation of that, plus a direct
            // way to actually look at or share what got saved without
            // leaving the chat to go hunt through a gallery app.
            if (message.autoSavedUri != null) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                AutoSavedRow(fileName = message.autoSavedFileName.orEmpty(), uri = message.autoSavedUri)
            }
        }
    }
}

@Composable
private fun AutoSavedRow(
    fileName: String,
    uri: android.net.Uri,
) {
    val context = LocalContext.current
    Row(
        modifier =
            Modifier
                .clip(ToolCallChipShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Download, contentDescription = null, tint = QualityPass, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.padding(start = 6.dp))
        Text(
            text = "Saved: $fileName",
            style = AgentLogTextStyle.copy(fontSize = 10.sp()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 140.dp),
        )
        Spacer(modifier = Modifier.padding(start = 8.dp))
        IconButton(
            modifier = Modifier.size(28.dp),
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW)
                            .setDataAndType(uri, "image/png")
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    )
                }
            },
        ) {
            Icon(Icons.Filled.Check, contentDescription = "View saved image", tint = GoldPrimary, modifier = Modifier.size(16.dp))
        }
        IconButton(
            modifier = Modifier.size(28.dp),
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND)
                                .setType("image/png")
                                .putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            "Share AI output",
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        ) {
            Icon(Icons.Filled.Share, contentDescription = "Share saved image", tint = GoldPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ToolCallChip(toolCall: ToolCallEntry) {
    val (icon, tint) =
        when (toolCall.status) {
            ToolCallStatus.PENDING -> Icons.Filled.Build to MaterialTheme.colorScheme.onSurfaceVariant
            ToolCallStatus.RUNNING -> Icons.Filled.Build to GoldPrimary
            ToolCallStatus.SUCCESS -> Icons.Filled.Check to QualityPass
            ToolCallStatus.FAILED -> Icons.Filled.Close to QualityFail
        }
    Row(
        modifier =
            Modifier
                .clip(ToolCallChipShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.padding(start = 6.dp))
        Column {
            Text(text = toolCall.toolName, style = AgentLogTextStyle, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = toolCall.argsPreview,
                style = AgentLogTextStyle.copy(fontSize = 10.sp()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Int.sp() =
    androidx.compose.ui.unit
        .TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
private fun TypingIndicatorBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier =
                Modifier
                    .clip(ChatBubbleAgentShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = GoldPrimary)
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    attachedImageUri: String?,
    isSending: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttachImage: () -> Unit,
    onClearAttachment: () -> Unit,
    onVoiceInput: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
    ) {
        if (attachedImageUri != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = "Reference image attached",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
                IconButton(onClick = onClearAttachment, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = QualityFail, modifier = Modifier.size(14.dp))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttachImage) {
                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = "Attach reference image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = { Text("Describe what to create or change…") },
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                maxLines = 4,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
            )

            IconButton(onClick = onVoiceInput) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice input", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(
                onClick = if (isSending) onStop else onSend,
                enabled = isSending || inputText.isNotBlank(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSending -> MaterialTheme.colorScheme.error
                                    inputText.isNotBlank() -> GoldPrimary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSending) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = "Stop generating",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint =
                                if (inputText.isNotBlank()) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
