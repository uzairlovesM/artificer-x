package com.waheed.artificerx.core.ai.intelligence

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class IntelligenceStrategy(
    val id: String,
    val mode: String,
    val priority: Int,
    val instruction: String,
)

@Singleton
class StrategyCatalog @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var items: List<IntelligenceStrategy> = emptyList()

    fun init(context: Context) {
        if (items.isNotEmpty()) return
        items = json.decodeFromString<List<IntelligenceStrategy>>(
            context.assets.open("builtin/intelligence-strategies.json").bufferedReader().use { it.readText() },
        )
    }

    fun forTask(task: String, limit: Int = 16): List<IntelligenceStrategy> {
        val q = task.lowercase()
        return items.sortedWith(
            compareByDescending<IntelligenceStrategy> { it.instruction.lowercase().split(' ').count { word -> q.contains(word) && word.length > 3 } }
                .thenByDescending { it.priority },
        ).take(limit.coerceIn(1, 64))
    }

    fun count(): Int = items.size
}
