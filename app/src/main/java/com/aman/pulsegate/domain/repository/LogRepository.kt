package com.aman.pulsegate.domain.repository

import com.aman.pulsegate.domain.model.DeliveryLog
import com.aman.pulsegate.domain.model.QueueStatus
import kotlinx.coroutines.flow.Flow

interface LogRepository {

    suspend fun insert(log: DeliveryLog): Long

    suspend fun getLogsForEvent(eventId: Long): List<DeliveryLog>

    suspend fun getLogsPaged(limit: Int, offset: Int): List<DeliveryLog>

    suspend fun getLogsByStatus(status: QueueStatus, limit: Int): List<DeliveryLog>

    suspend fun getLogsForDestination(destinationId: Long, limit: Int): List<DeliveryLog>

    suspend fun deleteOldLogs(threshold: Long)

    fun observeRecentLogs(limit: Int): Flow<List<DeliveryLog>>

    fun observeTotalCount(): Flow<Int>

    fun observeFailedCount(): Flow<Int>
}