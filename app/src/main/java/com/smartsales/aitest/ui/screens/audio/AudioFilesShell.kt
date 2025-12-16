// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/audio/AudioFilesShell.kt
// 模块：:app
// 说明：底部导航 Audio 页的真实功能壳，复用 AudioFilesRoute 并对接聊天上下文
// 作者：创建于 2025-12-02
package com.smartsales.aitest.ui.screens.audio

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.aitest.AiFeatureTestTags
import com.smartsales.aitest.audio.AudioFilesRoute
import com.smartsales.aitest.audio.TranscriptionProvider
import com.smartsales.feature.chat.home.HomeScreenViewModel
import com.smartsales.feature.chat.home.TranscriptionChatRequest
import java.util.Locale

@Composable
fun AudioFilesShell(
    homeViewModel: HomeScreenViewModel,
    onNavigateHome: () -> Unit
) {
    val settingsViewModel: AiParaSettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val provider = TranscriptionProvider.entries.firstOrNull {
        it.name == settings.transcriptionProvider.trim().uppercase(Locale.US)
    } ?: TranscriptionProvider.XFYUN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AiFeatureTestTags.PAGE_AUDIO_FILES)
    ) {
        // 重要：测试壳写入 AiParaSettings，作为转写运行时开关的唯一来源，不侵入 feature/media API。
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
                    settingsViewModel.setTranscriptionProvider(TranscriptionProvider.XFYUN)
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
                    settingsViewModel.setTranscriptionProvider(TranscriptionProvider.TINGWU)
                },
                label = { Text("Tingwu") },
                leadingIcon = if (provider == TranscriptionProvider.TINGWU) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )

            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "顺滑：",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = settings.xfyunEngSmoothproc,
                onClick = {
                    settingsViewModel.setXfyunEngSmoothproc(!settings.xfyunEngSmoothproc)
                },
                label = { Text("eng_smoothproc") },
                leadingIcon = if (settings.xfyunEngSmoothproc) {
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
