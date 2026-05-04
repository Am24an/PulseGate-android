package com.aman.pulsegate.domain.usecase

import com.aman.pulsegate.domain.repository.EventRepository
import com.aman.pulsegate.domain.repository.LogRepository
import com.aman.pulsegate.domain.repository.QueueRepository
import timber.log.Timber
import javax.inject.Inject

class CleanupOldDataUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val queueRepository: QueueRepository,
    private val logRepository: LogRepository
) {

    suspend operator fun invoke(): Result<Unit> {
        val now = System.currentTimeMillis()

        return runCatching {
            deleteSentEvents(now)
            deleteSentQueueItems(now)
            deleteOldLogs(now)
        }.onSuccess {
            Timber.d("CleanupOldDataUseCase: cleanup completed successfully")
        }.onFailure { error ->
            Timber.e(error, "CleanupOldDataUseCase: cleanup failed")
        }
    }

    private suspend fun deleteSentEvents(now: Long) {
        val threshold = now - SENT_EVENT_RETENTION_MS
        eventRepository.deleteOldSentEvents(olderThanMs = threshold)
        Timber.d("CleanupOldDataUseCase: deleted sent events older than ${SENT_EVENT_RETENTION_HOURS}h")
    }

    private suspend fun deleteSentQueueItems(now: Long) {
        val threshold = now - SENT_QUEUE_RETENTION_MS
        queueRepository.deleteOldSentItems(threshold = threshold)
        Timber.d("CleanupOldDataUseCase: deleted sent queue items older than ${SENT_QUEUE_RETENTION_DAYS}d")
    }

    private suspend fun deleteOldLogs(now: Long) {
        val threshold = now - LOG_RETENTION_MS
        logRepository.deleteOldLogs(threshold = threshold)
        Timber.d("CleanupOldDataUseCase: deleted logs older than ${LOG_RETENTION_DAYS}d")
    }

    companion object {
        private const val SENT_EVENT_RETENTION_HOURS = 72L
        private const val SENT_EVENT_RETENTION_MS = SENT_EVENT_RETENTION_HOURS * 60 * 60 * 1000L

        private const val SENT_QUEUE_RETENTION_DAYS = 7L
        private const val SENT_QUEUE_RETENTION_MS = SENT_QUEUE_RETENTION_DAYS * 24 * 60 * 60 * 1000L

        private const val LOG_RETENTION_DAYS = 30L
        private const val LOG_RETENTION_MS = LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000L
    }
}