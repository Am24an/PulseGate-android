package com.aman.pulsegate.domain.usecase

import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.model.DestinationType
import com.aman.pulsegate.domain.repository.DestinationRepository
import timber.log.Timber
import javax.inject.Inject

class AddDestinationUseCase @Inject constructor(
    private val destinationRepository: DestinationRepository
) {

    suspend operator fun invoke(destination: Destination): Result<Long> {
        val validationError = validate(destination)
        if (validationError != null) {
            Timber.w("AddDestinationUseCase: validation failed — $validationError")
            return Result.failure(IllegalArgumentException(validationError))
        }

        return runCatching {
            destinationRepository.insert(destination)
        }.onSuccess { id ->
            Timber.d("AddDestinationUseCase: inserted destination id=$id name=${destination.name}")
        }.onFailure { error ->
            Timber.e(
                error,
                "AddDestinationUseCase: failed to insert destination name=${destination.name}"
            )
        }
    }

    private fun validate(destination: Destination): String? {
        if (destination.name.isBlank())
            return "Destination name must not be blank"

        if (destination.baseUrl.isBlank())
            return "URL / Chat ID must not be blank"

        if (destination.type == DestinationType.WEBHOOK &&
            !destination.baseUrl.startsWith("https://")
        )
            return "Webhook URL must start with https://"

        if (destination.timeoutSeconds !in TIMEOUT_MIN..TIMEOUT_MAX)
            return "Timeout must be between ${TIMEOUT_MIN}s and ${TIMEOUT_MAX}s"

        if (destination.headersJson.isNotBlank()) {
            val trimmed = destination.headersJson.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}"))
                return "Headers must be a valid JSON object"
        }

        if (destination.type == DestinationType.TELEGRAM && destination.apiKey.isNullOrBlank())
            return "Telegram bot token (API key) must not be blank"

        // NEW: Payload template validation — only for WEBHOOK type.
        // Blank is allowed — WebhookSender falls back to default template automatically.

        if (destination.type == DestinationType.WEBHOOK &&
            destination.payloadTemplate.isNotBlank()
        ) {
            val trimmed = destination.payloadTemplate.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}"))
                return "Payload template must be a valid JSON object"

            if (UNCLOSED_PLACEHOLDER_REGEX.containsMatchIn(trimmed))
                return "Payload template has an unclosed placeholder — check for typos like {{sender}"
        }

        return null
    }

    companion object {
        private const val TIMEOUT_MIN = 5
        private const val TIMEOUT_MAX = 60

        // Matches {{ not followed by a closing }} — catches {{sender} or {{message typos
        private val UNCLOSED_PLACEHOLDER_REGEX = Regex("""\{\{(?![^{}]*\}\})""")
    }
}