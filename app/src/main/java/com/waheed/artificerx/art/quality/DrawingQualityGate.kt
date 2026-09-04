package com.waheed.artificerx.art.quality

import com.waheed.artificerx.ai.vision.VisionObservation

data class QualityGateResult(val passed: Boolean, val score: Float, val failures: List<String>)

class DrawingQualityGate {
    fun evaluate(observation: VisionObservation, requiredObjectCount: Int, target: Float): QualityGateResult {
        val failures = buildList {
            if (observation.compositionScore < .45f) add("composition")
            if (requiredObjectCount > 0 && observation.objects.size < requiredObjectCount) add("object completeness")
            if (observation.perspectiveScore < .30f) add("perspective")
            if (observation.issues.any { it.severity.name == "BLOCKING" }) add("blocking visual defect")
        }
        val score = (observation.compositionScore * .35f + observation.perspectiveScore * .30f + observation.completenessScore * .35f).coerceIn(0f, 1f)
        return QualityGateResult(failures.isEmpty() && score >= target, score, failures)
    }
}
