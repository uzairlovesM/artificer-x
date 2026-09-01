package com.waheed.artificerx.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.waheed.artificerx.domain.model.GgufQuantizationHint
import com.waheed.artificerx.domain.model.LocalModelInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.localModelDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "local_models",
)

/** Pure-data mirror of LocalModelInfo for JSON persistence — kept
 *  separate from the domain model so LocalModelInfo can carry
 *  behavior (estimatedRamUsageBytes, etc.) without dragging
 *  kotlinx.serialization annotations into the domain layer, matching
 *  the Dto/domain-model split used elsewhere (see ChatCompletionModels
 *  vs. the ChatMessage-shaped types consumed by the UI layer). */
@Serializable
private data class LocalModelRecord(
    val id: String,
    val displayName: String,
    val modelUri: String,
    val modelFileName: String,
    val modelSizeBytes: Long,
    val quantizationHint: String,
    val mmprojUri: String? = null,
    val mmprojFileName: String? = null,
    val mmprojSizeBytes: Long? = null,
    val contextLength: Int,
    val threadCount: Int,
    val useGpuOffloadIfAvailable: Boolean,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val minP: Float,
    val importedAtEpochMillis: Long,
    val lastUsedAtEpochMillis: Long? = null,
)

private fun LocalModelInfo.toRecord() =
    LocalModelRecord(
        id = id,
        displayName = displayName,
        modelUri = modelUri,
        modelFileName = modelFileName,
        modelSizeBytes = modelSizeBytes,
        quantizationHint = quantizationHint.name,
        mmprojUri = mmprojUri,
        mmprojFileName = mmprojFileName,
        mmprojSizeBytes = mmprojSizeBytes,
        contextLength = contextLength,
        threadCount = threadCount,
        useGpuOffloadIfAvailable = useGpuOffloadIfAvailable,
        temperature = temperature,
        topK = topK,
        topP = topP,
        minP = minP,
        importedAtEpochMillis = importedAtEpochMillis,
        lastUsedAtEpochMillis = lastUsedAtEpochMillis,
    )

private fun LocalModelRecord.toDomain() =
    LocalModelInfo(
        id = id,
        displayName = displayName,
        modelUri = modelUri,
        modelFileName = modelFileName,
        modelSizeBytes = modelSizeBytes,
        quantizationHint =
            runCatching { GgufQuantizationHint.valueOf(quantizationHint) }
                .getOrDefault(GgufQuantizationHint.UNKNOWN),
        mmprojUri = mmprojUri,
        mmprojFileName = mmprojFileName,
        mmprojSizeBytes = mmprojSizeBytes,
        contextLength = contextLength,
        threadCount = threadCount,
        useGpuOffloadIfAvailable = useGpuOffloadIfAvailable,
        temperature = temperature,
        topK = topK,
        topP = topP,
        minP = minP,
        isVisionCapable = mmprojUri != null,
        importedAtEpochMillis = importedAtEpochMillis,
        lastUsedAtEpochMillis = lastUsedAtEpochMillis,
    )

/**
 * Persists every GGUF model the user has imported (Section: Local Model
 * provider) plus which one is currently the active local model. A list
 * rather than a single slot because switching between, say, a fast
 * small text model and a heavier vision model is a real workflow —
 * re-importing and re-granting URI permission every time would be
 * hostile.
 *
 * Only metadata + content:// URIs are stored here — never the model
 * bytes themselves, which stay wherever the user's file picker chose
 * (their own Downloads folder, SD card, etc.), matching the "import,
 * don't copy multi-GB files into app storage" approach used by every
 * production local-LLM Android app.
 */
@Singleton
class LocalModelDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val modelsKey = stringPreferencesKey("imported_models_json")
        private val activeModelIdKey = stringPreferencesKey("active_model_id")
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        val importedModels: Flow<List<LocalModelInfo>> =
            context.localModelDataStore.data.map { prefs ->
                val raw = prefs[modelsKey] ?: return@map emptyList()
                runCatching {
                    json
                        .decodeFromString(kotlinx.serialization.builtins.ListSerializer(LocalModelRecord.serializer()), raw)
                        .map { it.toDomain() }
                }.getOrDefault(emptyList())
            }

        val activeModelId: Flow<String?> = context.localModelDataStore.data.map { it[activeModelIdKey] }

        suspend fun upsertModel(model: LocalModelInfo) {
            context.localModelDataStore.edit { prefs ->
                val current = decodeCurrentList(prefs[modelsKey])
                val updated = current.filterNot { it.id == model.id } + model.toRecord()
                prefs[modelsKey] =
                    json.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(LocalModelRecord.serializer()),
                        updated,
                    )
            }
        }

        suspend fun removeModel(modelId: String) {
            context.localModelDataStore.edit { prefs ->
                val current = decodeCurrentList(prefs[modelsKey])
                val updated = current.filterNot { it.id == modelId }
                prefs[modelsKey] =
                    json.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(LocalModelRecord.serializer()),
                        updated,
                    )
                if (prefs[activeModelIdKey] == modelId) prefs.remove(activeModelIdKey)
            }
        }

        suspend fun setActiveModel(modelId: String?) {
            context.localModelDataStore.edit { prefs ->
                if (modelId == null) prefs.remove(activeModelIdKey) else prefs[activeModelIdKey] = modelId
            }
        }

        suspend fun touchLastUsed(modelId: String) {
            context.localModelDataStore.edit { prefs ->
                val current = decodeCurrentList(prefs[modelsKey])
                val updated =
                    current.map {
                        if (it.id == modelId) it.copy(lastUsedAtEpochMillis = System.currentTimeMillis()) else it
                    }
                prefs[modelsKey] =
                    json.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(LocalModelRecord.serializer()),
                        updated,
                    )
            }
        }

        private fun decodeCurrentList(raw: String?): List<LocalModelRecord> {
            if (raw == null) return emptyList()
            return runCatching {
                json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(LocalModelRecord.serializer()), raw)
            }.getOrDefault(emptyList())
        }
    }
