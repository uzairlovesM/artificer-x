package com.waheed.artificerx.core.storage

import java.io.File

data class DirectoryLayout(
    val root: File,
    val projects: File,
    val cache: File,
    val exports: File,
    val imports: File,
    val logs: File,
    val plugins: File,
    val models: File
) {
    fun ensure() {
        listOf(projects, cache, exports, imports, logs, plugins, models).forEach {
            require(it.exists() || it.mkdirs()) { "Cannot create ${it.absolutePath}" }
        }
    }

    companion object {
        fun from(root: File): DirectoryLayout = DirectoryLayout(
            root,
            File(root, "projects"),
            File(root, "cache"),
            File(root, "exports"),
            File(root, "imports"),
            File(root, "logs"),
            File(root, "plugins"),
            File(root, "models")
        )
    }
}
