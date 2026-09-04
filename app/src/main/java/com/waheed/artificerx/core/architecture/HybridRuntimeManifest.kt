package com.waheed.artificerx.core.architecture

data class HybridRuntimeManifest(
    val kotlinUi:Boolean=true,
    val cppRaster:Boolean=true,
    val localModelImport:Boolean=true,
    val dynamicRuntimeTools:Boolean=true,
    val artifactPersistence:Boolean=true,
    val automationEngine:Boolean=true,
    val multiChatProfiles:Boolean=true,
)
