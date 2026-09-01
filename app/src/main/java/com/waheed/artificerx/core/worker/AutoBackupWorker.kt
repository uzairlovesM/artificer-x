package com.waheed.artificerx.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.waheed.artificerx.data.repository.ProjectRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Section 139 Backup & Restore's background half. Runs periodically
 * (scheduled by BackupScheduler below) even if the app is fully
 * backgrounded or the Studio screen isn't composed, creating a
 * version-history checkpoint for every project that's changed since
 * its last checkpoint — a safety net beyond StudioViewModel's
 * in-session auto-save for the case where the process is killed by
 * the OS between app opens rather than crashing mid-session.
 */
@HiltWorker
class AutoBackupWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val projectRepository: ProjectRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            return runCatching {
                val projects = projectRepository.allProjects.first()
                projects.forEach { projectEntity ->
                    val state = projectRepository.loadProject(projectEntity.id) ?: return@forEach
                    projectRepository.createVersionCheckpoint(
                        state = state,
                        triggeredBy = "auto_backup_worker",
                        label = "Auto-backup ${java.text.SimpleDateFormat(
                            "MMM d, HH:mm",
                            java.util.Locale.getDefault(),
                        ).format(java.util.Date())}",
                    )
                }
                Result.success()
            }.getOrElse {
                Result.retry()
            }
        }
    }
