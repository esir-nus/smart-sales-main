package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartsales.prism.ui.drawers.scheduler.ReminderBellState
import com.smartsales.prism.ui.drawers.scheduler.ReminderBellVisual
import com.smartsales.prism.ui.theme.TextMuted

@Composable
fun ReminderBellStrip(
    bells: List<ReminderBellVisual>,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    if (bells.isEmpty()) return

    val firedCount = bells.count { it.state == ReminderBellState.FIRED }

    Row(
        modifier = modifier.semantics {
            contentDescription = "Reminder cascade $firedCount of ${bells.size} fired"
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        bells.forEach { bell ->
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = if (bell.state == ReminderBellState.FIRED) {
                    TextMuted.copy(alpha = 0.8f)
                } else {
                    activeColor
                },
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
