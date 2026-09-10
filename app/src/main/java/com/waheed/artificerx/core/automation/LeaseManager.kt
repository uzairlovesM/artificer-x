package com.waheed.artificerx.core.automation
data class Lease(val key:String,val owner:String,val expiresAt:Long)
class LeaseManager(private val clock:()->Long=System::currentTimeMillis) {
    private val leases=HashMap<String,Lease>()
    @Synchronized fun acquire(key:String,owner:String,duration:Long):Boolean {
        require(duration>0); val current=leases[key]; val now=clock()
        if(current!=null && current.expiresAt>now && current.owner!=owner)return false
        leases[key]=Lease(key,owner,now+duration); return true
    }
    @Synchronized fun release(key:String,owner:String):Boolean=if(leases[key]?.owner==owner){leases.remove(key);true}else false
    @Synchronized fun active(key:String):Lease?=leases[key]?.takeIf{it.expiresAt>clock()}
}
