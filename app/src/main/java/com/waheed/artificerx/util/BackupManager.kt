package com.waheed.artificerx.util

import android.app.Application
import android.net.Uri
import android.provider.OpenableStream
import android.util.Log
import androidx.work.ExistingPeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.waheed.artificerx.core.worker.AutoBackupWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext val app: Application
) {
    private val workManager = WorkManager.getInstance(app)
    private val backupWork = PeriodicWorkRequestBuilder<AutoBackupWorker>(15, TimeUnit.MINUTES)
        .build()

    private val _backupState = MutableStateFlow("idle")
    val backupState: StateFlow<String> = _backupState

    fun schedulePeriodicBackup() {
        workManager.enqueueUniquePeriodicWork(
            "artificerx_backup",
            ExistingPeriodicWorkRequest.Builder::class.java,
            backupWork
        )
    }

    fun performImmediateBackup(project: com.waheed.artificerx.domain.Project) {
        val data = androidx.work.Data.Builder()
            .putString("project_id", project.projectId)
            .build()
        workManager.enqueue(
            androidx.work.OneTimeWorkRequestBuilder<AutoBackupWorker>(
                project.projectId
            )
                .setInputData(data)
                .build()
        )
        _backupState.value = "working"
    }

    fun cancelAllBackups() {
        workManager.cancelAllWork()
        _backupState.value = "idle"
    }

    fun getAllRecentBackups(): List<Uri> = emptyList()  // Placeholder for actual implementation
}