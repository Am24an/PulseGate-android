package com.aman.pulsegate.notification

import android.service.notification.StatusBarNotification
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationDeduplicator @Inject constructor() {

    fun generateHash(sbn: StatusBarNotification): String {
        return generateHash(
            packageName = sbn.packageName,
            notificationKey = sbn.key,
            postTime = sbn.postTime
        )
    }

    fun generateHash(
        packageName: String,
        notificationKey: String,
        postTime: Long
    ): String {
        val input = "$packageName\u0000$notificationKey\u0000$postTime"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        Timber.v("NotificationDeduplicator: hash=$hash pkg=$packageName key=$notificationKey ts=$postTime")
        return hash
    }
}