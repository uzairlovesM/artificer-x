package com.waheed.artificerx.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import com.waheed.artificerx.core.agent.LocalInferenceEngine
import com.waheed.artificerx.core.agent.RemoteInferenceEngine
import com.waheed.artificerx.core.builtin.BuiltinRecipeCatalog
import com.waheed.artificerx.core.runtime.NetworkManager
import com.waheed.artificerx.util.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReasoningEngine @Inject constructor(
    @ApplicationContext val context: Context,
    private val networkManager: NetworkManager,
    private val permissionManager: PermissionManager,
    private val localInferenceEngine: LocalInferenceEngine,
    private val remoteInferenceEngine: RemoteInferenceEngine,
) {
    private val _agentStatus = MutableStateFlow(AgentStatus.Idle)
    val agentStatus: StateFlow<AgentStatus> = _agentStatus

    val hasInternetAccess: Boolean get() = networkManager.isConnected()
    val hasStorageAccess: Boolean get() = permissionManager.isStoragePermissionGranted()

    data class AssetMetadata(
        val assetId: String,
        val projectId: String,
        val version: Int,
        val checksum: String,
        val dependencyGraph: List<String>,
    )

    enum class ProcessingStrategy { LocalOptimized, LocalBasic, Offline }

    enum class AgentStatus { Idle, Processing, Complete, Failed }

    data class AgentConfig(
        val strategy: ProcessingStrategy = ProcessingStrategy.LocalOptimized,
        val maxProcessingTimeMs: Long = Long.MAX_VALUE,
        val qualityThreshold: Float = 0.72f,
        val retryCount: Int = 2,
        val enableLogging: Boolean = true,
    )

    fun generateArtwork(baseImage: Bitmap, prompt: String, config: AgentConfig): Bitmap {
        _agentStatus.value = AgentStatus.Processing
        return runCatching {
            val prepared = processImagePreWork(baseImage, config)
            val inferred = when (config.strategy) {
                ProcessingStrategy.LocalOptimized, ProcessingStrategy.LocalBasic -> prepared.copy(prepared.config ?: Bitmap.Config.ARGB_8888, true)
                ProcessingStrategy.Offline -> prepared.copy(prepared.config ?: Bitmap.Config.ARGB_8888, true)
            }
            val final = processPostInference(inferred, config)
            _agentStatus.value = AgentStatus.Complete
            final
        }.getOrElse {
            _agentStatus.value = AgentStatus.Failed
            throw it
        }
    }

    private fun processImagePreWork(source: Bitmap, config: AgentConfig): Bitmap = when (config.strategy) {
        ProcessingStrategy.LocalOptimized -> optimizeLocally(source)
        ProcessingStrategy.LocalBasic -> basicProcessing(source)
        ProcessingStrategy.Offline -> offlineQualityFilter(source)
    }

    private fun processPostInference(source: Bitmap, config: AgentConfig): Bitmap = when (config.strategy) {
        ProcessingStrategy.LocalOptimized -> postInferenceOptimization(source)
        ProcessingStrategy.LocalBasic -> basicPostProcessing(source)
        ProcessingStrategy.Offline -> offlinePostProcessing(source)
    }

    private fun optimizeLocally(bitmap: Bitmap): Bitmap = adjust(bitmap, saturation = 1.05f, contrast = 1.03f)
    private fun basicProcessing(bitmap: Bitmap): Bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
    private fun offlineQualityFilter(bitmap: Bitmap): Bitmap = adjust(bitmap, saturation = 1.02f, contrast = 1.01f)
    private fun postInferenceOptimization(bitmap: Bitmap): Bitmap = adjust(bitmap, saturation = 1.04f, contrast = 1.02f)
    private fun basicPostProcessing(bitmap: Bitmap): Bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
    private fun offlinePostProcessing(bitmap: Bitmap): Bitmap = adjust(bitmap, saturation = 1.01f, contrast = 1.01f)

    private fun adjust(source: Bitmap, saturation: Float, contrast: Float): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val matrix = ColorMatrix().apply { setSaturation(saturation) }
        val scale = contrast
        val translate = 128f * (1f - scale)
        matrix.postConcat(ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f,
        )))
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, null, Rect(0, 0, out.width, out.height), paint)
        return out
    }
}

data class ReasoningRuntimePolicy(
    val allowRemote: Boolean = true,
    val allowLocal: Boolean = true,
    val maxIterations: Int = 64,
    val maxRepairPasses: Int = 8,
    val minimumConfidence: Double = 0.72,
)

fun ReasoningRuntimePolicy.normalized(): ReasoningRuntimePolicy = copy(
    maxIterations = maxIterations.coerceIn(1, 4096),
    maxRepairPasses = maxRepairPasses.coerceIn(0, 128),
    minimumConfidence = minimumConfidence.coerceIn(0.0, 1.0),
)
