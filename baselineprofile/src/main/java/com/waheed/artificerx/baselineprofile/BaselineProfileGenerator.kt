package com.waheed.artificerx.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupAndCanvasJourney() = baselineProfileRule.collect(
        packageName = "com.waheed.artificerx",
        includeInStartupProfile = true,
    ) {
        startActivityAndWait()
        device.waitForIdle()
        device.pressBack()
        device.pressHome()
        startActivityAndWait()
        device.waitForIdle()
    }

    @Test
    fun studioJourney() = baselineProfileRule.collect(
        packageName = "com.waheed.artificerx",
    ) {
        startActivityAndWait()
        device.waitForIdle()
        repeat(3) { device.waitForIdle() }
    }
}
