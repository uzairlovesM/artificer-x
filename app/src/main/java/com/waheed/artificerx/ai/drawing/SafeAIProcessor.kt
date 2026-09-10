package com.waheed.artificerx.ai.drawing

import com.waheed.artificerx.ai.ai.common.DrawingContext
import com.waheed.artificerx.ai.ai.common.DrawingResult
import com.waheed.artificerx.ai.ai.common.DrawingCapability
import com.waheed.artificerx.ai.ai.common.ModelError
import com.waheed.artificerx.ai.ai.common.MultipleModelFailureException

class SafeDrawingProcessor : DrawingCapability {
    private val aggregator = MultiModalErrorAggregator()

    override suspend fun process(context: DrawingContext): DrawingResult = try {
        MultiModalEngine().process(context)
    } catch (error: Exception) {
        val errors = aggregator.handle("drawing", error)
        throw MultipleModelFailureException(errors.ifEmpty { listOf(ModelError("drawing", error.message)) })
    }
}
