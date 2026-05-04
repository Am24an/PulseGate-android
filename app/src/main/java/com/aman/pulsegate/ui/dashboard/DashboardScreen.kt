package com.aman.pulsegate.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aman.pulsegate.ui.theme.AmberRetry
import com.aman.pulsegate.ui.theme.BluePending
import com.aman.pulsegate.ui.theme.GreenSent
import com.aman.pulsegate.ui.theme.PulseGateTheme
import com.aman.pulsegate.ui.theme.RedFailed

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(uiState = uiState)
}

@Composable
private fun DashboardContent(uiState: DashboardUiState) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "PulseGate",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            ServiceStatusChip(isRunning = uiState.isServiceRunning)

            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Destinations",
                    value = uiState.activeDestinations.toString(),
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Pending",
                    value = uiState.pendingCount.toString(),
                    valueColor = BluePending,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Sent",
                    value = uiState.sentCount.toString(),
                    valueColor = GreenSent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Failed",
                    value = uiState.failedCount.toString(),
                    valueColor = RedFailed,
                    modifier = Modifier.weight(1f)
                )
            }

            StatCard(
                label = "Total Delivery Logs",
                value = uiState.totalLogs.toString(),
                valueColor = AmberRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ServiceStatusChip(
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, label, tint, background) = if (isRunning) {
        StatusChipTokens(
            icon = Icons.Filled.CheckCircle,
            label = "Gateway Running",
            tint = GreenSent,
            background = GreenSent.copy(alpha = 0.12f)
        )
    } else {
        StatusChipTokens(
            icon = Icons.Filled.Warning,
            label = "Gateway Stopped",
            tint = RedFailed,
            background = RedFailed.copy(alpha = 0.12f)
        )
    }

    Row(
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class StatusChipTokens(
    val icon: ImageVector,
    val label: String,
    val tint: Color,
    val background: Color
)

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun DashboardPreview() {
    PulseGateTheme(darkTheme = true) {
        DashboardContent(
            uiState = DashboardUiState(
                activeDestinations = 3,
                pendingCount = 2,
                sentCount = 147,
                failedCount = 4,
                totalLogs = 312,
                isServiceRunning = true
            )
        )
    }
}