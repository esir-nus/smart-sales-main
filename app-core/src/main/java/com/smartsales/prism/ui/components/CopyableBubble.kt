// File: app-core/src/main/java/com/smartsales/prism/ui/components/CopyableBubble.kt
// Module: :app-core
// Summary: 长按聊天气泡弹出"复制"菜单的通用包装组件
// Author: created on 2026-04-20

package com.smartsales.prism.ui.components

import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartsales.prism.ui.util.ClipboardHelper
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 长按聊天气泡弹出"复制"菜单。
 * 使用手动 awaitEachGesture 轮询替代 detectTapGestures，通过 changedToUpIgnoreConsumed
 * 忽略父级 LazyColumn scrollable 对事件的消费，避免长按被滚动手势竞争取消。
 */
@Composable
fun CopyableBubble(
    textToCopy: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = modifier.pointerInput(textToCopy) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                // withTimeoutOrNull 返回 null = 超时且手指仍按下 = 长按
                val liftedBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent()
                        // 忽略父级消费状态，仅判断手指是否抬起
                        if (event.changes.any { it.changedToUpIgnoreConsumed() || !it.pressed }) break
                    }
                    true
                }
                if (liftedBeforeTimeout == null) showMenu = true
            }
        }
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
