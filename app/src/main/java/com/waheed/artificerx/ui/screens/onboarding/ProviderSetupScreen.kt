package com.waheed.artificerx.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.domain.model.AiProviderPreset
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GlassCardShape
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.PurpleAccent
import com.waheed.artificerx.ui.theme.QualityFail
import com.waheed.artificerx.ui.theme.QualityPass
import com.waheed.artificerx.ui.theme.glassSurface

/**
 * Section 195.A / 206 / 211: the built-in preset picker plus manual
 * credential entry, live connection test, and a clear success/failure
 * outcome — this is the very first thing a fresh install shows once
 * the user taps "Get Started" on WelcomeScreen. Every state
 * (choose → enter → testing → success/failed) is a distinct screen,
 * never a silent spinner over a form that might already be broken.
 */
@Composable
fun ProviderSetupScreen(
    onProviderConfigured: () -> Unit,
    onSkipForNow: () -> Unit,
    viewModel: ProviderSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ArtificerXGradients.backgroundWash),
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
            },
            label = "provider_setup_step",
        ) { step ->
            when (step) {
                ProviderSetupStep.CHOOSE_PRESET ->
                    PresetChoiceContent(
                        presets = state.availablePresets,
                        onPresetSelected = viewModel::selectPreset,
                        onCustomSelected = viewModel::selectCustomProvider,
                        onSkip = onSkipForNow,
                    )

                ProviderSetupStep.ENTER_CREDENTIALS ->
                    CredentialEntryContent(
                        state = state,
                        onApiKeyChanged = viewModel::onApiKeyChanged,
                        onAccountIdChanged = viewModel::onAccountIdChanged,
                        onCustomBaseUrlChanged = viewModel::onCustomBaseUrlChanged,
                        onCustomDisplayNameChanged = viewModel::onCustomDisplayNameChanged,
                        onFetchModels = viewModel::fetchModels,
                        onModelSelected = viewModel::onModelSelected,
                        onManualModelIdChanged = viewModel::onManualModelIdChanged,
                        onBack = viewModel::backToPresetChoice,
                        onSubmit = viewModel::testAndSaveConnection,
                    )

                ProviderSetupStep.TESTING ->
                    TestingContent(
                        providerName =
                            state.selectedPreset?.displayName
                                ?: state.customDisplayNameInput.ifBlank { "provider" },
                    )

                ProviderSetupStep.SUCCESS ->
                    SuccessContent(
                        providerName =
                            state.selectedPreset?.displayName
                                ?: state.customDisplayNameInput.ifBlank { "Custom Provider" },
                        configuredCount = state.configuredProviderCount,
                        onAddAnother = viewModel::addAnotherProvider,
                        onContinue = onProviderConfigured,
                    )

                ProviderSetupStep.FAILED ->
                    FailureContent(
                        errorMessage = state.connectionErrorMessage.orEmpty(),
                        onRetry = viewModel::retryAfterFailure,
                    )
            }
        }
    }
}

@Composable
private fun PresetChoiceContent(
    presets: List<AiProviderPreset>,
    onPresetSelected: (AiProviderPreset) -> Unit,
    onCustomSelected: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Connect an AI Provider",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This is the Reasoning Brain — it plans scenes and drives every tool. Pick a free provider to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(presets) { preset ->
                PresetCard(preset = preset, onClick = { onPresetSelected(preset) })
            }
            item {
                CustomProviderCard(onClick = onCustomSelected)
            }
        }

        TextButton(
            onClick = onSkip,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
        ) {
            Text(
                text = "Skip for now",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: AiProviderPreset,
    onClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .glassSurface()
                .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(PurpleAccent.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.padding(start = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { uriHandler.openUri(preset.signupUrl) }) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Sign up for ${preset.displayName}", tint = GoldPrimary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.padding(start = 6.dp))
            Text("Connect ${preset.displayName}")
        }
    }
}

@Composable
private fun CustomProviderCard(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .glassSurface()
                .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.padding(start = 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Custom Provider",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Any OpenAI-compatible endpoint — self-hosted or otherwise",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onClick) {
            Text("Set Up")
        }
    }
}

