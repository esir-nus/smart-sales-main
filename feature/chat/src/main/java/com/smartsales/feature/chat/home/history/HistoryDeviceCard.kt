// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/history/HistoryDeviceCard.kt
// 模块：:feature:chat
// 说明：历史抽屉内的设备状态卡片
// 作者：从 HomeScreen.kt 提取于 2026-01-11

package com.smartsales.feature.chat.home.history

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.DeviceConnectionStateUi
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.theme.AppColors

/**
 * 历史抽屉内的设备状态卡片。
 *
 * 显示设备连接状态、设备名称和状态描述。
 *
 * @param snapshot 设备快照状态
 */
@Composable
fun HistoryDeviceCard(snapshot: DeviceSnapshotUi?) {
    val isConnected = snapshot?.connectionState == DeviceConnectionStateUi.CONNECTED
    
    // Adaptive Crystal Logic (Pro Max Theme Awareness)
    val isDark = isSystemInDarkTheme()
    
    // Light Mode: "Obsidian Schematic" (High Contrast for Visibility)
    // Dark Mode: "Crystal Schematic" (White/Glass technical details)
    val gridColor = when {
        !isConnected -> Color.Transparent
        isDark -> Color.White.copy(alpha = 0.10f)
        else -> Color.Black.copy(alpha = 0.12f)
    }
    val borderColor = when {
        !isConnected -> Color.Transparent
        isDark -> Color.White.copy(alpha = 0.40f)
        else -> Color.Black.copy(alpha = 0.20f)
    }
    val scrimColor = when {
        !isConnected -> Color.Transparent
        isDark -> Color.White.copy(alpha = 0.02f)
        else -> Color.Black.copy(alpha = 0.05f)
    }
    val cardBgColor = if (isDark) AppColors.CrystalCardBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f) // Solid plate look (was 0.30)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 24.dp)
            .testTag(HomeScreenTestTags.HISTORY_DEVICE_STATUS)
            .shadow(
                elevation = if (isDark) 0.dp else 2.dp, // Subtle shadow in light mode for lift
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent, // Managed by Glass Stack
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .background(cardBgColor, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .drawBehind {
                     // Glass Scrim (Simulates Blur density)
                     drawRect(scrimColor)

                     // Technical Mesh Overlay (Adaptive)
                     val gridSize = 20.dp.toPx()
                     
                     // Vertical lines
                     for (x in 0..size.width.toInt() step gridSize.toInt()) {
                         drawLine(
                             color = gridColor,
                             start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                             end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height),
                             strokeWidth = 1f
                         )
                     }
                     // Horizontal lines
                     for (y in 0..size.height.toInt() step gridSize.toInt()) {
                         drawLine(
                             color = gridColor,
                             start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()),
                             end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()),
                             strokeWidth = 1f
                         )
                     }
                }
                .clickable(onClick = { /* TODO: Device Manager */ })
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Precision Beacon
                PrecisionBeacon(isConnected = isConnected)
                
                Column {
                    Text(
                        text = snapshot?.deviceName ?: "SmartBadge",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isConnected) "已连接 • 电量 85%" else "未连接",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PrecisionBeacon(isConnected: Boolean) {
    if (!isConnected) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
             // Static Ring (Disconnected)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .border(1.dp, AppColors.StatusDotDisconnectedRing, CircleShape)
            )
            // Core Dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AppColors.LightDangerText, CircleShape)
            )
        }
        return
    }

    // Schematic Pulse
    val infiniteTransition = rememberInfiniteTransition(label = "schematic_pulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3f, // 100% -> 300%
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "size"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
        // Expanding Ring
        Box(
            modifier = Modifier
                .size(8.dp) // Base size
                .graphicsLayer { 
                    scaleX = pulseSize
                    scaleY = pulseSize
                    alpha = pulseAlpha
                }
                .border(1.dp, AppColors.AuroraMint, CircleShape)
        )
        // Static Core
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(AppColors.AuroraMint, CircleShape)
                .shadow(elevation = 10.dp, spotColor = AppColors.AuroraMint)
        )
    }
}
