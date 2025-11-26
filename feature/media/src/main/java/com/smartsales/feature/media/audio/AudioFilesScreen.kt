package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesScreen.kt
// 模块：:feature:media
// 说明：音频库 Compose 界面，重构卡片列表与转写查看底sheet，突出同步与转写状态
// 作者：创建于 2025-11-26

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.feature.media.audio.TingwuSmartSummaryUi
import com.smartsales.feature.media.audio.TingwuChapterUi

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AudioFilesScreen(
    uiState: AudioFilesUiState,
    onRefresh: () -> Unit,
    onSyncClicked: () -> Unit,
    onRecordingClicked: (String) -> Unit,
    onPlayPauseClicked: (String) -> Unit,
    onDeleteClicked: (String) -> Unit,
    onTranscribeClicked: (String) -> Unit,
    onTranscriptClicked: (String) -> Unit,
    onAskAiClicked: (AudioRecordingUi) -> Unit,
    onTranscriptDismissed: () -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
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
                        onClick = onSyncClicked,
                        modifier = Modifier.testTag(AudioFilesTestTags.SYNC_BUTTON),
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "同步录音")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        .testTag(AudioFilesTestTags.ERROR_BANNER),
                )
            }
            uiState.loadErrorMessage?.let {
                ErrorBanner(
                    message = "加载录音失败，请稍后重试。",
                    onDismiss = onErrorDismissed,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onRefresh) {
                    Text(text = "重试")
                }
            }
            when {
                uiState.isLoading -> LoadingState()
                uiState.recordings.isEmpty() -> EmptyState(
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(AudioFilesTestTags.EMPTY_STATE),
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(AudioFilesTestTags.RECORDING_LIST),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.recordings, key = { it.id }) { recording ->
                            AudioRecordingCard(
                                recording = recording,
                                isSyncing = uiState.isSyncing,
                                onClick = {
                                    onRecordingClicked(recording.id)
                                    onTranscriptClicked(recording.id)
                                },
                                onPlayPauseClicked = onPlayPauseClicked,
                                onDeleteClicked = onDeleteClicked,
                                onTranscribeClicked = onTranscribeClicked,
                                onTranscriptClicked = onTranscriptClicked,
                                onSyncClicked = onSyncClicked,
                            )
                        }
                    }
                }
            }
        }
    }
    uiState.transcriptPreviewRecording?.let { recording ->
        TranscriptViewerSheet(
            recording = recording,
            onDismiss = onTranscriptDismissed,
            onAskAiClicked = onAskAiClicked,
            onRetranscribeClicked = onTranscribeClicked,
        )
    }
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onErrorDismissed()
        }
    }
}

object AudioFilesTestTags {
    const val ROOT = "audio_files_screen_root"
    const val SYNC_BUTTON = "audio_files_sync_button"
    const val RECORDING_LIST = "audio_files_list"
    const val EMPTY_STATE = "audio_files_empty_state"
    const val ERROR_BANNER = "audio_files_error_banner"
    const val CARD_PREFIX = "audio_files_card_"
    const val STATUS_TEXT_PREFIX = "audio_files_status_text_"
    const val TRANSCRIBE_BUTTON_PREFIX = "audio_files_transcribe_"
    const val TRANSCRIPT_BUTTON_PREFIX = "audio_files_transcript_"
    const val STATUS_CHIP_PREFIX = "audio_files_status_chip_"
    const val TRANSCRIPT_DIALOG = "audio_files_transcript_dialog"
    const val TRANSCRIPT_CONTENT = "audio_files_transcript_content"
    const val TRANSCRIPT_SUMMARY = "audio_files_transcript_summary"
    const val TRANSCRIPT_STATUS = "audio_files_transcript_status"
}

private data class StatusDisplay(
    val syncLabel: String,
    val syncColor: Color,
    val transcriptionLabel: String,
    val transcriptionColor: Color,
    val statusLine: String,
)