@Composable
private fun CredentialEntryContent(
    state: ProviderSetupUiState,
    onApiKeyChanged: (String) -> Unit,
    onAccountIdChanged: (String) -> Unit,
    onCustomBaseUrlChanged: (String) -> Unit,
    onCustomDisplayNameChanged: (String) -> Unit,
    onFetchModels: () -> Unit,
    onModelSelected: (String) -> Unit,
    onManualModelIdChanged: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.padding(start = 4.dp))
            Text(
                text = state.selectedPreset?.displayName ?: "Custom Provider",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isCustomProviderMode) {
            OutlinedTextField(
                value = state.customDisplayNameInput,
                onValueChange = onCustomDisplayNameChanged,
                label = { Text("Provider name") },
                modifier = Modifier.fillMaxWidth(),
                colors = artificerXTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = state.customBaseUrlInput,
                onValueChange = onCustomBaseUrlChanged,
                label = { Text("Base URL (e.g. https://api.example.com/v1)") },
                modifier = Modifier.fillMaxWidth(),
                colors = artificerXTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (state.selectedPreset?.requiresAccountId == true) {
            OutlinedTextField(
                value = state.accountIdInput,
                onValueChange = onAccountIdChanged,
                label = { Text("Cloudflare Account ID") },
                modifier = Modifier.fillMaxWidth(),
                colors = artificerXTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        OutlinedTextField(
            value = state.apiKeyInput,
            onValueChange = onApiKeyChanged,
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            isError = state.apiKeyValidationError != null,
            supportingText = {
                state.apiKeyValidationError?.let {
                    Text(it, color = QualityFail)
                } ?: Text(
                    "Stored encrypted on this device only — never leaves your phone except in direct calls to the provider you're connecting.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = artificerXTextFieldColors(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        ModelSelectionSection(
            state = state,
            onFetchModels = onFetchModels,
            onModelSelected = onModelSelected,
            onManualModelIdChanged = onManualModelIdChanged,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            shape = GlassCardShape,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Text("Test & Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Section 196's live model-fetch step: a "Fetch Models" button that
 * calls the provider's /models endpoint right from the credentials
 * screen (no need to save first), shows the resulting list as
 * selectable rows, and always keeps a manual model-id text field
 * available as a fallback — some custom/self-hosted endpoints don't
 * implement /models at all, or return a shape this app doesn't parse,
 * and the user must never be blocked from proceeding because of that.
 */
@Composable
private fun ModelSelectionSection(
    state: ProviderSetupUiState,
    onFetchModels: () -> Unit,
    onModelSelected: (String) -> Unit,
    onManualModelIdChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Model",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onFetchModels,
            enabled = state.canFetchModels,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
        ) {
            if (state.isFetchingModels) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = GoldPrimary,
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text("Fetching models…", color = GoldPrimary)
            } else {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.padding(start = 6.dp))
                Text(
                    if (state.fetchedModels.isEmpty()) "Fetch available models" else "Refresh models (${state.fetchedModels.size} found)",
                    color = GoldPrimary,
                )
            }
        }

        state.modelFetchError?.let { errorText ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(errorText, style = MaterialTheme.typography.labelSmall, color = QualityFail)
        }

        if (state.fetchedModels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .glassSurface(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.fetchedModels, key = { it.id }) { model ->
                        val isSelected = state.selectedModelId == model.id
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onModelSelected(model.id) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.padding(start = 10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.id,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (model.supportsVision) {
                                    Text(
                                        text = "Vision-capable",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PurpleAccent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.manualModelIdInput,
            onValueChange = onManualModelIdChanged,
            label = { Text("Or type a model name manually") },
            supportingText = {
                Text(
                    "Optional — leave blank to let the provider use its own default model.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = artificerXTextFieldColors(),
        )

        if (state.effectiveModelId != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Selected: ${state.effectiveModelId}",
                style = MaterialTheme.typography.labelSmall,
                color = QualityPass,
            )
        }
    }
}

@Composable
private fun artificerXTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GoldPrimary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = GoldPrimary,
        cursorColor = GoldPrimary,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    )

@Composable
private fun TestingContent(providerName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Testing connection to $providerName…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SuccessContent(
    providerName: String,
    configuredCount: Int,
    onAddAnother: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = QualityPass, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$providerName connected",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$configuredCount provider${if (configuredCount == 1) "" else "s"} configured",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = GlassCardShape,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Text("Enter Studio", fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(onClick = onAddAnother, modifier = Modifier.fillMaxWidth()) {
            Text("Add Another Provider")
        }
    }
}

@Composable
private fun FailureContent(
    errorMessage: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Error, contentDescription = null, tint = QualityFail, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connection failed",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = GlassCardShape,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Text("Try Again", fontWeight = FontWeight.SemiBold)
        }
    }
}
