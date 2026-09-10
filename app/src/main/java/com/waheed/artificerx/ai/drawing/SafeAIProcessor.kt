/**
Safe processing layer for AI capabilities
Collects all errors across multi-modal workflow
```
import com.waheed.artificerx.ai.common.Capability

class SafeAIProcessor : MultiModalEngine, MultiModalErrorAggregator() {
    @Throws(MultipleModelFailureException::class)
    override suspend fun process(context: DrawingContext): DrawingResult = try {
        val textResult = processWithErrorHandling("TextGenerator", context, this::generateText)
        val visionResult = processWithErrorHandling("VisionEngine", textResult, this::analyzeVision)
        val audioResult = processWithErrorHandling("AudioEngine", context, this::processAudio)
        val depthResult = processWithErrorHandling("DepthEstimator", visionResult, this::estimateDepth)
        val finalResult = processWithErrorHandling("CanvasProcessor", depthResult, this::processCanvas)
        DrawingResult(finalResult)
    } catch (e: MultipleModelFailureException) {
        throw e // Pass aggregated errors up the chain
    }

    // Central error collection using aggregator
    private suspend fun <T> processWithErrorHandling(
        modelName: String,
        input: Any,
        operation: (Any) -> T
    ): T = try {
        operation(input)
    } catch (e: Exception) {
        // Add model-specific error handling
        val errors = MultiModalErrorAggregator().aggregateErrors(
            ModelError(modelName, e.message)
        )
        throw MultipleModelFailureException(errors)
    }
}