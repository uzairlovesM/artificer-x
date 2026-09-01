package com.waheed.artificerx.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.providerConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "provider_configs",
)

/**
 * Serializable, non-secret mirror of AiProviderConfig (Section 199's
 * ProviderConfig schema) persisted as a JSON blob in Jetpack DataStore.
 * Deliberately excludes any raw key material — only keyAlias (a pointer
 * into EncryptedKeyStore) and maskedKeyPreview are stored here.
 */
@Serializable
data class ProviderConfigRecord(
    val id: String,
    val type: String,
    val displayName: String,
    val baseUrl: String,
    val keyAlias: String,
    val maskedKeyPreview: String,
    val isEnabled: Boolean,
    val isPrimary: Boolean,
    val supportsVision: Boolean,
    val supportsToolCalling: Boolean,
    val defaultModelId: String?,
    val usageTodayCallCount: Int,
    val lastResetEpochDay: Long = 0L,
    val knownDailyQuota: Int?,
    val createdAtEpochMillis: Long,
)

@Singleton
class ProviderConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        private val configsKey = stringPreferencesKey("provider_configs_json")

        val configs: Flow<List<ProviderConfigRecord>> =
            context.providerConfigDataStore.data.map { prefs ->
                val raw = prefs[configsKey] ?: return@map emptyList()
                runCatching { json.decodeFromString<List<ProviderConfigRecord>>(raw) }.getOrDefault(emptyList())
            }

        suspend fun saveAll(records: List<ProviderConfigRecord>) {
            context.providerConfigDataStore.edit { prefs ->
                prefs[configsKey] = json.encodeToString(records)
            }
        }

        suspend fun upsert(record: ProviderConfigRecord) {
            context.providerConfigDataStore.edit { prefs ->
                val existing =
                    prefs[configsKey]?.let {
                        runCatching { json.decodeFromString<List<ProviderConfigRecord>>(it) }.getOrDefault(emptyList())
                    } ?: emptyList()
                val updated = existing.filterNot { it.id == record.id } + record
                prefs[configsKey] = json.encodeToString(updated)
            }
        }

        /** Atomically makes [primaryId] the only primary provider, clearing
         *  isPrimary on every other record in the same write. Using upsert
         *  for this (one record at a time) would leave a window where two
         *  providers are briefly both marked primary, or where switching
         *  primary providers silently leaves the old one still flagged if
         *  the caller forgets to clear it — Section 165's "one Reasoning
         *  Brain is authoritative at a time" invariant. */
        suspend fun setAsOnlyPrimary(primaryId: String) {
            context.providerConfigDataStore.edit { prefs ->
                val existing =
                    prefs[configsKey]?.let {
                        runCatching { json.decodeFromString<List<ProviderConfigRecord>>(it) }.getOrDefault(emptyList())
                    } ?: emptyList()
                val updated = existing.map { it.copy(isPrimary = it.id == primaryId) }
                prefs[configsKey] = json.encodeToString(updated)
            }
        }

        suspend fun delete(id: String) {
            context.providerConfigDataStore.edit { prefs ->
                val existing =
                    prefs[configsKey]?.let {
                        runCatching { json.decodeFromString<List<ProviderConfigRecord>>(it) }.getOrDefault(emptyList())
                    } ?: emptyList()
                prefs[configsKey] = json.encodeToString(existing.filterNot { it.id == id })
            }
        }
    }
