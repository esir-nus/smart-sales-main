package com.smartsales.prism.ui.drawers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    // Observe connectivity state
    val batteryLevel by connectivityViewModel.batteryLevel.collectAsState()
    val connectionState by connectivityViewModel.effectiveState.collectAsState()
    // Main Container (Glass Sheet)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(BackgroundApp.copy(alpha = 0.98f)) // High opacity for legibility
    ) {
        // 2. Content List (Collapsible Cards)
        HistorySessionList(
            groupedSessions = groupedSessions,
            onSessionClick = onSessionClick,
            onPinSession = onPinSession,
            onRenameSession = onRenameSession,
            onDeleteSession = onDeleteSession,
            contentPadding = PaddingValues(top = 100.dp, bottom = 120.dp) // Space for floating elements
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
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
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
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = BackgroundSurface.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
        shadowElevation = 4.dp, // Glass shadow
        modifier = modifier.height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            // Battery
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.BatteryStd, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                Text("$batteryLevel%", style = MaterialTheme.typography.labelMedium, color = AccentGreen, fontWeight = FontWeight.Bold)
            }
            
            Divider(modifier = Modifier.height(12.dp).width(1.dp), color = Color.Black.copy(alpha = 0.1f))
            
            // Device
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Wifi, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                val deviceName = when (connectionState) {
                    ConnectionState.CONNECTED -> "SmartBadge"
                    ConnectionState.DISCONNECTED -> "未连接"
                    ConnectionState.RECONNECTING -> "连接中..."
                    else -> "SmartBadge"
                }
                Text(deviceName, style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
            
            val statusText = when (connectionState) {
                ConnectionState.CONNECTED -> "• 正常"
                ConnectionState.DISCONNECTED -> "• 离线"
                ConnectionState.RECONNECTING -> "• 重连中"
                else -> ""
            }
            if (statusText.isNotEmpty()) {
                Text(statusText, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BackgroundSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 点击头像/名字区域 → 打开个人中心
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary)
                }
                
                Column {
                    Text(displayName.ifBlank { "用户" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("高级会员", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary)
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
    if (groupedSessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "暂无会话",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "创建新对话或从音频入口进入讨论后，会话会显示在这里。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color(0xFFFAFAFA)), // Subtle header bg
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Text(
                        text = title.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "").trim().uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp).rotate(rotateAngle)
                )
            }

            // Divider
            Divider(color = BorderSubtle)

            // Content
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
                            Divider(color = BorderSubtle, modifier = Modifier.padding(start = 16.dp))
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hover indicator placeholder (invisible unless active)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(KnotPrimary.copy(alpha=0.1f), Color.Transparent)))
                    .alpha(0f) // Only visible on hover in desktop, mobile logic separate
            )
            
// Wave 4 UI Update: Horizontal Layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // Spacer between Name and Summary
                modifier = Modifier.weight(1f, fill = false) // Allow name to take space but not push summary off if short
            ) {
                Text(
                    text = session.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Text(
                    text = session.summary, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        Box {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Session actions",
                tint = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.clickable { showMenu = true }
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
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
