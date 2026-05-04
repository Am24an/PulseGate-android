package com.aman.pulsegate.sender

import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.model.DestinationType
import com.aman.pulsegate.domain.model.IncomingEvent
import com.aman.pulsegate.domain.model.SendResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SenderEngine @Inject constructor(
    private val webhookSender: WebhookSender,
    private val telegramSender: TelegramSender
) {

    suspend fun dispatch(event: IncomingEvent, destination: Destination): SendResult {
        Timber.d("SenderEngine: dispatching eventId=${event.id} to=${destination.name} type=${destination.type}")

        return when (destination.type) {
            DestinationType.WEBHOOK -> webhookSender.send(event, destination)
            DestinationType.TELEGRAM -> telegramSender.send(event, destination)
        }
    }
}