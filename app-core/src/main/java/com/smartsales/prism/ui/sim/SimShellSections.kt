package com.smartsales.prism.ui.sim

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.ui.theme.TextPrimary
import com.smartsales.prism.ui.theme.TextSecondary

@Composable
internal fun SimSchedulerFollowUpPrompt(
    onOpen: () -> Unit
) {
    val accent = Color(0xFF38BDF8)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xCC111827),
        contentColor = TextPrimary,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "工牌已创建新日程",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "点击进入任务级跟进会话",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            TextButton(
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColors(contentColor = accent)
            ) {
                Text("继续跟进")
            }
        }
    }
}

@Composable
internal fun SimSchedulerFollowUpActionStrip(
    context: SchedulerFollowUpContext,
    selectedTaskId: String?,
    onSelectTask: (String) -> Unit,
    onAction: (SimSchedulerFollowUpQuickAction) -> Unit
) {
    val accent = Color(0xFF38BDF8)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xE6111827),
        contentColor = TextPrimary,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "任务级跟进",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                context.taskSummaries.forEach { task ->
                    AssistChip(
                        onClick = { onSelectTask(task.taskId) },
                        label = { Text(task.title) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (task.taskId == selectedTaskId) {
                                accent.copy(alpha = 0.18f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(
                    SimSchedulerFollowUpQuickAction.EXPLAIN to "说明",
                    SimSchedulerFollowUpQuickAction.STATUS to "状态",
                    SimSchedulerFollowUpQuickAction.PREFILL_RESCHEDULE to "改期",
                    SimSchedulerFollowUpQuickAction.MARK_DONE to "完成",
                    SimSchedulerFollowUpQuickAction.DELETE to "删除"
                ).forEach { (action, label) ->
                    AssistChip(
                        onClick = { onAction(action) },
                        label = { Text(label) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}
