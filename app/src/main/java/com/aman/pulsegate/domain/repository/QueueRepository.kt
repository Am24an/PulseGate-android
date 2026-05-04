package com.aman.pulsegate.domain.repository

import com.aman.pulsegate.domain.model.DeliveryQueue
import com.aman.pulsegate.domain.model.QueueStatus
import kotlinx.coroutines.flow.Flow

interface QueueRepository {

    suspend fun insert(queue: DeliveryQueue): Long

    suspend fun insertAll(queues: List<DeliveryQueue>)

    suspend fun getPendingItems(currentTime: Long, limit: Int): List<DeliveryQueue>

    suspend fun lockItem(
        id: Long,
        workerId: String,
        lockedAt: Long,
        updatedAt: Long
    ): Int

    suspend fun updateAfterAttempt(
        id: Long,
        status: QueueStatus,
        retryCount: Int,
        nextRetryAt: Long,
        lastAttemptAt: Long,
        updatedAt: Long
    )

    suspend fun releaseStaleLocksAndReset(threshold: Long, currentTime: Long)

    suspend fun resetFailedItem(id: Long, currentTime: Long): Int

    suspend fun countActiveItemsForDestination(destinationId: Long): Int

    fun observeCountByStatus(status: QueueStatus): Flow<Int>

    suspend fun deleteOldSentItems(threshold: Long)
}