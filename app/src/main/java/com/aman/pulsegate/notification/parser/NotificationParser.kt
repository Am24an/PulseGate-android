package com.aman.pulsegate.notification.parser

import android.service.notification.StatusBarNotification

interface NotificationParser {
    fun canParse(sbn: StatusBarNotification): Boolean
    fun parse(sbn: StatusBarNotification): ParsedNotification?
}