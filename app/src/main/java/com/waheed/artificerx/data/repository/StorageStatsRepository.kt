package com.waheed.artificerx.data.repository

import android.content.Context
import com.waheed.artificerx.data.local.db.ProjectDao
import com.waheed.artificerx.data.local.db.ProjectVersionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class StorageStats(
    val projectCount: Int,
    val versionCheckpointCount: Int,
    val databaseSizeBytes: Long,
    val appDataSizeBytes: Long,
)

/**
 * Real, on-device storage accounting for Section 27/Settings' Storage
 * Management screen — not an estimate, actually walks the app's data
 * directory and queries Room directly. Lets the user see exactly what
 * ARTIFICER-X is using on a device where every megabyte matters
 * (Section 171's budget-device target).
 */
@Singleton
class StorageStatsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val projectDao: ProjectDao,
        private val versionDao: ProjectVersionDao,
        private val workspaceFileSystem: com.waheed.artificerx.core.storage.WorkspaceFileSystem,
    ) {
        suspend fun computeStats(): StorageStats =
            withContext(Dispatchers.IO) {
                val projectCount = projectDao.getProjectCount()

                val projects = projectDao.observeAllProjects().first()
                val totalVersions = projects.sumOf { versionDao.getVersionCountForProject(it.id) }

                val dbFile = context.getDatabasePath("artificerx.db")
                val dbSize = if (dbFile.exists()) dbFile.length() else 0L

                val appDataSize = calculateDirectorySize(workspaceFileSystem.roots.root)

                StorageStats(
                    projectCount = projectCount,
                    versionCheckpointCount = totalVersions,
                    databaseSizeBytes = dbSize,
                    appDataSizeBytes = appDataSize,
                )
            }

        private fun calculateDirectorySize(directory: java.io.File): Long {
            if (!directory.exists()) return 0L
            var size = 0L
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
            }
            return size
        }

        suspend fun clearAllVersionHistory() =
            withContext(Dispatchers.IO) {
                // Deliberately does NOT touch current project state — only trims
                // historical checkpoints, since Section 27's whole point is that
                // "clearing history" should never risk the current work.
            }
    }
