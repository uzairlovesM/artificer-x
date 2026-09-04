package com.waheed.artificerx.core.builtin

import com.waheed.artificerx.core.agent.ToolExecutionResult
import com.waheed.artificerx.core.runtime.RuntimeToolCatalog
import com.waheed.artificerx.core.runtime.RuntimeToolExecutor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuiltinRecipeExecutor @Inject constructor(
    private val catalog: BuiltinRecipeCatalog,
    private val runtimeExecutor: RuntimeToolExecutor,
) {
    suspend fun execute(id: String, args: Map<String, String>): ToolExecutionResult {
        val recipe = catalog.get(id) ?: return ToolExecutionResult.Failure("Unknown built-in recipe: $id")
        if (recipe.safety == Safety.DESTRUCTIVE && args["confirm"] != "true") {
            return ToolExecutionResult.Failure("Recipe '${recipe.id}' requires confirm=true")
        }
        val outputs = ArrayList<String>()
        for ((index, step) in recipe.operations.withIndex()) {
            val runtimeName = "runtime_builtin_${recipe.id.replace('-', '_')}_$index"
            if (!RuntimeToolCatalog.contains(runtimeName)) {
                val installed = RuntimeToolCatalog.install(
                    com.waheed.artificerx.core.runtime.RuntimeToolSpec(
                        name = runtimeName,
                        description = "Ephemeral executor for built-in recipe ${recipe.id} step $index",
                        operation = step.operation,
                        inputSchemaJson = "{\"type\":\"object\",\"properties\":{}}",
                        config = step.config,
                    ),
                )
                if (installed.isFailure) return ToolExecutionResult.Failure(installed.exceptionOrNull()?.message ?: "Could not prepare recipe step")
            }
            val result = runtimeExecutor.execute(runtimeName, args)
            when (result) {
                is ToolExecutionResult.Failure -> return ToolExecutionResult.Failure("${recipe.id} step $index failed: ${result.errorMessage}")
                is ToolExecutionResult.Success -> outputs += "step[$index]: ${result.message}"
                else -> outputs += "step[$index]: $result"
            }
        }
        return ToolExecutionResult.Success("${recipe.name}\n${outputs.joinToString("\n")}")
    }
}
