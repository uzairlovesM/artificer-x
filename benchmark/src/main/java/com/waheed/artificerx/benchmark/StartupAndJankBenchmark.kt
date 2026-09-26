package com.waheed.artificerx.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupAndJankBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupAndFrameTiming() = benchmarkRule.measureRepeated(
        packageName = "com.waheed.artificerx",
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = {
            pressHome()
        },
    ) {
        startActivityAndWait()
        device.waitForIdle()
        device.wait(Until.findObject(By.text("Skip for now")), 2_000)?.click()
        device.wait(Until.findObject(By.descStartsWith("Canvas artwork")), 5_000)
        repeat(5) { device.waitForIdle() }
    }
}
