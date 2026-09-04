package com.waheed.artificerx.core.util

import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Shared JSON-Schema builder DSL used by every tool-definition site
 * (ToolRegistry, BuiltinRecipeTools, and any future tool source).
 * kotlinx.serialization's JsonObjectBuilder only ships `put(key, value)`
 * out of the box — nested-object and nested-array sugar is intentionally
 * centralized here once instead of being redeclared (and drifting) per
 * file, which is what caused the previous split-definition bug.
 */

/** Puts a nested JSON object under [key], built via [builderAction]. */
public fun JsonObjectBuilder.putJsonObject(
    key: String,
    builderAction: JsonObjectBuilder.() -> Unit,
) {
    put(key, buildJsonObject(builderAction))
}

/** Puts a nested JSON array under [key], built via [builderAction]. */
public fun JsonObjectBuilder.putJsonArray(
    key: String,
    builderAction: JsonArrayBuilder.() -> Unit,
) {
    put(key, buildJsonArray(builderAction))
}
