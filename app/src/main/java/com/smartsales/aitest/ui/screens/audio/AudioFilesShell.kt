// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/audio/AudioFilesShell.kt
// 模块：:app
// 说明：底部导航 Audio 页的真实功能壳，复用 AudioFilesRoute 并对接聊天上下文
// 作者：创建于 2025-12-02
package com.smartsales.aitest.ui.screens.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.smartsales.aitest.AiFeatureTestTags
import com.smartsales.aitest.audio.AudioFilesRoute
import com.smartsales.feature.chat.home.HomeViewModel
import com.smartsales.feature.chat.home.TranscriptionChatRequest

@Composable
fun AudioFilesShell(
    homeViewModel: HomeViewModel,
    onNavigateHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AiFeatureTestTags.PAGE_AUDIO_FILES)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AudioFilesRoute(
                modifier = Modifier.fillMaxSize(),
                onAskAiAboutTranscript = { recordingId, fileName, jobId, sessionId, preview, full ->
                    val resolvedJobId = jobId ?: "transcription-$recordingId"
                    homeViewModel.onTranscriptionRequested(
                        TranscriptionChatRequest(
                            jobId = resolvedJobId,
                            fileName = fileName,
                            recordingId = recordingId,
                            sessionId = sessionId,
                            transcriptPreview = preview,
                            transcriptMarkdown = full
                        )
                    )
                    onNavigateHome()
                }
            )
        }
    }
}
