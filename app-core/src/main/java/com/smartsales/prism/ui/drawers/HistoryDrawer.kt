package com.smartsales.prism.ui.drawers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.smartsales.prism.ui.components.prismStatusBarPadding
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.components.connectivity.ConnectionState

/**
 * History Drawer (Variant 4: Hybrid Collapsible)
 * Source of Truth: docs/specs/modules/HistoryDrawer.md
 * 
 * Features:
 * - Floating Capsule Header (Aurora Glass)
 * - Collapsible Structured Cards
 * - Floating Glass Dock Footer
 */
@Composable
fun HistoryDrawer(
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
    groupedSessions: Map<String, List<SessionPreview>>,
    onSessionClick: (String) -> Unit,
    onDeviceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    displayName: String = "",
    onPinSession: (String) -> Unit = {},
    onRenameSession: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteSession: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = colors.appBackground.luminance() < 0.5f
    // Observe connectivity state
    val batteryLevel by connectivityViewModel.batteryLevel.collectAsState()
    val connectionState by connectivityViewModel.effectiveState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .prismStatusBarPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.appBackground.copy(alpha = 0.98f),
                        colors.appBackground.copy(alpha = 0.99f),
                        colors.surfaceMuted.copy(alpha = if (isDarkTheme) 0.18f else 0.60f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 72.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.accentBlue.copy(alpha = if (isDarkTheme) 0.18f else 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(210.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-52).dp, y = 56.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.accentSecondary.copy(alpha = if (isDarkTheme) 0.15f else 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Content List (Collapsible Cards)
        HistorySessionList(
            groupedSessions = groupedSessions,
            onSessionClick = onSessionClick,
            onPinSession = onPinSession,
            onRenameSession = onRenameSession,
            onDeleteSession = onDeleteSession,
            contentPadding = PaddingValues(top = 108.dp, bottom = 124.dp)
        )

        // 1. Header (Floating Capsule)
        FloatingCapsuleHeader(
            batteryLevel = batteryLevel,
            connectionState = connectionState,
            onClick = onDeviceClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp) // Status bar clearance
        )

        // 3. Footer (Floating Dock)
        FloatingGlassDock(
            displayName = displayName,
            onSettingsClick = onSettingsClick,
            onProfileClick = onProfileClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp, start = 14.dp, end = 14.dp)
        )
    }
}

