package com.smartsales.aitest.ui.home.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/home/components/HomeHeader.kt
// 模块：:app
// 说明：首页顶部栏 — 包含历史记录触发器、会话标题、设备状态
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.PrismButton
import com.smartsales.aitest.ui.components.PrismButtonStyle
import com.smartsales.aitest.ui.theme.AccentSecondary
import com.smartsales.aitest.ui.theme.TextMuted
import com.smartsales.aitest.ui.theme.TextPrimary

/**
 * HomeHeader — 首页顶部导航栏
 *
 * 布局结构:
 * [Menu] [Signal]  Session Title...   [Debug] [New]
 *
 * @param title 当前会话标题
 * @param isConnected 设备连接状态
 * @param onHistoryClick 历史记录点击
 * @param onNewSessionClick 新会话点击
 * @param onDebugClick 调试模式点击
 */
@Composable
fun HomeHeader(
    title: String,
    isConnected: Boolean,
    onHistoryClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onConnectionClick: () -> Unit,
    onDebugClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: History Trigger
        IconButton(onClick = onHistoryClick) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "History",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Connection State
        IconButton(onClick = onConnectionClick) {
            Icon(
                imageVector = Icons.Rounded.SignalCellularAlt,
                contentDescription = "Connection",
                tint = if (isConnected) AccentSecondary else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Center: Session Title (Editable in future)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        // Right: Debug & New Session
        IconButton(onClick = onDebugClick) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Debug",
                tint = TextMuted
            )
        }

        PrismButton(
            text = "新会话",
            onClick = onNewSessionClick,
            style = PrismButtonStyle.GHOST,
            modifier = Modifier.height(36.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}
