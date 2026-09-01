package com.waheed.artificerx.data.repository

import com.waheed.artificerx.core.security.EncryptedKeyStore
import com.waheed.artificerx.data.local.datastore.ProviderConfigDataStore
import com.waheed.artificerx.data.local.datastore.ProviderConfigRecord
import com.waheed.artificerx.domain.model.AiProviderConfig
import com.waheed.artificerx.domain.model.AiProviderType
import com.waheed.artificerx.domain.model.ProviderConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for configured AI providers (Section 195-199).
 * Coordinates two storage layers so secrets and metadata never mix:
 *  - ProviderConfigDataStore: non-secret fields (id, display name,
 *    base URL, usage counters, masked preview) — safe to appear in
 *    backups/logs.
 *  - EncryptedKeyStore: the raw API key, addressed only by keyAlias.
 *
 * This is the repository StartupViewModel (MainActivity.kt) and the
 * onboarding Provider Setup screen both depend on — first real,
 * non-placeholder data source in the app.
 */
@Singleton
class ProviderConfigRepository
    @Inject
    constructor(
        private val dataStore: ProviderConfigDataStore,
        private val keyStore: EncryptedKeyStore,
    ) {
        val configs: Flow<List<AiProviderConfig>> =
            dataStore.configs.map { records ->
                records.map { it.toDomain() }
            }

        suspend fun hasAnyProviderConfigured(): Boolean = dataStore.configs.first().any { it.isEnabled }

        suspend fun addProvider(
            type: AiProviderType,
            displayName: String,
            baseUrl: String,
            rawApiKey: String,
            supportsVision: Boolean,
            supportsToolCalling: Boolean,
            knownDailyQuota: Int?,
            makePrimary: Boolean,
            defaultModelId: String? = null,
        ): AiProviderConfig {
            val id = UUID.randomUUID().toString()
            val keyAlias = "provider_key_$id"
            keyStore.storeKey(keyAlias, rawApiKey)

            val record =
                ProviderConfigRecord(
                    id = id,
                    type = type.name,
                    displayName = displayName,
                    baseUrl = baseUrl,
                    keyAlias = keyAlias,
                    maskedKeyPreview = keyStore.maskedPreview(rawApiKey),
                    isEnabled = true,
                    isPrimary = makePrimary,
                    supportsVision = supportsVision,
                    supportsToolCalling = supportsToolCalling,
                    defaultModelId = defaultModelId,
                    usageTodayCallCount = 0,
                    lastResetEpochDay = currentEpochDay(),
                    knownDailyQuota = knownDailyQuota,
                    createdAtEpochMillis = System.currentTimeMillis(),
                )
            dataStore.upsert(record)
            if (makePrimary) {
                dataStore.setAsOnlyPrimary(id)
            }
            return record.toDomain()
        }

        suspend fun removeProvider(
            id: String,
            keyAlias: String,
        ) {
            keyStore.deleteKey(keyAlias)
            dataStore.delete(id)
        }

        suspend fun setEnabled(
            record: ProviderConfigRecord,
            enabled: Boolean,
        ) {
            dataStore.upsert(record.copy(isEnabled = enabled))
        }

        suspend fun setPrimary(record: ProviderConfigRecord) {
            dataStore.setAsOnlyPrimary(record.id)
        }

        suspend fun updateDefaultModel(
            record: ProviderConfigRecord,
            modelId: String,
        ) {
            dataStore.upsert(record.copy(defaultModelId = modelId))
        }

        /** Bumps the usage counter, first rolling it over to 0 if the last
         *  reset happened on an earlier calendar day (local device time).
         *  Section 199's free-tier daily quota tracking is meaningless
         *  without this: without a reset, usageTodayCallCount only ever
         *  grows, and a provider that hit its quota once stays permanently
         *  marked as exhausted even the next day. */
        suspend fun incrementUsage(record: ProviderConfigRecord) {
            val today = currentEpochDay()
            val resetRecord =
                if (record.lastResetEpochDay != today) {
                    record.copy(usageTodayCallCount = 0, lastResetEpochDay = today)
                } else {
                    record
                }
            dataStore.upsert(resetRecord.copy(usageTodayCallCount = resetRecord.usageTodayCallCount + 1))
        }

        private fun currentEpochDay(): Long = System.currentTimeMillis() / MILLIS_PER_DAY

        fun rawKeyFor(keyAlias: String): String? = keyStore.retrieveKey(keyAlias)

        private fun ProviderConfigRecord.toDomain(): AiProviderConfig =
            AiProviderConfig(
                id = id,
                type = runCatching { AiProviderType.valueOf(type) }.getOrDefault(AiProviderType.CUSTOM),
                displayName = displayName,
                baseUrl = baseUrl,
                keyAlias = keyAlias,
                maskedKeyPreview = maskedKeyPreview,
                isEnabled = isEnabled,
                isPrimary = isPrimary,
                supportsVision = supportsVision,
                supportsToolCalling = supportsToolCalling,
                defaultModelId = defaultModelId,
                connectionState = ProviderConnectionState.UNKNOWN,
                usageTodayCallCount = if (lastResetEpochDay != currentEpochDay()) 0 else usageTodayCallCount,
                lastResetEpochDay = lastResetEpochDay,
                knownDailyQuota = knownDailyQuota,
                createdAtEpochMillis = createdAtEpochMillis,
            )

        private companion object {
            const val MILLIS_PER_DAY = 86_400_000L
        }
    }
