package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesScreen.kt
// 模块：:feature:media
// 说明：音频库 Compose 界面，展示列表并触发同步、播放、应用及删除
// 作者：创建于 2025-11-21

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AudioFilesScreen(
    uiState: AudioFilesUiState,
    onRefresh: () -> Unit,
    onSyncClicked: () -> Unit,
    onRecordingClicked: (String) -> Unit,
    onPlayPauseClicked: (String) -> Unit,
    onApplyClicked: (String) -> Unit,
    onDeleteClicked: (String) -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(AudioFilesTestTags.ROOT),
        topBar = {
            TopAppBar(
                title = { Text(text = "音频库") },
                actions = {
                    IconButton(
                        onClick = onSyncClicked,
                        modifier = Modifier.testTag(AudioFilesTestTags.SYNC_BUTTON)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "同步")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isSyncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.errorMessage?.let { message ->
                ErrorBanner(
                    message = message,
                    onDismiss = onErrorDismissed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AudioFilesTestTags.ERROR_BANNER)
                )
            }
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.recordings.isEmpty()) {
                EmptyState(
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(AudioFilesTestTags.EMPTY_STATE)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.recordings, key = { it.id }) { recording ->
                        AudioRecordingItem(
                            recording = recording,
                            onClick = onRecordingClicked,
                            onPlayPauseClicked = onPlayPauseClicked,
                            onApplyClicked = onApplyClicked,
                            onDeleteClicked = onDeleteClicked
                        )
                    }
                }
            }
        }
    }
}

object AudioFilesTestTags {
    const val ROOT = "audio_files_screen_root"
    const val SYNC_BUTTON = "audio_files_sync_button"
    const val EMPTY_STATE = "audio_files_empty_state"
    const val ERROR_BANNER = "audio_files_error_banner"
}

@Composable
private fun AudioRecordingItem(
    recording: AudioRecordingUi,
    onClick: (String) -> Unit,
    onPlayPauseClicked: (String) -> Unit,
    onApplyClicked: (String) -> Unit,
    onDeleteClicked: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${recording.createdAtText} · ${recording.fileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ElevatedAssistChip(
                    onClick = { onClick(recording.id) },
                    label = {
                        Text(
                            text = when (recording.transcriptionStatus) {
                                AudioTranscriptionStatus.None -> "未转写"
                                AudioTranscriptionStatus.InProgress -> "转写中"
                                AudioTranscriptionStatus.Done -> "已转写"
                                AudioTranscriptionStatus.Error -> "失败"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null
                        )
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onPlayPauseClicked(recording.id) }) {
                    Icon(
                        imageVector = if (recording.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (recording.isPlaying) "暂停" else "播放"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { onApplyClicked(recording.id) }) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "应用")
                }
                TextButton(onClick = { onDeleteClicked(recording.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "删除")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "暂无音频", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "同步设备录音或刷新列表以获取最新音频。",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "刷新")
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
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onDismiss) {
            Text(text = "知道了")
        }
    }
}
