package com.aman.pulsegate.sender

import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.model.IncomingEvent
import com.aman.pulsegate.domain.model.SendResult

interface Sender {
    suspend fun send(event: IncomingEvent, destination: Destination): SendResult
}