package com.waheed.artificerx.core.agent

/**
 * Plans continuation turns when a provider stops because its own output
 * ceiling was reached. This preserves the user's request for maximum output
 * without pretending a remote API has infinite context/output capacity.
 */
object LongOutputContinuationPlanner {
    private val continuationReasons = setOf("length", "max_tokens", "max_output_tokens", "token_limit")

    fun shouldContinue(finishReason: String?): Boolean =
        finishReason?.lowercase()?.let(continuationReasons::contains) == true

    fun prompt(previousTail: String): String =
        "Continue exactly where you stopped. Do not restart or summarize. Preserve all " +
            "formatting and tool-state. Continue until the task is genuinely complete.\n" +
            "Previous tail:\n" + previousTail.takeLast(4000)
}
