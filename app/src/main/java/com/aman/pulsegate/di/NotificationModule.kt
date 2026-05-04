package com.aman.pulsegate.di

import com.aman.pulsegate.notification.NotificationFilter
import com.aman.pulsegate.notification.NotificationFilterManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationFilter(
        impl: NotificationFilterManager
    ): NotificationFilter
}