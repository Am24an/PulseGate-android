package com.aman.pulsegate.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aman.pulsegate.domain.model.DeliveryLog
import com.aman.pulsegate.domain.model.QueueStatus
import com.aman.pulsegate.ui.theme.AmberRetry
import com.aman.pulsegate.ui.theme.BluePending
import com.aman.pulsegate.ui.theme.GreenSent
import com.aman.pulsegate.ui.theme.PulseGateTheme
import com.aman.pulsegate.ui.theme.RedFailed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.retryError) {
        uiState.retryError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearRetryError()
        }
    }

    LogsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::retry
    )
}

@Composable
private fun LogsContent(
    uiState: LogsUiState,
    snackbarHostState: SnackbarHostState,
    onRetry: (Long) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Delivery Logs",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
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

                uiState.logs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No logs yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Delivery attempts will appear here",
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
                            items = uiState.logs,
                            key = { it.id }
                        ) { log ->
                            LogCard(
                                log = log,
                                isRetrying = uiState.retryingQueueId == log.queueId,
                                onRetry = { onRetry(log.queueId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogCard(
    log: DeliveryLog,
    isRetrying: Boolean,
    onRetry: () -> Unit
) {
    val statusColor = when (log.status) {
        QueueStatus.SENT -> GreenSent
        QueueStatus.FAILED -> RedFailed
        QueueStatus.RETRY -> AmberRetry
        QueueStatus.PENDING, QueueStatus.PROCESSING -> BluePending
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = log.status, color = statusColor)
                Text(
                    text = dateFormat.format(Date(log.attemptedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabelValue(label = "Event", value = "#${log.eventId}")
                LabelValue(label = "Dest", value = "#${log.destinationId}")
                log.httpCode?.let { LabelValue(label = "HTTP", value = it.toString()) }
                log.latencyMs?.let { LabelValue(label = "Latency", value = "${it}ms") }
            }

            if (log.retryAttempt > 0) {
                Text(
                    text = "Attempt ${log.retryAttempt + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberRetry
                )
            }

            log.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = RedFailed,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (log.status == QueueStatus.FAILED) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onRetry,
                    enabled = !isRetrying,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedFailed.copy(alpha = 0.15f),
                        contentColor = RedFailed
                    ),
                    elevation = null
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RedFailed,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: QueueStatus, color: androidx.compose.ui.graphics.Color) {
    val label = when (status) {
        QueueStatus.SENT -> "SENT"
        QueueStatus.FAILED -> "FAILED"
        QueueStatus.RETRY -> "RETRY"
        QueueStatus.PENDING -> "PENDING"
        QueueStatus.PROCESSING -> "PROCESSING"
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun LogsPreview() {
    PulseGateTheme(darkTheme = true) {
        LogsContent(
            uiState = LogsUiState(
                logs = listOf(
                    DeliveryLog(
                        id = 1L, queueId = 10L, eventId = 1L, destinationId = 1L,
                        status = QueueStatus.SENT, httpCode = 200,
                        responseBody = null, errorMessage = null,
                        latencyMs = 342L, retryAttempt = 0,
                        attemptedAt = System.currentTimeMillis() - 60_000
                    ),
                    DeliveryLog(
                        id = 2L, queueId = 11L, eventId = 2L, destinationId = 2L,
                        status = QueueStatus.FAILED, httpCode = 503,
                        responseBody = null, errorMessage = "Service Unavailable",
                        latencyMs = 1200L, retryAttempt = 2,
                        attemptedAt = System.currentTimeMillis() - 120_000
                    ),
                    DeliveryLog(
                        id = 3L, queueId = 12L, eventId = 3L, destinationId = 1L,
                        status = QueueStatus.RETRY, httpCode = null,
                        responseBody = null, errorMessage = "Network error",
                        latencyMs = null, retryAttempt = 1,
                        attemptedAt = System.currentTimeMillis() - 300_000
                    )
                ),
                isLoading = false,
                retryingQueueId = null
            ),
            snackbarHostState = SnackbarHostState(),
            onRetry = {}
        )
    }
}