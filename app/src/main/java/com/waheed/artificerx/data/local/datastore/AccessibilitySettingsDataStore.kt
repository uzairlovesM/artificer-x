package com.waheed.artificerx.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.accessibilityDataStore: DataStore<Preferences> by preferencesDataStore(name = "accessibility_settings")

data class AccessibilitySettings(
    val highContrastMode: Boolean = false,
    val reduceMotion: Boolean = false,
    val textScale: Float = 1.0f,
    val hapticFeedbackEnabled: Boolean = true,
)

/** Section 208 Accessibility — persisted, real user-controllable
 *  settings rather than hardcoded content-description-only support. */
@Singleton
class AccessibilitySettingsDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val highContrastKey = booleanPreferencesKey("high_contrast_mode")
        private val reduceMotionKey = booleanPreferencesKey("reduce_motion")
        private val textScaleKey = floatPreferencesKey("text_scale")
        private val hapticKey = booleanPreferencesKey("haptic_feedback_enabled")

        val settings: Flow<AccessibilitySettings> =
            context.accessibilityDataStore.data.map { prefs ->
                AccessibilitySettings(
                    highContrastMode = prefs[highContrastKey] ?: false,
                    reduceMotion = prefs[reduceMotionKey] ?: false,
                    textScale = prefs[textScaleKey] ?: 1.0f,
                    hapticFeedbackEnabled = prefs[hapticKey] ?: true,
                )
            }

        suspend fun setHighContrast(enabled: Boolean) {
            context.accessibilityDataStore.edit { it[highContrastKey] = enabled }
        }

        suspend fun setReduceMotion(enabled: Boolean) {
            context.accessibilityDataStore.edit { it[reduceMotionKey] = enabled }
        }

        suspend fun setTextScale(scale: Float) {
            context.accessibilityDataStore.edit { it[textScaleKey] = scale.coerceIn(0.8f, 1.6f) }
        }

        suspend fun setHapticFeedback(enabled: Boolean) {
            context.accessibilityDataStore.edit { it[hapticKey] = enabled }
        }
    }
