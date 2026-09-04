package com.waheed.artificerx.core.agent

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import com.waheed.artificerx.domain.model.LocalModelInfo
import com.waheed.artificerx.domain.model.LocalModelLoadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Local-inference equivalent of ConnectionTestResult/ChatCompletionResponse
 *  for a completed (non-streaming-consumer) generation — the streaming
 *  path itself is exposed separately via [LocalInferenceEngine.rawEventFlow]
 *  for AgentOrchestrator to consume token-by-token, but callers that
 *  just want "give me the final text" (e.g. a quick Archivist-style
 *  pass) can await this instead of hand-rolling flow collection. */
sealed class LocalGenerationResult {
    data class Success(
        val fullText: String,
        val tokenCount: Int,
        val durationMillis: Long,
    ) : LocalGenerationResult()

    data class Failure(
        val message: String,
    ) : LocalGenerationResult()

    object Aborted : LocalGenerationResult()
}

/**
 * Owns the single on-device llama.cpp inference context for the whole
 * app process (Section: Local Model provider). Deliberately a Hilt
 * @Singleton, not a per-ViewModel instance: a loaded GGUF model can be
 * several GB resident in RAM, so there must only ever be one — loading
 * a second model without unloading the first would very likely OOM the
 * process. Every consumer (AgentOrchestrator for real agent turns,
 * LocalModelViewModel for the settings-screen "test this model"
 * button) shares this same engine and its current-load state.
 *
 * Wraps io.github.ljcamargo:llamacpp-kotlin's LlamaHelper, translating
 * its LLMEvent stream into this app's own AgentEvent /
 * LocalGenerationResult vocabulary so the rest of the codebase never
 * depends on the third-party library's types directly outside this
 * one file — keeps a future engine swap (e.g. if a better-maintained
 * binding appears) to a single-file change.
 */
