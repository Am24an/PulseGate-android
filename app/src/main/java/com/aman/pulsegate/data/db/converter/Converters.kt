package com.aman.pulsegate.data.db.converter

import androidx.room.TypeConverter
import com.aman.pulsegate.domain.model.DestinationType
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.model.SourceType

class Converters {

    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun fromQueueStatus(value: QueueStatus): String = value.name

    @TypeConverter
    fun toQueueStatus(value: String): QueueStatus = QueueStatus.valueOf(value)

    @TypeConverter
    fun fromDestinationType(value: DestinationType): String = value.name

    @TypeConverter
    fun toDestinationType(value: String): DestinationType = DestinationType.valueOf(value)
}