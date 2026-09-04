package com.waheed.artificerx.runtime.capability

/** Process-local index of the advanced capabilities exposed to the agent planner. */
class AdvancedCapabilityCatalog {
    private val capabilities = linkedSetOf<String>()

    fun register(id: String) { if (id.isNotBlank()) capabilities += id }
    fun registerAll(ids: Iterable<String>) = ids.forEach(::register)
    fun contains(id: String): Boolean = id in capabilities
    fun all(): Set<String> = capabilities.toSet()

    fun registerDefaults() {
        registerAll(listOf(
            "vision.inspect", "vision.compare", "vision.detect", "vision.reference",
            "research.search", "research.fetch", "research.cross_check",
            "canvas.scene", "canvas.draw", "canvas.edit", "canvas.inspect",
            "brush.engine", "brush.dynamics", "ruler.perspective", "ruler.radial",
            "layer.stack", "layer.mask", "layer.clipping", "layer.alpha_lock",
            "artifact.publish", "artifact.verify", "model.import", "model.local",
            "agent.plan", "agent.verify", "agent.repair", "agent.remember",
            "automation.workflow", "runtime.extension", "runtime.tool_generation",
            "repository.project", "repository.artifact", "terminal.private"
        ))
    }
}
