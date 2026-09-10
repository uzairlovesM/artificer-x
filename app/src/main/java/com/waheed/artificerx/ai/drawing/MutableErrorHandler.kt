package com.waheed.artificerx.ai.drawing

import com.waheed.artificerx.ai.ai.common.DrawingCapability
import com.waheed.artificerx.ai.ai.common.DrawingContext
import com.waheed.artificerx.ai.ai.common.DrawingResult
import com.waheed.artificerx.ai.ai.common.ErrorHandler
import com.waheed.artificerx.ai.ai.common.ModelError
import com.waheed.artificerx.ai.ai.common.MultipleModelFailureException

class MultiModalErrorAggregator : ErrorHandler {
    private val errors = ArrayDeque<ModelError>()

    override fun handle(modelName: String, exception: Exception): List<ModelError> {
        val entry = ModelError(modelName, exception.message)
        errors.add(entry)
        while (errors.size > 128) errors.removeFirst()
        return errors.toList()
    }

    fun aggregateErrors(error: ModelError): List<ModelError> = handle(error.model, IllegalStateException(error.errorMessage))
    fun snapshot(): List<ModelError> = errors.toList()
    fun clear() = errors.clear()
}

/** Safe, deterministic adapter around the real canvas pipeline. */
class SafeAIProcessor : DrawingCapability, ErrorHandler by MultiModalErrorAggregator() {
    override suspend fun process(context: DrawingContext): DrawingResult {
        return runCatching {
            MultiModalEngine().process(context)
        }.getOrElse { error ->
            val collected = handle("multimodal", error as? Exception ?: Exception(error))
            throw MultipleModelFailureException(collected)
        }
    }
}
