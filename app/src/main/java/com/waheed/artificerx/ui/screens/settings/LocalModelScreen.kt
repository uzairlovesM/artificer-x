package com.waheed.artificerx.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.domain.model.LocalModelInfo
import com.waheed.artificerx.domain.model.LocalModelLoadState
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.PurpleAccent
import com.waheed.artificerx.ui.theme.QualityFail
import com.waheed.artificerx.ui.theme.QualityPass
import com.waheed.artificerx.ui.theme.QualityWarn
import com.waheed.artificerx.ui.theme.glassSurface
import kotlin.math.roundToInt

/**
 * Section: Local Model provider ("mera khud ka local model jiski
 * files mere paas hongi wo upload karke chala sako, saath hi
 * mmproj bhi"). Fully offline model management:
 *  - Import a GGUF file (and optional mmproj vision projector) from
 *    the device's own storage via the system file picker.
 *  - Per-model settings: context length, thread count.
 *  - Load & Test panel: run a real generation against the loaded
 *    model right in this screen before trusting it in the actual
 *    agent chat.
 *  - Delete, switch active model.
 *
 * Mirrors AiProvidersSettingsScreen's glass-card visual language so
 * a local model reads as a first-class provider, not a bolted-on
 * afterthought.
 */
@Composable
fun LocalModelScreen(
    onBack: () -> Unit,
    viewModel: LocalModelViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var modelPendingDelete by remember { mutableStateOf<LocalModelInfo?>(null) }

    val baseModelPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            result.data?.data?.let { uri: Uri -> viewModel.onBaseModelPicked(uri) }
        }
    val mmprojPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            result.data?.data?.let { uri: Uri -> viewModel.onMmprojPicked(uri) }
        }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.startImportDraft()
                    baseModelPicker.launch(viewModel.buildPickerIntent())
                },
                containerColor = GoldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Import GGUF model")
            }
        },
    ) { scaffoldPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .background(ArtificerXGradients.backgroundWash),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Column {
                        Text(
                            text = "Local Model",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "GGUF + mmproj — fully offline, zero network",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (state.models.isEmpty()) {
                    EmptyLocalModelsState()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.models, key = { it.id }) { model ->
                            LocalModelCard(
                                model = model,
                                isActive = model.id == state.activeModelId,
                                isLoaded = model.id == state.loadedModelId,
                                loadState = if (model.id == state.loadedModelId) state.loadState else LocalModelLoadState.NOT_LOADED,
                                testPrompt = state.testPrompt,
                                testOutput = state.testOutput,
                                isTesting = state.isTesting,
                                onSetActive = { viewModel.setActiveModel(model) },
                                onDelete = { modelPendingDelete = model },
                                onLoad = { viewModel.loadModel(model) },
                                onUnload = { viewModel.unloadModel() },
                                onTestPromptChange = viewModel::updateTestPrompt,
                                onRunTest = { viewModel.runTestGeneration(model) },
                                onAbortTest = viewModel::abortTest,
                                onContextLengthChange = { viewModel.updateSettings(model, contextLength = it) },
                                onThreadCountChange = { viewModel.updateSettings(model, threadCount = it) },
                                onTemperatureChange = { viewModel.updateSettings(model, temperature = it) },
                            )
                        }
                    }
                }
            }
        }
    }

    state.importDraft?.let { draft ->
        ImportDraftSheet(
            draft = draft,
            onPickMmproj = { mmprojPicker.launch(viewModel.buildPickerIntent()) },
            onClearMmproj = viewModel::clearMmproj,
            onNameChange = viewModel::updateDraftName,
            onContextLengthChange = viewModel::updateDraftContextLength,
            onThreadCountChange = viewModel::updateDraftThreadCount,
            onConfirm = viewModel::confirmImport,
            onCancel = viewModel::cancelImportDraft,
        )
    }

    modelPendingDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelPendingDelete = null },
            title = { Text("Remove ${model.displayName}?") },
            text = { Text("This forgets the model reference. The GGUF file on your device is not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeModel(model)
                    modelPendingDelete = null
                }) { Text("Remove", color = QualityFail) }
            },
            dismissButton = {
                TextButton(onClick = { modelPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EmptyLocalModelsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.Memory, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No local models imported",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap + to import a GGUF file from your device. Add an mmproj file too for image understanding.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocalModelCard(
    model: LocalModelInfo,
    isActive: Boolean,
    isLoaded: Boolean,
    loadState: LocalModelLoadState,
    testPrompt: String,
    testOutput: String,
    isTesting: Boolean,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onTestPromptChange: (String) -> Unit,
    onRunTest: () -> Unit,
    onAbortTest: () -> Unit,
    onContextLengthChange: (Int) -> Unit,
    onThreadCountChange: (Int) -> Unit,
    onTemperatureChange: (Float) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .glassSurface()
                .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSetActive, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Set as active local model",
                    tint = if (isActive) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        "${model.quantizationHint.label} · ${formatBytes(model.totalOnDiskSizeBytes)}" +
                            if (model.isVisionCapable) " · Vision" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = "Model settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove model", tint = QualityFail, modifier = Modifier.size(18.dp))
            }
        }

        LoadStateChip(loadState)

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            LabeledSlider(
                label = "Context length: ${model.contextLength}",
                value = model.contextLength.toFloat(),
                valueRange = LocalModelInfo.MIN_CONTEXT_LENGTH.toFloat()..LocalModelInfo.MAX_CONTEXT_LENGTH.toFloat(),
                onValueChange = { onContextLengthChange(it.roundToInt()) },
            )
            LabeledSlider(
                label = "Threads: ${model.threadCount}",
                value = model.threadCount.toFloat(),
                valueRange = LocalModelInfo.MIN_THREAD_COUNT.toFloat()..LocalModelInfo.MAX_THREAD_COUNT.toFloat(),
                onValueChange = { onThreadCountChange(it.roundToInt()) },
            )
            LabeledSlider(
                label = "Temperature: ${"%.2f".format(model.temperature)}",
                value = model.temperature,
                valueRange = 0f..1.5f,
                onValueChange = onTemperatureChange,
            )
            Text(
                text = "Est. RAM: ${formatBytes(model.estimatedRamUsageBytes())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loadState == LocalModelLoadState.READY) {
                    OutlinedButton(onClick = onUnload) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unload")
                    }
                } else {
                    Button(
                        onClick = onLoad,
                        enabled = loadState != LocalModelLoadState.LOADING,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    ) {
                        if (loadState == LocalModelLoadState.LOADING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load")
                    }
                }
            }

            if (loadState == LocalModelLoadState.READY) {
                Spacer(modifier = Modifier.height(12.dp))
                TestPromptPanel(
                    testPrompt = testPrompt,
                    testOutput = testOutput,
                    isTesting = isTesting,
                    onPromptChange = onTestPromptChange,
                    onRun = onRunTest,
                    onAbort = onAbortTest,
                )
            }
        }
    }
}

