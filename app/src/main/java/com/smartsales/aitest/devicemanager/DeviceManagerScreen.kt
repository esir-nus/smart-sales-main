package com.smartsales.aitest.devicemanager

// 文件：app/src/main/java/com/smartsales/aitest/devicemanager/DeviceManagerScreen.kt
// 模块：:app
// 说明：设备管理页面的 Route 与 Compose UI
// 作者：创建于 2025-11-20

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.core.util.AppDesignTokens
import com.smartsales.feature.media.devicemanager.DeviceConnectionUiState
import com.smartsales.feature.media.devicemanager.DeviceFileUi
import com.smartsales.feature.media.devicemanager.DeviceManagerUiState
import com.smartsales.feature.media.devicemanager.DeviceManagerViewModel
import com.smartsales.feature.media.devicemanager.DeviceUploadSource

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
        onRetryLoad = viewModel::onRetryLoad,
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
    onRetryLoad: () -> Unit,
    onSelectFile: (String) -> Unit,
    onApplyFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRequestUpload: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        val isConnected = state.isConnected
        val designTokens = AppDesignTokens.current()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(designTokens.mutedSurface)
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeviceManagerHero(
                state = state,
                onBaseUrlChange = onBaseUrlChange
            )
            if (isConnected) {
                DevicePreviewRow(
                    onUpload = onRequestUpload,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ActionButtons(
                isConnected = isConnected,
                isLoading = state.isLoading,
                isUploading = state.isUploading,
                onRefresh = onRefresh,
                onUpload = onRequestUpload
            )
            state.loadErrorMessage?.let {
                ErrorBanner(
                    message = it,
                    primaryActionLabel = "重试",
                    onPrimaryAction = onRetryLoad,
                    onDismiss = onClearError,
                    modifier = Modifier.testTag(DeviceManagerTestTags.ERROR_BANNER)
                )
            } ?: state.errorMessage?.let {
                ErrorBanner(
                    message = it,
                    primaryActionLabel = null,
                    onPrimaryAction = null,
                    onDismiss = onClearError,
                    modifier = Modifier.testTag(DeviceManagerTestTags.ERROR_BANNER)
                )
            }
            if (!isConnected) {
                DisconnectedHint(onRefresh)
                return@Column
            }
            when {
                state.isLoading -> {
                    LoadingBanner()
                }
                state.loadErrorMessage != null -> Unit
                state.visibleFiles.isEmpty() -> {
                    FileListHeader(total = state.files.size, modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UploadTile(onUpload = onRequestUpload, modifier = Modifier.weight(1f))
                        EmptyFilesCard(modifier = Modifier.weight(1f))
                    }
                }

                else -> {
                    FileListHeader(total = state.files.size, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "当前展示 ${state.visibleFiles.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(DeviceManagerTestTags.FILE_LIST),
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
            }
            state.selectedFile?.let { selected ->
                SelectedFileCard(file = selected)
            }
        }
    }
}

@Composable
private fun DeviceManagerHero(
    state: DeviceManagerUiState,
    onBaseUrlChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "设备管理", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "管理您的销售助手设备",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DeviceConnectionBanner(
                status = state.connectionStatus,
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("媒体服务地址") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )
                Text(
                    text = state.autoDetectStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun DeviceConnectionBanner(
    status: DeviceConnectionUiState,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = when (status) {
        is DeviceConnectionUiState.Disconnected -> "设备未连接" to (status.reason ?: "请连接设备以管理文件和查看预览。")
        is DeviceConnectionUiState.Connecting -> "正在连接设备..." to (status.detail ?: "请确保设备在附近")
        is DeviceConnectionUiState.Connected -> ("已连接 ${status.deviceName ?: ""}").trim() to "可预览、刷新和上传文件。"
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
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
            Text(text = if (isUploading) "上传中..." else "上传新文件")
        }
    }
}

@Composable
private fun LoadingBanner() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(modifier = Modifier.weight(1f))
        Text(text = "正在同步设备文件列表...")
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
            .fillMaxWidth()
            .testTag("${DeviceManagerTestTags.FILE_CARD_PREFIX}${file.id}"),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        onClick = onSelect
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.isApplied) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(text = "当前展示") },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
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
            if (file.isApplied) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "设备当前展示此文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "时长：${file.durationText ?: "待获取"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "预览链接",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                Spacer(modifier = Modifier.weight(1f))
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
    Card(modifier = Modifier.testTag(DeviceManagerTestTags.SELECTED_FILE_CARD)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "选中项", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            if (file.isApplied) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = "当前展示") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(text = file.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(text = "类型：${file.mimeType}")
            Text(text = "大小：${file.sizeText}")
            Text(text = "更新时间：${file.modifiedAtText}")
            Text(text = "下载链接：${file.downloadUrl}", maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyFilesCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(140.dp)
            .testTag(DeviceManagerTestTags.EMPTY_STATE),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "暂无文件，请上传。", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DisconnectedHint(onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "设备未连接", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "请连接设备以管理文件和查看预览。",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onRefresh) {
                Text("重试连接")
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    primaryActionLabel: String?,
    onPrimaryAction: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (primaryActionLabel != null && onPrimaryAction != null) {
                TextButton(onClick = onPrimaryAction) {
                    Text(text = primaryActionLabel, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text(text = "知道了", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

object DeviceManagerTestTags {
    const val REFRESH_BUTTON = "device_manager_refresh_button"
    const val UPLOAD_BUTTON = "device_manager_upload_button"
    const val EMPTY_STATE = "device_manager_empty_state"
    const val ERROR_BANNER = "device_manager_error_banner"
    const val FILE_LIST = "device_manager_file_list"
    const val FILE_CARD_PREFIX = "device_manager_file_card_"
    const val SELECTED_FILE_CARD = "device_manager_selected_file"
}

object DeviceManagerRouteTestTags {
    const val ROOT = "device_manager_screen_root"
}

@Composable
private fun DevicePreviewRow(
    onUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            ),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "选择文件预览",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "DEVICE SIMULATOR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun UploadTile(
    onUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onUpload),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "上传文件",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "上传新文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FileListHeader(total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "文件列表 ($total)",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "支持刷新、上传与应用展示文件。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
