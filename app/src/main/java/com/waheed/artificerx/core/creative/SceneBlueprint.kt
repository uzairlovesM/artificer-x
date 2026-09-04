package com.waheed.artificerx.core.creative

import kotlinx.serialization.Serializable

@Serializable
data class SceneBlueprint(
    val subject: String,
    val style: String = "anime",
    val camera: CameraSpec = CameraSpec(),
    val palette: PaletteSpec = PaletteSpec(),
    val layers: List<SceneLayerSpec> = defaultLayers(),
    val details: List<SceneDetail> = emptyList(),
    val quality: Int = 3,
) {
    companion object {
        fun defaultLayers(): List<SceneLayerSpec> = listOf(
            SceneLayerSpec("01_Background", "background"),
            SceneLayerSpec("02_Architecture", "architecture"),
            SceneLayerSpec("03_Lighting", "lighting"),
            SceneLayerSpec("04_Furniture", "furniture"),
            SceneLayerSpec("05_Details", "details"),
            SceneLayerSpec("06_LineArt", "lineart"),
            SceneLayerSpec("07_Atmosphere", "atmosphere"),
        )
    }
}

@Serializable
data class CameraSpec(
    val perspective: String = "one_point",
    val horizonRatio: Float = 0.42f,
    val vanishingXRatio: Float = 0.52f,
    val tiltDegrees: Float = 0f,
)

@Serializable
data class PaletteSpec(
    val wall: String = "#EEE7DD",
    val floor: String = "#B77B5E",
    val ceiling: String = "#F7F3EC",
    val trim: String = "#6D5348",
    val shadow: String = "#6C5870",
    val light: String = "#FFDFA3",
    val accent: String = "#9B6AC2",
    val foliage: String = "#5E8F72",
)

@Serializable
data class SceneLayerSpec(val name: String, val semanticRole: String)

@Serializable
data class SceneDetail(
    val kind: String,
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val label: String = "",
)
