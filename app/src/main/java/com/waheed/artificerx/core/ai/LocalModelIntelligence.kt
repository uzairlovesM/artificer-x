package com.waheed.artificerx.core.ai

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class LocalModelInspection(val format:String,val bytes:Long,val likelyVision:Boolean,val warnings:List<String>)
@Singleton
class LocalModelIntelligence @Inject constructor() {
    fun inspect(file:File):LocalModelInspection {
        val name=file.name.lowercase()
        val vision=name.contains("vision") || name.contains("vl") || name.contains("mmproj")
        val warnings=buildList { if(!name.endsWith(".gguf") && !name.endsWith(".bin")) add("Unknown model extension") ; if(file.length()<1024*1024) add("Model file is unusually small") }
        return LocalModelInspection(if(name.endsWith(".gguf"))"GGUF" else "BINARY",file.length(),vision,warnings)
    }
}
