package com.aman.pulsegate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aman.pulsegate.data.db.entity.IncomingEventEntity
import com.aman.pulsegate.domain.model.QueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomingEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: IncomingEventEntity): Long

    @Query("SELECT * FROM incoming_events WHERE id = :id")
    suspend fun getById(id: Long): IncomingEventEntity?

    @Query("SELECT * FROM incoming_events ORDER BY created_at DESC")
    fun observeAll(): Flow<List<IncomingEventEntity>>

    @Query("SELECT COUNT(*) FROM incoming_events WHERE processing_status = :status")
    fun observeCountByStatus(status: QueueStatus): Flow<Int>

    @Query(
        """
        UPDATE incoming_events 
        SET processing_status = :status 
        WHERE id = :id
    """
    )
    suspend fun updateStatus(id: Long, status: QueueStatus)

    @Query(
        """
        DELETE FROM incoming_events 
        WHERE processing_status = 'SENT' 
        AND created_at < :threshold
    """
    )
    suspend fun deleteOldSentEvents(threshold: Long)
}