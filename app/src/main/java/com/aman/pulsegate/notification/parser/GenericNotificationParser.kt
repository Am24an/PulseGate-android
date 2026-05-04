package com.aman.pulsegate.notification.parser

import android.service.notification.StatusBarNotification
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenericNotificationParser @Inject constructor() : NotificationParser {

    override fun canParse(sbn: StatusBarNotification): Boolean = true

    override fun parse(sbn: StatusBarNotification): ParsedNotification? {
        return try {
            val extras = sbn.notification?.extras ?: run {
                Timber.w("GenericNotificationParser: null extras pkg=${sbn.packageName}")
                return null
            }
            val title = extras.getCharSequence("android.title")?.toString()
            val body = extras.getCharSequence("android.text")?.toString()
                ?: extras.getCharSequence("android.bigText")?.toString()

            if (body.isNullOrBlank()) {
                Timber.w("GenericNotificationParser: blank body pkg=${sbn.packageName}, skipping")
                return null
            }

            ParsedNotification(
                sender = sbn.packageName,
                body = body,
                title = title,
                appPackage = sbn.packageName,
                rawPayload = title ?: body,
                receivedTimestamp = sbn.postTime
            )
        } catch (e: Exception) {
            Timber.e(e, "GenericNotificationParser: unexpected error pkg=${sbn.packageName}")
            null
        }
    }
}