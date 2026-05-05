package com.aman.pulsegate.di

import android.content.Context
import androidx.room.Room
import com.aman.pulsegate.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideIncomingEventDao(db: AppDatabase) = db.incomingEventDao()

    @Provides
    @Singleton
    fun provideDeliveryQueueDao(db: AppDatabase) = db.deliveryQueueDao()

    @Provides
    @Singleton
    fun provideDestinationDao(db: AppDatabase) = db.destinationDao()

    @Provides
    @Singleton
    fun provideDeliveryLogDao(db: AppDatabase) = db.deliveryLogDao()
}