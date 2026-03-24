package com.smartsales.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.ui.components.DynamicIsland
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.DynamicIslandUiState
import com.smartsales.prism.ui.components.PrismSurface

internal val ProMaxTextPrimary = Color.White.copy(alpha = 0.95f)
internal val ProMaxTextSecondary = Color.White.copy(alpha = 0.6f)
internal val ProMaxTextMuted = Color.White.copy(alpha = 0.3f)
internal val ProMaxDanger = Color(0xFFEF4444)
internal val ProMaxWarning = Color(0xFFF59E0B)
internal val ProMaxSuccess = Color(0xFF10B981)
internal val ProMaxAccent = Color(0xFF38BDF8)

private fun Modifier.glassPanel(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) = this
    .clip(shape)
    .background(Color.White.copy(alpha = 0.05f))
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.02f)
            )
        ),
        shape = shape
    )

@Composable
internal fun ProMaxHeader(
    dynamicIslandState: DynamicIslandUiState,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onSchedulerClick: (DynamicIslandTapAction) -> Unit,
    onDebugClick: () -> Unit,
    onDeviceClick: () -> Unit,
    showDebugButton: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(icon = Icons.Filled.Menu, onClick = onMenuClick)
            GlassCircleButton(
                icon = Icons.Filled.Bluetooth,
                onClick = onDeviceClick,
                tint = ProMaxAccent
            )
        }
        DynamicIsland(
            state = dynamicIslandState,
            modifier = Modifier.weight(1f),
            onTap = onSchedulerClick
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showDebugButton) {
                GlassCircleButton(
                    icon = Icons.Filled.BugReport,
                    onClick = onDebugClick,
                    tint = ProMaxTextMuted
                )
            }
            GlassCircleButton(icon = Icons.Filled.Add, onClick = onNewSessionClick)
        }
    }
}

@Composable
internal fun HomeHeroDashboard(
    greeting: String,
    upcoming: List<ScheduledTask>,
    accomplished: List<ScheduledTask>,
    onProfileClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        Text(
            text = "TODAY OVERVIEW",
            style = MaterialTheme.typography.labelSmall,
            color = ProMaxTextMuted,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = greeting.replace(", ", ",\n").replace("，", "，\n"),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = ProMaxTextPrimary,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.clickable { onProfileClick() }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Schedule,
                label = "待办事项",
                value = upcoming.size.toString()
            )
            OverviewStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DoneAll,
                label = "已完成",
                value = accomplished.size.toString()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        PrismSurface(
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ProMaxAccent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = ProMaxAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "更自然的输入方式",
                        color = ProMaxTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "可以直接输入，也可以按住工牌按键进行语音记录。",
                        color = ProMaxTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "待办",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ProMaxTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(upcoming.size.toString(), color = ProMaxTextPrimary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                upcoming.forEach { HeroTaskRow(task = it, isDone = false) }
            }
        }
        if (accomplished.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "已完成",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ProMaxTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                accomplished.forEach { HeroTaskRow(task = it, isDone = true) }
            }
        }
    }
}

@Composable
private fun HeroTaskRow(task: ScheduledTask, isDone: Boolean) {
    val barColor = when (task.urgencyLevel) {
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L1_CRITICAL -> ProMaxDanger
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT -> ProMaxWarning
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(45.dp)
        ) {
            Text(
                text = task.timeDisplay.split(" - ").firstOrNull() ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDone) ProMaxTextMuted else ProMaxTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(
                    if (isDone) {
                        ProMaxSuccess
                    } else if (barColor == Color.Transparent) {
                        Color.White.copy(alpha = 0.1f)
                    } else {
                        barColor
                    },
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 15.sp,
                color = if (isDone) ProMaxTextSecondary else ProMaxTextPrimary,
                fontWeight = FontWeight.Medium,
                textDecoration = if (isDone) {
                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                } else {
                    null
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            val meta = listOfNotNull(
                if (task.urgencyLevel == com.smartsales.prism.domain.scheduler.UrgencyLevel.L1_CRITICAL) {
                    "核心"
                } else {
                    null
                },
                task.location,
                task.keyPerson
            ).joinToString(" • ")
            if (meta.isNotEmpty()) {
                Text(meta, fontSize = 12.sp, color = ProMaxTextMuted)
            }
        }
    }
}

@Composable
private fun OverviewStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    PrismSurface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = ProMaxAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(value, color = ProMaxTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(label, color = ProMaxTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun GlassInputCapsule(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .glassPanel(RoundedCornerShape(28.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                    .clickable(onClick = onAttachClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AttachFile,
                    "Attach",
                    tint = ProMaxTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text("Assistant input", color = ProMaxTextMuted, fontSize = 11.sp, letterSpacing = 0.8.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = text,
                        onValueChange = onTextChanged,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = ProMaxTextPrimary,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(ProMaxAccent),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text("输入消息，或长按工牌按键说话...", color = ProMaxTextSecondary)
                            }
                            innerTextField()
                        }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (text.isBlank()) Color.White.copy(alpha = 0.92f) else ProMaxAccent,
                        CircleShape
                    )
                    .clickable { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (text.isEmpty()) Icons.Filled.Mic else Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color(0xFF0B0C10),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = ProMaxTextPrimary
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .glassPanel(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
