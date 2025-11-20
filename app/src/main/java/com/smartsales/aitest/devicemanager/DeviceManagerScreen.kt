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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.media.devicemanager.DeviceFileUi
import com.smartsales.feature.media.devicemanager.DeviceManagerUiState
import com.smartsales.feature.media.devicemanager.DeviceManagerViewModel
import com.smartsales.feature.media.devicemanager.DeviceMediaTab
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
        onSelectTab = viewModel::onSelectTab,
        onSelectFile = viewModel::onSelectFile,
        onApplyFile = viewModel::onApplyFile,
        onDeleteFile = viewModel::onDeleteFile,
        onRequestUpload = { uploadLauncher.launch(arrayOf("image/*", "video/*")) },
        onBaseUrlChange = viewModel::onBaseUrlChanged,
        onClearError = viewModel::onClearError,
        modifier = modifier
    )
}

@Composable
fun DeviceManagerScreen(
    state: DeviceManagerUiState,
    onRefresh: () -> Unit,
    onSelectTab: (DeviceMediaTab) -> Unit,
    onSelectFile: (String) -> Unit,
    onApplyFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRequestUpload: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "连接状态：${describeConnection(state.connectionState)}",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("服务地址") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                Button(onClick = onRequestUpload, enabled = !state.isUploading) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (state.isUploading) "上传中..." else "上传")
                }
            }
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.errorMessage?.let {
                ErrorBanner(message = it, onDismiss = onClearError)
            }
            TabRow(state.activeTab, onSelectTab)
            if (state.visibleFiles.isEmpty()) {
                EmptyState()
            } else {
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

@Composable
private fun TabRow(activeTab: DeviceMediaTab, onSelectTab: (DeviceMediaTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DeviceMediaTab.entries.forEach { tab ->
            FilterChip(
                selected = tab == activeTab,
                onClick = { onSelectTab(tab) },
                label = {
                    Text(
                        text = when (tab) {
                            DeviceMediaTab.Images -> "图片"
                            DeviceMediaTab.Videos -> "视频"
                            DeviceMediaTab.Other -> "其它"
                        }
                    )
                }
            )
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
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        delay(2800)
        onDismiss()
    }
    Row(
        modifier = Modifier
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

private fun describeConnection(state: ConnectionState): String = when (state) {
    ConnectionState.Disconnected -> "未连接"
    is ConnectionState.Pairing -> "配对中 ${state.deviceName}"
    is ConnectionState.WifiProvisioned -> "Wi-Fi 已同步"
    is ConnectionState.Syncing -> "同步中"
    is ConnectionState.Error -> "错误：${state.error.javaClass.simpleName}"
}
