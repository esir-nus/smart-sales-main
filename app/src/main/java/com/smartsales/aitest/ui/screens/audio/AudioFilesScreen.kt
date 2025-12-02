package com.smartsales.aitest.ui.screens.audio

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/audio/AudioFilesScreen.kt
// 模块：:app
// 说明：底部导航 Audio 页 UI，使用本地模拟数据展示同步与转写流程
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.AudioFileCard
import com.smartsales.aitest.ui.components.TranscriptBottomSheet
import com.smartsales.aitest.ui.screens.audio.model.AudioFile
import com.smartsales.aitest.ui.screens.audio.model.SyncStatus
import com.smartsales.aitest.ui.screens.audio.model.TranscriptionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFilesScreen() {
    var audioFiles by remember { mutableStateOf(getMockAudioFiles()) }
    var selectedAudio by remember { mutableStateOf<AudioFile?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "录音文件") },
                actions = {
                    IconButton(onClick = {
                        isRefreshing = true
                        scope.launch {
                            delay(1_000)
                            audioFiles = getMockAudioFiles()
                            isRefreshing = false
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("page_audio_files")
        ) {
            if (audioFiles.isEmpty()) {
                EmptyAudioState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                ) {
                    items(audioFiles, key = { it.id }) { audioFile ->
                        AudioFileCard(
                            audioFile = audioFile,
                            onCardClick = {
                                if (audioFile.transcriptionStatus == TranscriptionStatus.COMPLETED) {
                                    selectedAudio = audioFile
                                }
                            },
                            onSyncClick = {
                                audioFiles = audioFiles.map { item ->
                                    if (item.id == audioFile.id) item.copy(syncStatus = SyncStatus.SYNCING) else item
                                }
                                scope.launch {
                                    delay(2_000)
                                    audioFiles = audioFiles.map { item ->
                                        if (item.id == audioFile.id) item.copy(syncStatus = SyncStatus.SYNCED) else item
                                    }
                                }
                            },
                            onTranscribeClick = {
                                audioFiles = audioFiles.map { item ->
                                    if (item.id == audioFile.id) item.copy(transcriptionStatus = TranscriptionStatus.PROCESSING) else item
                                }
                                scope.launch {
                                    delay(3_000)
                                    audioFiles = audioFiles.map { item ->
                                        if (item.id == audioFile.id) {
                                            item.copy(
                                                transcriptionStatus = TranscriptionStatus.COMPLETED,
                                                transcript = "这是模拟的转写内容。客户询问了产品价格与功能，我们进行了详细介绍，并确认了后续跟进时间。"
                                            )
                                        } else item
                                    }
                                }
                            },
                            onViewTranscriptClick = {
                                if (audioFile.transcriptionStatus == TranscriptionStatus.COMPLETED) {
                                    selectedAudio = audioFile
                                }
                            }
                        )
                    }
                }
            }
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }

    selectedAudio?.let { audio ->
        TranscriptBottomSheet(
            audioFile = audio,
            onDismiss = { selectedAudio = null },
            onAnalyzeClick = {
                // TODO: 后续接入聊天上下文分析
                selectedAudio = null
            }
        )
    }
}

@Composable
private fun EmptyAudioState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "暂无录音文件",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "录音文件将在这里显示",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getMockAudioFiles(): List<AudioFile> {
    val now = System.currentTimeMillis()
    return listOf(
        AudioFile(
            id = "1",
            fileName = "客户通话_2024_001.m4a",
            duration = 245,
            recordedAt = now - 3_600_000L,
            syncStatus = SyncStatus.SYNCED,
            transcriptionStatus = TranscriptionStatus.COMPLETED,
            transcript = "这是一段完整的转写内容，包含客户询问、价格讨论与后续安排。",
            fileSize = 2_048_000L
        ),
        AudioFile(
            id = "2",
            fileName = "销售电话_2024_002.m4a",
            duration = 180,
            recordedAt = now - 7_200_000L,
            syncStatus = SyncStatus.SYNCED,
            transcriptionStatus = TranscriptionStatus.NONE,
            fileSize = 1_536_000L
        ),
        AudioFile(
            id = "3",
            fileName = "会议录音_2024_003.m4a",
            duration = 420,
            recordedAt = now - 86_400_000L,
            syncStatus = SyncStatus.LOCAL,
            transcriptionStatus = TranscriptionStatus.NONE,
            fileSize = 3_584_000L
        )
    )
}
