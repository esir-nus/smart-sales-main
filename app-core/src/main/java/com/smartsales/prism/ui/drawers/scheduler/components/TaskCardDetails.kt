package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerVisualMode
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisualMode
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisuals
import com.smartsales.prism.ui.drawers.scheduler.simDetailDateLabel
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.AccentAmber
import com.smartsales.prism.ui.theme.BorderSubtle

@Composable
fun TaskCardDetails(
    state: TimelineItem.Task,
    modifier: Modifier = Modifier
) {
    val visuals = currentSchedulerDrawerVisuals
    val isSimVisualMode = currentSchedulerDrawerVisualMode == SchedulerDrawerVisualMode.SIM
    val detailStartPadding = if (isSimVisualMode) {
        0.dp
    } else {
        58.dp
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = detailStartPadding, top = 16.dp)
    ) {
        val metaStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
            color = visuals.taskMetaColor,
            fontSize = 12.sp
        )

        state.conflictSummary?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(it, style = metaStyle.copy(color = AccentAmber, fontWeight = FontWeight.Medium))
            }
        }

        // Date Row
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Outlined.Event, contentDescription = null, tint = visuals.taskTimeColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSimVisualMode) state.simDetailDateLabel() else state.dateRange,
                style = metaStyle
            )
        }
        // Location Row
        state.location?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = visuals.taskTimeColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(it, style = metaStyle)
            }
        }
        // Key Person
        state.keyPerson?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = visuals.taskTimeColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(it, style = metaStyle)
            }
        }
        // Highlights Row
        state.highlights?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(it, style = metaStyle.copy(color = AccentBlue, fontWeight = FontWeight.Medium))
            }
        }
        // Alarm Row
        state.alarmCascade?.let {
            if (it.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = visuals.taskTimeColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it.joinToString(", "), style = metaStyle)
                }
            }
        }

        if (state.notes != null || state.tipsLoading || state.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
        }

        state.notes?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = visuals.cardBackground.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Notes,
                        contentDescription = null,
                        tint = visuals.taskTimeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "用户备注",
                        style = metaStyle.copy(fontWeight = FontWeight.Medium)
                    )
                }
                Text(
                    text = it,
                    style = metaStyle,
                    modifier = Modifier.padding(top = 8.dp),
                    lineHeight = 18.sp
                )
            }
        }

        if (state.tipsLoading) {
            Spacer(modifier = Modifier.height(10.dp))
            repeat(3) {
                ShimmerLine(modifier = Modifier.padding(vertical = 3.dp))
            }
        } else if (state.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI 提示",
                style = metaStyle.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            state.tips.forEach { tip ->
                TipBubble(text = tip, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
    }
}
