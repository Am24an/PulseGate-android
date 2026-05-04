package com.aman.pulsegate.di

import com.aman.pulsegate.data.repository.DestinationRepositoryImpl
import com.aman.pulsegate.data.repository.EventRepositoryImpl
import com.aman.pulsegate.data.repository.LogRepositoryImpl
import com.aman.pulsegate.data.repository.QueueRepositoryImpl
import com.aman.pulsegate.domain.repository.DestinationRepository
import com.aman.pulsegate.domain.repository.EventRepository
import com.aman.pulsegate.domain.repository.LogRepository
import com.aman.pulsegate.domain.repository.QueueRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        impl: EventRepositoryImpl
    ): EventRepository

    @Binds
    @Singleton
    abstract fun bindQueueRepository(
        impl: QueueRepositoryImpl
    ): QueueRepository

    @Binds
    @Singleton
    abstract fun bindDestinationRepository(
        impl: DestinationRepositoryImpl
    ): DestinationRepository

    @Binds
    @Singleton
    abstract fun bindLogRepository(
        impl: LogRepositoryImpl
    ): LogRepository
}