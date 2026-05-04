package com.aman.pulsegate.background

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aman.pulsegate.domain.usecase.ProcessDeliveryQueueUseCase
import com.aman.pulsegate.notification.DeliveryNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class PeriodicDeliveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val processDeliveryQueueUseCase: ProcessDeliveryQueueUseCase,
    private val notificationManager: DeliveryNotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("PeriodicDeliveryWorker: started runAttemptCount=$runAttemptCount")

        return try {
            setForeground(createForegroundInfo())

            var totalProcessed = 0
            while (true) {
                val processed = processDeliveryQueueUseCase(batchSize = CHUNK_SIZE)
                totalProcessed += processed
                if (processed == 0) break
            }

            Timber.d("PeriodicDeliveryWorker: completed totalProcessed=$totalProcessed")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "PeriodicDeliveryWorker: failed runAttemptCount=$runAttemptCount")
            if (runAttemptCount < MAX_WORKER_RETRIES) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = notificationManager.buildWorkerNotification()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val WORK_NAME = "PeriodicDeliveryWorker"
        private const val CHUNK_SIZE = 10
        private const val MAX_WORKER_RETRIES = 2
        private const val NOTIFICATION_ID = 1002

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<PeriodicDeliveryWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,                    // ← explicit 10 seconds minimum, clean and clear
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Timber.d("PeriodicDeliveryWorker: scheduled every 15 minutes")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("PeriodicDeliveryWorker: cancelled")
        }
    }
}