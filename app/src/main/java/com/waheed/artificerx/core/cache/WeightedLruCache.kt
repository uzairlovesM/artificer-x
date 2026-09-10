package com.waheed.artificerx.core.cache
class WeightedLruCache<K,V>(private val maxWeight:Long, private val weight:(V)->Long) {
    init{require(maxWeight>0)}
    private val map=LinkedHashMap<K,V>(16,.75f,true); private var current=0L
    @Synchronized fun put(key:K,value:V){val w=weight(value).coerceAtLeast(0);map.remove(key)?.let{current-=weight(it).coerceAtLeast(0)};map[key]=value;current+=w;trim()}
    @Synchronized fun get(key:K):V?=map[key]
    @Synchronized fun remove(key:K):V?=map.remove(key)?.also{current-=weight(it).coerceAtLeast(0)}
    @Synchronized fun clear(){map.clear();current=0}
    @Synchronized fun weight()=current
    private fun trim(){val i=map.entries.iterator();while(current>maxWeight&&i.hasNext()){val e=i.next();current-=weight(e.value).coerceAtLeast(0);i.remove()}}
}
