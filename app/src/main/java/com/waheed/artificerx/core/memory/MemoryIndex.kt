package com.waheed.artificerx.core.memory
data class MemoryRecord(val id:String,val text:String,val tags:Set<String>,val createdAt:Long)
class MemoryIndex {
    private val records=LinkedHashMap<String,MemoryRecord>()
    fun upsert(record:MemoryRecord){records[record.id]=record}
    fun remove(id:String)=records.remove(id)
    fun search(query:String,limit:Int=20):List<MemoryRecord>{
        val terms=query.lowercase().split(Regex("\\W+")).filter{it.isNotBlank()}
        return records.values.map{r->r to terms.count{t->r.text.lowercase().contains(t)||r.tags.any{tag->tag.lowercase().contains(t)}}}
            .filter{it.second>0}.sortedByDescending{it.second}.take(limit.coerceAtLeast(0)).map{it.first}
    }
}
