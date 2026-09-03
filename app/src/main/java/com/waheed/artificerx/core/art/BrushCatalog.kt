package com.waheed.artificerx.core.art

import com.waheed.artificerx.domain.model.BrushType

data class BrushPreset(
    val id: String,
    val name: String,
    val family: String,
    val type: BrushType,
    val sizeMultiplier: Float,
    val opacity: Float,
    val spacing: Float,
    val flow: Float,
    val texture: Float,
)

/** Procedural brush catalogue. The renderer consumes parameters, so presets are actual stateful brush recipes. */
object BrushCatalog {
    private val families = listOf(
        "Ink", "Pencil", "Watercolor", "Gouache", "Acrylic", "Oil", "Pastel", "Marker",
        "Airbrush", "Charcoal", "Chalk", "Stamp", "Texture", "Pixel", "Halftone", "Pattern",
        "Fur", "Grass", "Leaf", "Cloud", "Glitter", "Calligraphy", "Eraser", "Smudge"
    )

    val presets: List<BrushPreset> = buildList {
        var index = 0
        repeat(256) {
            val family = families[index % families.size]
            val type = when (family) {
                "Ink", "Calligraphy" -> BrushType.INK_PEN
                "Pencil" -> BrushType.PENCIL
                "Watercolor" -> BrushType.WATERCOLOR
                "Marker" -> BrushType.MARKER
                "Airbrush" -> BrushType.AIRBRUSH
                "Charcoal", "Chalk", "Pastel" -> BrushType.CHARCOAL
                "Eraser" -> BrushType.ERASER_SOFT
                else -> BrushType.INK_PEN
            }
            add(BrushPreset(
                id = "brush-${index.toString().padStart(4, '0')}",
                name = "$family ${index + 1}",
                family = family,
                type = type,
                sizeMultiplier = 0.35f + (index % 17) * 0.07f,
                opacity = 0.55f + (index % 9) * 0.05f,
                spacing = 0.02f + (index % 8) * 0.025f,
                flow = 0.5f + (index % 10) * 0.05f,
                texture = (index % 13) / 12f,
            ))
            index++
        }
    }

    fun byFamily(family: String): List<BrushPreset> = presets.filter { it.family.equals(family, true) }
}
