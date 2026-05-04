package com.aman.pulsegate.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aman.pulsegate.data.db.converter.Converters
import com.aman.pulsegate.data.db.dao.DeliveryLogDao
import com.aman.pulsegate.data.db.dao.DeliveryQueueDao
import com.aman.pulsegate.data.db.dao.DestinationDao
import com.aman.pulsegate.data.db.dao.IncomingEventDao
import com.aman.pulsegate.data.db.entity.DeliveryLogEntity
import com.aman.pulsegate.data.db.entity.DeliveryQueueEntity
import com.aman.pulsegate.data.db.entity.DestinationEntity
import com.aman.pulsegate.data.db.entity.IncomingEventEntity

@Database(
    entities = [
        IncomingEventEntity::class,
        DeliveryQueueEntity::class,
        DestinationEntity::class,
        DeliveryLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun incomingEventDao(): IncomingEventDao
    abstract fun deliveryQueueDao(): DeliveryQueueDao
    abstract fun destinationDao(): DestinationDao
    abstract fun deliveryLogDao(): DeliveryLogDao

    companion object {
        const val DATABASE_NAME = "pulsegate.db"
    }
}