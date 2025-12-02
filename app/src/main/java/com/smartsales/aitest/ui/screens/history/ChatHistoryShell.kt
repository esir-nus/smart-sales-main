package com.smartsales.aitest.ui.screens.history

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/history/ChatHistoryShell.kt
// 模块：:app
// 说明：底部导航壳内的聊天历史承载层，复用 feature/chat 的 ChatHistoryRoute
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.smartsales.feature.chat.history.ChatHistoryRoute
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.HomeScreenViewModel

@Composable
fun ChatHistoryShell(
    homeViewModel: HomeScreenViewModel,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("page_chat_history")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(HomeScreenTestTags.HISTORY_PANEL)
        ) {
            ChatHistoryRoute(
                modifier = Modifier.fillMaxSize(),
                onBackClick = onBack,
                onSessionSelected = { sessionId ->
                    homeViewModel.setSession(sessionId, allowHero = false)
                    onBack()
                }
            )
        }
    }
}
