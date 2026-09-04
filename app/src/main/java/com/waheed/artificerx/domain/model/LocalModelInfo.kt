package com.waheed.artificerx.domain.model

/**
 * Load/inference lifecycle state of the on-device GGUF engine
 * (Section: Local Model provider). Mirrors ProviderConnectionState's
 * shape for remote providers so the settings UI can reuse the same
 * status-chip patterns, but the states themselves are specific to a
 * local llama.cpp context: there's no "rate limited" here, but there
 * is "loading" (multi-second, must be shown distinctly from a quick
 * remote ping) and "out of memory" (a very real failure mode on-device
 * that a remote provider never surfaces the same way).
 */
enum class LocalModelLoadState {
    NOT_LOADED,
    LOADING,
    READY,
    LOAD_FAILED,
    OUT_OF_MEMORY,
    UNLOADED_LOW_MEMORY,
}

/**
 * Best-effort quantization guess parsed from a GGUF filename (Section:
 * Local Model Import). llama.cpp's GGUF header technically encodes the
 * real quant type, but reading it requires opening the file; the
 * filename convention (`*-Q4_K_M.gguf`, `*-Q8_0.gguf`, etc.) is what
 * every model publisher on Hugging Face already follows, so a filename
 * parse gives an instant UI hint before the user commits to a load.
 */
enum class GgufQuantizationHint(
    val label: String,
    val approxBitsPerWeight: Double,
) {
    Q2_K("Q2_K", 2.6),
    Q3_K_S("Q3_K_S", 3.4),
    Q3_K_M("Q3_K_M", 3.9),
    Q3_K_L("Q3_K_L", 4.3),
    Q4_0("Q4_0", 4.5),
    Q4_K_S("Q4_K_S", 4.6),
    Q4_K_M("Q4_K_M", 4.85),
    Q5_0("Q5_0", 5.5),
    Q5_K_S("Q5_K_S", 5.5),
    Q5_K_M("Q5_K_M", 5.7),
    Q6_K("Q6_K", 6.6),
    Q8_0("Q8_0", 8.5),
    F16("F16", 16.0),
    UNKNOWN("Unknown", 0.0),
    ;

    companion object {
        /** Parses the quant tag out of a filename like
         *  "gemma-3-4b-it-Q4_K_M.gguf" — case-insensitive, checks the
         *  longer/more-specific tags first so "Q4_K_M" isn't
         *  mis-matched as "Q4_0" via a naive substring scan. */
        fun fromFileName(fileName: String): GgufQuantizationHint {
            val upper = fileName.uppercase()
            return entries
                .filter { it != UNKNOWN }
                .sortedByDescending { it.label.length }
                .firstOrNull { upper.contains(it.label) }
                ?: UNKNOWN
        }
    }
}

/**
 * A GGUF model the user has imported from their own device storage
 * (Section: Local Model provider — "mera khud ka local model"). Every
 * field here is derived from the file itself or from user input at
 * import time; nothing is fetched from a network model registry,
 * matching the fully offline nature of this provider.
 *
 * modelUri / mmprojUri are persisted `content://` URIs (never raw
 * filesystem paths — see LocalModelRepository's scoped-storage
 * handling) for which ACTION_OPEN_DOCUMENT persistable read
 * permission has been requested, so they survive app restarts.
 */
data class LocalModelInfo(
    val id: String,
    val displayName: String,
    val modelUri: String,
    val modelFileName: String,
    val modelSizeBytes: Long,
    val quantizationHint: GgufQuantizationHint,
    val mmprojUri: String? = null,
    val mmprojFileName: String? = null,
    val mmprojSizeBytes: Long? = null,
    val contextLength: Int = DEFAULT_CONTEXT_LENGTH,
    val threadCount: Int = DEFAULT_THREAD_COUNT,
    val useGpuOffloadIfAvailable: Boolean = true,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Float = DEFAULT_TOP_P,
    val minP: Float = DEFAULT_MIN_P,
    val isVisionCapable: Boolean = mmprojUri != null,
    val importedAtEpochMillis: Long = System.currentTimeMillis(),
    val lastUsedAtEpochMillis: Long? = null,
) {
    val totalOnDiskSizeBytes: Long
        get() = modelSizeBytes + (mmprojSizeBytes ?: 0L)

    /** Rough RAM estimate: resident weights ≈ file size (GGUF is
     *  already the on-disk quantized representation loaded near-1:1
     *  into RAM) plus the KV cache, which scales with context length
     *  and is the single biggest lever the user has via the context
     *  length setting. This is intentionally conservative (slightly
     *  over-estimates) so the low-RAM warning in the import UI errs
     *  toward caution rather than promising a load that then OOMs. */
    fun estimatedRamUsageBytes(): Long {
        val kvCacheBytesPerToken = KV_CACHE_BYTES_PER_TOKEN_ESTIMATE
        val kvCacheBytes = contextLength.toLong() * kvCacheBytesPerToken
        return modelSizeBytes + kvCacheBytes + RUNTIME_OVERHEAD_BYTES
    }

    companion object {
        /** Best-effort high context default; the native runtime/model remains authoritative. */
        const val DEFAULT_CONTEXT_LENGTH = 32768
        const val DEFAULT_THREAD_COUNT = 4
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_TOP_P = 0.9f
        const val DEFAULT_MIN_P = 0.05f
        const val MIN_CONTEXT_LENGTH = 512
        const val MAX_CONTEXT_LENGTH = 32768
        const val MIN_THREAD_COUNT = 1
        const val MAX_THREAD_COUNT = 16

        private const val KV_CACHE_BYTES_PER_TOKEN_ESTIMATE = 128L
        private const val RUNTIME_OVERHEAD_BYTES = 256L * 1024 * 1024
    }
}

/** GGUF magic bytes ("GGUF" in ASCII) — the first 4 bytes of every
 *  valid GGUF file. Checked at import time before the file is
 *  registered as a LocalModelInfo, so a mis-picked non-model file
 *  fails fast with a clear message instead of surfacing as a cryptic
 *  native-layer crash the first time inference is attempted. */
object GgufFileValidator {
    val MAGIC_BYTES = byteArrayOf(0x47, 0x47, 0x55, 0x46) // "GGUF"

    fun isValidGgufHeader(headerBytes: ByteArray): Boolean =
        headerBytes.size >= MAGIC_BYTES.size &&
            headerBytes.take(MAGIC_BYTES.size).toByteArray().contentEquals(MAGIC_BYTES)
}
