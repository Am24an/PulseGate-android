package com.aman.pulsegate.notification.parser

import android.service.notification.StatusBarNotification
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParserDispatcher @Inject constructor(
    private val bankingParser: BankingNotificationParser,
    private val genericParser: GenericNotificationParser
) {
    private val parsers: List<NotificationParser> by lazy {
        listOf(bankingParser, genericParser)
    }

    fun dispatch(sbn: StatusBarNotification): ParsedNotification? {
        val parser = parsers.firstOrNull { it.canParse(sbn) }
        if (parser == null) {
            Timber.e("ParserDispatcher: no parser found for pkg=${sbn.packageName} — this is a bug")
            return null
        }
        Timber.d("ParserDispatcher: using ${parser::class.simpleName} for pkg=${sbn.packageName}")
        return parser.parse(sbn)
    }
}