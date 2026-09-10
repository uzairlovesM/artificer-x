package com.waheed.artificerx.ai.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Path
import com.waheed.artificerx.core.ai.DrawingIntent
import com.waheed.artificerx.core.runtime.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StrokePathCapability {
    private val brushEngines = ConcurrentHashMap<String, BrushEngine>()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun drawStroke(intent: DrawingIntent, path: Path, callback: Callback<Bitmap>) {
        val strokeId = "stroke_${UUID.randomUUID()}"
        val contextKey = contextKey(intent.context)
        val engine = brushEngines.getOrPut(contextKey) { BrushEngine(intent.context) }
        val job = workerScope.async {
            val brushId = engine.registerBrushAndReturnId(
                BrushEngine.BrushData(
                    brushType = BrushEngine.BrushType.SOFT,
                    color = intent.color,
                    size = intent.size.coerceAtLeast(0.5f),
                    texture = intent.texture,
                    opacity = intent.opacity.coerceIn(0f, 1f),
                ),
            )
            engine.applyBrush(path, brushId, intent.baseImage)
        }
        activeJobs[strokeId] = job
        runCatching {
            val result = job.await()
            callback.onSuccess(result)
            intent.completionCallback?.onSuccess(result)
        }.onFailure { error ->
            callback.onFailure(error)
            intent.completionCallback?.onFailure(error)
        }.also {
            activeJobs.remove(strokeId)
        }
    }

    suspend fun multiStrokeDrawing(intent: DrawingIntent, strokePaths: List<Path>, callback: Callback<Bitmap>) =
        withContext(Dispatchers.Default) {
            val contextKey = contextKey(intent.context)
            val engine = brushEngines.getOrPut(contextKey) { BrushEngine(intent.context) }
            runCatching {
                var current = intent.baseImage
                strokePaths.forEach { path ->
                    val brushId = engine.registerBrushAndReturnId(
                        BrushEngine.BrushData(
                            brushType = BrushEngine.BrushType.AIRBRUSH,
                            color = intent.color,
                            size = (intent.size * 0.75f).coerceAtLeast(0.5f),
                            texture = intent.texture,
                            opacity = (intent.opacity * 0.8f).coerceIn(0f, 1f),
                        ),
                    )
                    current = engine.applyBrush(path, brushId, current)
                }
                callback.onSuccess(current)
                intent.completionCallback?.onSuccess(current)
            }.onFailure { error ->
                callback.onFailure(error)
                intent.completionCallback?.onFailure(error)
            }
        }

    fun cancelStrokeProcessing(context: Context, strokeId: String): Boolean {
        val job = activeJobs.remove(strokeId) ?: return false
        job.cancel()
        return true
    }

    fun clear(context: Context) {
        brushEngines.remove(contextKey(context))?.clearBrushRegistry()
    }

    private fun contextKey(context: Context): String =
        context.applicationContext.packageName
}
