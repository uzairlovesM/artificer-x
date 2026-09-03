package com.waheed.artificerx.core.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WorkspaceMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileSystem: WorkspaceFileSystem,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        fileSystem.ensureReady()
        cleanupTemp()
        Result.success()
    }.getOrElse { Result.retry() }

    private fun cleanupTemp() {
        fileSystem.listFiles(fileSystem.roots.temp).filter { System.currentTimeMillis() - it.lastModified() > MAX_TEMP_AGE_MS }.forEach { it.delete() }
    }

    companion object { private const val MAX_TEMP_AGE_MS = 24L * 60 * 60 * 1000 }
}
