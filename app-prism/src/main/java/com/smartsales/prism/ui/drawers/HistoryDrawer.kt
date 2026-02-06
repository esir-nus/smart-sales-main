package com.smartsales.prism.ui.drawers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
            onSettingsClick = onSettingsClick,
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
    onSettingsClick: () -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Avatar (Mock Gradient)
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
                    Text("Frank Chen", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Premium Plan", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
    contentPadding: PaddingValues
) {
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
                        HistoryCardItem(session, onSessionClick)
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
private fun HistoryCardItem(
    session: SessionPreview,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(session.id) }
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
            
            Column {
                Text(session.clientName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(session.summary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

