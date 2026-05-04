package com.aman.pulsegate.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.pulsegate.background.WorkScheduler
import com.aman.pulsegate.domain.model.DeliveryLog
import com.aman.pulsegate.domain.usecase.GetDeliveryLogsUseCase
import com.aman.pulsegate.domain.usecase.RetryFailedEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LogsUiState(
    val logs: List<DeliveryLog> = emptyList(),
    val isLoading: Boolean = true,
    // queueId of the item currently being retried — drives per-row loading indicator
    val retryingQueueId: Long? = null,
    val retryError: String? = null
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val getDeliveryLogsUseCase: GetDeliveryLogsUseCase,
    private val retryFailedEventUseCase: RetryFailedEventUseCase,
    private val workScheduler: WorkScheduler
) : ViewModel() {

    private val _retryState = MutableStateFlow<Pair<Long?, String?>>(null to null)

    val uiState: StateFlow<LogsUiState> = getDeliveryLogsUseCase
        .observeRecent(limit = 100)
        .map { logs ->
            val (retryingId, retryError) = _retryState.value
            LogsUiState(
                logs = logs,
                isLoading = false,
                retryingQueueId = retryingId,
                retryError = retryError
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogsUiState()
        )

    fun retry(queueId: Long) {
        if (_retryState.value.first != null) return
        viewModelScope.launch {
            _retryState.value = queueId to null
            retryFailedEventUseCase(queueId)
                .onSuccess {
                    workScheduler.scheduleImmediateDelivery()
                    Timber.d("LogsViewModel: retry queued for queueId=$queueId")
                    _retryState.value = null to null
                }
                .onFailure { error ->
                    Timber.e(error, "LogsViewModel: retry failed for queueId=$queueId")
                    _retryState.value = null to (error.message ?: "Retry failed")
                }
        }
    }

    fun clearRetryError() {
        _retryState.update { it.copy(second = null) }
    }
}