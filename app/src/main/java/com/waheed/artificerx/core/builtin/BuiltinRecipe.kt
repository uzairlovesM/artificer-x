package com.waheed.artificerx.core.builtin

import kotlinx.serialization.Serializable

@Serializable
data class BuiltinRecipe(
    val id: String,
    val category: String,
    val name: String,
    val description: String,
    val operations: List<BuiltinOperation>,
    val tags: List<String> = emptyList(),
    val safety: Safety = Safety.SAFE,
)

@Serializable
data class BuiltinOperation(
    val operation: String,
    val config: Map<String, String> = emptyMap(),
)

enum class Safety { SAFE, GUARDED, DESTRUCTIVE }
