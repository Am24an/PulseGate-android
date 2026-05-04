package com.aman.pulsegate.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.model.DestinationType

@Entity(
    tableName = "destinations",
    indices = [
        Index(value = ["is_active"])
    ]
)
data class DestinationEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: DestinationType,

    @ColumnInfo(name = "base_url")
    val baseUrl: String,

    @ColumnInfo(name = "method")
    val method: String,

    @ColumnInfo(name = "headers_json")
    val headersJson: String,

    @ColumnInfo(name = "api_key")
    val apiKey: String?,

    @ColumnInfo(name = "timeout_seconds")
    val timeoutSeconds: Int,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    fun toDomain(): Destination = Destination(
        id = id,
        name = name,
        type = type,
        baseUrl = baseUrl,
        method = method,
        headersJson = headersJson,
        apiKey = apiKey,
        timeoutSeconds = timeoutSeconds,
        isActive = isActive,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: Destination): DestinationEntity = DestinationEntity(
            id = domain.id,
            name = domain.name,
            type = domain.type,
            baseUrl = domain.baseUrl,
            method = domain.method,
            headersJson = domain.headersJson,
            apiKey = domain.apiKey,
            timeoutSeconds = domain.timeoutSeconds,
            isActive = domain.isActive,
            createdAt = domain.createdAt
        )
    }
}