package com.waheed.artificerx.core.agent

/**
 * User-selected "maximum / no artificial application cap" execution profile.
 *
 * The app intentionally does not impose a small token, tool-call, iteration,
 * or wall-clock budget. Provider/model limits and Android resource limits still
 * exist outside the application's control, so this object describes the
 * application's policy rather than promising physically infinite generation.
 */
object UnboundedExecutionPolicy {
    const val MAX_APPLICATION_ITERATIONS: Int = Int.MAX_VALUE
    const val MAX_APPLICATION_TOOL_CALLS: Int = Int.MAX_VALUE

    fun isUnlimitedRequested(): Boolean = true

    fun outputTokenParameter(): Int? = null
}
