package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/AudioFileCard.kt
// 模块：:app
// 说明：录音卡片 UI，展示同步与转写状态
// 作者：创建于 2025-12-02

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.screens.audio.model.AudioFile
import com.smartsales.aitest.ui.screens.audio.model.SyncStatus
import com.smartsales.aitest.ui.screens.audio.model.TranscriptionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AudioFileCard(
    audioFile: AudioFile,
    onCardClick: () -> Unit,
    onSyncClick: () -> Unit,
    onTranscribeClick: () -> Unit,
    onViewTranscriptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = audioFile.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDuration(audioFile.duration)} · ${dateFormatter.format(Date(audioFile.recordedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SyncStatusIcon(status = audioFile.syncStatus)
            }
            TranscriptionStatusRow(
                status = audioFile.transcriptionStatus,
                onTranscribeClick = onTranscribeClick,
                onViewClick = onViewTranscriptClick
            )
            if (audioFile.syncStatus == SyncStatus.LOCAL) {
                OutlinedButton(
                    onClick = onSyncClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Text(text = "同步到云端", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun SyncStatusIcon(status: SyncStatus) {
    when (status) {
        SyncStatus.LOCAL -> Icon(
            imageVector = Icons.Filled.PhoneAndroid,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SyncStatus.SYNCING -> CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 3.dp)
        SyncStatus.SYNCED -> Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TranscriptionStatusRow(
    status: TranscriptionStatus,
    onTranscribeClick: () -> Unit,
    onViewClick: () -> Unit
) {
    when (status) {
        TranscriptionStatus.NONE -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "未转写",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onTranscribeClick) { Text(text = "开始转写") }
            }
        }
        TranscriptionStatus.PROCESSING -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 3.dp)
                Text(
                    text = "转写中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        TranscriptionStatus.COMPLETED -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "转写完成",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = onViewClick) { Text(text = "查看") }
            }
        }
        TranscriptionStatus.FAILED -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "转写失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = onTranscribeClick) { Text(text = "重试") }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
