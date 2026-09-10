package com.waheed.artificerx.util

import android.app.Application
import android.net.Uri
import java.io.File
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.waheed.artificerx.core.worker.AutoBackupWorker
import com.waheed.artificerx.domain.Project
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext val app: Application,
) {
    private val workManager = WorkManager.getInstance(app)
    private val _backupState = MutableStateFlow("idle")
    val backupState: StateFlow<String> = _backupState

    fun schedulePeriodicBackup() {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(6, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("artificerx_backup", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun performImmediateBackup(project: Project) {
        val data = workDataOf("project_id" to project.projectId)
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>().setInputData(data).build()
        workManager.enqueueUniqueWork("backup:${project.projectId}", ExistingWorkPolicy.REPLACE, request)
        _backupState.value = "working"
    }

    fun cancelAllBackups() {
        workManager.cancelAllWork(); _backupState.value = "idle"
    }

    fun getAllRecentBackups(): List<Uri> {
        val candidates = linkedSetOf<File>()
        val internal = File(app.filesDir, "ARTIFICER-X/backups")
        internal.listFiles { file -> file.isFile && (file.extension == "json" || file.extension == "zip") }?.let { candidates.addAll(it) }
        app.getExternalFilesDir(null)?.resolve("backups")?.listFiles { file -> file.isFile && (file.extension == "json" || file.extension == "zip") }?.let { candidates.addAll(it) }
        return candidates.sortedByDescending { it.lastModified() }.take(100).map(Uri::fromFile)
    }
}
