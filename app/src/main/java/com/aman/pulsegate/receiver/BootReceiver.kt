package com.aman.pulsegate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aman.pulsegate.background.WorkScheduler
import com.aman.pulsegate.domain.repository.QueueRepository
import com.aman.pulsegate.service.GatewayForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workScheduler: WorkScheduler

    @Inject
    lateinit var queueRepository: QueueRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        Timber.d("BootReceiver: triggered by action=$action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // Step 1: Release any locks that were held before the device rebooted.
                // Without this, those queue items stay PROCESSING forever — never retried.
                val now = System.currentTimeMillis()
                queueRepository.releaseStaleLocksAndReset(
                    threshold = now - STALE_LOCK_THRESHOLD_MS,
                    currentTime = now
                )
                Timber.d("BootReceiver: stale locks released")

                // Step 2: Start the foreground service.
                // GatewayForegroundService.onCreate() owns scheduleAll() — no duplication here.
                GatewayForegroundService.start(context)
                Timber.d("BootReceiver: foreground service started")

                // Step 3: Trigger an immediate delivery pass for any events queued
                // before the reboot that are now ready to send.
                workScheduler.scheduleDeliveryOnBoot()

                Timber.d("BootReceiver: state fully restored for action=$action")
            } catch (e: Exception) {
                Timber.e(e, "BootReceiver: failed to restore state")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val STALE_LOCK_THRESHOLD_MS = 5 * 60 * 1000L
    }
}