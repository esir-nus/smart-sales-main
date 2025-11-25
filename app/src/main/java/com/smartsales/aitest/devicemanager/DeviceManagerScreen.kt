package com.smartsales.aitest.devicemanager

// 文件：app/src/main/java/com/smartsales/aitest/devicemanager/DeviceManagerScreen.kt
// 模块：:app
// 说明：设备文件管理页面的 Route 与 Compose UI
// 作者：创建于 2025-11-20

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.media.devicemanager.DeviceConnectionUiState
import com.smartsales.feature.media.devicemanager.DeviceFileUi
import com.smartsales.feature.media.devicemanager.DeviceManagerUiState
import com.smartsales.feature.media.devicemanager.DeviceManagerViewModel
import com.smartsales.feature.media.devicemanager.DeviceUploadSource
import kotlinx.coroutines.delay

@Composable
fun DeviceManagerRoute(modifier: Modifier = Modifier) {
    val viewModel: DeviceManagerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onUploadFile(DeviceUploadSource.AndroidUri(uri))
        }
    }
    DeviceManagerScreen(
        state = state,
        onRefresh = viewModel::onRefreshFiles,
        onSelectFile = viewModel::onSelectFile,
        onApplyFile = viewModel::onApplyFile,
        onDeleteFile = viewModel::onDeleteFile,
        onRequestUpload = { uploadLauncher.launch(arrayOf("video/*", "image/gif")) },
        onBaseUrlChange = viewModel::onBaseUrlChanged,
        onClearError = viewModel::onClearError,
        modifier = modifier.testTag(DeviceManagerRouteTestTags.ROOT)
    )
}

@Composable
fun DeviceManagerScreen(
    state: DeviceManagerUiState,
    onRefresh: () -> Unit,
    onSelectFile: (String) -> Unit,
    onApplyFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRequestUpload: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        val isConnected = state.connectionStatus.isReadyForFiles()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeviceConnectionBanner(status = state.connectionStatus)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("服务地址") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )
                Text(
                    text = state.autoDetectStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ActionButtons(
                isConnected = isConnected,
                isLoading = state.isLoading,
                isUploading = state.isUploading,
                onRefresh = onRefresh,
                onUpload = onRequestUpload
            )
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.errorMessage?.let {
                ErrorBanner(
                    message = it,
                    onDismiss = onClearError,
                    modifier = Modifier.testTag(DeviceManagerTestTags.ERROR_BANNER)
                )
            }
            if (!isConnected) {
                DisconnectedHint()
            } else {
                val hasFiles = state.visibleFiles.isNotEmpty()
                if (!hasFiles && !state.isLoading) {
                    DeviceManagerEmptyState()
                } else if (hasFiles) {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.visibleFiles, key = { it.id }) { file ->
                            DeviceFileCard(
                                file = file,
                                isSelected = state.selectedFile?.id == file.id,
                                onSelect = { onSelectFile(file.id) },
                                onApply = { onApplyFile(file.id) },
                                onDelete = { onDeleteFile(file.id) }
                            )
                        }
                    }
                }
                state.selectedFile?.let {
                    SelectedFileCard(file = it)
                }
            }
        }
    }
}

@Composable
private fun DeviceConnectionBanner(status: DeviceConnectionUiState) {
    val (title, subtitle) = when (status) {
        is DeviceConnectionUiState.Disconnected -> "未连接设备" to (status.reason ?: "请开启设备并保持 Wi-Fi/BLE 可用。")
        is DeviceConnectionUiState.Connecting -> "正在连接设备" to (status.detail ?: "请稍候...")
        is DeviceConnectionUiState.Connected -> ("已连接 ${status.deviceName ?: ""}").trim() to "可以浏览、刷新与上传文件。"
    }
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isConnected: Boolean,
    isLoading: Boolean,
    isUploading: Boolean,
    onRefresh: () -> Unit,
    onUpload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onRefresh,
            enabled = isConnected && !isLoading,
            modifier = Modifier.testTag(DeviceManagerTestTags.REFRESH_BUTTON)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = if (isLoading) "刷新中..." else "刷新")
        }
        OutlinedButton(
            onClick = onUpload,
            enabled = isConnected && !isUploading,
            modifier = Modifier.testTag(DeviceManagerTestTags.UPLOAD_BUTTON)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = if (isUploading) "上传中..." else "上传文件")
        }
    }
}

@Composable
private fun DeviceFileCard(
    file: DeviceFileUi,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        onClick = onSelect
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = file.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${file.mimeType} · ${file.sizeText}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "修改时间：${file.modifiedAtText}",
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "时长：${file.durationText ?: "待获取"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "静态预览",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = file.thumbnailUrl ?: "无缩略图",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (file.isApplied) {
                    Text(text = "已应用", color = MaterialTheme.colorScheme.primary)
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApply) {
                        Text("应用")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedFileCard(file: DeviceFileUi) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "选中项", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = file.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(text = "类型：${file.mimeType}")
            Text(text = "大小：${file.sizeText}")
            Text(text = "更新时间：${file.modifiedAtText}")
            Text(text = "下载链接：${file.downloadUrl}", maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DeviceManagerEmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DeviceManagerTestTags.EMPTY_STATE),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "暂无文件", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "点击上传按钮或触发设备同步后即可在此看到文件列表。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DisconnectedHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "设备未连接", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "连接 SmartSales 录音设备后即可浏览和刷新文件。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(message) {
        delay(2800)
        onDismiss()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
        TextButton(onClick = onDismiss) {
            Text(text = "知道了", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

private object DeviceManagerTestTags {
    const val REFRESH_BUTTON = "device_manager_refresh_button"
    const val UPLOAD_BUTTON = "device_manager_upload_button"
    const val EMPTY_STATE = "device_manager_empty_state"
    const val ERROR_BANNER = "device_manager_error_banner"
}

object DeviceManagerRouteTestTags {
    const val ROOT = "device_manager_screen_root"
}
