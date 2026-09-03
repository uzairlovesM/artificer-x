package com.waheed.artificerx.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.agentSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "agent_settings",
)

enum class QualityPreset(
    val label: String,
    val maxIterations: Int,
    val temperature: Float,
    val snapshotEveryNCalls: Int,
) {
    FAST("Fast", maxIterations = 6, temperature = 0.6f, snapshotEveryNCalls = 3),
    BALANCED("Balanced", maxIterations = 12, temperature = 0.4f, snapshotEveryNCalls = 2),
    THOROUGH("Thorough", maxIterations = 24, temperature = 0.25f, snapshotEveryNCalls = 1),
    // v0.4.30 "Deep Studio" — the explicit "jitna time lage lage, deep
    // thinking, full research, heavy multi-layer construction" mode.
    // 60 iterations (vs THOROUGH's 24) so a real research phase
    // (web_search + web_fetch calls) plus a genuinely multi-layer build
    // (sketch/lineart/base-color/shading/highlights/background as
    // SEPARATE layers, not flattened into one draw_path spree) actually
    // fits inside the turn's tool-call budget instead of getting cut
    // off. Lower temperature than THOROUGH (more deliberate/consistent
    // tool choices over such a long turn). This burns noticeably more
    // free-tier quota per turn than the other presets — that's the
    // honest tradeoff of "heavy", not hidden from the user.
    DEEP_STUDIO("Deep Studio", maxIterations = 60, temperature = 0.3f, snapshotEveryNCalls = 1),
}

data class AgentSettings(
    val qualityPreset: QualityPreset = QualityPreset.BALANCED,
    val customMaxIterations: Int? = null,
    val customTemperature: Float? = null,
) {
    val effectiveMaxIterations: Int get() = customMaxIterations ?: qualityPreset.maxIterations
    val effectiveTemperature: Float get() = customTemperature ?: qualityPreset.temperature
    val effectiveSnapshotFrequency: Int get() = qualityPreset.snapshotEveryNCalls

    /** Section "Critic/Repair" loop: an extra provider round-trip after
     *  every finish_turn to sanity-check the result and trigger a
     *  targeted repair pass if something's clearly wrong. Gated to
     *  THOROUGH and DEEP_STUDIO only — Fast and Balanced already exist
     *  specifically to keep free-tier quota usage low, and doubling API
     *  calls on every single turn would work against that. */
    val enableCriticPass: Boolean get() = qualityPreset == QualityPreset.THOROUGH || qualityPreset == QualityPreset.DEEP_STUDIO

    /** v0.4.30: Deep Studio's system prompt gets the mandatory
     *  research-then-plan-then-multi-layer-build workflow block (see
     *  AgentOrchestrator.systemPromptMessage) instead of the normal
     *  terse instruction set. */
    val isDeepStudioMode: Boolean get() = qualityPreset == QualityPreset.DEEP_STUDIO

    /** v0.4.30: passed through as OpenAI-compatible `reasoning_effort`
     *  on the completion request when the active model supports it
     *  (Groq's qwen3.6 and several OpenRouter routes do) — null for
     *  every other preset so providers that reject an unrecognized
     *  field for models that don't support it aren't sent one
     *  needlessly. */
    val reasoningEffort: String? get() = if (qualityPreset == QualityPreset.DEEP_STUDIO) "high" else null
}

/**
 * Section 84's "quality vs speed" tradeoff, made into a real persisted
 * control instead of a hardcoded constant in AgentOrchestrator. Fast
 * favors fewer tool-call iterations and higher temperature (quicker,
 * looser results, less free-tier quota burned); Thorough allows deep
 * self-correction loops at the cost of more provider calls per turn.
 */
@Singleton
class AgentSettingsDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val presetKey = stringPreferencesKey("quality_preset")
        private val customMaxIterKey = intPreferencesKey("custom_max_iterations")
        private val customTempKey = floatPreferencesKey("custom_temperature")

        val settings: Flow<AgentSettings> =
            context.agentSettingsDataStore.data.map { prefs ->
                AgentSettings(
                    qualityPreset =
                        runCatching {
                            QualityPreset.valueOf(prefs[presetKey] ?: QualityPreset.BALANCED.name)
                        }.getOrDefault(QualityPreset.BALANCED),
                    customMaxIterations = prefs[customMaxIterKey],
                    customTemperature = prefs[customTempKey],
                )
            }

        suspend fun setPreset(preset: QualityPreset) {
            context.agentSettingsDataStore.edit { prefs ->
                prefs[presetKey] = preset.name
                prefs.remove(customMaxIterKey)
                prefs.remove(customTempKey)
            }
        }

        suspend fun setCustomOverrides(
            maxIterations: Int?,
            temperature: Float?,
        ) {
            context.agentSettingsDataStore.edit { prefs ->
                if (maxIterations != null) prefs[customMaxIterKey] = maxIterations else prefs.remove(customMaxIterKey)
                if (temperature != null) prefs[customTempKey] = temperature else prefs.remove(customTempKey)
            }
        }
    }
