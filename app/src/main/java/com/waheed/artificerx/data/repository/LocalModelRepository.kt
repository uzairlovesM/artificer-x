package com.waheed.artificerx.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.waheed.artificerx.data.local.datastore.LocalModelDataStore
import com.waheed.artificerx.domain.model.GgufFileValidator
import com.waheed.artificerx.domain.model.GgufQuantizationHint
import com.waheed.artificerx.domain.model.LocalModelInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of attempting to import a picked file as a GGUF model or
 *  mmproj projector — a sealed result rather than a raw exception so
 *  the import screen can show a specific, actionable message instead
 *  of a generic "import failed". */
sealed class GgufImportResult {
    data class Success(
        val fileName: String,
        val sizeBytes: Long,
        val uri: String,
    ) : GgufImportResult()

    object InvalidHeader : GgufImportResult()

    object FileTooSmallToRead : GgufImportResult()

    data class PermissionDenied(
        val message: String,
    ) : GgufImportResult()

    data class UnknownError(
        val message: String,
    ) : GgufImportResult()
}

/**
 * Owns the whole "user picks a GGUF file from their own storage"
 * pipeline (Section: Local Model provider — "mera khud ka local model
 * jiski files mere paas hongi wo upload karke chala sako"):
 *
 *  1. Build the ACTION_OPEN_DOCUMENT intent the UI launches.
 *  2. On the returned URI, take a *persistable* read permission grant
 *     so the file stays reachable across app restarts (a plain
 *     content:// URI's grant is otherwise revoked when the launching
 *     activity finishes — Android's scoped-storage contract).
 *  3. Validate it's actually a GGUF file by checking the magic bytes
 *     before ever registering it, so a mis-tapped random file fails
 *     immediately with a clear message instead of crashing native
 *     code on first load.
 *  4. Persist the resulting LocalModelInfo via LocalModelDataStore.
 *
 * Never copies the (often multi-GB) file into app-private storage —
 * inference reads directly from the picked content:// URI via a file
 * descriptor, matching every production local-LLM Android app's
 * approach and avoiding doubling the user's storage footprint.
 */
