package com.waheed.artificerx.core.util

object JsonSanitizer {
    fun safeText(input:String,max:Int=260_000)=input.replace("\u0000"," ").take(max)
    fun safeName(input:String)=input.replace(Regex("[^A-Za-z0-9._-]"),"_").take(120).ifBlank{"artifact"}
}