@Composable
private fun FloatingCapsuleHeader(
    batteryLevel: Int,
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = colors.appBackground.luminance() < 0.5f
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.surface.copy(alpha = if (isDarkTheme) 0.82f else 0.92f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colors.borderSubtle.copy(alpha = if (isDarkTheme) 0.90f else 0.65f)
        ),
        shadowElevation = 6.dp,
        modifier = modifier.height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.BatteryStd,
                    contentDescription = null,
                    tint = colors.accentSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "$batteryLevel%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .height(12.dp)
                    .width(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.borderSubtle.copy(alpha = if (isDarkTheme) 0.80f else 1f))
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
                val deviceName = when (connectionState) {
                    ConnectionState.CONNECTED -> "SmartBadge"
                    ConnectionState.DISCONNECTED -> "未连接"
                    ConnectionState.RECONNECTING -> "连接中..."
                    else -> "SmartBadge"
                }
                Text(
                    deviceName,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val statusText = when (connectionState) {
                ConnectionState.CONNECTED -> "• 正常"
                ConnectionState.DISCONNECTED -> "• 离线"
                ConnectionState.RECONNECTING -> "• 重连中"
                else -> ""
            }
            if (statusText.isNotEmpty()) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun FloatingGlassDock(
    displayName: String,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = colors.appBackground.luminance() < 0.5f
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface.copy(alpha = if (isDarkTheme) 0.84f else 0.92f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colors.borderSubtle.copy(alpha = if (isDarkTheme) 0.90f else 0.65f)
        ),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.surface,
                                    colors.surfaceHover
                                )
                            )
                        )
                        .border(1.dp, colors.borderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = colors.textSecondary)
                }

                Column {
                    Text(
                        displayName.ifBlank { "用户" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        "高级会员",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceMuted.copy(alpha = if (isDarkTheme) 0.74f else 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySessionList(
    groupedSessions: Map<String, List<SessionPreview>>,
    onSessionClick: (String) -> Unit,
    onPinSession: (String) -> Unit,
    onRenameSession: (String, String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = colors.appBackground.luminance() < 0.5f
    if (groupedSessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surface.copy(alpha = if (isDarkTheme) 0.84f else 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "暂无会话",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "创建新对话或从音频入口进入讨论后，会话会显示在这里。",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        groupedSessions.forEach { (group, sessions) ->
            item(key = group) {
                CollapsibleGroupCard(
                    title = group,
                    sessions = sessions,
                    onSessionClick = onSessionClick,
                    onPinSession = onPinSession,
                    onRenameSession = onRenameSession,
                    onDeleteSession = onDeleteSession,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CollapsibleGroupCard(
    title: String,
    sessions: List<SessionPreview>,
    onSessionClick: (String) -> Unit,
    onPinSession: (String) -> Unit,
    onRenameSession: (String, String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = colors.appBackground.luminance() < 0.5f
    var expanded by remember { mutableStateOf(true) }
    val rotateAngle by animateFloatAsState(targetValue = if (expanded) 0f else -90f)
    
    // Icon Mapping (Mock logic based on title strings)
    val (icon, color) = when {
        title.contains("置顶") -> Icons.Filled.PushPin to Color(0xFFF59E0B) // Amber
        title.contains("今天") -> Icons.Filled.Today to Color(0xFF3B82F6) // Blue
        title.contains("30天") -> Icons.Filled.DateRange to Color(0xFF6366F1) // Indigo
        else -> Icons.Filled.Folder to TextSecondary // Gray (FolderLike placeholder or common icon)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface.copy(alpha = if (isDarkTheme) 0.84f else 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .background(colors.surfaceMuted.copy(alpha = if (isDarkTheme) 0.68f else 0.78f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Text(
                        text = title.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "").trim(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp).rotate(rotateAngle)
                )
            }

            HorizontalDivider(color = colors.borderSubtle.copy(alpha = if (isDarkTheme) 0.90f else 1f))

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    sessions.forEachIndexed { index, session ->
                        HistoryCardItem(
                            session = session,
                            onClick = onSessionClick,
                            onPinSession = onPinSession,
                            onRenameSession = onRenameSession,
                            onDeleteSession = onDeleteSession
                        )
                        if (index < sessions.lastIndex) {
                            HorizontalDivider(
                                color = colors.borderSubtle.copy(alpha = if (isDarkTheme) 0.70f else 1f),
                                modifier = Modifier.padding(start = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HistoryCardItem(
    session: SessionPreview,
    onClick: (String) -> Unit,
    onPinSession: (String) -> Unit,
    onRenameSession: (String, String, String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = colors.appBackground.luminance() < 0.5f
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameClientName by remember(session.id) { mutableStateOf(session.clientName) }
    var renameSummary by remember(session.id) { mutableStateOf(session.summary) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(session.id) },
                onLongClick = { showMenu = true }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.accentBlue.copy(alpha = if (isDarkTheme) 0.35f else 0.22f),
                                Color.Transparent
                            )
                        )
                    )
                    .alpha(0.9f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = session.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box {
            Surface(
                shape = CircleShape,
                color = colors.surfaceMuted.copy(alpha = if (isDarkTheme) 0.74f else 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Session actions",
                    tint = colors.textSecondary.copy(alpha = 0.82f),
                    modifier = Modifier
                        .size(30.dp)
                        .padding(6.dp)
                        .clickable { showMenu = true }
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = colors.surface.copy(alpha = if (isDarkTheme) 0.96f else 0.98f),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                DropdownMenuItem(
                    text = { Text(if (session.isPinned) "取消置顶" else "置顶") },
                    onClick = {
                        showMenu = false
                        onPinSession(session.id)
                    }
                )
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = {
                        showMenu = false
                        showRenameDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        showMenu = false
                        onDeleteSession(session.id)
                    }
                )
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("重命名会话") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = renameClientName,
                        onValueChange = { renameClientName = it },
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
                        onRenameSession(session.id, renameClientName, renameSummary)
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
