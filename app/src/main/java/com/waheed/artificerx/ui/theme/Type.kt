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
 * A local system-font fallback chain is attached to every FontFamily so
 * typography never breaks if the device has no Google Play Services
 * (rare, but real on some OEM/region builds) or the font fetch is
 * offline on first cold start.
 */

private val googleFontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

private val soraFontName = GoogleFont("Sora")
private val interFontName = GoogleFont("Inter")

val SoraFontFamily =
    FontFamily(
        GoogleFontFactory(googleFont = soraFontName, fontProvider = googleFontProvider, weight = FontWeight.Light),
        GoogleFontFactory(googleFont = soraFontName, fontProvider = googleFontProvider, weight = FontWeight.Normal),
        GoogleFontFactory(googleFont = soraFontName, fontProvider = googleFontProvider, weight = FontWeight.Medium),
        GoogleFontFactory(googleFont = soraFontName, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
        GoogleFontFactory(googleFont = soraFontName, fontProvider = googleFontProvider, weight = FontWeight.Bold),
        GoogleFontFactory(googleFont = soraFontName, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
        FontFamily.SansSerif,
    )

val InterFontFamily =
    FontFamily(
        GoogleFontFactory(googleFont = interFontName, fontProvider = googleFontProvider, weight = FontWeight.Light),
        GoogleFontFactory(googleFont = interFontName, fontProvider = googleFontProvider, weight = FontWeight.Normal),
        GoogleFontFactory(googleFont = interFontName, fontProvider = googleFontProvider, weight = FontWeight.Medium),
        GoogleFontFactory(googleFont = interFontName, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
        GoogleFontFactory(googleFont = interFontName, fontProvider = googleFontProvider, weight = FontWeight.Bold),
        FontFamily.SansSerif,
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
