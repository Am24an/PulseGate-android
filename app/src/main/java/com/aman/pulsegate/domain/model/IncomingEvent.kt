package com.aman.pulsegate.domain.model

data class IncomingEvent(
    val id: Long = 0,
    val eventHash: String,
    val sourceType: SourceType,
    val sender: String,
    val title: String?,
    val message: String,
    val rawPayload: String,
    val appPackage: String?,
    val receivedTimestamp: Long,
    val processingStatus: QueueStatus,
    val createdAt: Long
)