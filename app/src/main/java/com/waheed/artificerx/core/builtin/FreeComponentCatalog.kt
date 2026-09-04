package com.waheed.artificerx.core.builtin

/**
 * Curated free/open-source component families that can be surfaced by the planner.
 * This is metadata, not a claim that every external project is bundled into the APK.
 */
object FreeComponentCatalog {
    data class Component(val id: String, val area: String, val license: String, val url: String)

    val components = listOf(
        Component("llama_cpp", "local_inference", "MIT", "https://github.com/ggml-org/llama.cpp"),
        Component("termux_app", "terminal", "GPL-3.0", "https://github.com/termux/termux-app"),
        Component("termux_packages", "packages", "mixed_per_package", "https://github.com/termux/termux-packages"),
        Component("coil", "image_loading", "Apache-2.0", "https://github.com/coil-kt/coil"),
        Component("koin", "dependency_injection", "Apache-2.0", "https://github.com/InsertKoinIO/koin"),
        Component("androidx", "platform", "Apache-2.0", "https://github.com/androidx/androidx"),
        Component("compose_multiplatform", "ui", "Apache-2.0", "https://github.com/JetBrains/compose-multiplatform"),
        Component("okhttp", "networking", "Apache-2.0", "https://github.com/square/okhttp"),
        Component("retrofit", "http_api", "Apache-2.0", "https://github.com/square/retrofit"),
        Component("kotlin_serialization", "serialization", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization"),
        Component("coroutines", "concurrency", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
        Component("room", "database", "Apache-2.0", "https://developer.android.com/training/data-storage/room"),
        Component("workmanager", "background_work", "Apache-2.0", "https://developer.android.com/develop/background-work/background-tasks/persistent"),
        Component("media3", "media", "Apache-2.0", "https://github.com/androidx/media"),
        Component("jsoup", "html_parsing", "MIT", "https://github.com/jhy/jsoup"),
        Component("osmdroid", "maps", "Apache-2.0", "https://github.com/osmdroid/osmdroid"),
    )
}
