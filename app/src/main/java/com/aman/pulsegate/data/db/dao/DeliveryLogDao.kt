package com.aman.pulsegate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aman.pulsegate.data.db.entity.DeliveryLogEntity
import com.aman.pulsegate.domain.model.QueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DeliveryLogEntity): Long

    @Query(
        """
        SELECT * FROM delivery_logs
        ORDER BY attempted_at DESC
        LIMIT :limit
    """
    )
    fun observeRecentLogs(limit: Int = 100): Flow<List<DeliveryLogEntity>>

    @Query(
        """
        SELECT * FROM delivery_logs
        ORDER BY attempted_at DESC
        LIMIT :limit OFFSET :offset
    """
    )
    suspend fun getLogsPaged(limit: Int = 50, offset: Int = 0): List<DeliveryLogEntity>

    @Query(
        """
        SELECT * FROM delivery_logs
        WHERE status = :status
        ORDER BY attempted_at DESC
        LIMIT :limit
    """
    )
    suspend fun getLogsByStatus(status: QueueStatus, limit: Int = 100): List<DeliveryLogEntity>

    @Query(
        """
        SELECT * FROM delivery_logs
        WHERE event_id = :eventId
        ORDER BY attempted_at DESC
    """
    )
    suspend fun getLogsForEvent(eventId: Long): List<DeliveryLogEntity>

    @Query(
        """
        SELECT * FROM delivery_logs
        WHERE destination_id = :destinationId
        ORDER BY attempted_at DESC
        LIMIT :limit
    """
    )
    suspend fun getLogsForDestination(destinationId: Long, limit: Int = 50): List<DeliveryLogEntity>

    @Query(
        """
        DELETE FROM delivery_logs
        WHERE attempted_at < :threshold
    """
    )
    suspend fun deleteOldLogs(threshold: Long)

    @Query("SELECT COUNT(*) FROM delivery_logs")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM delivery_logs WHERE status = 'FAILED'")
    fun observeFailedCount(): Flow<Int>
}