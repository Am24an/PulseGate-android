package com.aman.pulsegate.domain.usecase

import com.aman.pulsegate.domain.model.DeliveryLog
import com.aman.pulsegate.domain.model.DeliveryQueue
import com.aman.pulsegate.domain.model.IncomingEvent
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.model.SendResult
import com.aman.pulsegate.domain.repository.DestinationRepository
import com.aman.pulsegate.domain.repository.EventRepository
import com.aman.pulsegate.domain.repository.LogRepository
import com.aman.pulsegate.domain.repository.QueueRepository
import com.aman.pulsegate.sender.SenderEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class ProcessDeliveryQueueUseCase @Inject constructor(
    private val queueRepository: QueueRepository,
    private val eventRepository: EventRepository,
    private val destinationRepository: DestinationRepository,
    private val logRepository: LogRepository,
    private val senderEngine: SenderEngine
) {

    private val concurrencySemaphore = Semaphore(MAX_CONCURRENT_DELIVERIES)

    suspend operator fun invoke(batchSize: Int): Int {
        val currentTime = System.currentTimeMillis()

        queueRepository.releaseStaleLocksAndReset(
            threshold = currentTime - STALE_LOCK_THRESHOLD_MS,
            currentTime = currentTime
        )

        val pendingItems = queueRepository.getPendingItems(
            currentTime = currentTime,
            limit = batchSize
        )

        if (pendingItems.isEmpty()) {
            Timber.d("ProcessDeliveryQueue: no pending items")
            return 0
        }

        Timber.d("ProcessDeliveryQueue: processing ${pendingItems.size} items")

        val results = supervisorScope {
            pendingItems.map { queueItem ->
                async {
                    concurrencySemaphore.withPermit {
                        processItem(queueItem)
                    }
                }
            }.awaitAll()
        }

        val processedCount = results.count { it }
        Timber.d("ProcessDeliveryQueue: completed $processedCount/${pendingItems.size}")
        return processedCount
    }

    private suspend fun processItem(queueItem: DeliveryQueue): Boolean {
        val lockTime = System.currentTimeMillis()
        val workerId = UUID.randomUUID().toString()

        val lockAcquired = queueRepository.lockItem(
            id = queueItem.id,
            workerId = workerId,
            lockedAt = lockTime,
            updatedAt = lockTime
        )

        if (lockAcquired == 0) {
            Timber.d("ProcessDeliveryQueue: lock not acquired for queueId=${queueItem.id}, skipping")
            return false
        }

        val event: IncomingEvent =
            eventRepository.getEventById(queueItem.eventId)  // ← getEventById not getById
                ?: run {
                    Timber.e("ProcessDeliveryQueue: event not found id=${queueItem.eventId}")
                    markAsFailed(
                        queueItem = queueItem,
                        errorMessage = "Event not found: id=${queueItem.eventId}"
                    )
                    return true
                }

        val destination = destinationRepository.getById(queueItem.destinationId)
            ?: run {
                Timber.e("ProcessDeliveryQueue: destination not found id=${queueItem.destinationId}")
                markAsFailed(
                    queueItem = queueItem,
                    errorMessage = "Destination not found: id=${queueItem.destinationId}"
                )
                return true
            }

        if (!destination.isActive) {
            Timber.w("ProcessDeliveryQueue: destination inactive queueId=${queueItem.id}")
            markAsFailed(
                queueItem = queueItem,
                errorMessage = "Destination inactive: ${destination.name}"
            )
            return true
        }

        val attemptTime = System.currentTimeMillis()
        val sendResult =
            senderEngine.dispatch(event, destination)  // ← IncomingEvent type guaranteed above
        val latencyMs = System.currentTimeMillis() - attemptTime

        handleSendResult(
            queueItem = queueItem,
            sendResult = sendResult,
            latencyMs = latencyMs,
            attemptTime = attemptTime
        )

        return true
    }

    private suspend fun handleSendResult(
        queueItem: DeliveryQueue,
        sendResult: SendResult,
        latencyMs: Long,
        attemptTime: Long
    ) {
        when (sendResult) {
            is SendResult.Success -> {
                val now = System.currentTimeMillis()
                Timber.d("ProcessDeliveryQueue: delivered queueId=${queueItem.id} code=${sendResult.httpCode}")

                queueRepository.updateAfterAttempt(
                    id = queueItem.id,
                    status = QueueStatus.SENT,
                    retryCount = queueItem.retryCount,
                    nextRetryAt = 0L,
                    lastAttemptAt = attemptTime,
                    updatedAt = now
                )

                logRepository.insert(
                    buildLog(
                        queueItem = queueItem,
                        status = QueueStatus.SENT,
                        httpCode = sendResult.httpCode,
                        responseBody = null,
                        errorMessage = null,
                        latencyMs = sendResult.latencyMs,
                        attemptedAt = attemptTime
                    )
                )
            }

            is SendResult.HttpError -> {
                val shouldRetry = sendResult.code >= 500
                val retryBudgetExhausted = queueItem.retryCount >= queueItem.maxRetry

                if (!shouldRetry || retryBudgetExhausted) {
                    Timber.e("ProcessDeliveryQueue: permanent failure queueId=${queueItem.id} code=${sendResult.code}")
                    markAsFailed(
                        queueItem = queueItem,
                        errorMessage = "HTTP ${sendResult.code}",
                        httpCode = sendResult.code,
                        responseBody = sendResult.body,
                        latencyMs = latencyMs,
                        attemptTime = attemptTime
                    )
                } else {
                    scheduleRetry(
                        queueItem = queueItem,
                        errorMessage = "HTTP ${sendResult.code}",
                        httpCode = sendResult.code,
                        responseBody = sendResult.body,
                        latencyMs = latencyMs,
                        attemptTime = attemptTime
                    )
                }
            }

            is SendResult.NetworkError -> {
                if (queueItem.retryCount >= queueItem.maxRetry) {
                    Timber.e("ProcessDeliveryQueue: max retries reached queueId=${queueItem.id}")
                    markAsFailed(
                        queueItem = queueItem,
                        errorMessage = sendResult.exception.message ?: "Network error",
                        latencyMs = latencyMs,
                        attemptTime = attemptTime
                    )
                } else {
                    scheduleRetry(
                        queueItem = queueItem,
                        errorMessage = sendResult.exception.message ?: "Network error",
                        httpCode = null,
                        responseBody = null,
                        latencyMs = latencyMs,
                        attemptTime = attemptTime
                    )
                }
            }

            is SendResult.UnknownError -> {
                Timber.e("ProcessDeliveryQueue: unknown error queueId=${queueItem.id} msg=${sendResult.message}")
                markAsFailed(
                    queueItem = queueItem,
                    errorMessage = sendResult.message,
                    latencyMs = latencyMs,
                    attemptTime = attemptTime
                )
            }
        }
    }

    private suspend fun scheduleRetry(
        queueItem: DeliveryQueue,
        errorMessage: String,
        httpCode: Int? = null,
        responseBody: String? = null,
        latencyMs: Long,
        attemptTime: Long
    ) {
        val newRetryCount = queueItem.retryCount + 1
        val now = System.currentTimeMillis()

        Timber.w("ProcessDeliveryQueue: scheduling retry $newRetryCount for queueId=${queueItem.id}")

        queueRepository.updateAfterAttempt(
            id = queueItem.id,
            status = QueueStatus.RETRY,
            retryCount = newRetryCount,
            nextRetryAt = now + retryDelayMs(newRetryCount),
            lastAttemptAt = attemptTime,
            updatedAt = now
        )

        logRepository.insert(
            buildLog(
                queueItem = queueItem,
                status = QueueStatus.RETRY,
                httpCode = httpCode,
                responseBody = responseBody,
                errorMessage = errorMessage,
                latencyMs = latencyMs,
                attemptedAt = attemptTime
            )
        )
    }

    private suspend fun markAsFailed(
        queueItem: DeliveryQueue,
        errorMessage: String,
        httpCode: Int? = null,
        responseBody: String? = null,
        latencyMs: Long? = null,
        attemptTime: Long = System.currentTimeMillis()
    ) {
        val now = System.currentTimeMillis()

        queueRepository.updateAfterAttempt(
            id = queueItem.id,
            status = QueueStatus.FAILED,
            retryCount = queueItem.retryCount,
            nextRetryAt = 0L,
            lastAttemptAt = attemptTime,
            updatedAt = now
        )

        logRepository.insert(
            buildLog(
                queueItem = queueItem,
                status = QueueStatus.FAILED,
                httpCode = httpCode,
                responseBody = responseBody,
                errorMessage = errorMessage,
                latencyMs = latencyMs,
                attemptedAt = attemptTime
            )
        )
    }

    private fun buildLog(
        queueItem: DeliveryQueue,
        status: QueueStatus,
        httpCode: Int?,
        responseBody: String?,
        errorMessage: String?,
        latencyMs: Long?,
        attemptedAt: Long
    ): DeliveryLog = DeliveryLog(
        queueId = queueItem.id,
        eventId = queueItem.eventId,
        destinationId = queueItem.destinationId,
        status = status,
        httpCode = httpCode,
        responseBody = responseBody,
        errorMessage = errorMessage,
        latencyMs = latencyMs,
        retryAttempt = queueItem.retryCount,
        attemptedAt = attemptedAt
    )

    private fun retryDelayMs(retryCount: Int): Long = when (retryCount) {
        1 -> 60_000L
        2 -> 300_000L
        3 -> 900_000L
        4 -> 1_800_000L
        5 -> 3_600_000L
        else -> 21_600_000L
    }

    companion object {
        private const val STALE_LOCK_THRESHOLD_MS = 5 * 60 * 1000L
        private const val MAX_CONCURRENT_DELIVERIES = 3
    }
}