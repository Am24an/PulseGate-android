package com.aman.pulsegate.domain.model

data class Destination(
    val id: Long = 0,
    val name: String,
    val type: DestinationType,
    val baseUrl: String,
    val method: String,
    val headersJson: String,
    val apiKey: String?,
    val timeoutSeconds: Int,
    val isActive: Boolean,
    val createdAt: Long
)