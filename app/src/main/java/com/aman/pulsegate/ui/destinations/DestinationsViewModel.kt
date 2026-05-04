package com.aman.pulsegate.ui.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.repository.DestinationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DestinationsUiState(
    val destinations: List<Destination> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DestinationsViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository
) : ViewModel() {

    val uiState: StateFlow<DestinationsUiState> = destinationRepository
        .observeAll()
        .map { list -> DestinationsUiState(destinations = list, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DestinationsUiState()
        )

    fun toggleActive(destination: Destination) {
        viewModelScope.launch {
            destinationRepository.setActive(destination.id, !destination.isActive)
        }
    }

    fun delete(destination: Destination) {
        viewModelScope.launch {
            destinationRepository.delete(destination.id)
        }
    }
}