package com.waheed.artificerx.ai.vision

data class VisionObservation(
    val sceneType: String,
    val objects: List<DetectedObject>,
    val spatialRelations: List<SpatialRelation>,
    val palette: List<String>,
    val compositionScore: Float,
    val perspectiveScore: Float,
    val completenessScore: Float,
    val issues: List<VisionIssue>
)

data class DetectedObject(val label: String, val confidence: Float, val x: Float, val y: Float, val width: Float, val height: Float)
data class SpatialRelation(val subject: String, val relation: String, val target: String)
data class VisionIssue(val code: String, val severity: Severity, val message: String)
enum class Severity { INFO, WARNING, ERROR, BLOCKING }
