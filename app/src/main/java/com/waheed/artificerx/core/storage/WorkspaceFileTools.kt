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
        Result.success(readBounded(file, maxChars.coerceIn(1, 500_000)))
    }

    suspend fun write(relativePath: String, content: String): Result<File> = withContext(Dispatchers.IO) {
        val file = safe(relativePath) ?: return@withContext Result.failure(IllegalArgumentException("Path is outside the managed works directory"))
        require(content.toByteArray(Charsets.UTF_8).size.toLong() <= MAX_WRITE_FILE_BYTES) {
            "File content exceeds the safe workspace write limit"
        }
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
        require(old.isNotEmpty()) { "Replacement target must not be empty" }
        if (file.length() > MAX_REPLACE_FILE_BYTES) return@withContext Result.failure(IllegalArgumentException("File is too large for safe text replacement"))
        val text = file.readText(Charsets.UTF_8)
        if (!text.contains(old)) return@withContext Result.failure(IllegalArgumentException("Target text not found"))
        val updated = if (all) text.replace(old, new) else text.replaceFirst(old, new)
        require(updated.toByteArray(Charsets.UTF_8).size.toLong() <= MAX_REPLACE_FILE_BYTES) {
            "Replacement result exceeds the safe workspace write limit"
        }
        fs.writeTextAtomic(file, updated)
        Result.success(file)
    }

    private fun readBounded(file: File, maxChars: Int): String {
        val out = StringBuilder(minOf(maxChars, 32 * 1024))
        file.bufferedReader(Charsets.UTF_8).use { reader ->
            val buffer = CharArray(8 * 1024)
            while (out.length < maxChars) {
                val n = reader.read(buffer, 0, minOf(buffer.size, maxChars - out.length))
                if (n < 0) break
                out.append(buffer, 0, n)
            }
        }
        return out.toString()
    }

    private fun safe(relative: String): File? {
        val root = fs.roots.works.canonicalFile
        val pieces = relative.replace('\\', '/').split('/').filter { it.isNotBlank() && it != "." }
        if (pieces.any { it == ".." || it.indexOf('\u0000') >= 0 }) return null
        if (pieces.size > 32 || relative.length > 180) return null
        if (pieces.isEmpty()) return root
        val target = pieces.fold(root) { acc, piece -> acc.resolve(piece) }.canonicalFile
        return if (target.path == root.path || target.path.startsWith(root.path + File.separator)) target else null
    }

    companion object {
        private const val MAX_WRITE_FILE_BYTES = 25L * 1024L * 1024L
        private const val MAX_REPLACE_FILE_BYTES = 10L * 1024L * 1024L
    }
}
