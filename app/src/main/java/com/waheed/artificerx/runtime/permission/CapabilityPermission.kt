package com.waheed.artificerx.runtime.permission

data class CapabilityPermission(val capability:String,val scopes:Set<String>,val granted:Boolean,val expiresAtEpochMs:Long?=null)
class CapabilityPermissionStore { private val grants=linkedMapOf<String,CapabilityPermission>(); fun grant(p:CapabilityPermission){grants[p.capability]=p}; fun revoke(id:String){grants.remove(id)}; fun isGranted(id:String,scope:String?=null):Boolean=grants[id]?.let{it.granted && (scope==null || scope in it.scopes)}==true; fun snapshot():List<CapabilityPermission>=grants.values.toList() }
