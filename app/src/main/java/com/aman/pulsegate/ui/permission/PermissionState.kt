package com.aman.pulsegate.ui.permission

import android.os.Build

enum class GrantState {
    GRANTED,
    DENIED,
    NOT_ASKED
}

data class PermissionUiState(
    val receiveSms: GrantState = GrantState.NOT_ASKED,
    val postNotifications: GrantState = GrantState.NOT_ASKED,
    val notificationListener: GrantState = GrantState.NOT_ASKED,
    val batteryOptimization: GrantState = GrantState.NOT_ASKED
) {
    val allCriticalGranted: Boolean
        get() {
            val notifSatisfied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotifications == GrantState.GRANTED
            } else {
                true  // permission does not exist below API 33, treat as satisfied
            }
            return receiveSms == GrantState.GRANTED &&
                    notifSatisfied &&
                    notificationListener == GrantState.GRANTED
        }
}