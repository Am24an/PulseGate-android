package com.aman.pulsegate.domain.usecase

import com.aman.pulsegate.domain.model.IncomingEvent
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.model.SourceType
import com.aman.pulsegate.domain.repository.EventRepository
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject

class SaveIncomingEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {

    suspend operator fun invoke(
        sender: String,
        body: String,
        receivedTimestamp: Long,
        sourceType: SourceType,
        title: String? = null,
        appPackage: String? = null,
        rawPayload: String = body
    ): Result<Long> {
        if (sender.isBlank() || body.isBlank()) {
            return Result.failure(IllegalArgumentException("Sender or body is blank"))
        }

        val hash = generateHash(sender, body, receivedTimestamp)
        val now = System.currentTimeMillis()

        val event = IncomingEvent(
            eventHash = hash,
            sourceType = sourceType,
            sender = sender,
            title = title,
            message = body,
            rawPayload = rawPayload,
            appPackage = appPackage,
            receivedTimestamp = receivedTimestamp,
            processingStatus = QueueStatus.PENDING,
            createdAt = now
        )

        return runCatching {
            eventRepository.saveEvent(event)
        }.onFailure { error ->
            Timber.e(error, "SaveIncomingEventUseCase failed for sender=$sender")
        }
    }

    private fun generateHash(sender: String, body: String, timestamp: Long): String {
        val input = "$sender\u0000$body\u0000$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}