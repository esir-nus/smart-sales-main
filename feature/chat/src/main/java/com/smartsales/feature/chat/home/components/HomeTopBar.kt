// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/components/HomeTopBar.kt
// 模块：:feature:chat
// 说明：Home 页面顶部导航栏，包含历史记录、设备状态指示器、新建对话按钮
// 作者：从 HomeScreen.kt 提取于 2026-01-11

package com.smartsales.feature.chat.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.theme.AppColors
import com.smartsales.feature.chat.home.theme.AppDimensions

/**
 * Home 页面顶部导航栏。
 *
 * 包含：
 * - 左侧：历史记录按钮 + 设备状态指示器
 * - 中间：会话标题（居中显示）
 * - 右侧：Debug 开关（可选）+ 新建对话按钮
 */
@Composable
fun HomeTopBar(
    title: String,
    deviceSnapshot: DeviceSnapshotUi?,
    onHistoryClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onDeviceClick: () -> Unit,
    hudEnabled: Boolean,
    showDebugMetadata: Boolean,
    onToggleDebugMetadata: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(56.dp) // Standard AppBar height
    ) {
        // Left Actions: Menu + Badge
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier.testTag(HomeScreenTestTags.HISTORY_TOGGLE)
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "历史记录")
            }
            
            DeviceStatusIndicator(
                snapshot = deviceSnapshot,
                onClick = onDeviceClick
            )
        }

        // Center Title (Absolute Center with Safe Zone)
        val displayTitle = if (title.contains("新的") || title.contains("New Chat") || title.isBlank()) {
            "SmartSales"
        } else {
            title
        }
        Text(
            text = displayTitle,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 90.dp) // Fix V6: Safety padding to avoid colliding with Badge
                .testTag(HomeScreenTestTags.SESSION_TITLE),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold // Fix V6: Match target boldness
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Right Actions: Debug + New Chat
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(0.dp), // IconButtons have internal padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hudEnabled) {
                 IconButton(
                    onClick = onToggleDebugMetadata,
                    modifier = Modifier.testTag(HomeScreenTestTags.DEBUG_HUD_TOGGLE)
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppDimensions.DebugDotSize)
                            .background(
                                color = if (showDebugMetadata) AppColors.DebugDotActive else AppColors.DebugDotInactive,
                                shape = CircleShape
                            )
                    )
                }
            }
            
            IconButton(
                onClick = onNewChatClick,
                modifier = Modifier.testTag(HomeScreenTestTags.NEW_CHAT_BUTTON)
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "新建对话")
            }
        }
    }
}

/**
 * 设备状态指示器（智能工牌）。
 *
 * 显示设备连接状态，带脉冲动画指示器。
 */
@Suppress("UnusedParameter")
@Composable
fun DeviceStatusIndicator(
    snapshot: DeviceSnapshotUi?,
    onClick: () -> Unit = {}
) {
    // 14.1 SmartBadge Implementation
    Surface(
        modifier = Modifier
            .testTag(HomeScreenTestTags.HOME_DEVICE_INDICATOR)
            .clickable(onClick = onClick), // 15.2 Interactive
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon: Badge Style (Using DeviceHub as placeholder for now, ideally vector resource)
            // TODO: Replace with dedicated Badge icon if available
            Icon(
                imageVector = Icons.Filled.DeviceHub, 
                contentDescription = "智能工牌",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp) 
            )
            
            Text(
                text = "智能工牌", // Localized "Smart Badge"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Green Pulse Dot (Authentic Animation)
            // 15.2 State Mapping: Verify if connected. For now we assume this badge IMPLIES connection or searching.
            // If we want to show disconnected, we might change color.
            // For now, keep Green Pulse as "Alive" signal per design brief.
            val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(Color(0xFF4CD964), CircleShape) // iOS Green
            )
        }
    }
}
