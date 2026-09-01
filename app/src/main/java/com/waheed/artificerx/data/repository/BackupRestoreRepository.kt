package com.waheed.artificerx.data.repository

import android.content.Context
import com.waheed.artificerx.data.local.db.ProjectDao
import com.waheed.artificerx.data.local.db.ProjectEntity
import com.waheed.artificerx.data.local.db.ProjectVersionDao
import com.waheed.artificerx.data.local.db.ProjectVersionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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

                    val bundle = BackupBundle(exportedAtEpochMillis = System.currentTimeMillis(), projects = entries)
                    val bundleJson = json.encodeToString(bundle)

                    val backupDir = File(context.getExternalFilesDir(null), "backups")
                    if (!backupDir.exists()) backupDir.mkdirs()
                    val file = File(backupDir, "artificerx_backup_${System.currentTimeMillis()}.json")
                    file.writeText(bundleJson)

                    BackupResult.ExportSuccess(file.absolutePath, entries.size)
                }.getOrElse { BackupResult.Failure(it.message ?: "Export failed") }
            }

        suspend fun importFromFile(filePath: String): BackupResult =
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(filePath)
                    if (!file.exists()) return@withContext BackupResult.Failure("Backup file not found: $filePath")

                    val bundle = json.decodeFromString<BackupBundle>(file.readText())
                    var versionCount = 0

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
                            versionDao.insertVersion(
                                ProjectVersionEntity(
                                    id = version.id,
                                    projectId = entry.project.id,
                                    versionLabel = version.versionLabel,
                                    layersJson = version.layersJson,
                                    thumbnailPath = null,
                                    triggeredBy = version.triggeredBy,
                                    createdAtEpochMillis = version.createdAtEpochMillis,
                                ),
                            )
                            versionCount++
                        }
                    }

                    BackupResult.ImportSuccess(bundle.projects.size, versionCount)
                }.getOrElse { BackupResult.Failure(it.message ?: "Import failed — file may be corrupted or from an incompatible version") }
            }

        fun listAvailableBackupFiles(): List<File> {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) return emptyList()
            return backupDir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }
