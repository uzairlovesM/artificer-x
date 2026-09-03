package com.waheed.artificerx.core.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Owns every app-managed data path. All folders are created lazily and safely. */
@Singleton
class WorkspaceFileSystem @Inject constructor(@ApplicationContext private val context: Context) {
    data class Roots(
        val root: File,
        val works: File,
        val cache: File,
        val system: File,
        val plugins: File,
        val models: File,
        val exports: File,
        val imports: File,
        val logs: File,
        val temp: File,
        val thumbnails: File,
        val backups: File,
        val autosave: File,
        val projects: File,
        val recipes: File,
    )

    val roots: Roots by lazy { createRoots() }

    fun ensureReady() { roots }

    fun projectDir(projectId: String): File = roots.projects.resolve(safeSegment(projectId)).also { it.mkdirs() }

    fun threadDir(threadId: String): File = roots.works.resolve("threads").resolve(safeSegment(threadId)).also { it.mkdirs() }

    fun threadArtifactsDir(threadId: String): File = threadDir(threadId).resolve("artifacts").also { it.mkdirs() }

    fun threadLogsDir(threadId: String): File = threadDir(threadId).resolve("logs").also { it.mkdirs() }

    fun pluginStateFile(pluginId: String): File = pluginDir(pluginId).resolve("state.json")

    fun modelDir(modelId: String): File = roots.models.resolve(safeSegment(modelId)).also { it.mkdirs() }

    fun exportFile(name: String): File = roots.exports.resolve(safeSegment(name))
    fun pluginDir(pluginId: String): File = roots.plugins.resolve(safeSegment(pluginId)).also { it.mkdirs() }
    fun cacheFile(name: String): File = roots.cache.resolve(safeSegment(name))
    fun tempFile(prefix: String, suffix: String = ".tmp"): File = File.createTempFile(safeSegment(prefix), suffix, roots.temp)

    fun writeTextAtomic(target: File, value: String): File {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.writing")
        tmp.writeText(value)
        if (!tmp.renameTo(target)) {
            target.writeText(value)
            tmp.delete()
        }
        return target
    }

    fun writeBytesAtomic(target: File, value: ByteArray): File {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.writing")
        tmp.writeBytes(value)
        if (!tmp.renameTo(target)) {
            target.writeBytes(value)
            tmp.delete()
        }
        return target
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun usageBytes(): Long = roots.root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearCache(): Long {
        val bytes = roots.cache.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        roots.cache.deleteRecursively(); roots.cache.mkdirs()
        return bytes
    }

    fun listFiles(directory: File, recursive: Boolean = true): List<File> {
        if (!directory.exists()) return emptyList()
        return if (recursive) directory.walkTopDown().filter { it.isFile }.toList() else directory.listFiles()?.filter { it.isFile }.orEmpty()
    }

    private fun createRoots(): Roots {
        val root = context.filesDir.resolve("ARTIFICER-X")
        val result = Roots(
            root = root,
            works = root.resolve("works"),
            cache = root.resolve("cache"),
            system = root.resolve("system"),
            plugins = root.resolve("plugins"),
            models = root.resolve("models"),
            exports = root.resolve("exports"),
            imports = root.resolve("imports"),
            logs = root.resolve("logs"),
            temp = root.resolve("temp"),
            thumbnails = root.resolve("thumbnails"),
            backups = root.resolve("backups"),
            autosave = root.resolve("autosave"),
            projects = root.resolve("projects"),
            recipes = root.resolve("recipes"),
        )
        listOf(result.root, result.works, result.cache, result.system, result.plugins, result.models,
            result.exports, result.imports, result.logs, result.temp, result.thumbnails, result.backups,
            result.autosave, result.projects, result.recipes).forEach { it.mkdirs() }
        writeTextAtomic(result.system.resolve("workspace.json"), "{\"schema\":3,\"initialized\":true}")
        return result
    }

    private fun safeSegment(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "item" }

    companion object { private const val DEFAULT_BUFFER = 32 * 1024 }
}
