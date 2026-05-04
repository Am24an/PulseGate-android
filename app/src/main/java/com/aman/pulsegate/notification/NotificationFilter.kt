package com.aman.pulsegate.notification

interface NotificationFilter {
    fun isAllowed(packageName: String): Boolean
}