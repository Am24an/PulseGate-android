package com.aman.pulsegate.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aman.pulsegate.domain.usecase.CleanupOldDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cleanupOldDataUseCase: CleanupOldDataUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("CleanupWorker: started")

        return cleanupOldDataUseCase()
            .fold(
                onSuccess = {
                    Timber.d("CleanupWorker: completed successfully")
                    Result.success()
                },
                onFailure = { error ->
                    Timber.e(error, "CleanupWorker: failed attempt=$runAttemptCount")
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                }
            )
    }

    companion object {
        private const val WORK_NAME = "CleanupWorker"
        private const val MAX_RETRIES = 2

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Timber.d("CleanupWorker: scheduled every 24 hours")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("CleanupWorker: cancelled")
        }
    }
}