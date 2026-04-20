// File: app-core/src/main/java/com/smartsales/prism/ui/components/CopyableBubble.kt
// Module: :app-core
// Summary: 长按聊天气泡弹出"复制"菜单的通用包装组件
// Author: created on 2026-04-20

package com.smartsales.prism.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartsales.prism.ui.util.ClipboardHelper

/**
 * 长按聊天气泡弹出"复制"菜单。
 * 仅注册 onLongClick，onClick 留空以避免覆盖内部可点击元素（如"展开/收起"行）。
 * @see HistoryDrawer 中 combinedClickable + DropdownMenu 模式
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CopyableBubble(
    textToCopy: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {},
            onLongClick = { showMenu = true }
        )
    ) {
        content()

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color(0xFF2A2A40),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            DropdownMenuItem(
                text = { Text("复制", color = Color.White) },
                onClick = {
                    showMenu = false
                    ClipboardHelper.copy(context, textToCopy)
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
