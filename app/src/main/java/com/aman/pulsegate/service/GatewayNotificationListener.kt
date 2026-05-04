package com.aman.pulsegate.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aman.pulsegate.background.WorkScheduler
import com.aman.pulsegate.domain.model.SourceType
import com.aman.pulsegate.domain.usecase.SaveIncomingEventUseCase
import com.aman.pulsegate.notification.NotificationDeduplicator
import com.aman.pulsegate.notification.NotificationFilter
import com.aman.pulsegate.notification.parser.ParserDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GatewayNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var notificationFilter: NotificationFilter

    @Inject
    lateinit var notificationDeduplicator: NotificationDeduplicator

    @Inject
    lateinit var parserDispatcher: ParserDispatcher

    @Inject
    lateinit var saveIncomingEventUseCase: SaveIncomingEventUseCase

    @Inject
    lateinit var workScheduler: WorkScheduler

    // Tied to listener connection lifecycle —canceled when permission is revoked
    private var serviceJob: Job = SupervisorJob()
    private var serviceScope: CoroutineScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceJob = SupervisorJob()
        serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
        Timber.d("GatewayNotificationListener: connected — listener active")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        serviceScope.cancel()
        Timber.d("GatewayNotificationListener: disconnected — scope cancelled")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: run {
            Timber.w("GatewayNotificationListener: received null sbn, skipping")
            return
        }

        // Step 1: Filter — runs synchronously, O(1) Set lookup, no coroutine overhead
        if (!notificationFilter.isAllowed(notification.packageName)) return

        serviceScope.launch {
            processNotification(notification)
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        // Step 2: Parse — extract structured data from the raw StatusBarNotification
        val parsed = parserDispatcher.dispatch(sbn) ?: run {
            Timber.w("GatewayNotificationListener: parser returned null pkg=${sbn.packageName}")
            return
        }

        // Step 3: Deduplicate — generate hash from packageName + key + postTime
        val hash = notificationDeduplicator.generateHash(sbn)
        Timber.d(
            "GatewayNotificationListener: processing pkg=${sbn.packageName} " +
                    "sender=${parsed.sender} hash=${hash.take(8)}…"
        )

        // Step 4: Save — delegates to SaveIncomingEventUseCase which handles
        // dedup at DB level via IGNORE on unique event_hash index
        val result = saveIncomingEventUseCase(
            sender = parsed.sender,
            body = parsed.body,
            receivedTimestamp = parsed.receivedTimestamp,
            sourceType = SourceType.NOTIFICATION,
            title = parsed.title,
            appPackage = parsed.appPackage,
            rawPayload = parsed.rawPayload
        )

        // Step 5: Trigger delivery — only on new unique events
        result.fold(
            onSuccess = { eventId ->
                when {
                    eventId == -1L -> {
                        Timber.d(
                            "GatewayNotificationListener: duplicate notification ignored " +
                                    "pkg=${sbn.packageName}"
                        )
                    }

                    else -> {
                        Timber.d(
                            "GatewayNotificationListener: saved eventId=$eventId " +
                                    "pkg=${sbn.packageName} — scheduling delivery"
                        )
                        workScheduler.scheduleImmediateDelivery()
                    }
                }
            },
            onFailure = { error ->
                Timber.e(
                    error,
                    "GatewayNotificationListener: failed to save notification " +
                            "pkg=${sbn.packageName}"
                )
            }
        )
    }
}