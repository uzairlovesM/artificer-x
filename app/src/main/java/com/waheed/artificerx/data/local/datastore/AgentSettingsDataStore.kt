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
     *  THOROUGH only — Fast and Balanced already exist specifically to
     *  keep free-tier quota usage low, and doubling API calls on every
     *  single turn would work against that. */
    val enableCriticPass: Boolean get() = qualityPreset == QualityPreset.THOROUGH
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
