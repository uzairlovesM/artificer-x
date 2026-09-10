package com.waheed.artificerx.core.ai.apex.generation

abstract class SourceAwareAdapter : GenerationAdapter {
    override fun supports(kind: GenerationKind): Boolean = true
    protected fun validateSource(request: GenerationRequest): Result<Unit> = if (GenerationRequestPlanner().requiresSource(request.kind) && (request.sourceImage?.isEmpty() != false)) Result.failure(IllegalArgumentException("source_image_required")) else Result.success(Unit)
}
