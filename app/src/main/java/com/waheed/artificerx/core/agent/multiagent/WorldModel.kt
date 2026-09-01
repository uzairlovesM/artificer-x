package com.waheed.artificerx.core.agent.multiagent

import kotlinx.serialization.Serializable

/**
 * A named character established in a project, tracked so later turns
 * (possibly handled by a different [AgentRole], possibly days later)
 * keep depicting the same character consistently rather than the LLM
 * silently reinventing their appearance each time.
 */
@Serializable
data class EstablishedCharacter(
    val name: String,
    /** Free-text visual traits: "orange fur, one torn ear, green eyes" —
     *  deliberately unstructured since this is meant to be dropped
     *  straight into a system prompt, not parsed. */
    val traits: String,
)

/**
 * Cross-turn persistent context for one project (Section "World Model /
 * persistent scene continuity"). This is intentionally a flat,
 * human-readable summary rather than a structured scene graph with
 * coordinates — the consumer is an LLM system prompt, and free-text
 * bullet points are both cheaper to maintain accurately (via the
 * [AgentRole.ARCHIVIST] role) and more robust to partial/approximate
 * information than a rigid schema would be.
 *
 * One WorldModel belongs to one project (Section "AI Art Project" —
 * see [com.waheed.artificerx.domain.model] project models) and is
 * loaded/saved by [WorldModelStore].
 */
@Serializable
data class WorldModel(
    val projectId: String,
    val establishedStyle: String? = null,
    val establishedPalette: String? = null,
    val establishedSetting: String? = null,
    val characters: List<EstablishedCharacter> = emptyList(),
    /** Free-text notes the Archivist role couldn't cleanly fit into the
     *  structured fields above — kept so information is never silently
     *  dropped just because it doesn't fit a category. */
    val additionalNotes: String? = null,
    val lastUpdatedEpochMillis: Long = System.currentTimeMillis(),
) {
    val isEmpty: Boolean
        get() =
            establishedStyle == null &&
                establishedPalette == null &&
                establishedSetting == null &&
                characters.isEmpty() &&
                additionalNotes == null

    /** Renders this World Model as a compact block suitable for
     *  inserting directly into a system prompt. Returns null when there
     *  is nothing established yet, so callers can skip the section
     *  entirely on a project's first turn instead of injecting an empty
     *  "World Model:" header that wastes tokens and mildly confuses the
     *  model about whether it should expect content there. */
    fun toPromptBlock(): String? {
        if (isEmpty) return null
        val lines =
            buildList {
                establishedStyle?.let { add("- Style: $it") }
                establishedPalette?.let { add("- Palette: $it") }
                establishedSetting?.let { add("- Setting: $it") }
                characters.forEach { add("- Character \"${it.name}\": ${it.traits}") }
                additionalNotes?.let { add("- Notes: $it") }
            }
        return "World Model (established continuity for this project):\n" + lines.joinToString("\n")
    }
}
