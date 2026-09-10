package com.waheed.artificerx.ai

import com.waheed.artificerx.ai.ai.common.ErrorHandler
import com.waheed.artificerx.ai.ai.common.ModelError
import com.waheed.artificerx.ai.ai.common.MultipleModelFailureException
import com.waheed.artificerx.ai.ai.common.VisionAnalysis
import com.waheed.artificerx.ai.ai.common.VisionCapability
import com.waheed.artificerx.ai.ai.common.VisionInput
import com.waheed.artificerx.ai.vision.VisionInspector

class VisionErrorAggregator : ErrorHandler {
    private val history = ArrayDeque<ModelError>()
    override fun handle(modelName: String, exception: Exception): List<ModelError> {
        history.add(ModelError(modelName, exception.message))
        while (history.size > 64) history.removeFirst()
        return history.toList()
    }
}

class VisionEngine(
    private val inspector: VisionInspector = VisionInspector(),
    private val errors: VisionErrorAggregator = VisionErrorAggregator(),
) : VisionCapability, ErrorHandler by errors {
    override suspend fun process(context: VisionInput): VisionAnalysis = try {
        val observation = inspector.inspect(com.waheed.artificerx.ai.vision.VisionFrame(context.bitmap, com.waheed.artificerx.ai.vision.VisionSource.IMPORT))
        VisionAnalysis(
            observation = observation,
            summary = "scene=${observation.sceneType}; objects=${observation.objects.size}; composition=${"%.3f".format(observation.compositionScore)}",
        )
    } catch (error: Exception) {
        throw MultipleModelFailureException(handle("vision", error))
    }
}
