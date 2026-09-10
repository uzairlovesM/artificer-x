package com.waheed.artificerx.core.automation
data class Job<T>(val id:String,val priority:Int,val payload:T)
class JobQueue<T> {
    private val queue=java.util.PriorityQueue<Job<T>>(compareByDescending<Job<T>>{it.priority}.thenBy{it.id})
    @Synchronized fun offer(job:Job<T>){queue.add(job)}
    @Synchronized fun poll():Job<T>?=queue.poll()
    @Synchronized fun size()=queue.size
    @Synchronized fun drain(limit:Int):List<Job<T>>{val out=mutableListOf<Job<T>>();repeat(limit.coerceAtLeast(0)){queue.poll()?.let(out::add)?:return@repeat};return out}
}
