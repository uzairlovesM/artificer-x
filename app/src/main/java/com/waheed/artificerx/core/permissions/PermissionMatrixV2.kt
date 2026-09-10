package com.waheed.artificerx.core.permissions
enum class PermissionLevel{NONE,OPTIONAL,REQUIRED}
data class CapabilityPermission(val capability:String,val level:PermissionLevel,val reason:String)
class PermissionMatrixV2(perms:List<CapabilityPermission>){
    private val map=perms.associateBy{it.capability}
    fun level(capability:String)=map[capability]?.level?:PermissionLevel.NONE
    fun reason(capability:String)=map[capability]?.reason
    fun requiredCapabilities()=map.filterValues{it.level==PermissionLevel.REQUIRED}.keys
}
