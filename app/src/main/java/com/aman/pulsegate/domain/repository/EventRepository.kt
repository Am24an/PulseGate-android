package com.aman.pulsegate.domain.repository

import com.aman.pulsegate.domain.model.IncomingEvent
import com.aman.pulsegate.domain.model.QueueStatus
import kotlinx.coroutines.flow.Flow

interface EventRepository {

    suspend fun saveEvent(event: IncomingEvent): Long

    suspend fun getEventById(id: Long): IncomingEvent?

    fun observeAll(): Flow<List<IncomingEvent>>

    fun observeCountByStatus(status: QueueStatus): Flow<Int>

    suspend fun updateStatus(id: Long, status: QueueStatus)

    suspend fun deleteOldSentEvents(olderThanMs: Long)
}