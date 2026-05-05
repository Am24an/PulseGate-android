package com.aman.pulsegate.ui.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aman.pulsegate.domain.model.Destination
import com.aman.pulsegate.domain.model.DestinationType
import com.aman.pulsegate.ui.theme.GreenSent
import com.aman.pulsegate.ui.theme.PulseGateTheme

@Composable
fun DestinationsScreen(
    onAddDestination: () -> Unit,
    onEditDestination: (Long) -> Unit,
    viewModel: DestinationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DestinationsContent(
        uiState = uiState,
        onAddDestination = onAddDestination,
        onEditDestination = onEditDestination,
        onToggleActive = viewModel::toggleActive,
        onDelete = viewModel::delete
    )
}

@Composable
private fun DestinationsContent(
    uiState: DestinationsUiState,
    onAddDestination: () -> Unit,
    onEditDestination: (Long) -> Unit,
    onToggleActive: (Destination) -> Unit,
    onDelete: (Destination) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDestination,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add destination",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Destinations",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                uiState.destinations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No destinations yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap + to add your first destination",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.destinations,
                            key = { it.id }
                        ) { destination ->
                            DestinationCard(
                                destination = destination,
                                onEdit = { onEditDestination(destination.id) },
                                onToggleActive = { onToggleActive(destination) },
                                onDelete = { onDelete(destination) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    destination: Destination,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete destination?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "\"${destination.name}\" will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = destination.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TypeBadge(type = destination.type)
                }
                Text(
                    text = destination.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Switch(
                checked = destination.isActive,
                onCheckedChange = { _ -> onToggleActive() },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(type: DestinationType) {
    val label = when (type) {
        DestinationType.WEBHOOK -> "Webhook"
        DestinationType.TELEGRAM -> "Telegram"
    }
    val color = when (type) {
        DestinationType.WEBHOOK -> MaterialTheme.colorScheme.primary
        DestinationType.TELEGRAM -> GreenSent
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun DestinationsListPreview() {
    PulseGateTheme(darkTheme = true) {
        DestinationsContent(
            uiState = DestinationsUiState(
                destinations = listOf(
                    Destination(
                        id = 1L,
                        name = "My Server",
                        type = DestinationType.WEBHOOK,
                        baseUrl = "https://myserver.com/webhook",
                        method = "POST",
                        headersJson = "",
                        apiKey = null,
                        payloadTemplate = Destination.DEFAULT_WEBHOOK_PAYLOAD_TEMPLATE,
                        timeoutSeconds = 15,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    Destination(
                        id = 2L,
                        name = "Telegram Bot",
                        type = DestinationType.TELEGRAM,
                        baseUrl = "-100123456789",
                        method = "POST",
                        headersJson = "",
                        apiKey = "bot-token",
                        payloadTemplate = Destination.DEFAULT_WEBHOOK_PAYLOAD_TEMPLATE,
                        timeoutSeconds = 15,
                        isActive = false,
                        createdAt = System.currentTimeMillis()
                    )
                ),
                isLoading = false
            ),
            onAddDestination = {},
            onEditDestination = {},
            onToggleActive = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun DestinationsEmptyPreview() {
    PulseGateTheme(darkTheme = true) {
        DestinationsContent(
            uiState = DestinationsUiState(
                destinations = emptyList(),
                isLoading = false
            ),
            onAddDestination = {},
            onEditDestination = {},
            onToggleActive = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun DestinationCardPreview() {
    PulseGateTheme(darkTheme = true) {
        DestinationCard(
            destination = Destination(
                id = 1L,
                name = "Production Webhook",
                type = DestinationType.WEBHOOK,
                baseUrl = "https://api.myserver.com/gateway/forward",
                method = "POST",
                headersJson = "",
                apiKey = null,
                payloadTemplate = Destination.DEFAULT_WEBHOOK_PAYLOAD_TEMPLATE,
                timeoutSeconds = 15,
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            onEdit = {},
            onToggleActive = {},
            onDelete = {}
        )
    }
}