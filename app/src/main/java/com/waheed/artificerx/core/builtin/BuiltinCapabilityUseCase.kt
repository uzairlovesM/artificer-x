package com.waheed.artificerx.core.builtin

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuiltinCapabilityUseCase @Inject constructor(
    private val catalog: BuiltinRecipeCatalog,
) {
    data class Snapshot(
        val total: Int,
        val categories: Map<String, Int>,
        val safe: Int,
        val guarded: Int,
        val destructive: Int,
    )

    fun snapshot(): Snapshot {
        val all = catalog.search("", 5000)
        return Snapshot(
            total = all.size,
            categories = all.groupingBy { it.category }.eachCount().toSortedMap(),
            safe = all.count { it.safety == Safety.SAFE },
            guarded = all.count { it.safety == Safety.GUARDED },
            destructive = all.count { it.safety == Safety.DESTRUCTIVE },
        )
    }
}
