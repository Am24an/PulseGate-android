package com.aman.pulsegate.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aman.pulsegate.domain.model.QueueStatus

@Composable
fun StatusBadge(
    status: QueueStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = statusTokens(status)

    Text(
        text = label,
        style = PulseGateTypography.labelSmall,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private data class StatusToken(
    val background: Color,
    val text: Color,
    val label: String
)

private fun statusTokens(status: QueueStatus): StatusToken = when (status) {
    QueueStatus.PENDING -> StatusToken(
        background = BluePending.copy(alpha = 0.15f),
        text = BluePending,
        label = "PENDING"
    )

    QueueStatus.PROCESSING -> StatusToken(
        background = PurpleProcess.copy(alpha = 0.15f),
        text = PurpleProcess,
        label = "PROCESSING"
    )

    QueueStatus.RETRY -> StatusToken(
        background = AmberRetry.copy(alpha = 0.15f),
        text = AmberRetry,
        label = "RETRY"
    )

    QueueStatus.SENT -> StatusToken(
        background = GreenSent.copy(alpha = 0.15f),
        text = GreenSent,
        label = "SENT"
    )

    QueueStatus.FAILED -> StatusToken(
        background = RedFailed.copy(alpha = 0.15f),
        text = RedFailed,
        label = "FAILED"
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun StatusBadgePreview() {
    PulseGateTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QueueStatus.entries.forEach { status ->
                StatusBadge(status = status)
            }
        }
    }
}