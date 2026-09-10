package com.waheed.artificerx.core.web
import java.net.URI
class UrlNormalizer {
    fun normalize(input:String):String {
        val uri=URI(input.trim())
        require(uri.scheme?.lowercase() in setOf("http","https"))
        require(uri.host!=null)
        require(uri.userInfo==null)
        return URI(uri.scheme.lowercase(),null,uri.host.lowercase(),uri.port,uri.path,uri.query,null).toString()
    }
}
