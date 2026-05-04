package com.aman.pulsegate.notification.parser

import android.service.notification.StatusBarNotification
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankingNotificationParser @Inject constructor() : NotificationParser {

    override fun canParse(sbn: StatusBarNotification): Boolean =
        sbn.packageName in BANKING_PACKAGES

    override fun parse(sbn: StatusBarNotification): ParsedNotification? {
        return try {
            val extras = sbn.notification?.extras ?: run {
                Timber.w("BankingNotificationParser: null extras pkg=${sbn.packageName}")
                return null
            }
            val title = extras.getCharSequence("android.title")?.toString()
            val body = extras.getCharSequence("android.text")?.toString()
                ?: extras.getCharSequence("android.bigText")?.toString()

            if (body.isNullOrBlank()) {
                Timber.w("BankingNotificationParser: blank body pkg=${sbn.packageName}")
                return null
            }

            ParsedNotification(
                sender = BANKING_PACKAGES[sbn.packageName] ?: sbn.packageName,
                body = body,
                title = title,
                appPackage = sbn.packageName,
                rawPayload = title ?: body,
                receivedTimestamp = sbn.postTime
            )
        } catch (e: Exception) {
            Timber.e(e, "BankingNotificationParser: unexpected error pkg=${sbn.packageName}")
            null
        }
    }

    companion object {
        val BANKING_PACKAGES: Map<String, String> = mapOf(
            // UPI Payment Apps
            "com.phonepe.app" to "PhonePe",
            "com.google.android.apps.nbu.paisa.user" to "Google Pay",
            "net.one97.paytm" to "Paytm",
            "in.amazon.mShop.android.shopping" to "Amazon Pay",
            "com.fampay.in" to "FamPay",
            // Banking Apps
            "com.sbi.lotusintouch" to "YONO SBI",
            "com.snapwork.hdfc" to "HDFC Bank",
            "com.hdfcbank.android.now" to "HDFC Bank",
            "com.csam.icici.bank.imobile" to "ICICI Bank",
            "com.axis.mobile" to "Axis Bank",
            "com.msf.kbank.mobile" to "Kotak Bank",
            "com.kotak811mobilebankingapp.instantsavingsupiscanandpayrecharge" to "Kotak 811",
            "com.yesmfbank.app" to "Yes Bank"
        )
    }
}