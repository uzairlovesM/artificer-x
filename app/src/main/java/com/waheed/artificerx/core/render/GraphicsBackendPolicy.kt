package com.waheed.artificerx.core.render

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device-level graphics capability matrix. It never assumes that a modern API
 * implies a working vendor implementation: each backend is treated as a
 * capability and a fallback, not as a promise.
 */
@Singleton
class GraphicsBackendPolicy @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val packageManager = context.packageManager

    val hasVulkan = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)

    val hasAgsl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val hasAgslCompositing = Build.VERSION.SDK_INT >= 36

    /**
     * Skiko is provisioned as a lazy backend. Loading the native library is
     * deliberately deferred to [SkiaRasterEngine] so app startup never pays
     * for a graphics path the current session does not need.
     */
    fun chooseOffscreenBackend(
        widthPx: Int,
        heightPx: Int,
        interactive: Boolean,
        complexBlend: Boolean,
    ): OffscreenBackend {
        val pixels = widthPx.toLong() * heightPx.toLong()
        return when {
            !interactive && pixels >= 1_000_000L -> OffscreenBackend.SKIA
            !interactive && complexBlend && pixels >= 262_144L -> OffscreenBackend.SKIA
            else -> OffscreenBackend.ANDROID_CANVAS
        }
    }

    fun describe(): Map<String, Any> = mapOf(
        "api" to Build.VERSION.SDK_INT,
        "vulkanFeature" to hasVulkan,
        "agsl" to hasAgsl,
        "agslCompositing" to hasAgslCompositing,
    )
}

enum class OffscreenBackend {
    ANDROID_CANVAS,
    SKIA,
}
