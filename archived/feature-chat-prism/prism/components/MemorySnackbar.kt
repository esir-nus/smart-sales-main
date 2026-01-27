package com.smartsales.feature.chat.prism.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartsales.data.prismlib.notifications.MemoryCategory
import com.smartsales.data.prismlib.notifications.MemoryNotification
import com.smartsales.data.prismlib.notifications.RealMemoryCenterNotifier
import kotlinx.coroutines.delay

/**
 * 内存更新 Snackbar — 订阅 MemoryCenterNotifier 展示通知
 * @see prism-ui-ux-contract.md, Prism-V1.md §2.2 #7
 */
@Composable
fun MemorySnackbar(
    notifier: RealMemoryCenterNotifier,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 订阅通知流
    LaunchedEffect(notifier) {
        notifier.notifications.collect { notification ->
            val result = snackbarHostState.showSnackbar(
                message = notification.format(),
                duration = SnackbarDuration.Short
            )
            // 可选：处理点击事件
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = getColorForCategory(data.visuals.message),
                contentColor = Color.White
            )
        }
    )
}

/**
 * 根据消息内容判断类别颜色
 */
private fun getColorForCategory(message: String): Color {
    return when {
        message.startsWith("习惯已更新") -> Color(0xFF4CAF50)   // Green
        message.startsWith("客户已更新") -> Color(0xFF2196F3)   // Blue
        message.startsWith("日程已更新") -> Color(0xFFFF9800)   // Orange
        message.startsWith("资料已更新") -> Color(0xFF9C27B0)   // Purple
        else -> Color(0xFF424242)  // Gray
    }
}
