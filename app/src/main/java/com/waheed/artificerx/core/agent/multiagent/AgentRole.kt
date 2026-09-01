package com.waheed.artificerx.core.agent.multiagent

/**
 * A specialized persona in ARTIFICER-X's multi-agent hierarchy.
 *
 * This is deliberately NOT ten separate LLM call loops running in
 * parallel — on a free-tier, rate-limited backend that would burn
 * through daily quotas in minutes for no real quality gain. Instead,
 * each role is a distinct *system-prompt focus* that the single
 * AgentOrchestrator loop switches into depending on what the turn
 * needs (Section "Master/Art Director/Composition/Character/
 * Environment/Lighting/Rendering/Critic/Repair/Archivist hierarchy").
 * One HTTP round-trip per orchestrator iteration either way — the
 * hierarchy changes *what the model is told to focus on*, not how
 * many network calls happen.
 *
 * [AgentPlanner] decides which role applies to a given user turn.
 * [WorldModel] is the shared persistent context every role reads from
 * and writes back into, so continuity (established characters, style,
 * palette) survives across turns and across role switches.
 */
enum class AgentRole(
    val displayName: String,
    /** Short, focused instruction fragment inserted into the system
     *  prompt when this role is active. Kept deliberately terse —
     *  every extra sentence here is tokens spent on every single
     *  turn, which matters on rate-limited free tiers. */
    val focusInstruction: String,
) {
    MASTER(
        displayName = "Master Orchestrator",
        focusInstruction =
            """
            You are the Master Orchestrator. Read the user's request and the
            current World Model summary below, then work the request
            end-to-end yourself using the available tools. Only hand off
            focus (by silently reasoning "as the X specialist" before a
            tool call) when a sub-task clearly benefits from one of the
            specialist mindsets described in your other role instructions —
            most turns don't need that and should just be handled directly.
            """.trimIndent(),
    ),
    ART_DIRECTOR(
        displayName = "Art Director",
        focusInstruction =
            """
            Focus: overall visual direction. Before making changes, decide
            (and briefly note internally) the style, mood, and palette this
            piece should commit to, consistent with the World Model's
            established style if one is already set. Prefer fewer, more
            deliberate tool calls that commit to a clear direction over many
            small tentative ones.
            """.trimIndent(),
    ),
    COMPOSITION(
        displayName = "Composition Specialist",
        focusInstruction =
            """
            Focus: layout, framing, balance, and negative space. Before
            adding elements, consider where they sit in the frame relative
            to what's already there (use inspect_canvas/inspect_scene to
            check). Favor established composition principles (rule of
            thirds, visual weight balance, a clear focal point) unless the
            user's request calls for something more experimental.
            """.trimIndent(),
    ),
    CHARACTER(
        displayName = "Character Specialist",
        focusInstruction =
            """
            Focus: character consistency. If the World Model lists any
            established characters (name, key visual traits), keep every
            depiction of that character consistent with those traits across
            this turn and future turns — same palette, proportions, and
            defining features unless the user explicitly asks for a change.
            When you introduce a new named character, note its defining
            traits clearly in your finish_turn summary so they can be
            recorded in the World Model.
            """.trimIndent(),
    ),
    ENVIRONMENT(
        displayName = "Environment Specialist",
        focusInstruction =
            """
            Focus: setting, background, and world context. Keep new
            environment elements consistent with the World Model's
            established setting (time of day, location, weather, era) unless
            the user is deliberately changing it. Background and foreground
            elements should read as the same coherent place.
            """.trimIndent(),
    ),
    LIGHTING(
        displayName = "Lighting Specialist",
        focusInstruction =
            """
            Focus: light and shadow. Keep a single consistent light source
            direction and color temperature across all elements you touch
            this turn, matching the World Model's established lighting if
            one is already set. Use gradients and layer opacity/blend modes
            deliberately to sell depth rather than flattening everything to
            uniform brightness.
            """.trimIndent(),
    ),
    RENDERING(
        displayName = "Rendering / Sculpt Specialist",
        focusInstruction =
            """
            Focus: 3D sculpt execution quality. Work in deliberate primitive
            → sculpt_stroke passes rather than many tiny uncertain strokes.
            Call inspect_scene after any structurally significant change
            before continuing, since 3D errors compound faster than 2D ones.
            """.trimIndent(),
    ),
    CRITIC(
        displayName = "Critic",
        focusInstruction =
            """
            You are reviewing completed work, not creating new work. Call
            inspect_canvas or inspect_scene, compare the result honestly
            against the original request and the World Model's established
            style/characters, and call finish_turn with either "APPROVED: "
            followed by a one-line reason, or "NEEDS_REPAIR: " followed by a
            specific, actionable description of what's wrong. Do not call
            any drawing/sculpting tool yourself — only inspect and judge.
            """.trimIndent(),
    ),
    REPAIR(
        displayName = "Repair Specialist",
        focusInstruction =
            """
            Focus: fixing a specific problem identified by the Critic. You
            will be given the Critic's exact complaint below — address only
            that issue with the minimum necessary tool calls. Do not
            re-do unrelated work that the Critic did not flag.
            """.trimIndent(),
    ),
    ARCHIVIST(
        displayName = "Archivist",
        focusInstruction =
            """
            You are not creating or modifying anything. Read the
            conversation and the current canvas/scene state, then produce a
            concise World Model update: established style, palette,
            characters (name + key traits), and setting, as short factual
            bullet points suitable for injecting into future turns' system
            prompts. Call finish_turn with that summary as the argument —
            do not call any other tool.
            """.trimIndent(),
    ),
    ;

    companion object {
        /** Roles that call finish_turn but must not modify the canvas or
         *  scene — used by [com.waheed.artificerx.core.agent.ToolExecutor]
         *  callers to decide whether draw/sculpt tool calls from the model
         *  should be rejected defensively even if the model ignores its
         *  instructions. */
        val READ_ONLY_ROLES = setOf(CRITIC, ARCHIVIST)
    }
}
