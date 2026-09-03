package com.waheed.artificerx.core.importexport

import com.waheed.artificerx.core.artifact.ArtifactRef
import com.waheed.artificerx.core.artifact.ArtifactStore
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Secure ZIP importer for workspace/artifact bundles. */
@Singleton
class WorkspaceBundleImporter @Inject constructor(
    private val artifactStore: ArtifactStore,
) {
    companion object {
        private const val MAX_ENTRIES = 2_000
        private const val MAX_ENTRY_BYTES = 25L * 1024L * 1024L
        private const val MAX_TOTAL_BYTES = 100L * 1024L * 1024L
    }

    suspend fun importIntoThread(threadId: String, input: InputStream): List<ArtifactRef> {
        val imported = mutableListOf<ArtifactRef>()
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            var entryCount = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entryCount > MAX_ENTRIES) break
                if (entry.isDirectory || entry.name == "ARTIFACT-MANIFEST.json") {
                    zip.closeEntry()
                    continue
                }
                val name = sanitizeEntry(entry.name)
                if (name == null) {
                    zip.closeEntry()
                    continue
                }
                val bytes = zip.readBounded(MAX_ENTRY_BYTES) { total + it > MAX_TOTAL_BYTES }
                total += bytes.size
                if (bytes.isNotEmpty()) {
                    val mime = guessMime(name)
                    imported += artifactStore.writeFile(threadId, "imported_$name", bytes, mime, "workspace_import")
                }
                zip.closeEntry()
            }
        }
        return imported
    }

    private fun sanitizeEntry(name: String): String? {
        val normalized = name.replace('\\', '/').trim('/')
        if (normalized.isBlank() || normalized.split('/').any { it == ".." }) return null
        return normalized.split('/').filter { it.isNotBlank() && it != "." }.joinToString("/").take(180).ifBlank { null }
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".json", true) -> "application/json"
        name.endsWith(".md", true) -> "text/markdown"
        name.endsWith(".html", true) -> "text/html"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        name.endsWith(".svg", true) -> "image/svg+xml"
        name.endsWith(".kt", true) -> "text/x-kotlin"
        name.endsWith(".java", true) -> "text/x-java-source"
        name.endsWith(".xml", true) -> "application/xml"
        else -> "application/octet-stream"
    }

    private fun ZipInputStream.readBounded(maxBytes: Long, wouldExceed: (Long) -> Boolean): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val count = read(buffer)
            if (count <= 0) break
            total += count
            if (total > maxBytes || wouldExceed(count.toLong())) throw IllegalArgumentException("Workspace import exceeded safety size limits.")
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }
}
