package com.waheed.artificerx.core.art

data class MaterialPreset(val id: String, val name: String, val family: String, val scale: Float, val rotation: Float, val opacity: Float)

object MaterialCatalog {
    private val families = listOf("Paper", "Manga Tone", "Fabric", "Metal", "Stone", "Wood", "Leaves", "Cloud", "Glitter", "Noise", "Grid", "Dots", "Hatching", "Vintage", "Comic")
    val presets: List<MaterialPreset> = buildList {
        repeat(220) { index ->
            val family = families[index % families.size]
            add(MaterialPreset("material-${index.toString().padStart(4,'0')}", "$family ${index + 1}", family, 0.25f + (index % 16) / 10f, (index * 17 % 360).toFloat(), 0.35f + (index % 14) / 20f))
        }
    }
}
