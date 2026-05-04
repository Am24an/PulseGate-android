package com.aman.pulsegate.notification

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFilterManager @Inject constructor() : NotificationFilter {

    // Phase 8 config loader can override via setAllowedPackages()
    @Volatile
    private var allowedPackages: Set<String> = buildDefaultAllowedPackages()

    override fun isAllowed(packageName: String): Boolean {
        if (packageName.isBlank()) {
            Timber.w("NotificationFilterManager: blank packageName rejected")
            return false
        }
        val allowed = packageName in allowedPackages
        if (!allowed) Timber.v("NotificationFilterManager: blocked packageName=$packageName")
        return allowed
    }

    // require guard prevents accidental empty set — would pass ALL notifications through
    fun setAllowedPackages(packages: Set<String>) {
        require(packages.isNotEmpty()) { "allowedPackages must not be empty" }
        allowedPackages = packages
        Timber.d("NotificationFilterManager: allowedPackages updated count=${packages.size}")
    }

    fun getAllowedPackages(): Set<String> = allowedPackages

    private fun buildDefaultAllowedPackages(): Set<String> = setOf(
        // UPI Payment Apps
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "in.amazon.mShop.android.shopping",
        "com.fampay.in",

        // Banking Apps
        "com.sbi.lotusintouch",
        "com.snapwork.hdfc",
        "com.hdfcbank.android.now",
        "com.csam.icici.bank.imobile",
        "com.axis.mobile",
        "com.msf.kbank.mobile",
        "com.kotak811mobilebankingapp.instantsavingsupiscanandpayrecharge",
        "com.yesmfbank.app"
    )
}