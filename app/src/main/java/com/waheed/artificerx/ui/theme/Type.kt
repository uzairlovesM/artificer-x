@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.waheed.artificerx.ui.theme

// Note: `Font` is imported below as an alias (GoogleFontFactory). Both
// androidx.compose.ui.text.googlefonts.Font and androidx.compose.ui.text.
// font.Font are top-level `Font(...)` factory functions that return the
// same `Font` type; an unqualified import of the googlefonts one let the
// resolver silently prefer the wrong (local-resource) overload on some
// Kotlin/K2 versions, producing "Too many arguments for constructor
// (canLoadSynchronously: Boolean)" errors. The alias makes the
// Google-Fonts overload unambiguous at every call site below.
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.waheed.artificerx.R
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFactory
import androidx.compose.ui.text.font.Font as PlatformFont

// NOTE: GoogleFont / the Google-Fonts-aware Font(...) overload and its
// provider type live in androidx.compose.ui.text.googlefonts (a separate
// package from the regular androidx.compose.ui.text.font.Font used for
// local/resource fonts) — this is where "FontProvider"/"GoogleFont"
// unresolved-reference and "protected FontFamily constructor" errors
// actually come from when someone imports from .font instead of
// .googlefonts. GoogleFont.Provider is the correct provider type here,
// not a standalone FontProvider class.

/**
 * ARTIFICER-X typography — two-family system fetched at runtime via the
 * Downloadable Fonts API (Google Play Services font provider), so no
 * font binary files are bundled in the APK: the certificate list and
 * provider authority below are the standard AndroidX values that ship
 * with androidx.compose.ui:ui-text-google-fonts, resolved from Google's
 * font CDN on first use and cached by the OS afterward.
 *
 *  - Display/heading family: "Sora" — geometric, premium, matches the
 *    "ultra pro max" luxury-app feel for titles, agent status headers,
 *    the Studio Mode top bar.
 *  - Body/UI family: "Inter" — the highest-legibility variable UI font
 *    available on the provider, used for chat bubbles, tool-call logs,
 *    settings, and anywhere dense text needs to stay readable on a
 *    dark canvas-heavy screen (Section 208 Accessibility contrast).
 *
 * No local font files are bundled in res/font — every weight here is a
 * downloadable Google Font (FontLoadingStrategy.Async). If the Google
 * Play Services font provider is unavailable (rare, but real on some
 * OEM/region builds) or the fetch fails/times out on first cold start,
 * Compose renders the text with the platform's default typeface rather
 * than crashing — but there is no branded fallback typeface in that
 * case. If a hard guarantee of the Sora/Inter look offline is ever
 * needed, add a bundled `Font(resId = R.font.sora_regular, ...)` etc.
 * as an extra chain entry per weight (see "Work with fonts" — Compose
 * tries each Font in a FontFamily's list in order until one loads).
 */

private val googleFontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

private val soraFontName = GoogleFont("Sora")
private val interFontName = GoogleFont("Inter")

// Each Font is built through an explicitly-typed helper (returns
// PlatformFont — androidx.compose.ui.text.font.Font, the actual return
// type of both the googlefonts and platform Font(...) factories, since
// GoogleFontFactory's result is just a Font instance backed by a
// downloadable-font AndroidFont under the hood) instead of being
// constructed inline inside the FontFamily(...) vararg call. This
// sidesteps a K2 overload-resolution failure where the compiler bound
// these calls to FontFamily's own internal
// `constructor(canLoadSynchronously: Boolean)` instead of the intended
// top-level `FontFamily(vararg fonts: Font)` factory function — the
// exact "Too many arguments for constructor(canLoadSynchronously:
// Boolean)" error seen in CI. Giving the compiler an unambiguous,
// pre-typed `Font` value at each call site (rather than an inline call
// it has to resolve itself in vararg position) forces it onto the
// correct factory every time.
private fun googleFont(
    font: GoogleFont,
    weight: FontWeight,
): PlatformFont = GoogleFontFactory(googleFont = font, fontProvider = googleFontProvider, weight = weight)

val SoraFontFamily: FontFamily =
    FontFamily(
        googleFont(soraFontName, FontWeight.Light),
        googleFont(soraFontName, FontWeight.Normal),
        googleFont(soraFontName, FontWeight.Medium),
        googleFont(soraFontName, FontWeight.SemiBold),
        googleFont(soraFontName, FontWeight.Bold),
        googleFont(soraFontName, FontWeight.ExtraBold),
    )

val InterFontFamily: FontFamily =
    FontFamily(
        googleFont(interFontName, FontWeight.Light),
        googleFont(interFontName, FontWeight.Normal),
        googleFont(interFontName, FontWeight.Medium),
        googleFont(interFontName, FontWeight.SemiBold),
        googleFont(interFontName, FontWeight.Bold),
    )

/** Monospace family for the Live Agent Log (Section 92) and tool-call
 *  JSON preview — needs fixed-width alignment for readable diffs. */
val MonoFontFamily = FontFamily.Monospace

val ArtificerXTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = SoraFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = SoraFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = SoraFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = SoraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = SoraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = SoraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = SoraFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

/** Live Agent Log entry style (Section 92) — monospace, tight, dim. */
val AgentLogTextStyle =
    TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    )
