package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.layout.*
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
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.AccentAmber
import com.smartsales.prism.ui.theme.BorderSubtle
import com.smartsales.prism.ui.theme.TextMuted
import com.smartsales.prism.ui.theme.TextSecondary

@Composable
fun TaskCardDetails(
    state: TimelineItem.Task,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 58.dp, top = 16.dp)
    ) {
        val metaStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)

        state.conflictSummary?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(it, style = metaStyle.copy(color = AccentAmber, fontWeight = FontWeight.Medium))
            }
        }

        // Date Row
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Outlined.Event, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(state.dateRange, style = metaStyle)
        }
        // Location Row
        state.location?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(it, style = metaStyle)
            }
        }
        // Notes Row
        state.notes?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Outlined.Notes, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(it, style = metaStyle)
            }
        }
        // Key Person
        state.keyPerson?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
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
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it.joinToString(", "), style = metaStyle)
                }
            }
        }

        // Smart Tips
        if (state.tipsLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
            repeat(3) {
                ShimmerLine(modifier = Modifier.padding(vertical = 3.dp))
            }
        } else if (state.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
            state.tips.forEach { tip ->
                TipBubble(text = tip, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
    }
}
