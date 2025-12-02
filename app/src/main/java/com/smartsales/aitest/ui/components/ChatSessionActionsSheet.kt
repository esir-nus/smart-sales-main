package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/ChatSessionActionsSheet.kt
// 模块：:app
// 说明：聊天会话操作底部弹窗，提供重命名/置顶/删除
// 作者：创建于 2025-12-02

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.screens.history.model.ChatSessionUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSessionActionsSheet(
    session: ChatSessionUi,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = session.title, color = Color.Unspecified)
            ActionItem(
                icon = Icons.Filled.PushPin,
                text = if (session.isPinned) "取消置顶" else "置顶会话",
                onClick = {
                    onPinToggle()
                    onDismiss()
                }
            )
            ActionItem(
                icon = Icons.Filled.Edit,
                text = "重命名",
                onClick = {
                    onRename()
                    onDismiss()
                }
            )
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                ActionItem(
                    icon = Icons.Filled.Delete,
                    text = "删除会话",
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "取消")
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(
                indication = null,
                interactionSource = MutableInteractionSource(),
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        Text(
            text = text,
            color = tint,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun clickableNoRipple(onClick: () -> Unit): Modifier = Modifier
    .fillMaxWidth()
    .padding(vertical = 2.dp)
    .clickable(
        indication = null,
        interactionSource = MutableInteractionSource(),
        onClick = onClick
    )
