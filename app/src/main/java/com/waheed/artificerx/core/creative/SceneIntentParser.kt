package com.waheed.artificerx.core.creative

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneIntentParser @Inject constructor() {
    fun parse(request: String): SceneBlueprint {
        val q = request.lowercase(Locale.ROOT)
        val style = when {
            "anime" in q -> "anime"
            "manga" in q -> "manga"
            "semi realistic" in q || "semirealistic" in q -> "semi-realistic"
            else -> "stylized"
        }
        val subject = when {
            "room" in q || "bedroom" in q || "bed room" in q -> "anime room"
            "classroom" in q -> "anime classroom"
            "kitchen" in q -> "anime kitchen"
            "studio" in q -> "anime studio"
            "street" in q || "road" in q -> "anime street"
            else -> request.trim().take(160).ifBlank { "stylized scene" }
        }
        val warm = listOf("cozy", "warm", "sunset", "golden").any { it in q }
        val night = listOf("night", "evening", "moonlight").any { it in q }
        val palette = when {
            night -> PaletteSpec(wall="#252B3E", floor="#4A3F49", ceiling="#343B50", trim="#171A27", shadow="#1C2133", light="#9DB7FF", accent="#B77CFF", foliage="#46675C")
            warm -> PaletteSpec(wall="#F0E0D0", floor="#B56F4A", ceiling="#FFF5E9", trim="#744A3D", shadow="#8C6172", light="#FFD38A", accent="#C77D9A", foliage="#5D866C")
            else -> PaletteSpec()
        }
        return SceneBlueprint(
            subject=subject,
            style=style,
            camera=CameraSpec(perspective=if ("two point" in q) "two_point" else "one_point", horizonRatio=0.42f, vanishingXRatio=0.52f),
            palette=palette,
            quality=when { "rough" in q -> 1; "detailed" in q || "high detail" in q -> 4; else -> 3 },
            details = listOf(
                SceneDetail("window",0.18f,0.24f,1.0f),
                SceneDetail("desk",0.50f,0.60f,1.0f),
                SceneDetail("chair",0.64f,0.70f,0.82f),
                SceneDetail("plant",0.84f,0.67f,0.75f),
                SceneDetail("bed",0.24f,0.67f,1.0f),
                SceneDetail("lamp",0.74f,0.46f,0.70f),
                SceneDetail("rug",0.48f,0.78f,1.0f),
            ).filter { detail -> when (subject) { "anime room" -> true; "anime classroom" -> detail.kind in setOf("window","desk","chair","lamp","rug"); else -> true } }
        )
    }
}
