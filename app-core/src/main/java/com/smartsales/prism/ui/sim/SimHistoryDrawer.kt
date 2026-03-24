package com.smartsales.prism.ui.sim

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.model.SessionPreview
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SimHistoryDrawerShape = RoundedCornerShape(topEnd = 36.dp, bottomEnd = 36.dp)
private val SimHistoryDrawerSurface = Color(0xD9141416)
private val SimHistoryDrawerBorder = Color.White.copy(alpha = 0.08f)
private val SimHistoryDrawerHover = Color.White.copy(alpha = 0.05f)
private val SimHistoryDrawerSelected = Color.White.copy(alpha = 0.08f)
private val SimHistoryDrawerActionSurface = Color.White.copy(alpha = 0.08f)
private val SimHistoryDrawerDangerSurface = Color(0x26FF453A)
private val SimHistoryDrawerDanger = Color(0xFFFF6B60)
private val SimHistoryGroupLabelColor = Color(0xFF86868B)
private val SimHistorySummaryColor = Color(0xFFAEAEB2)
private val SimHistoryMuted = Color(0xFFAEAEB2)
private val SimHistoryHeaderColor = Color.White
private val SimHistoryEmptyBorder = Color.White.copy(alpha = 0.08f)
private val SimHistoryTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val SimHistoryDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d", Locale.getDefault())

@Composable
internal fun SimHistoryDrawer(
    groupedSessions: Map<String, List<SessionPreview>>,
    currentSessionId: String?,
    onSessionClick: (String) -> Unit,
    onPinSession: (String) -> Unit,
    onRenameSession: (String, String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .shadow(
                elevation = 24.dp,
                shape = SimHistoryDrawerShape,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            ),
        color = SimHistoryDrawerSurface,
        contentColor = Color.White,
        shape = SimHistoryDrawerShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 0.5.dp, color = SimHistoryDrawerBorder, shape = SimHistoryDrawerShape)
                .padding(top = 40.dp, bottom = 28.dp)
        ) {
            Text(
                text = "历史记录",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SimHistoryHeaderColor,
                letterSpacing = 0.4.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (groupedSessions.isEmpty()) {
                SimHistoryEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    groupedSessions.forEach { (groupLabel, sessions) ->
                        item(key = groupLabel) {
                            SimHistoryGroupHeader(label = groupLabel)
                        }
                        items(items = sessions, key = { it.id }) { session ->
                            SimHistoryRow(
                                session = session,
                                isCurrent = session.id == currentSessionId,
                                onClick = { onSessionClick(session.id) },
                                onPinToggle = { onPinSession(session.id) },
                                onRename = { title, summary ->
                                    onRenameSession(session.id, title, summary)
                                },
                                onDelete = { onDeleteSession(session.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimHistoryGroupHeader(label: String) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (label) {
            SIM_HISTORY_GROUP_PINNED -> {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            SIM_HISTORY_GROUP_TODAY -> {
                Icon(
                    imageVector = Icons.Filled.Today,
                    contentDescription = null,
                    tint = SimHistoryGroupLabelColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            SIM_HISTORY_GROUP_LAST_7_DAYS,
            SIM_HISTORY_GROUP_EARLIER -> {
                Icon(
                    imageVector = Icons.Filled.WatchLater,
                    contentDescription = null,
                    tint = SimHistoryGroupLabelColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SimHistoryGroupLabelColor,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SimHistoryRow(
    session: SessionPreview,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember(session.id) { mutableStateOf(false) }
    var showRenameDialog by remember(session.id) { mutableStateOf(false) }
    var renameTitle by remember(session.id) { mutableStateOf(session.clientName) }
    var renameSummary by remember(session.id) { mutableStateOf(session.summary) }

    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isCurrent) SimHistoryDrawerSelected else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(start = 12.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.isPinned) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (session.isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = SimHistoryMuted.copy(alpha = 0.55f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.clientName,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = formatSimHistoryRecency(session.timestamp),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        color = SimHistoryMuted
                    )
                }

                Text(
                    text = session.summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = SimHistorySummaryColor
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "会话操作",
                        tint = Color.White.copy(alpha = 0.72f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = Color(0xFF1B1B1F)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (session.isPinned) "取消置顶" else "置顶",
                                color = Color.White
                            )
                        },
                        onClick = {
                            showMenu = false
                            onPinToggle()
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SimHistoryDrawerActionSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("重命名", color = Color.White) },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = SimHistoryDrawerDanger) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SimHistoryDrawerDangerSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "−",
                                    color = SimHistoryDrawerDanger,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名会话") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = renameTitle,
                        onValueChange = { renameTitle = it },
                        label = { Text("标题") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = renameSummary,
                        onValueChange = { renameSummary = it },
                        label = { Text("摘要") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(renameTitle, renameSummary)
                        showRenameDialog = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SimHistoryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.03f))
                .border(width = 1.dp, color = SimHistoryEmptyBorder, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                tint = SimHistoryMuted,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "暂无会话",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "您的历史沟通记录会在这里按时间归档，方便稍后继续跟进。",
            style = MaterialTheme.typography.bodySmall,
            color = SimHistorySummaryColor,
            lineHeight = 20.sp
        )
    }
}

internal fun formatSimHistoryRecency(
    timestamp: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val sessionDateTime = Instant.ofEpochMilli(timestamp).atZone(zoneId)
    val currentDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    val sessionDate = sessionDateTime.toLocalDate()
    val today = currentDateTime.toLocalDate()

    return when {
        !sessionDate.isBefore(today) -> sessionDateTime.format(SimHistoryTimeFormatter)
        sessionDate == today.minusDays(1) -> "昨天"
        !sessionDate.isBefore(today.minusDays(6)) -> sessionDateTime.format(SimHistoryDateFormatter)
        else -> sessionDateTime.format(SimHistoryDateFormatter)
    }
}
