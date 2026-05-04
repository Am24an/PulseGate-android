package com.aman.pulsegate.domain.usecase

import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.repository.QueueRepository
import timber.log.Timber
import javax.inject.Inject

class RetryFailedEventUseCase @Inject constructor(
    private val queueRepository: QueueRepository
) {

    // Resets a FAILED queue item back to PENDING with clean retry state.
    // Caller (LogsViewModel) is responsible for triggering WorkScheduler after this.
    suspend operator fun invoke(queueId: Long): Result<Unit> {
        if (queueId <= 0)
            return Result.failure(IllegalArgumentException("Invalid queueId=$queueId"))

        val currentTime = System.currentTimeMillis()

        return runCatching {
            val rowsUpdated = queueRepository.resetFailedItem(
                id = queueId,
                currentTime = currentTime
            )

            if (rowsUpdated == 0) {
                Timber.w("RetryFailedEventUseCase: no row updated for queueId=$queueId — item may not be FAILED or does not exist")
                throw IllegalStateException("Queue item not found or not in FAILED state: queueId=$queueId")
            }

            Timber.d("RetryFailedEventUseCase: reset queueId=$queueId back to ${QueueStatus.PENDING}")
        }.onFailure { error ->
            Timber.e(error, "RetryFailedEventUseCase: failed for queueId=$queueId")
        }
    }
}