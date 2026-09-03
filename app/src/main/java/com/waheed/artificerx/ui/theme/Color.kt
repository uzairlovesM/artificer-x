package com.waheed.artificerx.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ARTIFICER-X brand palette — dark luxury glassmorphism with gold accents
 * on a deep purple base. Every color here is intentional, not a Material
 * default: this app lives on a canvas-heavy dark UI (Section 111 Mobile UI,
 * Section 208 Accessibility contrast requirement), so contrast ratios
 * against PurpleBase00/PurpleBase01 were chosen to stay readable at
 * small sizes on OLED panels while keeping the "ultra pro max" feel.
 *
 * Core brand
 */
val GoldPrimary = Color(0xFFD97757)
val GoldPrimaryDim = Color(0xFFA4513A)
val GoldPrimaryContainer = Color(0xFF3A241F)
val GoldAccentSoft = Color(0xFFE3A389)

val PurpleBase00 = Color(0xFF1F1D1A) // deepest background
val PurpleBase01 = Color(0xFF272522) // surface
val PurpleBase02 = Color(0xFF302D28) // elevated surface / cards
val PurpleBase03 = Color(0xFF39352F) // highest elevation / dialogs
val PurpleAccent = Color(0xFF9A6B5B)
val PurpleAccentDim = Color(0xFF765146)

// ── Glassmorphism surfaces (semi-transparent overlays) ──
val GlassSurfaceLight = Color(0x1FFFFFFF) // 12% white overlay
val GlassSurfaceMedium = Color(0x33FFFFFF) // 20% white overlay
val GlassBorder = Color(0x33FFD700) // 20% gold border
val GlassScrimDark = Color(0xCC0D0517) // 80% near-black scrim

// ── Semantic / state colors ──
val SuccessGreen = Color(0xFF3DDC97)
val WarningAmber = Color(0xFFFFB020)
val ErrorRed = Color(0xFFFF5C7C)
val InfoBlue = Color(0xFF5CA8FF)

// ── Quality Gate / Agent status accent colors (Section 36/156) ──
val QualityPass = Color(0xFF3DDC97)
val QualityWarn = Color(0xFFFFB020)
val QualityFail = Color(0xFFFF5C7C)
val AgentThinking = Color(0xFF9A6B5B)
val AgentToolCalling = Color(0xFFD97757)
val AgentIdle = Color(0xFF6B6480)

// ── Canvas / layer panel neutrals ──
val CanvasCheckerLight = Color(0xFF2A1F3D)
val CanvasCheckerDark = Color(0xFF1F1530)
val LayerPanelBackground = Color(0xFF150A24)
val OnGoldText = Color(0xFF1F1D1A)
val OnPurpleText = Color(0xFFF5F0FF)
val OnPurpleTextMuted = Color(0xFFB8ADD1)

// ── Neutral scale for text/icons on dark surfaces ──
val NeutralWhite100 = Color(0xFFFFFFFF)
val NeutralWhite87 = Color(0xDEFFFFFF)
val NeutralWhite60 = Color(0x99FFFFFF)
val NeutralWhite38 = Color(0x61FFFFFF)
val NeutralWhite12 = Color(0x1FFFFFFF)
