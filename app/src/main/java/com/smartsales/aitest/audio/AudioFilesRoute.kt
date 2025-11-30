package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/AudioFilesRoute.kt
// 模块：:app
// 说明：桥接 AudioFilesViewModel 与 Compose 界面的 Route
// 作者：创建于 2025-11-21

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import android.app.ActivityManager
import com.smartsales.aitest.BuildConfig
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.media.audio.AudioFilesScreen
import com.smartsales.feature.media.audio.AudioFilesViewModel

@Composable
fun AudioFilesRoute(
    modifier: Modifier = Modifier,
    viewModel: AudioFilesViewModel = hiltViewModel(),
    onAskAiAboutTranscript: (recordingId: String, fileName: String, jobId: String?, preview: String?, fullTranscript: String?) -> Unit = { _, _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (ActivityManager.isRunningInTestHarness() || BuildConfig.DEBUG) {
            viewModel.seedDemoDataIfEmpty()
        }
    }

    AudioFilesScreen(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onSyncClicked = viewModel::onSyncClicked,
        onRecordingClicked = viewModel::onRecordingClicked,
        onPlayPauseClicked = viewModel::onPlayPauseClicked,
        onDeleteClicked = viewModel::onDeleteClicked,
        onTranscribeClicked = viewModel::onTranscribeClicked,
        onTranscriptClicked = viewModel::onTranscriptClicked,
        onAskAiClicked = { recording ->
            viewModel.onTranscriptDismissed()
            onAskAiAboutTranscript(
                recording.id,
                recording.fileName,
                uiState.tingwuTaskIds[recording.id],
                recording.transcriptPreview,
                recording.fullTranscriptMarkdown
            )
        },
        onTranscriptDismissed = viewModel::onTranscriptDismissed,
        onErrorDismissed = viewModel::onErrorDismissed,
        modifier = modifier
    )
}
