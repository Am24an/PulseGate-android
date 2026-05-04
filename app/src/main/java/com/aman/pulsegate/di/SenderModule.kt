package com.aman.pulsegate.di

import com.aman.pulsegate.sender.Sender
import com.aman.pulsegate.sender.TelegramSender
import com.aman.pulsegate.sender.WebhookSender
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SenderModule {

    @Binds
    @Singleton
    @Named("webhook")
    abstract fun bindWebhookSender(impl: WebhookSender): Sender

    @Binds
    @Singleton
    @Named("telegram")
    abstract fun bindTelegramSender(impl: TelegramSender): Sender
}