package com.aman.pulsegate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aman.pulsegate.data.db.entity.DeliveryQueueEntity
import com.aman.pulsegate.domain.model.QueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryQueueDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DeliveryQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<DeliveryQueueEntity>)

    @Query(
        """
        SELECT * FROM delivery_queue
        WHERE status IN ('PENDING', 'RETRY')
        AND next_retry_at <= :currentTime
        AND locked = 0
        ORDER BY created_at ASC
        LIMIT :limit
    """
    )
    suspend fun getPendingItems(currentTime: Long, limit: Int): List<DeliveryQueueEntity>

    @Query(
        """
        UPDATE delivery_queue
        SET locked = 1,
            locked_at = :lockedAt,
            worker_id = :workerId,
            status = 'PROCESSING',
            updated_at = :updatedAt
        WHERE id = :id AND locked = 0
    """
    )
    suspend fun lockItem(
        id: Long,
        workerId: String,
        lockedAt: Long,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE delivery_queue
        SET locked = 0,
            locked_at = NULL,
            worker_id = NULL,
            status = :status,
            retry_count = :retryCount,
            next_retry_at = :nextRetryAt,
            last_attempt_at = :lastAttemptAt,
            updated_at = :updatedAt
        WHERE id = :id
    """
    )
    suspend fun updateAfterAttempt(
        id: Long,
        status: QueueStatus,
        retryCount: Int,
        nextRetryAt: Long,
        lastAttemptAt: Long,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE delivery_queue
        SET locked = 0,
            locked_at = NULL,
            worker_id = NULL,
            status = CASE
                WHEN retry_count = 0 THEN 'PENDING'
                ELSE 'RETRY'
            END,
            next_retry_at = CASE
                WHEN retry_count = 0 THEN 0
                ELSE next_retry_at
            END,
            updated_at = :currentTime
        WHERE locked = 1
        AND locked_at IS NOT NULL
        AND locked_at < :threshold
    """
    )
    suspend fun releaseStaleLocksAndReset(threshold: Long, currentTime: Long)

    @Query(
        """
        UPDATE delivery_queue
        SET status = 'PENDING',
            retry_count = 0,
            next_retry_at = 0,
            last_attempt_at = NULL,
            updated_at = :currentTime
        WHERE id = :id AND status = 'FAILED'
    """
    )
    suspend fun resetFailedItem(id: Long, currentTime: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM delivery_queue
        WHERE destination_id = :destinationId
        AND status IN ('PENDING', 'PROCESSING', 'RETRY')
    """
    )
    suspend fun countActiveItemsForDestination(destinationId: Long): Int

    @Query("SELECT COUNT(*) FROM delivery_queue WHERE status = :status")
    fun observeCountByStatus(status: QueueStatus): Flow<Int>

    @Query(
        """
        DELETE FROM delivery_queue
        WHERE status = 'SENT'
        AND updated_at < :threshold
    """
    )
    suspend fun deleteOldSentItems(threshold: Long)
}