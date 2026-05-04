package com.aman.pulsegate.ui.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.model.DestinationType
import com.aman.pulsegate.domain.repository.DestinationRepository
import com.aman.pulsegate.domain.usecase.AddDestinationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormState(
    val name: String = "",
    val type: DestinationType = DestinationType.WEBHOOK,
    val baseUrl: String = "",
    val method: String = "POST",
    val headersJson: String = "",
    val apiKey: String = "",
    val timeoutSeconds: Int = 15,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val nameError: String? = null,
    val baseUrlError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddEditDestinationViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository,
    private val addDestinationUseCase: AddDestinationUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    private var editingId: Long? = null

    fun loadDestination(destinationId: Long) {
        viewModelScope.launch {
            val destination = destinationRepository.getById(destinationId) ?: return@launch
            editingId = destinationId
            _formState.update {
                FormState(
                    name = destination.name,
                    type = destination.type,
                    baseUrl = destination.baseUrl,
                    method = destination.method,
                    headersJson = destination.headersJson,
                    apiKey = destination.apiKey ?: "",
                    timeoutSeconds = destination.timeoutSeconds,
                    isActive = destination.isActive,
                    createdAt = destination.createdAt
                )
            }
        }
    }

    fun onNameChange(value: String) =
        _formState.update { it.copy(name = value, nameError = null) }

    fun onTypeChange(value: DestinationType) =
        _formState.update { it.copy(type = value, baseUrlError = null) }

    fun onBaseUrlChange(value: String) =
        _formState.update { it.copy(baseUrl = value, baseUrlError = null) }

    fun onMethodChange(value: String) =
        _formState.update { it.copy(method = value) }

    fun onHeadersJsonChange(value: String) =
        _formState.update { it.copy(headersJson = value) }

    fun onApiKeyChange(value: String) =
        _formState.update { it.copy(apiKey = value) }

    fun onTimeoutChange(value: Int) =
        _formState.update { it.copy(timeoutSeconds = value) }

    fun onIsActiveChange(value: Boolean) =
        _formState.update { it.copy(isActive = value) }

    fun save() {
        if (!validate()) return
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            val state = _formState.value
            val id = editingId

            val destination = Destination(
                id = id ?: 0L,
                name = state.name.trim(),
                type = state.type,
                baseUrl = state.baseUrl.trim(),
                method = state.method,
                headersJson = state.headersJson.trim(),
                apiKey = state.apiKey.trim().ifBlank { null },
                timeoutSeconds = state.timeoutSeconds,
                isActive = state.isActive,
                createdAt = if (id == null) System.currentTimeMillis() else state.createdAt
            )

            val result = if (id == null) {
                addDestinationUseCase(destination)
            } else {
                runCatching { destinationRepository.update(destination) }
                    .map { 1L }
            }

            result.fold(
                onSuccess = {
                    _formState.update { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { error ->
                    val message = error.message ?: "Failed to save destination"
                    _formState.update {
                        it.copy(
                            isSaving = false,
                            nameError = if (it.name.isBlank()) message else null
                        )
                    }
                }
            )
        }
    }

    private fun validate(): Boolean {
        val state = _formState.value
        var valid = true
        if (state.name.isBlank()) {
            _formState.update { it.copy(nameError = "Name is required") }
            valid = false
        }
        if (state.baseUrl.isBlank()) {
            _formState.update {
                it.copy(
                    baseUrlError = if (state.type == DestinationType.TELEGRAM)
                        "Chat ID is required" else "URL is required"
                )
            }
            valid = false
        }
        return valid
    }
}