@Composable
private fun buildStatusDisplay(
    recording: AudioRecordingUi,
    isSyncing: Boolean,
): StatusDisplay {
    val syncLabel = when {
        isSyncing -> "同步中"
        recording.hasLocalCopy -> "已同步"
        else -> "未同步"
    }
    val syncColor = when {
        isSyncing -> MaterialTheme.colorScheme.primary
        recording.hasLocalCopy -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val transcriptionLabel = when (recording.transcriptionStatus) {
        TranscriptionStatus.NONE -> "未转写"
        TranscriptionStatus.IN_PROGRESS -> "转写中"
        TranscriptionStatus.DONE -> "转写完成"
        TranscriptionStatus.ERROR -> "转写失败"
    }
    val transcriptionColor = when (recording.transcriptionStatus) {
        TranscriptionStatus.NONE -> MaterialTheme.colorScheme.outline
        TranscriptionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TranscriptionStatus.DONE -> MaterialTheme.colorScheme.primary
        TranscriptionStatus.ERROR -> MaterialTheme.colorScheme.error
    }
    val statusLine = when (recording.transcriptionStatus) {
        TranscriptionStatus.IN_PROGRESS -> "转写中，请稍等…"
        TranscriptionStatus.DONE -> "转写完成，可查看转写和智能总结。"
        TranscriptionStatus.ERROR -> "转写失败，可重新发起转写。"
        TranscriptionStatus.NONE -> when {
            isSyncing -> "同步中，请稍后再转写。"
            recording.hasLocalCopy -> "已同步，可开始转写。"
            else -> "未同步，请先同步后再转写。"
        }
    }
    return StatusDisplay(
        syncLabel = syncLabel,
        syncColor = syncColor,
        transcriptionLabel = transcriptionLabel,
        transcriptionColor = transcriptionColor,
        statusLine = statusLine,
    )
}

@Composable
private fun AudioRecordingCard(
    recording: AudioRecordingUi,
    isSyncing: Boolean,
    onClick: () -> Unit,
    onPlayPauseClicked: (String) -> Unit,
    onDeleteClicked: (String) -> Unit,
    onTranscribeClicked: (String) -> Unit,
    onTranscriptClicked: (String) -> Unit,
    onSyncClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusDisplay = buildStatusDisplay(recording, isSyncing)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${AudioFilesTestTags.CARD_PREFIX}${recording.id}")
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(recording.createdAtText.ifBlank { "时间未知" })
                            val duration = recording.durationMillis?.let { formatDuration(it) }
                            if (!duration.isNullOrBlank()) {
                                append(" · $duration")
                            }
                            append(" · ${recording.fileName}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusBadge(
                        label = statusDisplay.syncLabel,
                        color = statusDisplay.syncColor,
                        icon = Icons.Default.Refresh,
                    )
                    StatusBadge(
                        label = statusDisplay.transcriptionLabel,
                        color = statusDisplay.transcriptionColor,
                        icon = Icons.Default.HourglassEmpty,
                        tag = "${AudioFilesTestTags.STATUS_CHIP_PREFIX}${recording.id}",
                    )
                }
            }
            Text(
                text = statusDisplay.statusLine,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("${AudioFilesTestTags.STATUS_TEXT_PREFIX}${recording.id}"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onPlayPauseClicked(recording.id) }) {
                    Icon(
                        imageVector = if (recording.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (recording.isPlaying) "暂停" else "播放",
                    )
                }
                TextButton(onClick = onSyncClicked) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "同步")
                }
                when (recording.transcriptionStatus) {
                    TranscriptionStatus.NONE -> {
                        TextButton(
                            onClick = { onTranscribeClicked(recording.id) },
                            modifier = Modifier.testTag("${AudioFilesTestTags.TRANSCRIBE_BUTTON_PREFIX}${recording.id}"),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "转写")
                        }
                    }
                    TranscriptionStatus.ERROR -> {
                        TextButton(
                            onClick = { onTranscribeClicked(recording.id) },
                            modifier = Modifier.testTag("${AudioFilesTestTags.TRANSCRIBE_BUTTON_PREFIX}${recording.id}"),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "重新转写")
                        }
                    }
                    TranscriptionStatus.IN_PROGRESS -> {
                        ElevatedAssistChip(
                            onClick = {},
                            label = { Text(text = "转写中…") },
                            leadingIcon = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                )
                            },
                        )
                    }
                    TranscriptionStatus.DONE -> {
                        TextButton(
                            onClick = { onTranscriptClicked(recording.id) },
                            modifier = Modifier.testTag("${AudioFilesTestTags.TRANSCRIPT_BUTTON_PREFIX}${recording.id}"),
                        ) {
                            Text(text = "查看转写")
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
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
private fun StatusBadge(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String? = null,
) {
    ElevatedAssistChip(
        modifier = tag?.let { Modifier.testTag(it) } ?: Modifier,
        onClick = {},
        label = {
            Text(
                text = label,
                color = color,
                fontWeight = FontWeight.Medium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
            )
        },
    )
}

@Composable
private fun EmptyState(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "暂无录音", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "暂无录音 · 请先从设备同步。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "同步最新录音")
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        TextButton(onClick = onDismiss) {
            Text(text = "知道了")
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "正在加载录音…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TranscriptViewerSheet(
    recording: AudioRecordingUi,
    onDismiss: () -> Unit,
    onAskAiClicked: (AudioRecordingUi) -> Unit,
    onRetranscribeClicked: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val statusLabel = when (recording.transcriptionStatus) {
        TranscriptionStatus.DONE -> "转写完成"
        TranscriptionStatus.IN_PROGRESS -> "转写中…"
        TranscriptionStatus.ERROR -> "转写失败"
        TranscriptionStatus.NONE -> "等待转写"
    }
    val statusColor = when (recording.transcriptionStatus) {
        TranscriptionStatus.ERROR -> MaterialTheme.colorScheme.error
        TranscriptionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusMessage = when (recording.transcriptionStatus) {
        TranscriptionStatus.DONE -> "转写完成，可查看智能总结与全文。"
        TranscriptionStatus.IN_PROGRESS -> "转写进行中，请稍后。"
        TranscriptionStatus.ERROR -> "转写失败，可重新转写。"
        TranscriptionStatus.NONE -> "尚未转写，点击转写后查看。"
    }
    ModalBottomSheet(
        modifier = Modifier.testTag(AudioFilesTestTags.TRANSCRIPT_DIALOG),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = recording.fileName, style = MaterialTheme.typography.titleMedium)
                    val durationText = recording.durationMillis?.let { formatDuration(it) }
                    Text(
                        text = listOfNotNull(durationText, recording.createdAtText.takeIf { it.isNotBlank() })
                            .joinToString(" · ")
                            .ifBlank { "基础信息缺失" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(
                    label = statusLabel,
                    color = statusColor,
                    icon = when (recording.transcriptionStatus) {
                        TranscriptionStatus.DONE -> Icons.Default.CheckCircle
                        TranscriptionStatus.ERROR -> Icons.Default.Error
                        TranscriptionStatus.IN_PROGRESS -> Icons.Default.HourglassEmpty
                        TranscriptionStatus.NONE -> Icons.Default.HourglassEmpty
                    },
                    tag = AudioFilesTestTags.TRANSCRIPT_STATUS,
                )
            }
            Text(
                text = statusMessage,
                color = statusColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            Divider()
            val summary = recording.smartSummary?.takeIf { it.isMeaningful() }
            summary?.let {
                SummaryBlock(
                    summary = it,
                    modifier = Modifier.testTag(AudioFilesTestTags.TRANSCRIPT_SUMMARY),
                )
                Divider()
            }
            Text(
                text = "完整逐字稿",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TranscriptContent(recording = recording)
            recording.chapters?.let { chapters ->
                Divider()
                Text(
                    text = "章节",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (chapters.isEmpty()) {
                    Text(
                        text = "暂无章节信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chapters.forEach { chapter ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = formatChapterTime(chapter.startMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onAskAiClicked(recording)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "用 AI 分析本次通话")
                }
                if (recording.transcriptionStatus == TranscriptionStatus.ERROR) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onRetranscribeClicked(recording.id)
                        },
                    ) {
                        Text(text = "重新转写")
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = "关闭")
            }
        }
    }
}

@Composable
private fun TranscriptContent(recording: AudioRecordingUi) {
    val content = when (recording.transcriptionStatus) {
        TranscriptionStatus.IN_PROGRESS -> "转写进行中，请稍后再试。"
        TranscriptionStatus.ERROR -> "转写失败，可以重新创建转写任务。"
        else -> recording.fullTranscriptMarkdown ?: recording.transcriptPreview ?: "暂无内容"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .testTag(AudioFilesTestTags.TRANSCRIPT_CONTENT),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content.lines().forEach { line ->
            Text(
                text = formatMarkdownLite(line),
                style = when {
                    line.startsWith("##") -> MaterialTheme.typography.titleSmall
                    line.startsWith("#") -> MaterialTheme.typography.titleMedium
                    line.startsWith("- ") -> MaterialTheme.typography.bodyMedium
                    else -> MaterialTheme.typography.bodyMedium
                },
            )
        }
    }
}

private fun formatMarkdownLite(line: String): String =
    line.trimStart('#', ' ', '\t')

private fun formatDuration(durationMillis: Long): String {
    if (durationMillis <= 0) return ""
    val totalSeconds = durationMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatChapterTime(startMs: Long): String {
    val totalSeconds = (startMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun SummaryBlock(
    summary: TingwuSmartSummaryUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "AI 智能总结",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        summary.summary?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (summary.keyPoints.isNotEmpty()) {
            Text(text = "要点", style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summary.keyPoints.forEach { point ->
                    Text(
                        text = "• $point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (summary.actionItems.isNotEmpty()) {
            Text(text = "行动项", style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summary.actionItems.forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun TingwuSmartSummaryUi.isMeaningful(): Boolean =
    !summary.isNullOrBlank() || keyPoints.isNotEmpty() || actionItems.isNotEmpty()
