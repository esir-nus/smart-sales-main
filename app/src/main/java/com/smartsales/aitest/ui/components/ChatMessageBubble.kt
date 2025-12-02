package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/ChatMessageBubble.kt
// 模块：:app
// 说明：聊天气泡组件，支持用户/助手/系统角色与时间戳
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.screens.home.model.ChatMessage
import com.smartsales.aitest.ui.screens.home.model.MessageRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isUser = message.role == MessageRole.USER
    val isSystem = message.role == MessageRole.SYSTEM
    val (bubbleColor, textColor) = when (message.role) {
        MessageRole.USER -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        MessageRole.ASSISTANT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
        MessageRole.SYSTEM -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isSystem) "SYS" else "AI",
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        }
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bubbleColor,
                shape = shape,
                tonalElevation = if (isUser) 0.dp else 1.dp,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier
                        .clip(shape)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                text = timeFormatter.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}
