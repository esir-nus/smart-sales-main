package com.smartsales.prism.ui.components.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data Contract for ActiveTaskHorizon.
 * Completely decoupled from ViewModel and network layer.
 */
data class ActiveTaskHorizonState(
    val isVisible: Boolean,
    val overarchingGoal: String,
    val currentActivity: String,
    val activeToolStream: String?,
    val canCancel: Boolean
)

/**
 * A universally reusable sticky container indicating long-running agent tasks.
 */
@Composable
fun ActiveTaskHorizon(
    state: ActiveTaskHorizonState,
    onCancelRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E2C), // surface.plan
        border = BorderStroke(1.dp, Color(0x404FC3F7)) // subtle pulsing border equivalent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚡",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.overarchingGoal,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.currentActivity,
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                    if (state.activeToolStream != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.activeToolStream,
                            color = Color(0xFF4FC3F7),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .border(1.dp, Color(0x404FC3F7), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (state.canCancel) {
                IconButton(
                    onClick = onCancelRequested,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Task",
                        tint = Color(0xFFAAAAAA)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ActiveTaskHorizonPreview() {
    ActiveTaskHorizon(
        state = ActiveTaskHorizonState(
            isVisible = true,
            overarchingGoal = "Analyzing Q3 Competitor Data",
            currentActivity = "Reading transcripts...",
            activeToolStream = "[Tool: query_relevancy_lib]",
            canCancel = true
        ),
        onCancelRequested = {}
    )
}
