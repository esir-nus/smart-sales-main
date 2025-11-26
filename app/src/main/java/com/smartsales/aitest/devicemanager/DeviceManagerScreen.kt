package com.smartsales.aitest.devicemanager

// 文件：app/src/main/java/com/smartsales/aitest/devicemanager/DeviceManagerScreen.kt
// 模块：:app
// 说明：设备文件管理页面的 Route 与 Compose UI
// 作者：创建于 2025-11-20

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeviceConnectionHeader(
                status = state.connectionStatus,
                isLoading = state.isLoading,
                isUploading = state.isUploading,
                isConnected = isConnected,
                onRefresh = onRefresh,
                onUpload = onRequestUpload,
                onConnect = onRefresh
            )
            ConnectionSettingsRow(
                baseUrl = state.baseUrl,
                hint = state.autoDetectStatus,
                enabled = !state.isLoading,
                onBaseUrlChange = onBaseUrlChange
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
                DisconnectedHint(onConnect = onRefresh)
            }
            Box(modifier = Modifier.weight(1f, fill = true)) {
                when {
                    state.isLoading -> LoadingBanner()
                    state.loadErrorMessage != null -> Unit
                    state.visibleFiles.isEmpty() -> DeviceManagerEmptyState()
                    else -> DeviceFileList(
                        files = state.visibleFiles,
                        selectedId = state.selectedFile?.id,
                        onSelect = onSelectFile
                    )
                }
            }
            DeviceSimulatorPanel(
                selected = state.selectedFile,
                isConnected = isConnected,
                onApply = { state.selectedFile?.let { onApplyFile(it.id) } },
                onDelete = { state.selectedFile?.let { onDeleteFile(it.id) } }
            )
        }
    }
}

@Composable
private fun DeviceConnectionHeader(
    status: DeviceConnectionUiState,
    isLoading: Boolean,
    isUploading: Boolean,
    isConnected: Boolean,
    onRefresh: () -> Unit,
    onUpload: () -> Unit,
    onConnect: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onConnect,
                        enabled = !isLoading,
                        modifier = Modifier.testTag(DeviceManagerTestTags.CONNECT_BUTTON)
                    ) {
                        Text(if (isConnected) "刷新" else "重试连接")
                    }
                    Button(
                        onClick = onRefresh,
                        enabled = isConnected && !isLoading,
                        modifier = Modifier.testTag(DeviceManagerTestTags.REFRESH_BUTTON)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isLoading) "刷新中..." else "刷新文件")
                    }
                    OutlinedButton(
                        onClick = onUpload,
                        enabled = isConnected && !isUploading,
                        modifier = Modifier.testTag(DeviceManagerTestTags.UPLOAD_BUTTON)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isUploading) "上传中..." else "上传")
                    }
                }
            }
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
        Text(text = "正在加载设备文件...")
    }
}

@Composable
private fun ConnectionSettingsRow(
    baseUrl: String,
    hint: String,
    enabled: Boolean,
    onBaseUrlChange: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "连接设置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("服务地址") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceFileList(
    files: List<DeviceFileUi>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files, key = { it.id }) { file ->
            DeviceFileCard(
                file = file,
                isSelected = selectedId == file.id,
                onSelect = { onSelect(file.id) }
            )
        }
    }
}

@Composable
private fun DeviceFileCard(
    file: DeviceFileUi,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Thumbnail(
                url = file.thumbnailUrl ?: file.mediaUrl,
                mimeType = file.mimeType,
                size = 72.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = file.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (file.isApplied) {
                        Text(
                            text = "已应用",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "${file.mimeType} · ${file.sizeText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DurationPill(duration = file.durationText)
                    if (file.isApplied) {
                        MetaBadge(text = "当前展示")
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationPill(duration: String?) {
    if (duration.isNullOrBlank()) return
    Text(
        text = duration,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun MetaBadge(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun Thumbnail(url: String?, mimeType: String, size: Dp) {
    val isVideo = mimeType.startsWith("video", ignoreCase = true)
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url != null && !isVideo) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isVideo) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            Text(text = "设备上还没有文件", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "录制或拍摄内容后，再次刷新列表。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DisconnectedHint(onConnect: () -> Unit) {
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
                text = "请先完成设备配网，然后重试连接并刷新设备文件。",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onConnect) {
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

@Composable
private fun DeviceSimulatorPanel(
    selected: DeviceFileUi?,
    isConnected: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DeviceManagerTestTags.SIMULATOR),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "设备预览", style = MaterialTheme.typography.titleMedium)
            if (selected == null) {
                Text(
                    text = "请选择文件预览",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(DeviceManagerTestTags.SIMULATOR_PLACEHOLDER)
                )
                return@Column
            }
            Text(
                text = selected.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(DeviceManagerTestTags.SIMULATOR_TITLE)
            )
            PreviewFrame(selected)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DurationPill(duration = selected.durationText)
                MetaBadge(text = selected.mimeType)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApply,
                    enabled = isConnected,
                    modifier = Modifier.testTag(DeviceManagerTestTags.APPLY_BUTTON)
                ) {
                    Text("应用到设备")
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = isConnected,
                    modifier = Modifier.testTag(DeviceManagerTestTags.DELETE_BUTTON)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun PreviewFrame(file: DeviceFileUi) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            val isVideo = file.mimeType.startsWith("video", ignoreCase = true)
            if (isVideo) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val previewUrl = file.thumbnailUrl ?: file.mediaUrl
                if (previewUrl != null) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private object DeviceManagerTestTags {
    const val CONNECT_BUTTON = "device_manager_connect_button"
    const val REFRESH_BUTTON = "device_manager_refresh_button"
    const val UPLOAD_BUTTON = "device_manager_upload_button"
    const val EMPTY_STATE = "device_manager_empty_state"
    const val ERROR_BANNER = "device_manager_error_banner"
    const val SIMULATOR = "device_manager_simulator"
    const val SIMULATOR_PLACEHOLDER = "device_manager_simulator_placeholder"
    const val SIMULATOR_TITLE = "device_manager_simulator_title"
    const val APPLY_BUTTON = "device_manager_apply_button"
    const val DELETE_BUTTON = "device_manager_delete_button"
}

object DeviceManagerRouteTestTags {
    const val ROOT = "device_manager_screen_root"
}
