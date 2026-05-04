package com.aman.pulsegate.domain.model

sealed class SendResult {

    data class Success(val httpCode: Int, val latencyMs: Long) : SendResult()

    data class HttpError(val code: Int, val body: String?) : SendResult()

    data class NetworkError(val exception: Throwable) : SendResult()

    data class UnknownError(val message: String) : SendResult()
}