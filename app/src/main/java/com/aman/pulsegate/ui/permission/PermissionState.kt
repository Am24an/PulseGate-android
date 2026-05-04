package com.aman.pulsegate.ui.permission

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
    // App cannot function without these three
    val allCriticalGranted: Boolean
        get() = receiveSms == GrantState.GRANTED &&
                postNotifications == GrantState.GRANTED &&
                notificationListener == GrantState.GRANTED
}