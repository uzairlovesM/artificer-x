package com.waheed.artificerx.core.terminal

import android.os.Build
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class AndroidToolchainSnapshot(
    val androidRelease:String,
    val sdkRoot:String?,
    val buildTools:List<String>,
    val platforms:List<String>,
    val ndks:List<String>,
    val cmake:List<String>,
    val javaVersion:String,
    val gitAvailable:Boolean,
    val adbAvailable:Boolean,
)

@Singleton
class AndroidToolchainManager @Inject constructor() {
    fun inspect(): AndroidToolchainSnapshot {
        val sdk=System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        fun dirs(child:String)=sdk?.let { File(it,child).listFiles()?.filter { f -> f.isDirectory }?.map { it.name }?.sorted()?.takeLast(20).orEmpty() } ?: emptyList()
        return AndroidToolchainSnapshot(
            androidRelease=Build.VERSION.RELEASE,
            sdkRoot=sdk,
            buildTools=dirs("build-tools"),
            platforms=dirs("platforms"),
            ndks=dirs("ndk"),
            cmake=dirs("cmake"),
            javaVersion=System.getProperty("java.version").orEmpty(),
            gitAvailable=commandExists("git"),
            adbAvailable=commandExists("adb"),
        )
    }

    private fun commandExists(command:String):Boolean = runCatching {
        ProcessBuilder(command,"--version").redirectErrorStream(true).start().use { it.waitFor()==0 }
    }.getOrDefault(false)
}
