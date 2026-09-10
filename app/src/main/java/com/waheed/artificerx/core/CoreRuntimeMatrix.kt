package com.waheed.artificerx.core

import com.waheed.artificerx.ai.AiPipelineRuntime
import com.waheed.artificerx.art.ArtRuntime
import com.waheed.artificerx.automation.AutomationRuntime
import com.waheed.artificerx.core.foundation.SubsystemHealth
import com.waheed.artificerx.data.DataIntegrityRuntime
import com.waheed.artificerx.di.DiRuntime
import com.waheed.artificerx.diagnostics.DeepDiagnosticRuntime
import com.waheed.artificerx.domain.DomainRuntime
import com.waheed.artificerx.drawing.DrawingRuntime
import com.waheed.artificerx.init.StartupReadiness
import com.waheed.artificerx.research.ResearchRuntime
import com.waheed.artificerx.runtime.RuntimeGuardRuntime
import com.waheed.artificerx.ui.UiRuntime
import com.waheed.artificerx.util.UtilityRuntime

/** Single read-only health facade spanning the app's major bounded areas. */
class CoreRuntimeMatrix {
    private val ai = AiPipelineRuntime()
    private val art = ArtRuntime()
    private val automation = AutomationRuntime()
    private val data = DataIntegrityRuntime()
    private val di = DiRuntime()
    private val diagnostics = DeepDiagnosticRuntime()
    private val domain = DomainRuntime()
    private val drawing = DrawingRuntime()
    private val init = StartupReadiness()
    private val research = ResearchRuntime()
    private val runtime = RuntimeGuardRuntime()
    private val ui = UiRuntime()
    private val util = UtilityRuntime()

    fun snapshot(): List<SubsystemHealth> = listOf(
        ai.inspect(), art.inspect(), automation.inspect(), data.inspect(), di.inspect(),
        diagnostics.inspect(), domain.inspect(), drawing.inspect(),
        init.summarize(emptyList()), research.inspect(), runtime.inspect(), ui.inspect(), util.inspect(),
    )

    fun readiness(): Double = snapshot().map { it.readiness }.average()
}
