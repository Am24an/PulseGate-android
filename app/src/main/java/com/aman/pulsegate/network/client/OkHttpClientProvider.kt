package com.aman.pulsegate.network.client

import com.aman.pulsegate.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpClientProvider @Inject constructor() {

    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (BuildConfig.ENABLE_LOGGING) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
        }
        .build()

    fun provide(timeoutSeconds: Int): OkHttpClient {
        return baseClient.newBuilder()
            .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
    }
}