package com.waheed.artificerx.core.ai

import com.waheed.artificerx.core.terminal.AndroidToolchainManager
import com.waheed.artificerx.domain.model.BrushType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiCapabilityPlanner @Inject constructor(private val toolchain: AndroidToolchainManager) {
    fun environmentBrief(): String {
        val s = toolchain.inspect()
        return buildString {
            append("Android ${s.androidRelease}; ")
            append("SDK=${s.sdkRoot ?: "unresolved"}; ")
            append("platforms=${s.platforms.joinToString()}; ")
            append("NDK=${s.ndks.joinToString()}; ")
            append("CMake=${s.cmake.joinToString()}; ")
            append("JDK=${s.javaVersion}; ")
            append("git=${s.gitAvailable}; adb=${s.adbAvailable}; ")
            append("brush engines=${BrushType.entries.joinToString()}")
        }
    }
}
