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
            val payload = buildPayload(event, destination)

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

    // ✅ CHANGED: Renders destination.payloadTemplate by replacing {{tokens}} with actual event values.
    // Falls back to DEFAULT_WEBHOOK_PAYLOAD_TEMPLATE if template is blank.
    // Type safety is the caller's responsibility via the template — strings need quotes in the
    // template itself (e.g. "{{sender}}"), numbers do not (e.g. {{received_at}}).

    private fun buildPayload(event: IncomingEvent, destination: Destination): String {
        val template = destination.payloadTemplate
            .takeIf { it.isNotBlank() }
            ?: run {
                Timber.w("WebhookSender: payloadTemplate is blank for destination=${destination.name}, using default")
                Destination.DEFAULT_WEBHOOK_PAYLOAD_TEMPLATE
            }

        return template
            .replace("{{sender}}", event.sender)
            .replace("{{message}}", event.message)
            .replace("{{received_at}}", event.receivedTimestamp.toString())
            .replace("{{title}}", event.title.orEmpty())
            .replace("{{source_type}}", event.sourceType.name)
            .replace("{{app_package}}", event.appPackage.orEmpty())
            .replace("{{id}}", event.id.toString())
            .replace("{{created_at}}", event.createdAt.toString())
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