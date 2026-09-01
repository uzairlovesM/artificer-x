package com.waheed.artificerx.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * ARTIFICER-X dark color scheme mapped onto Material3 roles. Every role
 * below is deliberately chosen (not auto-generated from a seed) since
 * the brand is a fixed dark-luxury-glassmorphism identity, not a
 * dynamic-color/Material You app — Section 111's Mobile UI is meant to
 * look identical regardless of the user's wallpaper.
 */
private val ArtificerXDarkColorScheme =
    darkColorScheme(
        primary = GoldPrimary,
        onPrimary = OnGoldText,
        primaryContainer = GoldPrimaryContainer,
        onPrimaryContainer = GoldPrimary,
        secondary = PurpleAccent,
        onSecondary = NeutralWhite100,
        secondaryContainer = PurpleAccentDim,
        onSecondaryContainer = NeutralWhite100,
        tertiary = GoldAccentSoft,
        onTertiary = OnGoldText,
        background = PurpleBase00,
        onBackground = OnPurpleText,
        surface = PurpleBase01,
        onSurface = OnPurpleText,
        surfaceVariant = PurpleBase02,
        onSurfaceVariant = OnPurpleTextMuted,
        surfaceTint = GoldPrimary,
        inverseSurface = NeutralWhite100,
        inverseOnSurface = PurpleBase00,
        inversePrimary = PurpleAccent,
        error = ErrorRed,
        onError = NeutralWhite100,
        errorContainer = Color(0xFF4A1420),
        onErrorContainer = ErrorRed,
        outline = GlassBorder,
        outlineVariant = NeutralWhite12,
        scrim = GlassScrimDark,
    )

/**
 * Extended color roles Material3's ColorScheme doesn't natively carry —
 * quality-gate status, agent-state indicators, canvas checker pattern,
 * glass-surface tokens. Exposed via a CompositionLocal so any Composable
 * can pull them the same way it pulls MaterialTheme.colorScheme, without
 * every call site needing to import ui.theme.Color directly.
 */
data class ArtificerXExtendedColors(
    val glassSurfaceLight: Color = GlassSurfaceLight,
    val glassSurfaceMedium: Color = GlassSurfaceMedium,
    val glassBorder: Color = GlassBorder,
    val glassScrim: Color = GlassScrimDark,
    val success: Color = SuccessGreen,
    val warning: Color = WarningAmber,
    val info: Color = InfoBlue,
    val qualityPass: Color = QualityPass,
    val qualityWarn: Color = QualityWarn,
    val qualityFail: Color = QualityFail,
    val agentThinking: Color = AgentThinking,
    val agentToolCalling: Color = AgentToolCalling,
    val agentIdle: Color = AgentIdle,
    val canvasCheckerLight: Color = CanvasCheckerLight,
    val canvasCheckerDark: Color = CanvasCheckerDark,
    val layerPanelBackground: Color = LayerPanelBackground,
)

val LocalArtificerXExtendedColors = staticCompositionLocalOf { ArtificerXExtendedColors() }

/**
 * App-wide gradient tokens for the luxury-glassmorphism look — the
 * background wash behind the Studio screen, the header gradient on the
 * onboarding/provider-setup flow, and the glass-panel fill used by
 * GlassSurface below.
 */
object ArtificerXGradients {
    val backgroundWash =
        Brush.verticalGradient(
            colors = listOf(PurpleBase00, Color(0xFF120820), PurpleBase00),
        )

    val heroGoldPurple =
        Brush.linearGradient(
            colors = listOf(GoldPrimary, PurpleAccent),
        )

    val glassPanelFill =
        Brush.verticalGradient(
            colors = listOf(GlassSurfaceMedium, GlassSurfaceLight),
        )

    val agentThinkingPulse =
        Brush.linearGradient(
            colors = listOf(PurpleAccent, AgentThinking, PurpleAccentDim),
        )

    val qualityGateGlow =
        Brush.radialGradient(
            colors = listOf(GoldPrimary.copy(alpha = 0.35f), Color.Transparent),
        )
}

@Composable
fun ArtificerXTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalArtificerXExtendedColors provides ArtificerXExtendedColors(),
    ) {
        MaterialTheme(
            colorScheme = ArtificerXDarkColorScheme,
            typography = ArtificerXTypography,
            shapes = ArtificerXShapes,
            content = content,
        )
    }
}

/** Convenience accessor mirroring MaterialTheme.colorScheme usage. */
object ArtificerXTheme2 {
    val extendedColors: ArtificerXExtendedColors
        @Composable
        get() = LocalArtificerXExtendedColors.current
}

/**
 * Glassmorphism surface modifier: a translucent, blurred, gold-bordered
 * panel matching the brand's glass-card language (Section 111's Studio
 * screen panels, tool palettes, chat bubbles). Real Compose blur
 * (Modifier.blur) requires API 31+ — a solid-tint fallback keeps the
 * layered translucent look on API 26-30 devices (min SDK 26 per stack
 * config) instead of an unsupported blur crashing or silently no-op'ing.
 */
fun Modifier.glassSurface(
    shape: Shape = GlassCardShape,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    borderColor: Color = GlassBorder,
    fillBrush: Brush = ArtificerXGradients.glassPanelFill,
    blurRadius: androidx.compose.ui.unit.Dp = 0.dp,
): Modifier {
    var modifier =
        this
            .clip(shape)
            .background(brush = fillBrush, shape = shape)
            .border(width = borderWidth, color = borderColor, shape = shape)

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && blurRadius > 0.dp) {
        modifier = modifier.blur(radius = blurRadius)
    }

    return modifier
}

/** Solid elevated surface (non-glass) for opaque panels like Settings
 *  list rows where readability matters more than the glass effect. */
fun Modifier.elevatedSurface(
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = PurpleBase02,
): Modifier =
    this
        .clip(shape)
        .background(color = color, shape = shape)
