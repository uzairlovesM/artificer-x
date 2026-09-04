package com.waheed.artificerx.art.quality

data class RepairStrategy(val defect: String, val priority: Int, val toolIds: List<String>, val expectedChange: String)
class RepairStrategyPlanner {
    fun plan(defects: List<String>): List<RepairStrategy> = defects.distinct().mapIndexed { i, d -> RepairStrategy(d, i+1, when(d){"perspective"->listOf("ruler.perspective", "geometry.transform");"composition"->listOf("canvas.rearrange", "layer.transform");"object completeness"->listOf("scene.add_object", "brush.engine");else->listOf("vision.reinspect", "canvas.refine")}, "defect $d reduced") }
}