@Singleton
class LocalInferenceEngine
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
    ) {
        private val contentResolver: ContentResolver get() = appContext.contentResolver

        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val llmEventFlow =
            MutableSharedFlow<LlamaHelper.LLMEvent>(
                replay = 0,
                extraBufferCapacity = LLM_EVENT_BUFFER_CAPACITY,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        private val llamaHelper: LlamaHelper by lazy {
            LlamaHelper(contentResolver = contentResolver, scope = engineScope, sharedFlow = llmEventFlow)
        }

        private val _loadState =
            MutableStateFlow<LocalModelLoadState>(
                LocalModelLoadState.NOT_LOADED,
            )
        val loadState: StateFlow<LocalModelLoadState> = _loadState.asStateFlow()

        private val _loadedModelId = MutableStateFlow<String?>(null)
        val loadedModelId: StateFlow<String?> = _loadedModelId.asStateFlow()

        /** True once [llamaHelper] has been touched at least once — guards
         *  release() from being called on an engine that was never
         *  actually initialized (lazy delegate would otherwise construct
         *  it just to immediately tear it down). */
        private var everInitialized = false

        /** Loads [model] into the shared engine, unloading whatever was
         *  previously loaded first (llama.cpp contexts aren't stacked —
         *  loading a second model reuses/replaces the native context, and
         *  attempting to hold two simultaneously is exactly the OOM risk
         *  this singleton exists to prevent). Suspends until the load
         *  either completes or fails, so callers (the settings
         *  screen's "Load & Test" button, or AgentOrchestrator on first
         *  use of a local provider this session) get a single clear
         *  yes/no rather than having to observe [loadState] themselves. */
        suspend fun loadModel(model: LocalModelInfo): Boolean {
            if (_loadedModelId.value == model.id && _loadState.value == LocalModelLoadState.READY) {
                return true
            }

            _loadState.value = LocalModelLoadState.LOADING
            everInitialized = true

            return withContext(Dispatchers.IO) {
                val loaded =
                    withTimeoutOrNull(MODEL_LOAD_TIMEOUT_SECONDS.seconds) {
                        suspendCancellableCoroutine { continuation ->
                            runCatching {
                                llamaHelper.load(
                                    path = model.modelUri,
                                    contextLength = model.contextLength,
                                    mmprojPath = model.mmprojUri,
                                ) {
                                    if (continuation.isActive) continuation.resume(true)
                                }
                            }.onFailure { error ->
                                Log.e(TAG, "Local model load threw", error)
                                if (continuation.isActive) continuation.resume(false)
                            }
                        }
                    }

                if (loaded == true) {
                    _loadState.value = LocalModelLoadState.READY
                    _loadedModelId.value = model.id
                    true
                } else {
                    val outOfMemory = isLikelyOomCondition()
                    _loadState.value =
                        if (outOfMemory) {
                            LocalModelLoadState.OUT_OF_MEMORY
                        } else {
                            LocalModelLoadState.LOAD_FAILED
                        }
                    _loadedModelId.value = null
                    false
                }
            }
        }

        /** Runs one full generation to completion and returns the final
         *  text — used where a caller wants one Result-shaped answer
         *  rather than hand-consuming the token stream (e.g. the
         *  Archivist summarization pass). For the interactive chat path,
         *  AgentOrchestrator instead collects [rawEventFlow] directly so
         *  it can emit per-token AgentEvent.AgentTextChunk updates. */
        suspend fun generateToCompletion(
            prompt: String,
            imageUri: String? = null,
        ): LocalGenerationResult {
            if (_loadState.value != LocalModelLoadState.READY) {
                return LocalGenerationResult.Failure("No local model is currently loaded.")
            }

            val builder = StringBuilder()
            return suspendCancellableCoroutine { continuation ->
                val collectorJob =
                    engineScope.launch {
                        llmEventFlow.collect { event ->
                            when (event) {
                                is LlamaHelper.LLMEvent.Ongoing -> builder.append(event.word)
                                is LlamaHelper.LLMEvent.Done -> {
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            LocalGenerationResult.Success(
                                                fullText = builder.toString(),
                                                tokenCount = event.tokenCount,
                                                durationMillis = event.duration,
                                            ),
                                        )
                                    }
                                }
                                is LlamaHelper.LLMEvent.Error -> {
                                    if (continuation.isActive) {
                                        continuation.resume(LocalGenerationResult.Failure(event.message))
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                continuation.invokeOnCancellation {
                    collectorJob.cancel()
                    runCatching { llamaHelper.abort() }
                }
                runCatching {
                    llamaHelper.predict(prompt, imageUri)
                }.onFailure { error ->
                    collectorJob.cancel()
                    if (continuation.isActive) {
                        continuation.resume(LocalGenerationResult.Failure(error.message ?: "Generation failed to start."))
                    }
                }
            }
        }

        /** Raw event stream for AgentOrchestrator's streaming chat path —
         *  exposed so it can render per-token AgentEvent.AgentTextChunk
         *  updates exactly like it does for a remote provider's SSE
         *  stream, keeping the two code paths visually consistent to the
         *  user even though one is local and one is remote. */
        val rawEventFlow: MutableSharedFlow<LlamaHelper.LLMEvent> get() = llmEventFlow

        fun startStreamingPredict(
            prompt: String,
            imageUri: String? = null,
        ) {
            llamaHelper.predict(prompt, imageUri)
        }

        fun abort() {
            if (everInitialized) runCatching { llamaHelper.abort() }
        }

        fun unload() {
            if (everInitialized) {
                runCatching { llamaHelper.abort() }
                runCatching { llamaHelper.release() }
            }
            _loadState.value = LocalModelLoadState.NOT_LOADED
            _loadedModelId.value = null
            everInitialized = false
        }

        /** Called from ArtificerXApp's onStop (Section 137 Thermal/Battery
         *  Awareness applied to local inference specifically): a
         *  multi-gigabyte model sitting resident in RAM while the app is
         *  backgrounded is the single biggest reason a backgrounded app
         *  gets killed by the OS's low-memory killer, taking any unsaved
         *  agent turn down with it. Unloading on background and requiring
         *  a re-load on foreground trades a few seconds of reload latency
         *  for materially better background survivability. */
        fun unloadDueToBackgrounding() {
            if (_loadState.value == LocalModelLoadState.READY) {
                _loadState.value = LocalModelLoadState.UNLOADED_LOW_MEMORY
                runCatching { llamaHelper.abort() }
                runCatching { llamaHelper.release() }
                everInitialized = false
            }
        }

        /** Best-effort OOM heuristic: checks whether the runtime's free
         *  heap headroom is critically low right after a failed load,
         *  since llama.cpp's native allocator throws/fails in ways that
         *  don't always surface as a catchable OutOfMemoryError up
         *  through the JNI boundary. Not perfectly precise, but gives the
         *  import/settings UI a meaningfully more actionable message than
         *  a generic "load failed" every time a large model won't fit. */
        private fun isLikelyOomCondition(): Boolean {
            val runtime = Runtime.getRuntime()
            val maxHeap = runtime.maxMemory()
            val usedHeap = runtime.totalMemory() - runtime.freeMemory()
            val freeRatio = 1.0 - (usedHeap.toDouble() / maxHeap.toDouble())
            return freeRatio < OOM_FREE_HEAP_RATIO_THRESHOLD
        }

        private companion object {
            const val TAG = "LocalInferenceEngine"
            const val LLM_EVENT_BUFFER_CAPACITY = 64
            /** No wall-clock generation ceiling; cancellation/user stop is the boundary. */
            const val MODEL_LOAD_TIMEOUT_SECONDS = Long.MAX_VALUE
            const val GENERATION_TIMEOUT_SECONDS = Long.MAX_VALUE
            const val OOM_FREE_HEAP_RATIO_THRESHOLD = 0.08
        }
    }

/** Local counterpart to LocalModelInfo's own directory conventions —
 *  not currently used to copy files (the repository intentionally
 *  reads directly from the picked content:// URI), but kept as the
 *  single named constant for where a future explicit-copy / offline-
 *  cache feature would live, so it's defined once rather than
 *  inlined as a string literal wherever it might eventually be
 *  needed. */
internal fun localModelsDirectory(baseDir: File): File = File(baseDir, "local-models")
