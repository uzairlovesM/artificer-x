import com.waheed.artificerx.ai.common.ErrorHandler
import com.waheed.artificerx.core.runtime.Callback

class VisionEngine : VisionCapability, ErrorHandler by VisionErrorAggregator() {
    @Throws(MultipleModelFailureException::class)
    override suspend fun process(context: VisionInput): VisionAnalysis = try {
        // Process multiple vision models with error aggregation
        val initialResult = processVisionModel(context)
        val enhancedResult = processEnhancementModel(initialResult)
        val depthEstimation = process3DModel(enhancedResult)
        return enhancedResult.merge(depthEstimation)
    } catch (e: MultipleModelFailureException) {
        // Pass aggregated errors to callback
        throw e
    }

    // Centralized error collection with 10k token capacity
    override fun handle(modelName: String, exception: Exception): List<ModelError> =
        MultiModalErrorAggregator().aggregateErrors(
            ModelError(modelName, exception.message)
        )
}