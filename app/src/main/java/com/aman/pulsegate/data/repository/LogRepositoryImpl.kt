package com.aman.pulsegate.data.repository

import com.aman.pulsegate.data.db.dao.DeliveryLogDao
import com.aman.pulsegate.data.db.entity.DeliveryLogEntity
import com.aman.pulsegate.domain.model.DeliveryLog
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LogRepositoryImpl @Inject constructor(
    private val dao: DeliveryLogDao
) : LogRepository {

    override suspend fun insert(log: DeliveryLog): Long {
        return dao.insert(DeliveryLogEntity.fromDomain(log))
    }

    override suspend fun getLogsForEvent(eventId: Long): List<DeliveryLog> {
        return dao.getLogsForEvent(eventId).map { it.toDomain() }
    }

    override suspend fun getLogsPaged(limit: Int, offset: Int): List<DeliveryLog> {
        return dao.getLogsPaged(limit, offset).map { it.toDomain() }
    }

    override suspend fun getLogsByStatus(status: QueueStatus, limit: Int): List<DeliveryLog> {
        return dao.getLogsByStatus(status, limit).map { it.toDomain() }
    }

    override suspend fun getLogsForDestination(destinationId: Long, limit: Int): List<DeliveryLog> {
        return dao.getLogsForDestination(destinationId, limit).map { it.toDomain() }
    }

    override suspend fun deleteOldLogs(threshold: Long) {
        dao.deleteOldLogs(threshold)
    }

    override fun observeRecentLogs(limit: Int): Flow<List<DeliveryLog>> {
        return dao.observeRecentLogs(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun observeTotalCount(): Flow<Int> = dao.observeTotalCount()

    override fun observeFailedCount(): Flow<Int> = dao.observeFailedCount()
}