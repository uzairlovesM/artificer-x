package com.waheed.artificerx.domain.usecase.ai

import com.waheed.artificerx.ai.drawing.DrawingIntent
import com.waheed.artificerx.ai.drawing.SceneCompiler
import com.waheed.artificerx.ai.reasoning.DeepReasoningEngine

data class DrawingPlanResult(val intent: DrawingIntent, val scene: com.waheed.artificerx.ai.drawing.SceneSpec, val missingCapabilities: List<String>)

class BuildDrawingPlanUseCase(private val reasoning: DeepReasoningEngine, private val compiler: SceneCompiler) {
    fun execute(intent: DrawingIntent, capabilities: Set<String>): DrawingPlanResult {
        val plan = reasoning.plan("draw ${intent.subject} in ${intent.environment}", capabilities)
        return DrawingPlanResult(intent, compiler.compile(intent), plan.assumptions)
    }
}
