package com.aman.pulsegate.notification.parser

data class ParsedNotification(
    val sender: String,
    val body: String,
    val title: String?,
    val appPackage: String,
    val rawPayload: String,
    val receivedTimestamp: Long
)