package com.waheed.artificerx.core.automation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AutomationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: AutomationEngine,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching { engine.runDue(AutomationTrigger.DAILY); Result.success() }.getOrElse { Result.retry() }
}
