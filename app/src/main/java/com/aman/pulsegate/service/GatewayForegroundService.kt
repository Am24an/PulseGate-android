package com.aman.pulsegate.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.aman.pulsegate.background.WorkScheduler
import com.aman.pulsegate.connectivity.ConnectivityObserver
import com.aman.pulsegate.notification.DeliveryNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GatewayForegroundService : Service() {

    @Inject
    lateinit var workScheduler: WorkScheduler

    @Inject
    lateinit var notificationManager: DeliveryNotificationManager

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    // Only set to true when ACTION_STOP intent is received — distinguishes intentional stop from OS kill.
    private var stoppedIntentionally = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Timber.d("GatewayForegroundService: onCreate")
        startForegroundWithNotification()
        workScheduler.scheduleAll()
        connectivityObserver.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Timber.d("GatewayForegroundService: ACTION_STOP received — stopping intentionally")
            stoppedIntentionally = true
            stopSelf()
            return START_NOT_STICKY
        }
        Timber.d("GatewayForegroundService: onStartCommand startId=$startId")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        connectivityObserver.stop()

        if (stoppedIntentionally) {
            workScheduler.cancelAll()
            Timber.d("GatewayForegroundService: onDestroy — intentional stop, workers cancelled")
        } else {
            Timber.d("GatewayForegroundService: onDestroy — OS kill, workers kept, START_STICKY will restart")
        }
    }

    private fun startForegroundWithNotification() {
        val notification = notificationManager.buildWorkerNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        Timber.d("GatewayForegroundService: foreground started notificationId=$NOTIFICATION_ID")
    }

    companion object {
        private const val NOTIFICATION_ID = 1000
        private const val ACTION_STOP = "com.aman.pulsegate.service.ACTION_STOP"

        private val _isRunning = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunning.asStateFlow()

        // Reflects whether the service is currently alive.
        // Read by DashboardViewModel to show service status chip.
        // @Volatile ensures visibility across threads without locking overhead.
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, GatewayForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Timber.d("GatewayForegroundService: start called")
        }

        fun stop(context: Context) {
            // Sends ACTION_STOP so onStartCommand sets stoppedIntentionally before onDestroy fires.
            val intent = Intent(context, GatewayForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Timber.d("GatewayForegroundService: stop called — ACTION_STOP sent")
        }
    }
}