package com.aman.pulsegate

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aman.pulsegate.background.WorkScheduler
import com.aman.pulsegate.service.GatewayForegroundService
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PulseGateApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workScheduler: WorkScheduler

    override fun onCreate() {
        super.onCreate()
        initLogging()
        // Guard: only auto-start service if permissions already granted (re-launch / device reboot)
        // First launch path: PermissionScreen → onAllPermissionsGranted → NavGraph starts service
        if (areCriticalPermissionsGranted()) {
            GatewayForegroundService.start(this)
        }
        workScheduler.scheduleAll()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.ENABLE_LOGGING) android.util.Log.DEBUG
                else android.util.Log.ERROR
            )
            .build()

    private fun areCriticalPermissionsGranted(): Boolean {
        val smsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val listenerGranted = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )?.contains(packageName) == true

        return smsGranted && notifGranted && listenerGranted
    }

    private fun initLogging() {
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
            Timber.d("PulseGateApp: onCreate — logging initialized")
        }
    }
}