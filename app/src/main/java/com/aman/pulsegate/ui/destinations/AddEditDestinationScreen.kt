package com.aman.pulsegate.ui.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.model.DestinationType
import com.aman.pulsegate.ui.theme.PulseGateTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDestinationScreen(
    destinationId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditDestinationViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    LaunchedEffect(destinationId) {
        destinationId?.let { viewModel.loadDestination(it) }
    }

    LaunchedEffect(formState.isSaved) {
        if (formState.isSaved) onNavigateBack()
    }

    AddEditDestinationScreenContent(
        destinationId = destinationId,
        formState = formState,
        onNavigateBack = onNavigateBack,
        onNameChange = viewModel::onNameChange,
        onTypeChange = viewModel::onTypeChange,
        onBaseUrlChange = viewModel::onBaseUrlChange,
        onMethodChange = viewModel::onMethodChange,
        onHeadersJsonChange = viewModel::onHeadersJsonChange,
        onApiKeyChange = viewModel::onApiKeyChange,
        onPayloadTemplateChange = viewModel::onPayloadTemplateChange,
        onTimeoutChange = viewModel::onTimeoutChange,
        onIsActiveChange = viewModel::onIsActiveChange,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditDestinationScreenContent(
    destinationId: Long?,
    formState: FormState,
    onNavigateBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (DestinationType) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onMethodChange: (String) -> Unit,
    onHeadersJsonChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onPayloadTemplateChange: (String) -> Unit,
    onTimeoutChange: (Int) -> Unit,
    onIsActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (destinationId == null) "Add Destination" else "Edit Destination",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = formState.name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = "Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DestinationType.entries.forEach { type ->
                    FilterChip(
                        selected = formState.type == type,
                        onClick = { onTypeChange(type) },
                        label = {
                            Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    )
                }
            }

            OutlinedTextField(
                value = formState.baseUrl,
                onValueChange = onBaseUrlChange,
                label = {
                    Text(
                        if (formState.type == DestinationType.TELEGRAM) "Chat ID" else "Webhook URL"
                    )
                },
                isError = formState.baseUrlError != null,
                supportingText = formState.baseUrlError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(12.dp)
            )

            if (formState.type == DestinationType.WEBHOOK) {
                Text(
                    text = "Method",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("POST", "GET", "PUT").forEach { method ->
                        FilterChip(
                            selected = formState.method == method,
                            onClick = { onMethodChange(method) },
                            label = { Text(method) }
                        )
                    }
                }

                OutlinedTextField(
                    value = formState.headersJson,
                    onValueChange = onHeadersJsonChange,
                    label = { Text("Headers (JSON)") },
                    placeholder = { Text("{\"X-Api-Key\": \"value\"}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 4,
                    maxLines = 8
                )

                OutlinedTextField(
                    value = formState.payloadTemplate,
                    onValueChange = onPayloadTemplateChange,
                    label = { Text("Payload Template (JSON)") },
                    supportingText = {
                        Text("Use placeholders like {{sender}}, {{message}}, {{received_at}}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 6,
                    maxLines = 12
                )
            }

            OutlinedTextField(
                value = formState.apiKey,
                onValueChange = onApiKeyChange,
                label = {
                    Text(
                        if (formState.type == DestinationType.TELEGRAM)
                            "Bot Token" else "Bearer Token (optional)"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp)
            )

            Column {
                Text(
                    text = "Timeout: ${formState.timeoutSeconds}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = formState.timeoutSeconds.toFloat(),
                    onValueChange = { onTimeoutChange(it.roundToInt()) },
                    valueRange = 5f..60f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = formState.isActive,
                    onCheckedChange = onIsActiveChange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSave,
                enabled = !formState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (destinationId == null) "Save Destination" else "Update Destination",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun AddDestinationPreview() {
    PulseGateTheme(darkTheme = true) {
        AddEditDestinationScreenContent(
            destinationId = null,
            formState = FormState(),
            onNavigateBack = {},
            onNameChange = {},
            onTypeChange = {},
            onBaseUrlChange = {},
            onMethodChange = {},
            onHeadersJsonChange = {},
            onApiKeyChange = {},
            onPayloadTemplateChange = {},
            onTimeoutChange = {},
            onIsActiveChange = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun EditWebhookPreview() {
    PulseGateTheme(darkTheme = true) {
        AddEditDestinationScreenContent(
            destinationId = 1L,
            formState = FormState(
                name = "My Server",
                type = DestinationType.WEBHOOK,
                baseUrl = "https://myserver.com/webhook",
                method = "POST",
                headersJson = "{\"X-Api-Key\": \"secret\"}",
                apiKey = "bearer token",
                payloadTemplate = Destination.DEFAULT_WEBHOOK_PAYLOAD_TEMPLATE,
                timeoutSeconds = 15,
                isActive = true
            ),
            onNavigateBack = {},
            onNameChange = {},
            onTypeChange = {},
            onBaseUrlChange = {},
            onMethodChange = {},
            onHeadersJsonChange = {},
            onApiKeyChange = {},
            onPayloadTemplateChange = {},
            onTimeoutChange = {},
            onIsActiveChange = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun AddTelegramPreview() {
    PulseGateTheme(darkTheme = true) {
        AddEditDestinationScreenContent(
            destinationId = null,
            formState = FormState(
                type = DestinationType.TELEGRAM,
                baseUrl = "-100123456789",
                apiKey = ""
            ),
            onNavigateBack = {},
            onNameChange = {},
            onTypeChange = {},
            onBaseUrlChange = {},
            onMethodChange = {},
            onHeadersJsonChange = {},
            onApiKeyChange = {},
            onPayloadTemplateChange = {},
            onTimeoutChange = {},
            onIsActiveChange = {},
            onSave = {}
        )
    }
}