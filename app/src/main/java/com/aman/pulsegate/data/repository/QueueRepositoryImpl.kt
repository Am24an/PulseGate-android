package com.aman.pulsegate.data.repository

import com.aman.pulsegate.data.db.dao.DeliveryQueueDao
import com.aman.pulsegate.data.db.entity.DeliveryQueueEntity
import com.aman.pulsegate.domain.model.DeliveryQueue
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.repository.QueueRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QueueRepositoryImpl @Inject constructor(
    private val dao: DeliveryQueueDao
) : QueueRepository {

    override suspend fun insert(queue: DeliveryQueue): Long {
        return dao.insert(DeliveryQueueEntity.fromDomain(queue))
    }

    override suspend fun insertAll(queues: List<DeliveryQueue>) {
        dao.insertAll(queues.map { DeliveryQueueEntity.fromDomain(it) })
    }

    override suspend fun getPendingItems(currentTime: Long, limit: Int): List<DeliveryQueue> {
        return dao.getPendingItems(currentTime, limit).map { it.toDomain() }
    }

    override suspend fun lockItem(
        id: Long,
        workerId: String,
        lockedAt: Long,
        updatedAt: Long
    ): Int = dao.lockItem(id, workerId, lockedAt, updatedAt)

    override suspend fun updateAfterAttempt(
        id: Long,
        status: QueueStatus,
        retryCount: Int,
        nextRetryAt: Long,
        lastAttemptAt: Long,
        updatedAt: Long
    ) = dao.updateAfterAttempt(id, status, retryCount, nextRetryAt, lastAttemptAt, updatedAt)

    override suspend fun releaseStaleLocksAndReset(threshold: Long, currentTime: Long) {
        dao.releaseStaleLocksAndReset(threshold, currentTime)
    }

    override suspend fun resetFailedItem(id: Long, currentTime: Long): Int {
        return dao.resetFailedItem(id, currentTime)
    }

    override suspend fun countActiveItemsForDestination(destinationId: Long): Int {
        return dao.countActiveItemsForDestination(destinationId)
    }

    override fun observeCountByStatus(status: QueueStatus): Flow<Int> {
        return dao.observeCountByStatus(status)
    }

    override suspend fun deleteOldSentItems(threshold: Long) {
        dao.deleteOldSentItems(threshold)
    }
}