package com.aman.pulsegate.data.repository

import androidx.room.withTransaction
import com.aman.pulsegate.data.db.AppDatabase
import com.aman.pulsegate.data.db.dao.DeliveryQueueDao
import com.aman.pulsegate.data.db.dao.DestinationDao
import com.aman.pulsegate.data.db.dao.IncomingEventDao
import com.aman.pulsegate.data.db.entity.DeliveryQueueEntity
import com.aman.pulsegate.data.db.entity.IncomingEventEntity
import com.aman.pulsegate.domain.model.DeliveryQueue
import com.aman.pulsegate.domain.model.IncomingEvent
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val eventDao: IncomingEventDao,
    private val queueDao: DeliveryQueueDao,
    private val destinationDao: DestinationDao
) : EventRepository {

    override suspend fun saveEvent(event: IncomingEvent): Long {
        return db.withTransaction {
            val eventId = eventDao.insert(IncomingEventEntity.fromDomain(event))

            if (eventId == -1L) {
                Timber.d("Duplicate event ignored: hash=${event.eventHash}")
                return@withTransaction -1L
            }

            val activeDestinations = destinationDao.getActiveDestinations()

            if (activeDestinations.isEmpty()) {
                Timber.w("No active destinations. Marking event as FAILED: eventId=$eventId")
                eventDao.updateStatus(eventId, QueueStatus.FAILED)
                return@withTransaction eventId
            }

            val now = System.currentTimeMillis()
            val queueEntries = activeDestinations.map { destination ->
                DeliveryQueueEntity(
                    eventId = eventId,
                    destinationId = destination.id,
                    status = QueueStatus.PENDING,
                    retryCount = 0,
                    maxRetry = DeliveryQueue.DEFAULT_MAX_RETRY,
                    nextRetryAt = 0L,
                    lastAttemptAt = null,
                    locked = false,
                    lockedAt = null,
                    workerId = null,
                    createdAt = now,
                    updatedAt = now
                )
            }

            queueDao.insertAll(queueEntries)

            Timber.d("Event saved: eventId=$eventId, queued for ${activeDestinations.size} destination(s)")
            eventId
        }
    }

    override suspend fun getEventById(id: Long): IncomingEvent? {
        return eventDao.getById(id)?.toDomain()
    }

    override fun observeAll(): Flow<List<IncomingEvent>> {
        return eventDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun observeCountByStatus(status: QueueStatus): Flow<Int> {
        return eventDao.observeCountByStatus(status)
    }

    override suspend fun updateStatus(id: Long, status: QueueStatus) {
        eventDao.updateStatus(id, status)
    }

    override suspend fun deleteOldSentEvents(olderThanMs: Long) {
        eventDao.deleteOldSentEvents(olderThanMs)
    }
}