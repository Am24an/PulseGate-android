package com.aman.pulsegate.domain.model

enum class QueueStatus {
    PENDING,
    PROCESSING,
    RETRY,
    SENT,
    FAILED
}