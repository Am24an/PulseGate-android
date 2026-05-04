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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhookSender @Inject constructor(
    private val okHttpClientProvider: OkHttpClientProvider
) : Sender {

    private val moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    override suspend fun send(event: IncomingEvent, destination: Destination): SendResult {
        val startTime = System.currentTimeMillis()

        return try {
            val client = okHttpClientProvider.provide(destination.timeoutSeconds)
            val method = destination.method.uppercase()
            val payload = buildPayload(event)

            val requestBuilder = Request.Builder().url(destination.baseUrl)
            if (method == "GET") {
                requestBuilder.get()
            } else {
                requestBuilder.method(
                    method,
                    payload.toRequestBody("application/json".toMediaType())
                )
            }
            applyHeaders(requestBuilder, destination)

            val response = withContext(Dispatchers.IO) {
                client.newCall(requestBuilder.build()).execute()
            }
            val latencyMs = System.currentTimeMillis() - startTime

            Timber.d("WebhookSender: code=${response.code} latency=${latencyMs}ms")

            response.use {
                if (it.isSuccessful) {
                    SendResult.Success(httpCode = it.code, latencyMs = latencyMs)
                } else {
                    val responseBody = runCatching { it.body?.string() }.getOrNull()
                    SendResult.HttpError(code = it.code, body = responseBody)
                }
            }
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "WebhookSender: timeout destination=${destination.name}")
            SendResult.NetworkError(exception = e)
        } catch (e: IOException) {
            Timber.e(e, "WebhookSender: IO error destination=${destination.name}")
            SendResult.NetworkError(exception = e)
        } catch (e: Exception) {
            Timber.e(e, "WebhookSender: unexpected error destination=${destination.name}")
            SendResult.UnknownError(message = e.message ?: "Unknown error in WebhookSender")
        }
    }

    private fun buildPayload(event: IncomingEvent): String {
        val map: Map<String, Any?> = mapOf(
            "id" to event.id,
            "source_type" to event.sourceType.name,
            "sender" to event.sender,
            "title" to event.title,
            "message" to event.message,
            "app_package" to event.appPackage,
            "received_at" to event.receivedTimestamp,
            "created_at" to event.createdAt
        )
        return mapAdapter.toJson(map)
    }

    private fun applyHeaders(builder: Request.Builder, destination: Destination) {
        runCatching {
            val headers = mapAdapter.fromJson(destination.headersJson) as? Map<*, *>
            headers?.forEach { (key, value) ->
                val k = key as? String ?: return@forEach
                val v = value as? String ?: return@forEach
                builder.addHeader(k, v)
            }
        }.onFailure {
            Timber.w("WebhookSender: failed to parse headers destination=${destination.name}")
        }

        destination.apiKey?.takeIf { it.isNotBlank() }?.let {
            builder.addHeader("Authorization", "Bearer $it")
        }
    }
}