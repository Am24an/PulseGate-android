package com.aman.pulsegate.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.aman.pulsegate.background.WorkScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val workScheduler: WorkScheduler
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("ConnectivityObserver: network available — triggering delivery")
            workScheduler.scheduleDeliveryOnConnectivity()
        }

        override fun onLost(network: Network) {
            Timber.d("ConnectivityObserver: network lost")
        }
    }

    fun start() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            Timber.d("ConnectivityObserver: registered")
        }.onFailure {
            Timber.e(it, "ConnectivityObserver: failed to register callback")
        }
    }

    fun stop() {
        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Timber.d("ConnectivityObserver: unregistered")
        }.onFailure {
            Timber.e(it, "ConnectivityObserver: failed to unregister callback")
        }
    }
}