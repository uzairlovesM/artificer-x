package com.waheed.artificerx.ai.drawing

import com.waheed.artificerx.ai.vision.VisionObservation
import com.waheed.artificerx.art.quality.DrawingQualityGate

data class DrawingIteration(val index:Int,val observation:VisionObservation,val score:Float,val repairs:List<String>)
class DrawingAgentLoop(private val gate:DrawingQualityGate) {
    fun evaluate(iteration:Int,observation:VisionObservation,requiredObjects:Int,target:Float):DrawingIteration {
        val result=gate.evaluate(observation,requiredObjects,target)
        return DrawingIteration(iteration,observation,result.score,result.failures.map { "repair:$it" })
    }
}
