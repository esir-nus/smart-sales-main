package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/AudioFilesScreen.kt
// 模块：:app
// 说明：音频库页面的 Route 与 Compose UI，串联媒体服务器、OSS、Tingwu 状态
// 作者：创建于 2025-11-21

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.media.audiofiles.AudioFilesUiState
import com.smartsales.feature.media.audiofiles.AudioFilesViewModel
import com.smartsales.feature.media.audiofiles.AudioRecordingStatus
import com.smartsales.feature.media.audiofiles.AudioRecordingUi

@Composable
fun AudioFilesRoute(
    modifier: Modifier = Modifier,
    viewModel: AudioFilesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AudioFilesScreen(
        state = state,
        onRefresh = viewModel::onRefresh,
        onSyncClicked = viewModel::onSyncClicked,
        onPlayPause = viewModel::onPlayPause,
        onApply = viewModel::onApply,
        onDelete = viewModel::onDelete,
        onDismissError = viewModel::onDismissError,
        modifier = modifier
    )
}

@Composable
fun AudioFilesScreen(
    state: AudioFilesUiState,
    onRefresh: () -> Unit,
    onSyncClicked: () -> Unit,
    onPlayPause: (String) -> Unit,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val endpointAvailable = !state.baseUrl.isNullOrBlank()
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = state.baseUrl?.let { "设备地址：$it" } ?: "设备未连接，等待 Wi-Fi & BLE 同步",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (endpointAvailable) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AudioFilesTestTags.DEVICE_STATUS)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onSyncClicked,
                            enabled = endpointAvailable,
                            modifier = Modifier.testTag(AudioFilesTestTags.SYNC_BUTTON)
                        ) {
                            Text("同步并转写")
                        }
                        OutlinedButton(
                            onClick = onRefresh,
                            enabled = endpointAvailable
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("刷新列表")
                        }
                    }
                    if (state.isSyncing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            state.errorMessage?.let {
                item {
                    ErrorBanner(
                        message = it,
                        onDismiss = onDismissError,
                        modifier = Modifier.testTag(AudioFilesTestTags.ERROR_BANNER)
                    )
                }
            }
            if (state.recordings.isEmpty()) {
                item {
                    AudioEmptyState(
                        isDeviceConnected = endpointAvailable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AudioFilesTestTags.EMPTY_STATE)
                    )
                }
            } else {
                items(state.recordings, key = { it.id }) { recording ->
                    AudioRecordingCard(
                        recording = recording,
                        onPlayPause = onPlayPause,
                        onApply = onApply,
                        onDelete = onDelete,
                        modifier = Modifier.testTag(AudioFilesTestTags.item(recording.id))
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioRecordingCard(
    recording: AudioRecordingUi,
    onPlayPause: (String) -> Unit,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = recording.fileName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "大小：${recording.sizeText} · 更新时间：${recording.modifiedAtText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = statusLabel(recording),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            recording.transcriptSummary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onPlayPause(recording.id) }) {
                    if (recording.isPlaying) {
                        Icon(Icons.Default.Pause, contentDescription = "暂停")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onApply(recording.id) }) {
                        Text("应用")
                    }
                    IconButton(onClick = { onDelete(recording.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioEmptyState(
    isDeviceConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isDeviceConnected) {
                Text(text = "暂无录音", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "点击上方按钮同步设备录音，或刷新以重新拉取列表。",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(text = "设备未连接", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "请在 Wi-Fi & BLE 页面连接设备后自动拉取录音列表。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onDismiss) {
            Text("知道了")
        }
    }
}

private fun statusLabel(recording: AudioRecordingUi): String = when (recording.status) {
    AudioRecordingStatus.Idle -> "等待同步"
    AudioRecordingStatus.Syncing -> "下载中..."
    AudioRecordingStatus.Transcribing -> "转写中 ${recording.progressPercent}%"
    AudioRecordingStatus.Transcribed -> "转写完成"
    AudioRecordingStatus.Error -> "出错"
}

object AudioFilesTestTags {
    const val SYNC_BUTTON = "audio_files_sync_button"
    const val EMPTY_STATE = "audio_files_empty_state"
    const val ERROR_BANNER = "audio_files_error_banner"
    const val DEVICE_STATUS = "audio_files_device_status"
    fun item(id: String) = "audio_files_item_$id"
}
