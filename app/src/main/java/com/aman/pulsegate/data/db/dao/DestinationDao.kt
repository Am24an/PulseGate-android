package com.aman.pulsegate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aman.pulsegate.data.db.entity.DestinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DestinationEntity): Long

    @Update
    suspend fun update(entity: DestinationEntity)

    @Query("SELECT * FROM destinations WHERE is_active = 1")
    suspend fun getActiveDestinations(): List<DestinationEntity>

    @Query("SELECT * FROM destinations ORDER BY created_at ASC")
    fun observeAll(): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun getById(id: Long): DestinationEntity?

    @Query("UPDATE destinations SET is_active = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Query("DELETE FROM destinations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM destinations WHERE is_active = 1")
    suspend fun getActiveCount(): Int
}