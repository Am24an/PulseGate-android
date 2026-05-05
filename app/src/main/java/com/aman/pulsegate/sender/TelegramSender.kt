package com.aman.pulsegate.sender

import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.model.IncomingEvent
import com.aman.pulsegate.domain.model.SendResult
import com.aman.pulsegate.network.client.OkHttpClientProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramSender @Inject constructor(
    private val okHttpClientProvider: OkHttpClientProvider
) : Sender {

    private val moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter<Map<String, Any>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )
    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH)

    override suspend fun send(event: IncomingEvent, destination: Destination): SendResult {
        val startTime = System.currentTimeMillis()

        return try {
            val token = destination.apiKey
            if (token.isNullOrBlank()) {
                Timber.e("TelegramSender: bot token missing destination=${destination.name}")
                return SendResult.UnknownError("Telegram bot token missing")
            }

            val chatId = destination.baseUrl
            if (chatId.isBlank()) {
                Timber.e("TelegramSender: chat_id missing destination=${destination.name}")
                return SendResult.UnknownError("Telegram chat_id missing")
            }

            val client = okHttpClientProvider.provide(destination.timeoutSeconds)
            val payload = buildPayload(chatId, buildMessage(event))
            val url = "https://api.telegram.org/bot$token/sendMessage"

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val latencyMs = System.currentTimeMillis() - startTime

            Timber.d("TelegramSender: code=${response.code} latency=${latencyMs}ms")

            response.use {
                if (it.isSuccessful) {
                    SendResult.Success(httpCode = it.code, latencyMs = latencyMs)
                } else {
                    val responseBody = runCatching { it.body.string() }.getOrNull()
                    SendResult.HttpError(code = it.code, body = responseBody)
                }
            }
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "TelegramSender: timeout destination=${destination.name}")
            SendResult.NetworkError(exception = e)
        } catch (e: IOException) {
            Timber.e(e, "TelegramSender: IO error destination=${destination.name}")
            SendResult.NetworkError(exception = e)
        } catch (e: Exception) {
            Timber.e(e, "TelegramSender: unexpected error destination=${destination.name}")
            SendResult.UnknownError(message = e.message ?: "Unknown error in TelegramSender")
        }
    }

    private fun buildMessage(event: IncomingEvent): String = buildString {
        appendLine("📨 *PulseGate Alert*")
        appendLine("Source: ${event.sourceType.name}")
        appendLine("From: `${event.sender}`")
        event.title?.let { appendLine("Title: $it") }
        appendLine("Message: ${event.message}")
        appendLine("Time: ${dateFormat.format(Date(event.receivedTimestamp))}")
    }.trim()

    private fun buildPayload(chatId: String, text: String): String {
        val map: Map<String, Any> = mapOf(
            "chat_id" to chatId,
            "text" to text,
            "parse_mode" to "Markdown"
        )
        return mapAdapter.toJson(map)
    }
}