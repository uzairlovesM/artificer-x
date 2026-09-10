package com.waheed.artificerx.core.memory
class MemoryRetention(private val maxRecords:Int=10_000) {
    init{require(maxRecords>0)}
    fun prune(records:List<MemoryRecord>,now:Long,maxAgeMillis:Long):List<MemoryRecord> {
        val fresh=records.filter{now-it.createdAt<=maxAgeMillis}
        return fresh.sortedByDescending{it.createdAt}.take(maxRecords)
    }
}
