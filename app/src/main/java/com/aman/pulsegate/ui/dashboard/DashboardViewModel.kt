package com.aman.pulsegate.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.domain.repository.DestinationRepository
import com.aman.pulsegate.domain.repository.LogRepository
import com.aman.pulsegate.domain.repository.QueueRepository
import com.aman.pulsegate.service.GatewayForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val activeDestinations: Int = 0,
    val pendingCount: Int = 0,
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val totalLogs: Int = 0,
    val isServiceRunning: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository,
    private val queueRepository: QueueRepository,
    private val logRepository: LogRepository
) : ViewModel() {

    val uiState = combine(
        destinationRepository.observeAll(),
        queueRepository.observeCountByStatus(QueueStatus.PENDING),
        queueRepository.observeCountByStatus(QueueStatus.SENT),
        queueRepository.observeCountByStatus(QueueStatus.FAILED),
        logRepository.observeTotalCount()
    ) { destinations, pending, sent, failed, totalLogs ->
        DashboardUiState(
            activeDestinations = destinations.count { it.isActive },
            pendingCount = pending,
            sentCount = sent,
            failedCount = failed,
            totalLogs = totalLogs,
            isServiceRunning = GatewayForegroundService.isRunning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )
}