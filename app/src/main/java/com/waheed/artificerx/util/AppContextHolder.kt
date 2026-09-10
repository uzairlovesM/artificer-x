package com.waheed.artificerx.util

/** Process-safe application context bridge for services that need a FileProvider URI. */
object AppContextHolder {
    lateinit var context: android.content.Context
        private set

    fun init(context: android.content.Context) {
        this.context = context.applicationContext
    }
}
