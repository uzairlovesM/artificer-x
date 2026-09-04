package com.waheed.artificerx.core.builtin

import com.waheed.artificerx.core.agent.ToolExecutionResult
import com.waheed.artificerx.core.network.FunctionDefinitionDto
import com.waheed.artificerx.core.network.ToolDefinitionDto
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonArray

object BuiltinRecipeTools {
    const val INVOKE = "invoke_builtin_recipe"
    const val SEARCH = "search_builtin_recipes"

    fun definitions(): List<ToolDefinitionDto> = listOf(
        ToolDefinitionDto(FunctionDefinitionDto(INVOKE, "Execute one of 1000+ built-in free capability recipes using the audited runtime operation layer.", buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("recipe_id") { put("type", "string") }
                putJsonObject("args_json") { put("type", "string") }
            }
            putJsonArray("required") { add(JsonPrimitive("recipe_id")) }
        })),
        ToolDefinitionDto(FunctionDefinitionDto(SEARCH, "Search the built-in free capability recipe catalog and return the best matches.", buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("query") { put("type", "string") }; putJsonObject("limit") { put("type", "integer") } }
            putJsonArray("required") { add(JsonPrimitive("query")) }
        })),
    )

    fun summarize(results: List<BuiltinRecipe>): ToolExecutionResult = ToolExecutionResult.Success(
        results.joinToString("\n") { "${it.id} | ${it.category} | ${it.name} | ${it.description}" }
    )
}
