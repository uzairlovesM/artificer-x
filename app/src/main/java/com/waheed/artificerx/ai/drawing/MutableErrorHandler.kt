/**
Error handling enhancement to aggregate all model failures
Current behavior: Individual exceptions propagate separately
New behavior: Collect all errors into structured error object
```
class MultiModalErrorAggregator : ErrorHandler {
    override fun handle(modelName: String, exception: Exception): List<ModelError> {
        val errors = mutableListOf<ModelError>()
        try {
            // Attempt to process error while collecting other sources
            errors += ModelError(modelName, exception.message)
        } catch (collectorError: Exception) {
            // Prevent error handling from failing the aggregation itself
            errors += ModelError("ErrorHandler", collectorError.message)
        }
        return errors
    }
}

data class ModelError(val model: String, val errorMessage: String?)

// Enhanced wrapper for all AI operations
class SafeAIProcessor : MultiModalEngine, ErrorHandler by MultiModalErrorAggregator() {
    @Throws(MultipleModelFailureException::class)
    override suspend fun process(context: DrawingContext): DrawingResult = try {
        val textResult = processWithErrorHandling(() -> TextGenerator().process(context))
        val visionResult = processWithErrorHandling(() -> VisionEngine().process(textResult))
        val audioResult = processWithErrorHandling(() -> AudioEngine().process(context))
        val depthResult = processWithErrorHandling(() -> DepthEstimator().process(visionResult))
        val finalResult = processWithErrorHandling(() -> CanvasProcessor().process(
            text = textResult,
            visuals = visionResult,
            audio = audioResult,
            depth = depthResult
        ))
        DrawingResult(finalResult)
    } catch (e: MultipleModelFailureException) {
        throw e // Pass aggregated errors up the chain
    }

    // Generic error handling wrapper
    private suspend fun <T> processWithErrorHandling(operation: () -> T): T = try {
        operation()
    } catch (e: Exception) {
        handle(e.modelName, e)
        throw e
    }
}