@Composable
private fun LoadStateChip(state: LocalModelLoadState) {
    val (label, color) =
        when (state) {
            LocalModelLoadState.NOT_LOADED -> "Not loaded" to MaterialTheme.colorScheme.onSurfaceVariant
            LocalModelLoadState.LOADING -> "Loading…" to PurpleAccent
            LocalModelLoadState.READY -> "Ready" to QualityPass
            LocalModelLoadState.LOAD_FAILED -> "Load failed" to QualityFail
            LocalModelLoadState.OUT_OF_MEMORY -> "Out of memory — try a smaller quant" to QualityFail
            LocalModelLoadState.UNLOADED_LOW_MEMORY -> "Unloaded (backgrounded)" to QualityWarn
        }
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(color, shape = RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors =
                androidx.compose.material3.SliderDefaults
                    .colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
        )
    }
}

@Composable
private fun TestPromptPanel(
    testPrompt: String,
    testOutput: String,
    isTesting: Boolean,
    onPromptChange: (String) -> Unit,
    onRun: () -> Unit,
    onAbort: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.RemoveRedEye, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Load & Test", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = testPrompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Try a prompt…") },
            singleLine = false,
            maxLines = 3,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isTesting) {
            OutlinedButton(onClick = onAbort, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating — tap to abort")
            }
        } else {
            Button(
                onClick = onRun,
                modifier = Modifier.fillMaxWidth(),
                enabled = testPrompt.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            ) {
                Text("Run", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        if (testOutput.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
            ) {
                Text(text = testOutput, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun ImportDraftSheet(
    draft: LocalModelImportDraft,
    onPickMmproj: () -> Unit,
    onClearMmproj: () -> Unit,
    onNameChange: (String) -> Unit,
    onContextLengthChange: (Int) -> Unit,
    onThreadCountChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Import Local Model") },
        text = {
            Column {
                if (draft.baseModelResult == null) {
                    if (draft.isImporting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating file…")
                        }
                    } else {
                        Text("Waiting for you to pick a GGUF file…", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    OutlinedTextField(
                        value = draft.displayName,
                        onValueChange = onNameChange,
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Base model: ${draft.baseModelResult.fileName} (${formatBytes(draft.baseModelResult.sizeBytes)})",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (draft.mmprojResult != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "mmproj: ${draft.mmprojResult.fileName} (${formatBytes(draft.mmprojResult.sizeBytes)})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onClearMmproj) { Text("Remove") }
                        }
                    } else {
                        OutlinedButton(onClick = onPickMmproj, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add vision projector (mmproj) — optional")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LabeledSlider(
                        label = "Context length: ${draft.contextLength}",
                        value = draft.contextLength.toFloat(),
                        valueRange = LocalModelInfo.MIN_CONTEXT_LENGTH.toFloat()..LocalModelInfo.MAX_CONTEXT_LENGTH.toFloat(),
                        onValueChange = { onContextLengthChange(it.roundToInt()) },
                    )
                    LabeledSlider(
                        label = "Threads: ${draft.threadCount}",
                        value = draft.threadCount.toFloat(),
                        valueRange = LocalModelInfo.MIN_THREAD_COUNT.toFloat()..LocalModelInfo.MAX_THREAD_COUNT.toFloat(),
                        onValueChange = { onThreadCountChange(it.roundToInt()) },
                    )
                }

                draft.lastError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = QualityFail, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(error, style = MaterialTheme.typography.labelSmall, color = QualityFail)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = draft.canConfirm) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}
