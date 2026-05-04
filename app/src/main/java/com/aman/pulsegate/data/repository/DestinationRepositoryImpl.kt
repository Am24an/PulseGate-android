package com.aman.pulsegate.data.repository

import androidx.room.withTransaction
import com.aman.pulsegate.data.db.AppDatabase
import com.aman.pulsegate.data.db.dao.DeliveryQueueDao
import com.aman.pulsegate.data.db.dao.DestinationDao
import com.aman.pulsegate.data.db.entity.DestinationEntity
import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DestinationRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val destinationDao: DestinationDao,
    private val queueDao: DeliveryQueueDao
) : DestinationRepository {

    override suspend fun insert(destination: Destination): Long {
        return destinationDao.insert(DestinationEntity.fromDomain(destination))
    }

    override suspend fun update(destination: Destination) {
        destinationDao.update(DestinationEntity.fromDomain(destination))
    }

    override suspend fun getById(id: Long): Destination? {
        return destinationDao.getById(id)?.toDomain()
    }

    override suspend fun getActiveDestinations(): List<Destination> {
        return destinationDao.getActiveDestinations().map { it.toDomain() }
    }

    override fun observeAll(): Flow<List<Destination>> {
        return destinationDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun setActive(id: Long, isActive: Boolean) {
        destinationDao.setActive(id, isActive)
    }

    override suspend fun delete(id: Long) {
        db.withTransaction {
            val activeCount = queueDao.countActiveItemsForDestination(id)
            if (activeCount > 0) {
                destinationDao.setActive(id, false)
            } else {
                destinationDao.deleteById(id)
            }
        }
    }

    override suspend fun getActiveCount(): Int {
        return destinationDao.getActiveCount()
    }
}