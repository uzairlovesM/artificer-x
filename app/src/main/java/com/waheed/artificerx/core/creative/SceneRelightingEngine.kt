package com.waheed.artificerx.core.creative

import com.waheed.artificerx.domain.model.LayerBlendMode
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real lighting-capability tool backing relight_scene. Unlike
 * SceneCompositionEngine (which builds a scene from scratch),
 * this ADDS two full-canvas overlay layers on top of whatever
 * already exists — it never touches existing pixels — so it works
 * equally on an AI-composed scene, a hand-drawn piece, or an
 * imported reference image.
 *
 * The technique: a warm SCREEN-blend directional gradient (brightens
 * toward the light source direction, has no effect on already-dark
 * pixels near black) paired with a cool MULTIPLY-blend gradient from
 * the opposite direction (darkens the far side, has no effect on
 * already-light pixels near white). This is the standard two-layer
 * digital-painting relight trick — SCREEN and MULTIPLY are exactly
 * the blend modes CanvasCompositor already implements natively via
 * PorterDuff (see CanvasCompositor.blendModeToPorterDuff), so this
 * needed no new pixel-blending code, only new layer/gradient calls
 * using tools that already exist.
 */
@Singleton
class SceneRelightingEngine
    @Inject
    constructor() {
        data class MoodPalette(val lightHex: String, val shadowHex: String)

        /** Named moods map to real, distinct color pairs rather than one
         *  fixed warm/cool pair, so "sunset", "moonlight", "horror",
         *  "neon" etc. produce genuinely different results, not just a
         *  brightness change. Unrecognized moods fall back to a neutral
         *  warm-key/cool-shadow pair (the most common photographic
         *  default) rather than failing the tool call. */
        private fun paletteForMood(mood: String): MoodPalette =
            when (mood.trim().lowercase()) {
                "sunset", "golden_hour", "golden hour" -> MoodPalette(lightHex = "#FFB347", shadowHex = "#2B1B4D")
                "moonlight", "night", "moody" -> MoodPalette(lightHex = "#9FC2FF", shadowHex = "#050A1A")
                "horror", "eerie" -> MoodPalette(lightHex = "#7CFF8C", shadowHex = "#1A0022")
                "neon", "cyberpunk" -> MoodPalette(lightHex = "#FF4FD8", shadowHex = "#00121A")
                "candlelight", "warm", "cozy" -> MoodPalette(lightHex = "#FFC773", shadowHex = "#241205")
                "overcast", "cold", "cool" -> MoodPalette(lightHex = "#D9E6F2", shadowHex = "#1B2330")
                "dawn", "morning" -> MoodPalette(lightHex = "#FFD9A0", shadowHex = "#232B4D")
                else -> MoodPalette(lightHex = "#FFE3B0", shadowHex = "#141826")
            }

        /** [directionDegrees]: standard gradient-angle convention already
         *  used by apply_gradient — 0=light from the left, 90=light from
         *  above, 180=light from the right, 270=light from below.
         *  [intensity] 0..1 controls both gradients' peak alpha (default
         *  0.55, a visible-but-not-overpowering relight); the far side of
         *  the canvas from each gradient naturally fades toward
         *  transparent since it's a two-stop linear gradient ending in a
         *  fully transparent version of the same color. */
        suspend fun relight(
            vm: StudioViewModel,
            directionDegrees: Float,
            mood: String,
            intensity: Float?,
        ): String {
            val palette = paletteForMood(mood)
            val peakAlpha = (intensity ?: 0.55f).coerceIn(0.1f, 1f)
            val width = vm.state.value.canvasWidthPx
            val height = vm.state.value.canvasHeightPx

            val lightAlphaHex = alphaHexPrefix(peakAlpha)
            val lightTransparentHex = "#00" + palette.lightHex.removePrefix("#")
            val lightOpaqueHex = lightAlphaHex + palette.lightHex.removePrefix("#")

            vm.addNamedLayer("Relight — Key Light")
            val keyLayerId = vm.state.value.activeLayerId
            applyDirectionalGradient(
                vm = vm,
                width = width,
                height = height,
                startHex = lightOpaqueHex,
                endHex = lightTransparentHex,
                angleDegrees = directionDegrees,
            )
            if (keyLayerId != null) {
                vm.setLayerOpacity(keyLayerId, 1f) // gradient alpha already carries the falloff; layer stays fully "on"
            }
            setBlendModeOnActiveLayer(vm, keyLayerId, LayerBlendMode.SCREEN)

            val shadowAlphaHex = alphaHexPrefix((peakAlpha * 0.8f).coerceIn(0.08f, 1f))
            val shadowTransparentHex = "#00" + palette.shadowHex.removePrefix("#")
            val shadowOpaqueHex = shadowAlphaHex + palette.shadowHex.removePrefix("#")
            val oppositeDirection = (directionDegrees + 180f).mod(360f)

            vm.addNamedLayer("Relight — Shadow")
            val shadowLayerId = vm.state.value.activeLayerId
            applyDirectionalGradient(
                vm = vm,
                width = width,
                height = height,
                startHex = shadowOpaqueHex,
                endHex = shadowTransparentHex,
                angleDegrees = oppositeDirection,
            )
            setBlendModeOnActiveLayer(vm, shadowLayerId, LayerBlendMode.MULTIPLY)

            return "Relit scene from ${directionDegrees.toInt()}° with '$mood' mood " +
                "(key light ${palette.lightHex}, shadow ${palette.shadowHex}, intensity ${"%.2f".format(peakAlpha)}) " +
                "using 2 new overlay layers — existing artwork pixels were not modified."
        }

        /** Applies a full-canvas linear gradient (real apply_gradient
         *  renderer, via StudioViewModel.applyGradientToActiveLayer) to
         *  whichever layer is currently active — used immediately after
         *  addNamedLayer creates the key-light / shadow overlay layer
         *  above, so the gradient always lands on the layer this engine
         *  just made, never on a layer left active from an earlier,
         *  unrelated tool call. */
        private fun applyDirectionalGradient(
            vm: StudioViewModel,
            width: Int,
            height: Int,
            startHex: String,
            endHex: String,
            angleDegrees: Float,
        ) {
            vm.applyGradientToActiveLayer(
                gradientType = "linear",
                startColorHex = startHex,
                endColorHex = endHex,
                x = 0f,
                y = 0f,
                width = width.toFloat(),
                height = height.toFloat(),
                angleDegrees = angleDegrees,
            )
        }

        private fun setBlendModeOnActiveLayer(
            vm: StudioViewModel,
            layerId: String?,
            mode: LayerBlendMode,
        ) {
            if (layerId != null) {
                vm.setLayerBlendMode(layerId, mode)
            }
        }

        /** Converts 0..1 alpha into a 2-digit uppercase hex prefix
         *  (e.g. 0.55 -> "8C") for building an "#AARRGGBB"-style hex
         *  string, matching CanvasCompositor.safeParseColor's
         *  android.graphics.Color.parseColor expectations. */
        private fun alphaHexPrefix(alpha: Float): String {
            val clamped = (alpha.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255)
            return String.format("%02X", clamped)
        }
    }
