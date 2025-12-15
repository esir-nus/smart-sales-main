package com.smartsales.aitest.ui.screens.audio

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/audio/AudioFilesShell.kt
// 模块：:app
// 说明：底部导航 Audio 页的真实功能壳，复用 AudioFilesRoute 并对接聊天上下文
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.AiFeatureTestTags
import com.smartsales.aitest.audio.AudioFilesRoute
import com.smartsales.aitest.audio.TranscriptionProvider
import com.smartsales.aitest.audio.TranscriptionProviderSettings
import com.smartsales.feature.chat.home.HomeScreenViewModel
import com.smartsales.feature.chat.home.TranscriptionChatRequest

@Composable
fun AudioFilesShell(
    homeViewModel: HomeScreenViewModel,
    onNavigateHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AiFeatureTestTags.PAGE_AUDIO_FILES)
    ) {
        // 重要：该开关仅用于测试壳快速切换，不侵入 feature/media 代码。
        var provider by remember { mutableStateOf(TranscriptionProviderSettings.current) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "转写提供方：",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = provider == TranscriptionProvider.XFYUN,
                onClick = {
                    provider = TranscriptionProvider.XFYUN
                    TranscriptionProviderSettings.current = provider
                },
                label = { Text("XFyun") },
                leadingIcon = if (provider == TranscriptionProvider.XFYUN) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterChip(
                selected = provider == TranscriptionProvider.TINGWU,
                onClick = {
                    provider = TranscriptionProvider.TINGWU
                    TranscriptionProviderSettings.current = provider
                },
                label = { Text("Tingwu") },
                leadingIcon = if (provider == TranscriptionProvider.TINGWU) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
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
