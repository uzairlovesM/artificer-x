package com.waheed.artificerx.core.project

class ProjectRevision(private var value: Long = 0L) {
    init { require(value >= 0) }
    @Synchronized fun current(): Long = value
    @Synchronized fun next(): Long { value += 1; return value }
    @Synchronized fun observe(external: Long) { if (external > value) value = external }
}
