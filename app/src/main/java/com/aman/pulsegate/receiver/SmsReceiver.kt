package com.aman.pulsegate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.aman.pulsegate.background.WorkScheduler
import com.aman.pulsegate.domain.model.SourceType
import com.aman.pulsegate.domain.usecase.SaveIncomingEventUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var saveIncomingEventUseCase: SaveIncomingEventUseCase

    @Inject
    lateinit var workScheduler: WorkScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                processSms(intent)
            } catch (e: Exception) {
                Timber.e(e, "SmsReceiver: unhandled exception")
            } finally {
                pendingResult.finish() // always release Android's process hold
                scope.cancel()        // always cancel scope — no ghost coroutines
            }
        }
    }

    private suspend fun processSms(intent: Intent) {
        // Official Android API — handles PDU format, multi-part SMS internally
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        if (messages.isNullOrEmpty()) {
            Timber.w("SmsReceiver: no messages in intent")
            return
        }

        val sender = messages[0].originatingAddress
        if (sender.isNullOrBlank()) {
            Timber.w("SmsReceiver: null or blank sender, dropping")
            return
        }

        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        if (body.isBlank()) {
            Timber.w("SmsReceiver: blank body from sender=$sender, dropping")
            return
        }

        // Fallback to current time if PDU timestamp is corrupt or zero
        val receivedTimestamp = messages[0].timestampMillis
            .takeIf { it > 0 } ?: System.currentTimeMillis()

        Timber.d("SmsReceiver: SMS from=$sender length=${body.length} ts=$receivedTimestamp")

        val result = saveIncomingEventUseCase(
            sender = sender,
            body = body,
            receivedTimestamp = receivedTimestamp,
            sourceType = SourceType.SMS
        )

        result.fold(
            onSuccess = { eventId ->
                when {
                    eventId == -1L -> Timber.d("SmsReceiver: duplicate ignored from sender=$sender")
                    else -> {
                        Timber.d("SmsReceiver: saved eventId=$eventId, scheduling delivery")
                        workScheduler.scheduleImmediateDelivery()
                    }
                }
            },
            onFailure = { error ->
                Timber.e(error, "SmsReceiver: failed to save from sender=$sender")
            }
        )
    }
}