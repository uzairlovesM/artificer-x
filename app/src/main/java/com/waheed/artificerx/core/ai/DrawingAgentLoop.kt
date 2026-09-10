package com.waheed.artificerx.core.ai

import android.graphics.Bitmap
import android.util.Log
import com.waheed.artificerx.core.agent.Callback
import com.waheed.artificerx.core.runtime.NetworkManager
import kotlinx.coroutines.*
import java.util.*

class DrawingAgentLoop {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val networkManager: NetworkManager

    constructor(networkManager: NetworkManager) {
        this.networkManager = networkManager
    }

    suspend fun processDrawingIntent(intent: DrawingIntent, baseImage: Bitmap, callback: Callback<Bitmap>) = coroutineScope {
        try {
            // Scene parsing phase
            val sceneParser = ParseSceneCapability(networkManager)
            val sceneGraph = sceneParser.parseScene(baseImage, com.waheed.artificerx.core.ai.ReasoningEngine.AgentConfig(
                strategy = com.waheed.artificerx.core.ai.ReasoningEngine.ProcessingStrategy.LocalOptimized,
                maxProcessingTimeMs = 5000,
                qualityThreshold = 0.75f,
                retryCount = 2,
                enableLogging = true
            ))

            if (sceneGraph == null) {
                throw ProcessingException("Scene parsing failed")
            }

            // Drawing pipeline with multi-capability coordination
            val drawingPipeline = DrawingPipelineBuilder()
            val drawingPipelineResult = drawingPipeline.buildPipeline {
                sceneGraph
            }.processScene(sceneGraph)

            // Final artwork generation
            val finalArtwork = RepairDrawingCapability().fixArtifacts(baseImage, sceneGraph)
            callback.onSuccess(finalArtwork)
        } catch (e: CancellationException) {
            callback.onFailure(e)
            throw e
        } catch (e: Exception) {
            DebugLogger.e("DrawingAgentLoop", "Drawing pipeline failed", e)
            callback.onFailure(e)
            throw e
        }
    }

    fun cancelProcessing() {
        scope.cancelAllChildren()
        Log.d("DrawingAgentLoop", "All processing jobs cancelled")
    }
}
