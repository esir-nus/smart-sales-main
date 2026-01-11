// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/history/HistoryDeviceCard.kt
// 模块：:feature:chat
// 说明：历史抽屉内的设备状态卡片
// 作者：从 HomeScreen.kt 提取于 2026-01-11

package com.smartsales.feature.chat.home.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.DeviceConnectionStateUi
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeScreenTestTags

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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag(HomeScreenTestTags.HISTORY_DEVICE_STATUS),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DeviceHub,
                    contentDescription = "设备状态",
                    tint = if (isConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isConnected && snapshot?.deviceName != null) {
                        snapshot.deviceName
                    } else {
                        "设备状态"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isConnected) {
                    Text(
                        text = "● 已连接",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = snapshot?.statusText ?: "设备状态将在硬件接入后展示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isConnected) {
                Text(
                    text = "硬件上线后将在此显示连接信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