@Singleton
class LocalModelRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStore: LocalModelDataStore,
    ) {
        val importedModels: Flow<List<LocalModelInfo>> = dataStore.importedModels
        val activeModelId: Flow<String?> = dataStore.activeModelId

        /** Intent for picking a GGUF (or mmproj — same extension/mime,
         *  distinguished only by which import call the caller uses)
         *  file. Filtered to a permissive octet-stream/any mime since
         *  Android has no registered MIME type for `.gguf`; the real
         *  validation happens after selection via magic-byte check. */
        fun buildOpenDocumentIntent(): Intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream"))
            }

        suspend fun importBaseModel(
            pickedUri: Uri,
            displayName: String? = null,
        ): GgufImportResult = validateAndDescribe(pickedUri)

        suspend fun importMmprojFile(pickedUri: Uri): GgufImportResult = validateAndDescribe(pickedUri)

        /** Registers a validated model + optional projector as a usable
         *  LocalModelInfo. Called after the UI has confirmed both
         *  GgufImportResult.Success results (or just the base model, for
         *  a text-only model) and collected a display name / initial
         *  settings from the user. */
        suspend fun registerModel(
            modelResult: GgufImportResult.Success,
            mmprojResult: GgufImportResult.Success?,
            displayName: String,
            contextLength: Int = LocalModelInfo.DEFAULT_CONTEXT_LENGTH,
            threadCount: Int = LocalModelInfo.DEFAULT_THREAD_COUNT,
        ): LocalModelInfo {
            val info =
                LocalModelInfo(
                    id = UUID.randomUUID().toString(),
                    displayName = displayName.ifBlank { modelResult.fileName },
                    modelUri = modelResult.uri,
                    modelFileName = modelResult.fileName,
                    modelSizeBytes = modelResult.sizeBytes,
                    quantizationHint = GgufQuantizationHint.fromFileName(modelResult.fileName),
                    mmprojUri = mmprojResult?.uri,
                    mmprojFileName = mmprojResult?.fileName,
                    mmprojSizeBytes = mmprojResult?.sizeBytes,
                    contextLength = contextLength.coerceIn(LocalModelInfo.MIN_CONTEXT_LENGTH, LocalModelInfo.MAX_CONTEXT_LENGTH),
                    threadCount = threadCount.coerceIn(LocalModelInfo.MIN_THREAD_COUNT, LocalModelInfo.MAX_THREAD_COUNT),
                )
            dataStore.upsertModel(info)
            return info
        }

        suspend fun updateModelSettings(
            modelId: String,
            contextLength: Int? = null,
            threadCount: Int? = null,
            temperature: Float? = null,
            topK: Int? = null,
            topP: Float? = null,
            minP: Float? = null,
            useGpuOffloadIfAvailable: Boolean? = null,
            displayName: String? = null,
        ) {
            val existing = dataStore.importedModels.first().firstOrNull { it.id == modelId } ?: return
            val updated =
                existing.copy(
                    contextLength =
                        contextLength?.coerceIn(LocalModelInfo.MIN_CONTEXT_LENGTH, LocalModelInfo.MAX_CONTEXT_LENGTH)
                            ?: existing.contextLength,
                    threadCount =
                        threadCount?.coerceIn(LocalModelInfo.MIN_THREAD_COUNT, LocalModelInfo.MAX_THREAD_COUNT)
                            ?: existing.threadCount,
                    temperature = temperature ?: existing.temperature,
                    topK = topK ?: existing.topK,
                    topP = topP ?: existing.topP,
                    minP = minP ?: existing.minP,
                    useGpuOffloadIfAvailable = useGpuOffloadIfAvailable ?: existing.useGpuOffloadIfAvailable,
                    displayName = displayName?.ifBlank { existing.displayName } ?: existing.displayName,
                )
            dataStore.upsertModel(updated)
        }

        suspend fun removeModel(modelId: String) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(
                        dataStore.importedModels
                            .first()
                            .firstOrNull { it.id == modelId }
                            ?.modelUri ?: return@runCatching,
                    ),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            dataStore.removeModel(modelId)
        }

        suspend fun setActiveModel(modelId: String?) = dataStore.setActiveModel(modelId)

        suspend fun touchLastUsed(modelId: String) = dataStore.touchLastUsed(modelId)

        suspend fun activeModel(): LocalModelInfo? {
            val activeId = dataStore.activeModelId.first() ?: return null
            return dataStore.importedModels.first().firstOrNull { it.id == activeId }
        }

        /** Takes a persistable read grant on [pickedUri] so it survives
         *  app/process restarts, then checks the first bytes against the
         *  GGUF magic header before accepting the file. Runs on IO since
         *  it touches the ContentResolver / opens a stream. */
        private suspend fun validateAndDescribe(pickedUri: Uri): GgufImportResult =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        pickedUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }.onFailure { error ->
                    return@withContext GgufImportResult.PermissionDenied(
                        error.message ?: "Could not obtain persistent read access to the selected file.",
                    )
                }

                val headerBytes =
                    runCatching {
                        context.contentResolver.openInputStream(pickedUri)?.use { stream ->
                            ByteArray(HEADER_PROBE_SIZE_BYTES).also { buffer ->
                                val read = stream.read(buffer)
                                if (read < GgufFileValidator.MAGIC_BYTES.size) {
                                    return@withContext GgufImportResult.FileTooSmallToRead
                                }
                            }
                        }
                    }.getOrElse { error ->
                        return@withContext GgufImportResult.UnknownError(error.message ?: "Could not read the selected file.")
                    } ?: return@withContext GgufImportResult.UnknownError("File stream unavailable.")

                if (!GgufFileValidator.isValidGgufHeader(headerBytes)) {
                    return@withContext GgufImportResult.InvalidHeader
                }

                val (fileName, sizeBytes) = queryFileMeta(pickedUri)
                GgufImportResult.Success(fileName = fileName, sizeBytes = sizeBytes, uri = pickedUri.toString())
            }

        private fun queryFileMeta(uri: Uri): Pair<String, Long> {
            var name = uri.lastPathSegment ?: "model.gguf"
            var size = 0L
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                        if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                    }
                }
            }
            return name to size
        }

        private companion object {
            const val HEADER_PROBE_SIZE_BYTES = 16
        }
    }
