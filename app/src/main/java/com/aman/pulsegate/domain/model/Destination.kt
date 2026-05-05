package com.aman.pulsegate.domain.model

data class Destination(
    val id: Long = 0,
    val name: String,
    val type: DestinationType,
    val baseUrl: String,
    val method: String,
    val headersJson: String,
    val apiKey: String?,
    val payloadTemplate: String,
    val timeoutSeconds: Int,
    val isActive: Boolean,
    val createdAt: Long
) {
    companion object {

        val DEFAULT_WEBHOOK_PAYLOAD_TEMPLATE: String = """
            {
              "sender": "{{sender}}",
              "message": "{{message}}",
              "received_at": {{received_at}}
            }
        """.trimIndent()
    }
}