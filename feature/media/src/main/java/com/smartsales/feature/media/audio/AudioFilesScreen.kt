package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesScreen.kt
// 模块：:feature:media
// 说明：音频库 Compose 界面，展示列表并触发同步、播放与删除（预留转写占位）
// 作者：创建于 2025-11-21

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.core.util.AppDesignTokens
import com.smartsales.feature.media.audio.TingwuChapterUi
import com.smartsales.feature.media.audio.TingwuSmartSummaryUi
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
fun AudioFilesScreen(
    uiState: AudioFilesUiState,
    onRefresh: () -> Unit,
    onSyncClicked: () -> Unit,
    onImportLocalClicked: () -> Unit,
    onRecordingClicked: (String) -> Unit,
    onPlayPauseClicked: (String) -> Unit,
    onDeleteClicked: (String) -> Unit,
    onTranscribeClicked: (String) -> Unit,
    onTranscriptClicked: (String) -> Unit,
    onFlagToggle: (String) -> Unit,  // V17: Star flag toggle
    onAskAiClicked: (AudioRecordingUi) -> Unit,
    onTranscriptDismissed: () -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val designTokens = AppDesignTokens.current()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isSyncing || uiState.isLoading,
        onRefresh = onSyncClicked
    )

    // P2: One-time swipe hint animation
    var swipeHintShown by remember { mutableStateOf(false) }
    val firstNoneIndex = uiState.recordings.indexOfFirst { it.transcriptionStatus == TranscriptionStatus.NONE }
    androidx.compose.runtime.LaunchedEffect(uiState.recordings.isNotEmpty()) {
        if (uiState.recordings.isNotEmpty() && !swipeHintShown) {
            kotlinx.coroutines.delay(600)
            swipeHintShown = true
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(AudioFilesTestTags.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "录音文件") },
                actions = {
                    IconButton(
                        onClick = onImportLocalClicked,
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.testTag(AudioFilesTestTags.IMPORT_LOCAL_BUTTON)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "导入本地")
                    }
                    IconButton(
                        onClick = onSyncClicked,
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.testTag(AudioFilesTestTags.SYNC_BUTTON)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "同步")
                    }
                    IconButton(onClick = onRefresh, enabled = !uiState.isSyncing) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(designTokens.mutedSurface)
                    .pullRefresh(pullRefreshState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "管理录音、同步 Tingwu 转写并用 AI 分析通话。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.isSyncing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Text(
                            text = "同步中…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
                uiState.loadErrorMessage?.let {
                    Text(
                        text = "加载录音失败，请检查网络或稍后重试。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onRefresh) {
                        Text(text = "重试")
                    }
                }
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "正在加载录音...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (uiState.recordings.isEmpty()) {
                    EmptyState(
                        onRefresh = onRefresh,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(AudioFilesTestTags.EMPTY_STATE)
                    )
                } else {
                    Text(
                        text = "录音列表 · 共 ${uiState.recordings.size} 条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(AudioFilesTestTags.RECORDING_LIST),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(uiState.recordings, key = { _, item -> item.id }) { index, recording ->
                            // V17: SwipeableRecordingCard with gesture-based actions
                            SwipeableRecordingCard(
                                recording = recording,
                                onTap = { onTranscriptClicked(recording.id) },  // V17: Opens transcript sheet
                                onPlayClicked = { onPlayPauseClicked(recording.id) },
                                onTranscribeClicked = { onTranscribeClicked(recording.id) },
                                onDeleteClicked = { onDeleteClicked(recording.id) },
                                onFlagToggle = { onFlagToggle(recording.id) },
                                onLongPress = { /* Multi-select deferred */ },
                                showHint = !swipeHintShown && index == firstNoneIndex && firstNoneIndex >= 0
                            )
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = uiState.isSyncing || uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
    uiState.transcriptPreviewRecording?.let { recording ->
        TranscriptViewerSheet(
            recording = recording,
            onDismiss = onTranscriptDismissed,
            onPlayClicked = onPlayPauseClicked,
            onAskAiClicked = onAskAiClicked
        )
    }
}

object AudioFilesTestTags {
    const val ROOT = "audio_files_screen_root"
    const val SYNC_BUTTON = "audio_files_sync_button"
    const val IMPORT_LOCAL_BUTTON = "audio_files_import_local_button"
    const val EMPTY_STATE = "audio_files_empty_state"
    const val ERROR_BANNER = "audio_files_error_banner"
    const val RECORDING_LIST = "audio_files_recording_list"
    const val TRANSCRIBE_BUTTON_PREFIX = "audio_files_transcribe_"
    const val TRANSCRIPT_BUTTON_PREFIX = "audio_files_transcript_"
    const val STATUS_CHIP_PREFIX = "audio_files_status_chip_"
    const val TRANSCRIPT_DIALOG = "audio_files_transcript_dialog"
    const val TRANSCRIPT_CONTENT = "audio_files_transcript_content"
    const val TRANSCRIPT_SUMMARY = "audio_files_transcript_summary"
    const val ASK_AI_BUTTON = "audio_files_ask_ai_button"
}

// V17: Dead code deleted - AudioRecordingItem, StatusIcon, StatusBadges, SourceLabel, ActionButtons
// These were replaced by SwipeableRecordingCard.kt

@Composable
private fun EmptyState(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "暂无录音文件", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "下拉或点击同步即可加载录音。",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "刷新录音")
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
            Text(text = "收起提示")
        }
    }
}

// 312-635 extracted to TranscriptViewerSheet.kt
