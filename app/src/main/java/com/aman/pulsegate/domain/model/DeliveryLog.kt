package com.aman.pulsegate.domain.model

data class DeliveryLog(
    val id: Long = 0,
    val queueId: Long,
    val eventId: Long,
    val destinationId: Long,
    val status: QueueStatus,
    val httpCode: Int?,
    val responseBody: String?,
    val errorMessage: String?,
    val latencyMs: Long?,
    val retryAttempt: Int,
    val attemptedAt: Long
)