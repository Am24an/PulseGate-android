package com.aman.pulsegate.ui.permission

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    init {
        refreshPermissionStates()
    }

    fun refreshPermissionStates() {
        _uiState.update {
            PermissionUiState(
                receiveSms = checkRuntimePermission(Manifest.permission.RECEIVE_SMS),
                postNotifications = checkPostNotifications(),
                notificationListener = checkNotificationListenerAccess(),
                batteryOptimization = checkBatteryOptimization()
            )
        }
        Timber.d("PermissionViewModel: state refreshed — ${_uiState.value}")
    }

    private fun checkRuntimePermission(permission: String): GrantState {
        val granted = ContextCompat.checkSelfPermission(
            getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
        return if (granted) GrantState.GRANTED else GrantState.DENIED
    }

    private fun checkPostNotifications(): GrantState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkRuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantState.GRANTED
        }
    }

    private fun checkNotificationListenerAccess(): GrantState {
        val enabledListeners = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            "enabled_notification_listeners"
        )
        val packageName = getApplication<Application>().packageName
        return if (!enabledListeners.isNullOrBlank() && enabledListeners.contains(packageName)) {
            GrantState.GRANTED
        } else {
            GrantState.DENIED
        }
    }

    private fun checkBatteryOptimization(): GrantState {
        val pm = getApplication<Application>().getSystemService(PowerManager::class.java)
        val packageName = getApplication<Application>().packageName
        return if (pm.isIgnoringBatteryOptimizations(packageName)) {
            GrantState.GRANTED
        } else {
            GrantState.DENIED
        }
    }
}