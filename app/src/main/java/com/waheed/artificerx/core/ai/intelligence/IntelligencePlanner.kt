package com.waheed.artificerx.core.ai.intelligence

import com.waheed.artificerx.core.builtin.BuiltinRecipe
import com.waheed.artificerx.core.builtin.BuiltinRecipeCatalog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntelligencePlanner @Inject constructor(
    private val catalog: BuiltinRecipeCatalog,
    private val policy: IntelligencePolicy,
) {
    fun recommend(task: String, limit: Int = 12): List<BuiltinRecipe> {
        val tokens = task.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        return catalog.search(task, limit.coerceIn(1, 50)).ifEmpty {
            catalog.search("", 100).sortedByDescending { recipe ->
                recipe.tags.count { it in tokens } * 4 + recipe.name.lowercase().split(' ').count { it in tokens }
            }.take(limit)
        }
    }

    fun plan(task: String): List<String> = policy.objectiveStack(task)
}
