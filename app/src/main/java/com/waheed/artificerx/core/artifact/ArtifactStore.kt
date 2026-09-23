package com.waheed.artificerx.core.artifact

import com.waheed.artificerx.data.repository.ChatWorkspaceRepository
import com.waheed.artificerx.core.importexport.ArtifactManifest
import com.waheed.artificerx.core.importexport.ArtifactManifestCodec
import com.waheed.artificerx.core.importexport.ArtifactManifestEntry
import java.security.MessageDigest
import com.waheed.artificerx.data.workspace.ArtifactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtifactStore @Inject constructor(
    private val workspaceFileSystem: com.waheed.artificerx.core.storage.WorkspaceFileSystem,
    private val workspaceRepository: ChatWorkspaceRepository,
) {
    suspend fun writeFile(threadId: String, fileName: String, bytes: ByteArray, mimeType: String, sourceTool: String? = null): ArtifactRef = withContext(Dispatchers.IO) {
        require(bytes.size.toLong() <= MAX_FILE_BYTES) { "Artifact exceeds ${MAX_FILE_BYTES / (1024 * 1024)} MB safety limit." }
        val safeName = sanitize(fileName)
        val dir = workspaceFileSystem.threadArtifactsDir(threadId).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}_$safeName")
        file.outputStream().use { it.write(bytes) }
        val id = UUID.randomUUID().toString()
        workspaceRepository.saveArtifact(ArtifactEntity(id, threadId, safeName, mimeType, file.absolutePath, file.length(), System.currentTimeMillis(), sourceTool))
        ArtifactRef(id, safeName, mimeType, file.absolutePath, file.length())
    }

    suspend fun writeText(threadId: String, fileName: String, content: String, mimeType: String = "text/plain", sourceTool: String? = null): ArtifactRef =
        writeFile(threadId, fileName, content.toByteArray(Charsets.UTF_8), mimeType, sourceTool)

    suspend fun writeZip(threadId: String, fileName: String, entries: List<ArtifactInput>, sourceTool: String? = null): ArtifactRef = withContext(Dispatchers.IO) {
        val dir = workspaceFileSystem.threadArtifactsDir(threadId).apply { mkdirs() }
        val safeName = sanitize(fileName).removeSuffix(".zip") + ".zip"
        val file = File(dir, "${UUID.randomUUID()}_$safeName")
        require(entries.none { it.name.isBlank() }) { "ZIP contains a blank entry name." }
        val safeEntries = entries
        require(safeEntries.size <= MAX_ZIP_ENTRIES) { "ZIP contains too many entries (max $MAX_ZIP_ENTRIES)." }
        val totalBytes = safeEntries.sumOf { it.bytes.size.toLong() }
        require(safeEntries.all { it.bytes.size.toLong() <= MAX_ZIP_ENTRY_BYTES }) { "ZIP contains an entry larger than the safety limit." }
        require(totalBytes <= MAX_ZIP_TOTAL_BYTES) { "ZIP exceeds the safety size limit." }
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            safeEntries.forEach { input ->
                zip.putNextEntry(ZipEntry(sanitizeZipEntry(input.name)))
                zip.write(input.bytes)
                zip.closeEntry()
            }
            val manifest = ArtifactManifest(
                entries = safeEntries.map { input ->
                    ArtifactManifestEntry(input.name, input.mimeType, input.bytes.size.toLong(), input.bytes.sha256())
                },
            )
            val manifestBytes = ArtifactManifestCodec.encode(manifest).toByteArray(Charsets.UTF_8)
            zip.putNextEntry(ZipEntry("ARTIFACT-MANIFEST.json"))
            zip.write(manifestBytes)
            zip.closeEntry()
        }
        val id = UUID.randomUUID().toString()
        workspaceRepository.saveArtifact(ArtifactEntity(id, threadId, safeName, "application/zip", file.absolutePath, file.length(), System.currentTimeMillis(), sourceTool))
        ArtifactRef(id, safeName, "application/zip", file.absolutePath, file.length())
    }

    suspend fun delete(artifactId: String): Boolean = withContext(Dispatchers.IO) {
        val entity = workspaceRepository.getArtifact(artifactId) ?: return@withContext false
        val deleted = File(entity.path).delete()
        if (deleted || !File(entity.path).exists()) workspaceRepository.deleteArtifact(artifactId)
        deleted
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }

    private fun sanitize(name: String): String = name.trim().replace(Regex("[^A-Za-z0-9._ -]"), "_").take(120).ifBlank { "artifact" }
    private fun sanitizeZipEntry(name: String): String {
        val normalized = name.replace('\\', '/').trim('/')
        require(normalized.indexOf('\u0000') < 0) { "Invalid ZIP entry name" }
        val parts = normalized.split('/').filter { it.isNotBlank() && it != "." }
        require(parts.none { it == ".." }) { "Unsafe ZIP entry path" }
        val clean = parts.joinToString("/")
        require(clean.isNotBlank() && clean.length <= 180) { "Invalid ZIP entry name" }
        return clean
    }

    companion object {
        private const val MAX_FILE_BYTES = 100L * 1024L * 1024L
        private const val MAX_ZIP_ENTRIES = 2_000
        private const val MAX_ZIP_ENTRY_BYTES = 25L * 1024L * 1024L
        private const val MAX_ZIP_TOTAL_BYTES = 100L * 1024L * 1024L
    }
}
