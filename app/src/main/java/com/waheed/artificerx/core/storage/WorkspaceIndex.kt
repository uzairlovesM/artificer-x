package com.waheed.artificerx.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceIndex @Inject constructor(private val fs: WorkspaceFileSystem) {
    data class Entry(val path: String, val name: String, val relativePath: String, val sizeBytes: Long, val modifiedAt: Long, val extension: String)

    suspend fun scan(query: String = "", limit: Int = 200): List<Entry> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        fs.roots.root.walkTopDown().filter { it.isFile }.mapNotNull { file ->
            val relative = file.relativeTo(fs.roots.root).path.replace(File.separatorChar, '/')
            val match = q.isBlank() || file.name.lowercase().contains(q) || relative.lowercase().contains(q)
            if (!match) null else Entry(file.absolutePath, file.name, relative, file.length(), file.lastModified(), file.extension.lowercase())
        }.sortedByDescending { it.modifiedAt }.take(limit).toList()
    }
}
