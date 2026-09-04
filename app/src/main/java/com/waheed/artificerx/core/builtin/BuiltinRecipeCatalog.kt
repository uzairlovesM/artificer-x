package com.waheed.artificerx.core.builtin

import android.content.Context
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuiltinRecipeCatalog @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    private val recipes = ConcurrentHashMap<String, BuiltinRecipe>()

    fun init(context: Context) {
        if (recipes.isNotEmpty()) return
        val raw = context.assets.open("builtin/builtin-recipes.json").bufferedReader().use { it.readText() }
        val loaded = json.decodeFromString<List<BuiltinRecipe>>(raw)
        loaded.forEach { recipes[it.id] = it }
    }

    fun count(): Int = recipes.size
    fun get(id: String): BuiltinRecipe? = recipes[id]
    fun search(query: String, limit: Int = 40): List<BuiltinRecipe> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return recipes.values.sortedBy { it.id }.take(limit)
        return recipes.values.asSequence()
            .filter { r ->
                r.id.contains(q) || r.name.lowercase().contains(q) ||
                    r.description.lowercase().contains(q) || r.tags.any { it.lowercase().contains(q) }
            }
            .sortedBy { it.id }
            .take(limit)
            .toList()
    }
}
