package com.aman.pulsegate.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aman.pulsegate.domain.model.IncomingEvent
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.model.SourceType

@Entity(
    tableName = "incoming_events",
    indices = [
        Index(value = ["event_hash"], unique = true),
        Index(value = ["processing_status"]),
        Index(value = ["created_at"])
    ]
)
data class IncomingEventEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "event_hash")
    val eventHash: String,

    @ColumnInfo(name = "source_type")
    val sourceType: SourceType,

    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "raw_payload")
    val rawPayload: String,

    @ColumnInfo(name = "app_package")
    val appPackage: String?,

    @ColumnInfo(name = "received_timestamp")
    val receivedTimestamp: Long,

    @ColumnInfo(name = "processing_status")
    val processingStatus: QueueStatus,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    fun toDomain(): IncomingEvent = IncomingEvent(
        id = id,
        eventHash = eventHash,
        sourceType = sourceType,
        sender = sender,
        title = title,
        message = message,
        rawPayload = rawPayload,
        appPackage = appPackage,
        receivedTimestamp = receivedTimestamp,
        processingStatus = processingStatus,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: IncomingEvent): IncomingEventEntity = IncomingEventEntity(
            id = domain.id,
            eventHash = domain.eventHash,
            sourceType = domain.sourceType,
            sender = domain.sender,
            title = domain.title,
            message = domain.message,
            rawPayload = domain.rawPayload,
            appPackage = domain.appPackage,
            receivedTimestamp = domain.receivedTimestamp,
            processingStatus = domain.processingStatus,
            createdAt = domain.createdAt
        )
    }
}