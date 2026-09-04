package com.waheed.artificerx.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Concrete file-agent API constrained to ARTIFICER-X/works. */
@Singleton
class WorkspaceFileTools @Inject constructor(private val fs: WorkspaceFileSystem) {
    suspend fun read(relativePath: String, maxChars: Int = 100_000): Result<String> = withContext(Dispatchers.IO) {
        val file = safe(relativePath) ?: return@withContext Result.failure(IllegalArgumentException("Path is outside the managed works directory"))
        if (!file.exists() || !file.isFile) return@withContext Result.failure(IllegalArgumentException("File not found: $relativePath"))
        Result.success(file.readText().take(maxChars.coerceIn(1, 500_000)))
    }

    suspend fun write(relativePath: String, content: String): Result<File> = withContext(Dispatchers.IO) {
        val file = safe(relativePath) ?: return@withContext Result.failure(IllegalArgumentException("Path is outside the managed works directory"))
        fs.writeTextAtomic(file, content)
        Result.success(file)
    }

    suspend fun list(relativePath: String = ""): Result<List<String>> = withContext(Dispatchers.IO) {
        val dir = safe(relativePath) ?: return@withContext Result.failure(IllegalArgumentException("Invalid directory"))
        if (!dir.exists() || !dir.isDirectory) return@withContext Result.failure(IllegalArgumentException("Directory not found"))
        Result.success(dir.listFiles().orEmpty().sortedBy { it.name.lowercase() }.map { it.name + if (it.isDirectory) "/" else "" }.take(500))
    }

    suspend fun replace(relativePath: String, old: String, new: String, all: Boolean = false): Result<File> = withContext(Dispatchers.IO) {
        val file = safe(relativePath) ?: return@withContext Result.failure(IllegalArgumentException("Invalid path"))
        if (!file.isFile) return@withContext Result.failure(IllegalArgumentException("File not found"))
        val text = file.readText()
        if (!text.contains(old)) return@withContext Result.failure(IllegalArgumentException("Target text not found"))
        val updated = if (all) text.replace(old, new) else text.replaceFirst(old, new)
        fs.writeTextAtomic(file, updated)
        Result.success(file)
    }

    private fun safe(relative: String): File? {
        val root = fs.roots.works.canonicalFile
        val pieces = relative.replace('\\', '/').split('/').filter { it.isNotBlank() && it != "." && it != ".." }
        if (pieces.isEmpty()) return root
        val target = pieces.fold(root) { acc, piece -> acc.resolve(piece) }.canonicalFile
        return if (target.path == root.path || target.path.startsWith(root.path + File.separator)) target else null
    }
}
