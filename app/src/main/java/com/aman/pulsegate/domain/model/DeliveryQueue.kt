package com.aman.pulsegate.domain.model

data class DeliveryQueue(
    val id: Long = 0,
    val eventId: Long,
    val destinationId: Long,
    val status: QueueStatus,
    val retryCount: Int,
    val maxRetry: Int,
    val nextRetryAt: Long,
    val lastAttemptAt: Long?,
    val locked: Boolean,
    val lockedAt: Long?,
    val workerId: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val DEFAULT_MAX_RETRY = 6
    }
}