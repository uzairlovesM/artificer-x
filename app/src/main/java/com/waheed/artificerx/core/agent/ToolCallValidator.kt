package com.waheed.artificerx.core.agent

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Validates a tool call's raw arguments against the exact JSON schema
 * ToolRegistry already declares for that tool — no separately
 * hand-maintained "required fields" list to drift out of sync, since
 * this reads the same schema object the LLM itself was sent.
 *
 * Why this exists: ToolCallParser used to silently default every
 * missing/malformed field (missing layer_id -> "", missing name ->
 * "New Layer", a garbled color -> quietly ignored). That's safe in the
 * sense that nothing crashes, but it means the model never learns it
 * made a mistake — the wrong thing just happens, or nothing visibly
 * happens, and the model moves on believing it succeeded. Returning a
 * specific validation error instead lets the model actually self-
 * correct on its next turn, which is the entire point of a tool-
 * calling repair loop.
 *
 * Deliberately conservative about what counts as a hard error:
 * - A required field (per the tool's own schema) that's absent, or
 *   present but blank/whitespace-only when it's a string.
 * - A "*_hex" field whose value isn't a real #RRGGBB / #RRGGBBAA color.
 * Anything else (unknown extra keys, numeric fields outside a "nice"
 * range) is left to the individual tool branch in ToolExecutor, which
 * already clamps/coerces those safely — this validator's job is only
 * to catch the cases that were being silently swallowed entirely.
 */
object ToolCallValidator {
    private val hexColorPattern = Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")

    /** Returns a list of human-readable validation error strings.
     *  Empty means the args are acceptable to hand off to ToolCallParser's
     *  normal per-tool decoding. [toolName] not being in the registry at
     *  all is NOT this function's concern — that's ParsedToolCall.Unknown's
     *  job, handled earlier in ToolCallParser. */
    fun validate(
        toolName: String,
        args: JsonObject,
    ): List<String> {
        val schema = ToolRegistry.ALL_TOOLS.firstOrNull { it.function.name == toolName }?.function?.parameters?.jsonObject ?: return emptyList()
        val properties = (schema["properties"] as? JsonObject)?.keys ?: emptySet()
        val required =
            (schema["required"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

        val errors = mutableListOf<String>()
        val unknownKeys = args.keys.filter { it !in properties }

        for (field in required) {
            val value = args[field]
            val isBlankString = value is JsonPrimitive && value.isString && value.jsonPrimitive.contentOrNull.isNullOrBlank()
            if (value == null || isBlankString) {
                val closeTypo = unknownKeys.minByOrNull { levenshtein(it, field) }?.takeIf { levenshtein(it, field) <= 2 }
                errors +=
                    if (closeTypo != null) {
                        "Missing required field '$field' for tool '$toolName' — you sent '$closeTypo' instead. " +
                            "Use the exact field name '$field'."
                    } else {
                        "Missing required field '$field' for tool '$toolName'."
                    }
            }
        }

        args.keys.filter { it.endsWith("_hex") }.forEach { key ->
            val raw = args[key]?.jsonPrimitive?.contentOrNull
            if (!raw.isNullOrBlank() && !hexColorPattern.matches(raw)) {
                errors += "Field '$key' on tool '$toolName' must be a hex color like '#RRGGBB' or '#RRGGBBAA' — got '$raw'."
            }
        }

        return errors
    }

    /** Standard iterative Levenshtein edit distance — used only to decide
     *  whether an unrecognized argument key is plausibly a misspelling of
     *  a missing required one (e.g. "colour_hex" vs "color_hex"), so the
     *  error message can point the model straight at the fix. */
    private fun levenshtein(
        a: String,
        b: String,
    ): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }
}
