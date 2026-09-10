package com.waheed.artificerx.core.architecture

/** Canonical boundaries used by the 1.0 foundation. */
enum class RuntimeBoundary {
    DOMAIN,
    PROJECT,
    CANVAS,
    DRAWING,
    RENDERING,
    COLOR,
    MEDIA,
    AI,
    AGENT,
    MEMORY,
    TOOL,
    RESEARCH,
    AUTOMATION,
    PLUGIN,
    DATA,
    PLATFORM,
    SETTINGS,
    UI,
    DIAGNOSTICS,
}

enum class SideEffect {
    PURE,
    MEMORY,
    FILE_READ,
    FILE_WRITE,
    NETWORK,
    PROCESS,
    DEVICE,
}

data class CapabilityContract(
    val id: String,
    val boundary: RuntimeBoundary,
    val sideEffects: Set<SideEffect> = setOf(SideEffect.PURE),
    val reversible: Boolean = false,
    val idempotent: Boolean = false,
) {
    init {
        require(id.isNotBlank())
        require(sideEffects.isNotEmpty())
    }
}

data class BoundaryViolation(
    val owner: RuntimeBoundary,
    val dependency: RuntimeBoundary,
    val reason: String,
)

object ArchitectureRules {
    private val allowed: Map<RuntimeBoundary, Set<RuntimeBoundary>> = mapOf(
        RuntimeBoundary.DOMAIN to setOf(RuntimeBoundary.DOMAIN),
        RuntimeBoundary.PROJECT to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.PROJECT, RuntimeBoundary.DATA),
        RuntimeBoundary.CANVAS to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.PROJECT, RuntimeBoundary.CANVAS, RuntimeBoundary.DRAWING, RuntimeBoundary.RENDERING),
        RuntimeBoundary.DRAWING to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.CANVAS, RuntimeBoundary.DRAWING),
        RuntimeBoundary.RENDERING to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.CANVAS, RuntimeBoundary.RENDERING, RuntimeBoundary.COLOR),
        RuntimeBoundary.COLOR to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.COLOR),
        RuntimeBoundary.MEDIA to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.DATA, RuntimeBoundary.MEDIA),
        RuntimeBoundary.AI to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.PROJECT, RuntimeBoundary.CANVAS, RuntimeBoundary.AI, RuntimeBoundary.TOOL, RuntimeBoundary.MEDIA),
        RuntimeBoundary.AGENT to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.PROJECT, RuntimeBoundary.CANVAS, RuntimeBoundary.AI, RuntimeBoundary.AGENT, RuntimeBoundary.TOOL, RuntimeBoundary.MEMORY, RuntimeBoundary.DATA),
        RuntimeBoundary.MEMORY to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.PROJECT, RuntimeBoundary.DATA, RuntimeBoundary.MEMORY),
        RuntimeBoundary.TOOL to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.PROJECT, RuntimeBoundary.CANVAS, RuntimeBoundary.MEDIA, RuntimeBoundary.TOOL, RuntimeBoundary.PLATFORM, RuntimeBoundary.DATA),
        RuntimeBoundary.RESEARCH to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.AI, RuntimeBoundary.RESEARCH, RuntimeBoundary.DATA),
        RuntimeBoundary.AUTOMATION to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.TOOL, RuntimeBoundary.DATA, RuntimeBoundary.AUTOMATION),
        RuntimeBoundary.PLUGIN to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.TOOL, RuntimeBoundary.PLUGIN, RuntimeBoundary.DATA),
        RuntimeBoundary.DATA to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.DATA),
        RuntimeBoundary.PLATFORM to setOf(RuntimeBoundary.PLATFORM, RuntimeBoundary.DATA),
        RuntimeBoundary.UI to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.PROJECT, RuntimeBoundary.CANVAS, RuntimeBoundary.DRAWING, RuntimeBoundary.AI, RuntimeBoundary.AGENT, RuntimeBoundary.MEDIA, RuntimeBoundary.AUTOMATION, RuntimeBoundary.PLUGIN, RuntimeBoundary.SETTINGS, RuntimeBoundary.DIAGNOSTICS),
        RuntimeBoundary.SETTINGS to setOf(RuntimeBoundary.DOMAIN, RuntimeBoundary.DATA, RuntimeBoundary.SETTINGS),
        RuntimeBoundary.DIAGNOSTICS to RuntimeBoundary.values().toSet(),
    )

    fun isAllowed(owner: RuntimeBoundary, dependency: RuntimeBoundary): Boolean =
        dependency in (allowed[owner] ?: emptySet())

    fun validate(owner: RuntimeBoundary, dependencies: Collection<RuntimeBoundary>): List<BoundaryViolation> =
        dependencies.filterNot { isAllowed(owner, it) }.map {
            BoundaryViolation(owner, it, "dependency is outside the boundary allow-list")
        }
}
