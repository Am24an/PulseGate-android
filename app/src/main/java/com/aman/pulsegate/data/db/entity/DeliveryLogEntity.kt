package com.aman.pulsegate.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aman.pulsegate.domain.model.DeliveryLog
import com.aman.pulsegate.domain.model.QueueStatus

@Entity(
    tableName = "delivery_logs",
    indices = [
        Index(value = ["event_id"]),
        Index(value = ["attempted_at"]),
        Index(value = ["destination_id"])
    ]
)
data class DeliveryLogEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "queue_id")
    val queueId: Long,

    @ColumnInfo(name = "event_id")
    val eventId: Long,

    @ColumnInfo(name = "destination_id")
    val destinationId: Long,

    @ColumnInfo(name = "status")
    val status: QueueStatus,

    @ColumnInfo(name = "http_code")
    val httpCode: Int?,

    @ColumnInfo(name = "response_body")
    val responseBody: String?,

    @ColumnInfo(name = "error_message")
    val errorMessage: String?,

    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long?,

    @ColumnInfo(name = "retry_attempt")
    val retryAttempt: Int,

    @ColumnInfo(name = "attempted_at")
    val attemptedAt: Long
) {
    fun toDomain(): DeliveryLog = DeliveryLog(
        id = id,
        queueId = queueId,
        eventId = eventId,
        destinationId = destinationId,
        status = status,
        httpCode = httpCode,
        responseBody = responseBody,
        errorMessage = errorMessage,
        latencyMs = latencyMs,
        retryAttempt = retryAttempt,
        attemptedAt = attemptedAt
    )

    companion object {
        fun fromDomain(domain: DeliveryLog): DeliveryLogEntity = DeliveryLogEntity(
            id = domain.id,
            queueId = domain.queueId,
            eventId = domain.eventId,
            destinationId = domain.destinationId,
            status = domain.status,
            httpCode = domain.httpCode,
            responseBody = domain.responseBody,
            errorMessage = domain.errorMessage,
            latencyMs = domain.latencyMs,
            retryAttempt = domain.retryAttempt,
            attemptedAt = domain.attemptedAt
        )
    }
}