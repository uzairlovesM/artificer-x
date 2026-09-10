package com.waheed.artificerx.core.automation
class AutosaveCoordinator(private val intervalMillis:Long=15_000L, private val clock:()->Long=System::currentTimeMillis) {
    init{require(intervalMillis>0)}
    private var dirty=false; private var lastSave=clock()
    fun markDirty(){dirty=true}
    fun shouldSave(now:Long=clock()):Boolean=dirty && now-lastSave>=intervalMillis
    fun saved(now:Long=clock()){dirty=false;lastSave=now}
    fun isDirty()=dirty
}
