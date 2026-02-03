package com.smartsales.aitest.ui.home.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/home/components/ActiveSessionArea.kt
// 模块：:app
// 说明：首页活跃会话区域 — 包含模式切换 (Coach/Analyst) 和输入框
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.PrismInput
import com.smartsales.aitest.ui.theme.AccentBlue
import com.smartsales.aitest.ui.theme.AccentTertiary
import com.smartsales.aitest.ui.theme.TextMuted

/**
 * 会话模式
 */
enum class SessionMode {
    COACH,
    ANALYST
}

/**
 * ActiveSessionArea — 底部输入和模式选择区域
 *
 * @param onSend 发送回调
 * @param modifier 修饰符
 */
@Composable
fun ActiveSessionArea(
    onSend: (String, SessionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var currentMode by remember { mutableStateOf(SessionMode.COACH) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Mode Toggle (Simplified for now - strictly text/icon toggle in future)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            ModeToggleItem(
                label = "Coach Mode",
                isSelected = currentMode == SessionMode.COACH,
                onClick = { currentMode = SessionMode.COACH },
                color = AccentTertiary // Purple for Coach
            )
            
            Spacer(modifier = Modifier.width(16.dp))

            ModeToggleItem(
                label = "Analyst Mode",
                isSelected = currentMode == SessionMode.ANALYST,
                onClick = { currentMode = SessionMode.ANALYST },
                color = AccentBlue // Blue for Analyst
            )
        }

        // Input Area
        PrismInput(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = "输入消息...",
            shimmerPlaceholder = true,
            leadingIcon = Icons.Default.AttachFile,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModeToggleItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (isSelected) color else TextMuted,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    )
}
