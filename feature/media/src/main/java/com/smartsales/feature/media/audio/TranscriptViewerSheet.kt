package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/TranscriptViewerSheet.kt
// 模块：:feature:media
// 说明：转写详情 bottom sheet，展示转写内容、章节、智能总结与 AI 分析入口
// 作者：创建于 2025-11-21，重构于 2026-01-14

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.core.util.AppDesignTokens
import kotlinx.coroutines.launch

/**
 * Bottom sheet displaying full transcript, summary, chapters, and AI analysis CTA.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TranscriptViewerSheet(
    recording: AudioRecordingUi,
    onDismiss: () -> Unit,
    onPlayClicked: (String) -> Unit,
    onAskAiClicked: (AudioRecordingUi) -> Unit
) {
    val scrollState = rememberScrollState()
    ModalBottomSheet(
        modifier = Modifier.testTag(AudioFilesTestTags.TRANSCRIPT_DIALOG),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "通话转写",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlayerStub(
                title = recording.fileName,
                duration = formatDuration(recording.durationMillis),
                onPlayPause = { onPlayClicked(recording.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            val summary = recording.smartSummary?.takeIf { it.isMeaningful() }
            summary?.let {
                SummaryBlock(summary = it, modifier = Modifier.testTag(AudioFilesTestTags.TRANSCRIPT_SUMMARY))
                Spacer(modifier = Modifier.height(12.dp))
            }
            val content = when (recording.transcriptionStatus) {
                TranscriptionStatus.IN_PROGRESS -> "转写进行中，请稍后查看。"
                TranscriptionStatus.ERROR -> "转写失败，请重试创建转写任务或检查网络。"
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
            if (recording.transcriptionStatus == TranscriptionStatus.DONE) {
                GradientCtaButton(
                    label = "用 AI 分析本次通话",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AudioFilesTestTags.ASK_AI_BUTTON),
                    onClick = {
                        onDismiss()
                        onAskAiClicked(recording)
                    }
                )
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

// ─────────────────────────────────────────────────────────────────────────────
// Supporting Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryBlock(
    summary: TingwuSmartSummaryUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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

@Composable
private fun PlayerStub(
    title: String,
    duration: String,
    onPlayPause: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "播放"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "转写摘要",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun GradientCtaButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val designTokens = AppDesignTokens.current()
    Box(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = designTokens.ctaGradient
                ),
                shape = MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilities
// ─────────────────────────────────────────────────────────────────────────────

private fun TingwuSmartSummaryUi.isMeaningful(): Boolean =
    !summary.isNullOrBlank() || keyPoints.isNotEmpty() || actionItems.isNotEmpty()

private fun formatMarkdownLite(line: String): String =
    line.trimStart('#', ' ', '\t')

private fun formatChapterTime(startMs: Long): String {
    val totalSeconds = (startMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

internal fun formatDuration(durationMillis: Long?): String {
    if (durationMillis == null || durationMillis <= 0) return "--:--"
    val totalSeconds = (durationMillis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
