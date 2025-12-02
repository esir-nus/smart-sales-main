package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/ChatInput.kt
// 模块：:app
// 说明：旧版聊天输入组件（已弃用，保留兼容性）
// 作者：创建于 2025-12-02

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ChatInput(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    onSkillClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 保留空壳以兼容旧引用，实际逻辑已迁移至新版 HomeScreen
}
