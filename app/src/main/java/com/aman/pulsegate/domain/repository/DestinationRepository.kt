package com.aman.pulsegate.domain.repository

import com.aman.pulsegate.domain.model.Destination
import kotlinx.coroutines.flow.Flow

interface DestinationRepository {

    suspend fun insert(destination: Destination): Long

    suspend fun update(destination: Destination)

    suspend fun getById(id: Long): Destination?

    suspend fun getActiveDestinations(): List<Destination>

    fun observeAll(): Flow<List<Destination>>

    suspend fun setActive(id: Long, isActive: Boolean)

    suspend fun delete(id: Long)

    suspend fun getActiveCount(): Int
}