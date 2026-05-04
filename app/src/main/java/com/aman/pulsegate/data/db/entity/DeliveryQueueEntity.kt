package com.aman.pulsegate.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aman.pulsegate.domain.model.DeliveryQueue
import com.aman.pulsegate.domain.model.QueueStatus

@Entity(
    tableName = "delivery_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["next_retry_at"]),
        Index(value = ["locked"]),
        Index(value = ["event_id", "destination_id"])
    ]
)
data class DeliveryQueueEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "event_id")
    val eventId: Long,

    @ColumnInfo(name = "destination_id")
    val destinationId: Long,

    @ColumnInfo(name = "status")
    val status: QueueStatus,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int,

    @ColumnInfo(name = "max_retry")
    val maxRetry: Int,

    @ColumnInfo(name = "next_retry_at")
    val nextRetryAt: Long,

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long?,

    @ColumnInfo(name = "locked")
    val locked: Boolean,

    @ColumnInfo(name = "locked_at")
    val lockedAt: Long?,

    @ColumnInfo(name = "worker_id")
    val workerId: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    fun toDomain(): DeliveryQueue = DeliveryQueue(
        id = id,
        eventId = eventId,
        destinationId = destinationId,
        status = status,
        retryCount = retryCount,
        maxRetry = maxRetry,
        nextRetryAt = nextRetryAt,
        lastAttemptAt = lastAttemptAt,
        locked = locked,
        lockedAt = lockedAt,
        workerId = workerId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: DeliveryQueue): DeliveryQueueEntity = DeliveryQueueEntity(
            id = domain.id,
            eventId = domain.eventId,
            destinationId = domain.destinationId,
            status = domain.status,
            retryCount = domain.retryCount,
            maxRetry = domain.maxRetry,
            nextRetryAt = domain.nextRetryAt,
            lastAttemptAt = domain.lastAttemptAt,
            locked = domain.locked,
            lockedAt = domain.lockedAt,
            workerId = domain.workerId,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}