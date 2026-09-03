package com.waheed.artificerx.core.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WorkspaceManifest(
    val schema: Int = 1,
    val app: String = "ArtificerX",
    val createdAt: Long = System.currentTimeMillis(),
    val directories: List<String>,
)

@Singleton
class WorkspaceManifestService @Inject constructor(
    private val fileSystem: WorkspaceFileSystem,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    fun refresh(): File {
        val manifest = WorkspaceManifest(directories = listOf(
            "works", "cache", "system", "plugins", "models", "exports", "imports", "logs",
            "temp", "thumbnails", "backups", "autosave", "projects", "recipes"
        ))
        return fileSystem.writeTextAtomic(fileSystem.roots.system.resolve("workspace-manifest.json"), json.encodeToString(manifest))
    }
}
