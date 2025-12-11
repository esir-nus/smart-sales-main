package com.smartsales.aitest.navigation

// 文件：app/src/main/java/com/smartsales/aitest/navigation/Screen.kt
// 模块：:app
// 说明：底部导航路由定义，提供标签与图标
// 作者：创建于 2025-12-02

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : Screen("home", "对话", Icons.Filled.Chat)
    data object Audio : Screen("audio_files", "录音", Icons.Filled.AudioFile)
    data object Device : Screen("device_manager", "设备", Icons.Filled.Devices)
    data object User : Screen("user_center", "我的", Icons.Filled.Person)
    data object ChatHistory : Screen("chat_history", "聊天记录", Icons.Filled.Chat)
    data object DebugStream : Screen("debug_llm_stream", "流式调试", Icons.Filled.Tune)

    companion object {
        val items = listOf(Home, Audio, Device, User)
    }
}
