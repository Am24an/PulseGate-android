package com.aman.pulsegate.background

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aman.pulsegate.domain.usecase.ProcessDeliveryQueueUseCase
import com.aman.pulsegate.notification.DeliveryNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ImmediateDeliveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val processDeliveryQueueUseCase: ProcessDeliveryQueueUseCase,
    private val notificationManager: DeliveryNotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("ImmediateDeliveryWorker: started runAttemptCount=$runAttemptCount")

        return try {
            setForeground(createForegroundInfo())

            var totalProcessed = 0
            while (true) {
                val processed = processDeliveryQueueUseCase(batchSize = CHUNK_SIZE)
                totalProcessed += processed
                if (processed == 0) break
            }

            Timber.d("ImmediateDeliveryWorker: completed totalProcessed=$totalProcessed")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "ImmediateDeliveryWorker: failed runAttemptCount=$runAttemptCount")
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
        private const val WORK_NAME = "ImmediateDeliveryWorker"
        private const val CHUNK_SIZE = 10
        private const val MAX_WORKER_RETRIES = 3
        private const val NOTIFICATION_ID = 1001

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ImmediateDeliveryWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
            Timber.d("ImmediateDeliveryWorker: enqueued")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("ImmediateDeliveryWorker: cancelled")
        }
    }
}