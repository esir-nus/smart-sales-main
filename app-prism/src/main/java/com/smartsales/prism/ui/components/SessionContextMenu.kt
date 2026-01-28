package com.smartsales.prism.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 会话上下文菜单 — 长按触发
 * 操作: 置顶/重命名/删除
 * @see prism-ui-ux-contract.md §1.4
 */
@Composable
fun SessionContextMenu(
    sessionId: String,
    isPinned: Boolean,
    onPin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1A1A2E),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .padding(8.dp)
            ) {
                // 置顶/取消置顶
                ContextMenuItem(
                    icon = Icons.Filled.PushPin,
                    text = if (isPinned) "取消置顶" else "📌 置顶",
                    tint = if (isPinned) Color.Gray else Color(0xFFFFB74D),
                    onClick = {
                        onPin()
                        onDismiss()
                    }
                )
                
                HorizontalDivider(color = Color(0xFF333333))
                
                // 重命名
                ContextMenuItem(
                    icon = Icons.Filled.Edit,
                    text = "✏️ 重命名",
                    tint = Color(0xFF64B5F6),
                    onClick = {
                        onRename()
                        onDismiss()
                    }
                )
                
                HorizontalDivider(color = Color(0xFF333333))
                
                // 删除
                ContextMenuItem(
                    icon = Icons.Filled.Delete,
                    text = "🗑️ 删除",
                    tint = Color(0xFFFF5252),
                    onClick = {
                        onDelete()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}
