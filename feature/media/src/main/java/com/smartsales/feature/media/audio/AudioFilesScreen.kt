package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesScreen.kt
// 模块：:feature:media
// 说明：音频库 Compose 界面，展示列表并触发同步、播放与删除（预留转写占位）
// 作者：创建于 2025-11-21

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.feature.media.audio.TingwuChapterUi
import com.smartsales.feature.media.audio.TingwuSmartSummaryUi
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(AudioFilesTestTags.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            uiState.loadErrorMessage?.let { message ->
                Text(
                    text = "加载录音失败，请稍后重试。",
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
                            onDeleteClicked = onDeleteClicked,
                            onTranscribeClicked = onTranscribeClicked,
                            onTranscriptClicked = onTranscriptClicked
                        )
                    }
                }
            }
        }
    }
    uiState.transcriptPreviewRecording?.let { recording ->
        TranscriptViewerSheet(
            recording = recording,
            onDismiss = onTranscriptDismissed,
            onAskAiClicked = onAskAiClicked
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
    const val EMPTY_STATE = "audio_files_empty_state"
    const val ERROR_BANNER = "audio_files_error_banner"
    const val TRANSCRIBE_BUTTON_PREFIX = "audio_files_transcribe_"
    const val TRANSCRIPT_BUTTON_PREFIX = "audio_files_transcript_"
    const val STATUS_CHIP_PREFIX = "audio_files_status_chip_"
    const val TRANSCRIPT_DIALOG = "audio_files_transcript_dialog"
    const val TRANSCRIPT_CONTENT = "audio_files_transcript_content"
    const val TRANSCRIPT_SUMMARY = "audio_files_transcript_summary"
}

@Composable
private fun AudioRecordingItem(
    recording: AudioRecordingUi,
    onClick: (String) -> Unit,
    onPlayPauseClicked: (String) -> Unit,
    onDeleteClicked: (String) -> Unit,
    onTranscribeClicked: (String) -> Unit,
    onTranscriptClicked: (String) -> Unit,
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
                if (recording.transcriptionStatus != TranscriptionStatus.NONE) {
                    ElevatedAssistChip(
                        modifier = Modifier.testTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}${recording.id}"),
                        onClick = { onClick(recording.id) },
                        label = {
                            Text(
                                text = when (recording.transcriptionStatus) {
                                    TranscriptionStatus.NONE -> "未转写"
                                    TranscriptionStatus.IN_PROGRESS -> "转写中…"
                                    TranscriptionStatus.DONE -> "已完成"
                                    TranscriptionStatus.ERROR -> "失败"
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
                if (recording.transcriptionStatus == TranscriptionStatus.NONE) {
                    TextButton(
                        onClick = { onTranscribeClicked(recording.id) },
                        modifier = Modifier.testTag("${AudioFilesTestTags.TRANSCRIBE_BUTTON_PREFIX}${recording.id}")
                    ) {
                        Text(text = "转写")
                    }
                }
                if (recording.transcriptionStatus == TranscriptionStatus.DONE) {
                    TextButton(
                        onClick = { onTranscriptClicked(recording.id) },
                        modifier = Modifier.testTag("${AudioFilesTestTags.TRANSCRIPT_BUTTON_PREFIX}${recording.id}")
                    ) {
                        Text(text = "查看转写")
                    }
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TranscriptViewerSheet(
    recording: AudioRecordingUi,
    onDismiss: () -> Unit,
    onAskAiClicked: (AudioRecordingUi) -> Unit
) {
    val scrollState = rememberScrollState()
    androidx.compose.material3.ModalBottomSheet(
        modifier = Modifier.testTag(AudioFilesTestTags.TRANSCRIPT_DIALOG),
        onDismissRequest = onDismiss
    ) {
        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = recording.fileName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            val summary = recording.smartSummary?.takeIf { it.isMeaningful() }
            summary?.let {
                SummaryBlock(summary = it, modifier = Modifier.testTag(AudioFilesTestTags.TRANSCRIPT_SUMMARY))
                Spacer(modifier = Modifier.height(12.dp))
            }
            val content = when (recording.transcriptionStatus) {
                TranscriptionStatus.IN_PROGRESS -> "转写进行中，请稍后再试。"
                TranscriptionStatus.ERROR -> "转写失败，可以重新创建转写任务。"
                else -> recording.fullTranscriptMarkdown ?: recording.transcriptPreview ?: "暂无内容"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AudioFilesTestTags.TRANSCRIPT_CONTENT),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content.lines().forEach { line ->
                    Text(
                        text = formatMarkdownLite(line),
                        style = when {
                            line.startsWith("##") -> MaterialTheme.typography.titleSmall
                            line.startsWith("#") -> MaterialTheme.typography.titleMedium
                            line.startsWith("- ") -> MaterialTheme.typography.bodyMedium
                            else -> MaterialTheme.typography.bodyMedium
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    onDismiss()
                    onAskAiClicked(recording)
                },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(text = "用 AI 分析本次通话")
            }
            Spacer(modifier = Modifier.height(8.dp))
            recording.chapters?.let { chapters ->
                Text(
                    text = "章节",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (chapters.isEmpty()) {
                    Text(
                        text = "暂无章节信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chapters.forEachIndexed { index: Int, chapter: TingwuChapterUi ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            val target = (index * 200).coerceAtLeast(0)
                                            scrollState.animateScrollTo(target)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatChapterTime(chapter.startMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(text = "关闭")
            }
        }
    }
}

@Composable
private fun formatMarkdownLite(line: String): String =
    line.trimStart('#', ' ', '\t')

private fun formatChapterTime(startMs: Long): String {
    val totalSeconds = (startMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun SummaryBlock(
    summary: TingwuSmartSummaryUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "智能总结",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        summary.summary?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (summary.keyPoints.isNotEmpty()) {
            Text(text = "要点", style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summary.keyPoints.forEach { point ->
                    Text(
                        text = "• $point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun TingwuSmartSummaryUi.isMeaningful(): Boolean =
    !summary.isNullOrBlank() || keyPoints.isNotEmpty() || actionItems.isNotEmpty()
