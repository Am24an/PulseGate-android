package com.aman.pulsegate.background

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    // PulseGateApp.onCreate() + BootReceiver — idempotent via KEEP
    fun scheduleAll() {
        Timber.d("WorkScheduler: scheduling all workers")
        PeriodicDeliveryWorker.schedule(context)
        CleanupWorker.schedule(context)
    }

    // SmsReceiver — after successful DB write
    fun scheduleImmediateDelivery() {
        Timber.d("WorkScheduler: immediate delivery triggered")
        ImmediateDeliveryWorker.enqueue(context)
    }

    // BootReceiver — after stale lock cleanup
    fun scheduleDeliveryOnBoot() {
        Timber.d("WorkScheduler: delivery triggered on boot")
        ImmediateDeliveryWorker.enqueue(context)
    }

    // ConnectivityObserver — on network restored
    fun scheduleDeliveryOnConnectivity() {
        Timber.d("WorkScheduler: delivery triggered on connectivity restored")
        ImmediateDeliveryWorker.enqueue(context)
    }

    // Testing / explicit shutdown only
    fun cancelAll() {
        Timber.d("WorkScheduler: cancelling all workers")
        ImmediateDeliveryWorker.cancel(context)
        PeriodicDeliveryWorker.cancel(context)
        CleanupWorker.cancel(context)
    }
}