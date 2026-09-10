package com.waheed.artificerx.core.ai.apex.generation

data class ArtifactValidation(val valid: Boolean, val reasons: List<String>)
class ArtifactValidator {
    fun validate(artifact: GeneratedArtifact, maxPixels: Long = 100_000_000L): ArtifactValidation {
        val reasons = mutableListOf<String>()
        if (artifact.width <= 0 || artifact.height <= 0) reasons += "invalid_dimensions"
        val pixels = artifact.width.toLong() * artifact.height.toLong()
        if (pixels > maxPixels) reasons += "pixel_budget_exceeded"
        if (artifact.mime.isBlank()) reasons += "missing_mime"
        if (artifact.pixels != null && artifact.pixels.size.toLong() != pixels) reasons += "pixel_buffer_mismatch"
        return ArtifactValidation(reasons.isEmpty(), reasons)
    }
}
