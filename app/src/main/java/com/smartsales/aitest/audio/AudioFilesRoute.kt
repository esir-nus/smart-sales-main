package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/AudioFilesRoute.kt
// 模块：:app
// 说明：桥接 AudioFilesViewModel 与 Compose 界面的 Route
// 作者：创建于 2025-11-21

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.media.audio.AudioFilesScreen
import com.smartsales.feature.media.audio.AudioFilesViewModel

@Composable
fun AudioFilesRoute(
    modifier: Modifier = Modifier
) {
    val viewModel: AudioFilesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AudioFilesScreen(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onSyncClicked = viewModel::onSyncClicked,
        onRecordingClicked = viewModel::onRecordingClicked,
        onPlayPauseClicked = viewModel::onPlayPauseClicked,
        onApplyClicked = viewModel::onApplyClicked,
        onDeleteClicked = viewModel::onDeleteClicked,
        onErrorDismissed = viewModel::onErrorDismissed,
        modifier = modifier
    )
}
