package com.waheed.artificerx.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.waheed.artificerx.data.local.db.ArtificerXDatabase
import com.waheed.artificerx.data.local.db.ProjectDao
import com.waheed.artificerx.data.local.db.ProjectEntity
import com.waheed.artificerx.data.local.db.ProjectVersionDao
import com.waheed.artificerx.data.local.db.ProjectVersionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupBundle(
    val formatVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val projects: List<ProjectBackupEntry>,
)

@Serializable
data class ProjectBackupEntry(
    val project: ProjectBackupRecord,
    val versions: List<VersionBackupRecord>,
)

@Serializable
data class ProjectBackupRecord(
    val id: String,
    val name: String,
    val canvasWidthPx: Int,
    val canvasHeightPx: Int,
    val layersJson: String,
    val activeLayerId: String?,
    val createdAtEpochMillis: Long,
    val lastModifiedEpochMillis: Long,
)

@Serializable
data class VersionBackupRecord(
    val id: String,
    val versionLabel: String,
    val layersJson: String,
    val triggeredBy: String,
    val createdAtEpochMillis: Long,
)

sealed class BackupResult {
    data class ExportSuccess(
        val filePath: String,
        val projectCount: Int,
    ) : BackupResult()

    data class ImportSuccess(
        val projectCount: Int,
        val versionCount: Int,
    ) : BackupResult()

    data class Failure(
        val message: String,
    ) : BackupResult()
}

/**
 * Section 139 Backup & Restore's manual, user-triggered half (as
 * opposed to AutoBackupWorker's silent periodic checkpoints). Exports
 * every project and its full version history as one portable JSON
 * bundle the user can move to a new device or keep as an off-device
 * safety copy — since this is a zero-budget personal build with no
 * cloud sync, this file IS the user's disaster-recovery plan.
 */
@Singleton
class BackupRestoreRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val projectDao: ProjectDao,
        private val versionDao: ProjectVersionDao,
        private val database: ArtificerXDatabase,
    ) {
        private val json =
            Json {
                encodeDefaults = true
                prettyPrint = false
            }

        suspend fun exportAllToFile(): BackupResult =
            withContext(Dispatchers.IO) {
                runCatching {
                    val projects = projectDao.observeAllProjects().first()
                    val entries =
                        projects.map { project ->
                            val versions = versionDao.observeVersionsForProject(project.id).first()
                            ProjectBackupEntry(
                                project =
                                    ProjectBackupRecord(
                                        id = project.id,
                                        name = project.name,
                                        canvasWidthPx = project.canvasWidthPx,
                                        canvasHeightPx = project.canvasHeightPx,
                                        layersJson = project.layersJson,
                                        activeLayerId = project.activeLayerId,
                                        createdAtEpochMillis = project.createdAtEpochMillis,
                                        lastModifiedEpochMillis = project.lastModifiedEpochMillis,
                                    ),
                                versions =
                                    versions.map {
                                        VersionBackupRecord(
                                            id = it.id,
                                            versionLabel = it.versionLabel,
                                            layersJson = it.layersJson,
                                            triggeredBy = it.triggeredBy,
                                            createdAtEpochMillis = it.createdAtEpochMillis,
                                        )
                                    },
                            )
                        }

                    if (entries.size > MAX_PROJECTS) {
                        return@withContext BackupResult.Failure("There are more than $MAX_PROJECTS projects; split the workspace before exporting.")
                    }
                    val totalVersions = entries.sumOf { it.versions.size.toLong() }
                    if (totalVersions > MAX_VERSIONS) {
                        return@withContext BackupResult.Failure("There are more than $MAX_VERSIONS checkpoints; reduce version history before exporting.")
                    }

                    val bundle = BackupBundle(exportedAtEpochMillis = System.currentTimeMillis(), projects = entries)
                    val bundleBytes = json.encodeToString(bundle).toByteArray(Charsets.UTF_8)
                    if (bundleBytes.size.toLong() > MAX_BACKUP_BYTES) {
                        return@withContext BackupResult.Failure("Backup exceeds the ${MAX_BACKUP_BYTES / (1024 * 1024)} MB safety limit.")
                    }

                    val backupDir = File(context.getExternalFilesDir(null), "backups")
                    if (!backupDir.exists()) backupDir.mkdirs()
                    val file = File(backupDir, "artificerx_backup_${System.currentTimeMillis()}.json")
                    file.writeBytes(bundleBytes)

                    BackupResult.ExportSuccess(file.absolutePath, entries.size)
                }.getOrElse { BackupResult.Failure(it.message ?: "Export failed") }
            }

        suspend fun importFromFile(filePath: String): BackupResult =
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(filePath)
                    if (!file.isFile) return@withContext BackupResult.Failure("Backup file not found: $filePath")
                    if (file.length() > MAX_BACKUP_BYTES) {
                        return@withContext BackupResult.Failure("Backup exceeds the ${MAX_BACKUP_BYTES / (1024 * 1024)} MB safety limit.")
                    }

                    val bundle = json.decodeFromString<BackupBundle>(file.readText(Charsets.UTF_8))
                    if (bundle.projects.size > MAX_PROJECTS) {
                        return@withContext BackupResult.Failure("Backup contains more than $MAX_PROJECTS projects.")
                    }
                    val totalVersions = bundle.projects.sumOf { it.versions.size.toLong() }
                    if (totalVersions > MAX_VERSIONS) {
                        return@withContext BackupResult.Failure("Backup contains more than $MAX_VERSIONS checkpoints.")
                    }

                    var versionCount = 0
                    database.withTransaction {
                        bundle.projects.forEach { entry ->
                            projectDao.upsertProject(
                                ProjectEntity(
                                    id = entry.project.id,
                                    name = entry.project.name,
                                    canvasWidthPx = entry.project.canvasWidthPx,
                                    canvasHeightPx = entry.project.canvasHeightPx,
                                    layersJson = entry.project.layersJson,
                                    activeLayerId = entry.project.activeLayerId,
                                    thumbnailPath = null,
                                    createdAtEpochMillis = entry.project.createdAtEpochMillis,
                                    lastModifiedEpochMillis = entry.project.lastModifiedEpochMillis,
                                    lastOpenedEpochMillis = null,
                                ),
                            )
                            entry.versions.forEach { version ->
                                projectVersionDaoInsert(version, entry.project.id)
                                versionCount++
                            }
                        }
                    }

                    BackupResult.ImportSuccess(bundle.projects.size, versionCount)
                }.getOrElse { BackupResult.Failure(it.message ?: "Import failed — file may be corrupted or from an incompatible version") }
            }


        private suspend fun projectVersionDaoInsert(version: VersionBackupRecord, projectId: String) {
            versionDao.insertVersion(
                ProjectVersionEntity(
                    id = version.id,
                    projectId = projectId,
                    versionLabel = version.versionLabel,
                    layersJson = version.layersJson,
                    thumbnailPath = null,
                    triggeredBy = version.triggeredBy,
                    createdAtEpochMillis = version.createdAtEpochMillis,
                ),
            )
        }

        fun listAvailableBackupFiles(): List<File> {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) return emptyList()
            return backupDir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        }

        private companion object {
            const val MAX_BACKUP_BYTES = 50L * 1024L * 1024L
            const val MAX_PROJECTS = 500
            const val MAX_VERSIONS = 10_000L
        }
    }
