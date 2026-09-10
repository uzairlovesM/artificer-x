package com.waheed.artificerx.core.drawing.engine

import com.waheed.artificerx.art.brush.BrushDefinition

object BrushPresetCatalog {
    val builtIns: List<BrushDefinition> = listOf(
        BrushDefinition("pencil-hb", "HB Pencil", 5f, .72f, .12f, .62f, .82f, .25f, .55f, .3f, .08f, stabilizer = .2f),
        BrushDefinition("ink-technical", "Technical Ink", 7f, 1f, .16f, .92f, 1f, .35f, .72f, .25f, .02f, stabilizer = .32f),
        BrushDefinition("ink-taper", "Taper Ink", 11f, 1f, .11f, .9f, .94f, .55f, .9f, .48f, .01f, stabilizer = .4f),
        BrushDefinition("marker-soft", "Soft Marker", 28f, .48f, .22f, .58f, .72f, .1f, .4f, .2f, .04f, stabilizer = .08f),
        BrushDefinition("airbrush", "Airbrush", 70f, .22f, .08f, .15f, .42f, .05f, .28f, .6f, .18f, stabilizer = .04f),
        BrushDefinition("calligraphy", "Calligraphy", 18f, .95f, .1f, .9f, .96f, .82f, .62f, .34f, .03f, stabilizer = .25f),
        BrushDefinition("charcoal", "Charcoal", 34f, .38f, .16f, .38f, .58f, .2f, .34f, .52f, .32f, textureId = "charcoal-grain", stabilizer = .05f),
        BrushDefinition("watercolor", "Watercolor", 42f, .24f, .06f, .22f, .36f, .08f, .45f, .72f, .22f, textureId = "paper-coldpress", stabilizer = .02f),
    )
}
