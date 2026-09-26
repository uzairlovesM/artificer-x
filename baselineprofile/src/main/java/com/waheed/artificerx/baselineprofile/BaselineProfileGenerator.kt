package com.waheed.artificerx.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
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

        // Fresh installs start in onboarding. Skip it so the profile captures
        // the production Studio/Canvas path rather than only the setup UI.
        device.wait(Until.findObject(By.text("Skip for now")), 5_000)?.click()
        device.waitForIdle()

        // This accessibility description is emitted by the real canvas
        // renderer. Waiting for it makes the journey behavior-driven.
        device.wait(Until.findObject(By.descStartsWith("Canvas artwork")), 5_000)
        repeat(3) { device.waitForIdle() }

        device.pressHome()
        startActivityAndWait()
        device.wait(Until.findObject(By.descStartsWith("Canvas artwork")), 5_000)
    }

    @Test
    fun studioJourney() = baselineProfileRule.collect(
        packageName = "com.waheed.artificerx",
    ) {
        startActivityAndWait()
        device.waitForIdle()
        device.wait(Until.findObject(By.text("Skip for now")), 2_000)?.click()
        device.wait(Until.findObject(By.descStartsWith("Canvas artwork")), 5_000)
        repeat(6) { device.waitForIdle() }
    }
}
