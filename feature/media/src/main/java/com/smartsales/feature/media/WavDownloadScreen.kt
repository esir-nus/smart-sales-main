package com.smartsales.feature.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

// 文件：feature/media/src/main/java/com/smartsales/feature/media/WavDownloadScreen.kt
// 模块：:feature:media
// 说明：WAV录音下载的Compose UI
// 作者：创建于 2026-01-10

/**
 * Screen for downloading WAV recordings from ESP32 badge.
 * 
 * States (per ux-experience.md):
 * - idle: "获取录音" button
 * - scanning: Spinner + "正在扫描录音文件..."
 * - files_found: File list with checkboxes
 * - downloading: Progress bar + "正在下载 {filename}..."
 * - complete: Success toast
 * - empty: "暂无录音"
 * - error: Error card with retry
 */
@Composable
fun WavDownloadScreen(
    viewModel: WavDownloadViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "录音文件",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Main content based on step
        when (uiState.step) {
            WavDownloadStep.Idle -> {
                IdleContent(onScan = { viewModel.startScan() })
            }
            WavDownloadStep.Scanning -> {
                ScanningContent(message = uiState.statusMessage ?: "扫描中...")
            }
            WavDownloadStep.FilesFound -> {
                FilesFoundContent(
                    files = uiState.availableFiles,
                    selectedFiles = uiState.selectedFiles,
                    onToggle = { viewModel.toggleFileSelection(it) },
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.deselectAll() },
                    onDownload = { viewModel.startDownload() },
                    isDownloadEnabled = uiState.isDownloadEnabled,
                    statusMessage = uiState.statusMessage
                )
            }
            WavDownloadStep.Empty -> {
                EmptyContent(
                    message = uiState.statusMessage ?: "暂无录音",
                    onRescan = { viewModel.retry() }
                )
            }
            WavDownloadStep.Downloading -> {
                DownloadingContent(
                    filename = uiState.currentFileName ?: "",
                    current = uiState.progress,
                    total = uiState.total,
                    statusMessage = uiState.statusMessage
                )
            }
            WavDownloadStep.Complete -> {
                CompleteContent(
                    message = uiState.statusMessage ?: "下载完成",
                    onDone = { viewModel.reset() }
                )
            }
            WavDownloadStep.Error -> {
                ErrorContent(
                    message = uiState.errorMessage ?: "发生错误",
                    onRetry = { viewModel.retry() },
                    onDismiss = { viewModel.dismissError() }
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("获取录音")
        }
    }
}

@Composable
private fun ScanningContent(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FilesFoundContent(
    files: List<String>,
    selectedFiles: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDownload: () -> Unit,
    isDownloadEnabled: Boolean,
    statusMessage: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Status message
        statusMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Select all / Deselect all
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onSelectAll) {
                Text("全选")
            }
            TextButton(onClick = onDeselectAll) {
                Text("取消全选")
            }
        }

        // File list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 300.dp)
        ) {
            items(files) { filename ->
                FileListItem(
                    filename = filename,
                    isSelected = filename in selectedFiles,
                    onToggle = { onToggle(filename) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Download button
        Button(
            onClick = onDownload,
            enabled = isDownloadEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("下载选中 (${selectedFiles.size})")
        }
    }
}

@Composable
private fun FileListItem(
    filename: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = filename,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyContent(message: String, onRescan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onRescan) {
            Text("重新扫描")
        }
    }
}

@Composable
private fun DownloadingContent(
    filename: String,
    current: Int,
    total: Int,
    statusMessage: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = statusMessage ?: "下载中...",
            style = MaterialTheme.typography.bodyMedium
        )
        if (total > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { current.toFloat() / total },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "$current / $total",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CompleteContent(message: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDone) {
            Text("完成")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重试",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
