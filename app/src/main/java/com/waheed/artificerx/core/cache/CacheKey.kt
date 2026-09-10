package com.waheed.artificerx.core.cache
import com.waheed.artificerx.core.security.Checksum
object CacheKey {
    fun of(namespace:String,vararg parts:String):String =
        namespace+":"+Checksum.sha256(parts.joinToString("\u001f"))
}
