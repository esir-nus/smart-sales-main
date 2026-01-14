package com.smartsales.aitest.devicemanager

// 文件：app/src/main/java/com/smartsales/aitest/devicemanager/DeviceManagerScreen.kt
// 模块：:app
// 说明：设备管理页面的 Route 与 Compose UI
// 作者：创建于 2025-12-03

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.smartsales.aitest.AiFeatureTestTags
import com.smartsales.core.util.AppDesignTokens
import com.smartsales.feature.connectivity.BleTrafficEvent
import com.smartsales.feature.connectivity.BleTrafficObserver
import com.smartsales.feature.media.devicemanager.DeviceConnectionUiState
import com.smartsales.feature.media.devicemanager.DeviceFileUi
import com.smartsales.feature.media.devicemanager.DeviceManagerEvent
import com.smartsales.feature.media.devicemanager.DeviceManagerUiState
import com.smartsales.feature.media.devicemanager.DeviceManagerViewModel
import com.smartsales.feature.media.devicemanager.DeviceMediaTab
import com.smartsales.feature.media.devicemanager.DeviceUploadSource
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceManagerRoute(
    modifier: Modifier = Modifier,
    onNavigateToDeviceSetup: () -> Unit = {},
    onNavigateToWavDownload: () -> Unit = {},
    onNavigateToGifUpload: () -> Unit = {},
    viewModelOverride: DeviceManagerViewModel? = null
) {
    val viewModel: DeviceManagerViewModel = viewModelOverride ?: hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                DeviceManagerEvent.NavigateToDeviceSetup -> onNavigateToDeviceSetup()
            }
        }
    }
    DeviceManagerScreen(
        state = state,
        onRefresh = viewModel::onRefreshFiles,
        onStartSetup = viewModel::onStartSetupClick,
        onRetryLoad = viewModel::onRetryLoad,
        onSelectFile = viewModel::onSelectFile,
        onDismissViewer = viewModel::onCloseViewer,
        onApplyFile = viewModel::onApplyFile,
        onDeleteFile = viewModel::onDeleteFile,

        onBaseUrlChange = viewModel::onBaseUrlChanged,
        onUseAutoBaseUrl = viewModel::onUseAutoBaseUrl,
        onClearError = viewModel::onClearError,
        onNavigateToWavDownload = onNavigateToWavDownload,
        onNavigateToGifUpload = onNavigateToGifUpload,
        modifier = modifier.testTag(DeviceManagerRouteTestTags.ROOT)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerScreen(
    state: DeviceManagerUiState,
    onRefresh: () -> Unit,
    onStartSetup: () -> Unit,
    onRetryLoad: () -> Unit,
    onSelectFile: (String) -> Unit,
    onDismissViewer: () -> Unit,
    onApplyFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,

    onBaseUrlChange: (String) -> Unit,
    onUseAutoBaseUrl: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToWavDownload: () -> Unit = {},
    onNavigateToGifUpload: () -> Unit = {},
    showCloseButton: Boolean = false,
    onCloseRequested: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val navigationIcon: @Composable () -> Unit = if (onBack != null) {
        {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        }
    } else {
        {}
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER),
        topBar = {
            TopAppBar(
                title = { Text(text = "设备管理") },
                navigationIcon = navigationIcon
            )
        }
    ) { innerPadding ->
        DeviceManagerContent(
            state = state,
            onRefresh = onRefresh,
            onStartSetup = onStartSetup,
            onRetryLoad = onRetryLoad,
            onSelectFile = onSelectFile,
            onDismissViewer = onDismissViewer,
            onApplyFile = onApplyFile,
            onDeleteFile = onDeleteFile,

            onBaseUrlChange = onBaseUrlChange,
            onUseAutoBaseUrl = onUseAutoBaseUrl,
            onClearError = onClearError,
            onNavigateToWavDownload = onNavigateToWavDownload,
            onNavigateToGifUpload = onNavigateToGifUpload,
            showCloseButton = showCloseButton,
            onCloseRequested = onCloseRequested,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
fun DeviceManagerContent(
    state: DeviceManagerUiState,
    onRefresh: () -> Unit,
    onStartSetup: () -> Unit,
    onRetryLoad: () -> Unit,
    onSelectFile: (String) -> Unit,
    onDismissViewer: () -> Unit,
    onApplyFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,

    onBaseUrlChange: (String) -> Unit,
    onUseAutoBaseUrl: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToWavDownload: () -> Unit = {},
    onNavigateToGifUpload: () -> Unit = {},
    showCloseButton: Boolean = false,
    onCloseRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var deleteTarget by remember { mutableStateOf<DeviceFileUi?>(null) }
    val designTokens = AppDesignTokens.current()
    val isBusy = state.isLoading || state.isUploading
    val refreshAction = if (state.loadErrorMessage != null) onRetryLoad else onRefresh

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(designTokens.mutedSurface)
            .testTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header (Full Width)
        item(span = { GridItemSpan(2) }) {
            DeviceManagerHeader(
                state = state,
                isBusy = isBusy,
                onRefresh = refreshAction,
                onStartSetup = onStartSetup,
                onBaseUrlChange = onBaseUrlChange,
                onUseAutoBaseUrl = onUseAutoBaseUrl,
                showCloseButton = showCloseButton,
                onCloseRequested = onCloseRequested
            )
        }

        // 2. BLE HUD (Full Width)
        item(span = { GridItemSpan(2) }) {
            BleTrafficHud()
        }

        // 3. Error Banners (Full Width)
        state.loadErrorMessage?.let { message ->
            item(span = { GridItemSpan(2) }) {
                ErrorBanner(
                    message = message,
                    primaryActionLabel = "重试",
                    onPrimaryAction = onRetryLoad,
                    onDismiss = onClearError,
                    modifier = Modifier.testTag(DeviceManagerTestTags.ERROR_BANNER)
                )
            }
        } ?: state.errorMessage?.let { message ->
            item(span = { GridItemSpan(2) }) {
                ErrorBanner(
                    message = message,
                    primaryActionLabel = null,
                    onPrimaryAction = null,
                    onDismiss = onClearError,
                    modifier = Modifier.testTag(DeviceManagerTestTags.ERROR_BANNER)
                )
            }
        }

        // 4. Action Buttons (Full Width)
        if (state.isConnected) {
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToWavDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Filled.AudioFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "下载录音")
                        }
                        Button(
                            onClick = onNavigateToGifUpload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "发送图片")
                        }
                    }
                }
            }
        }

        // 5. Connection Hint OR (Simulator + File List)
        if (!state.isConnected && state.canRetryConnect) {
            item(span = { GridItemSpan(2) }) {
                DisconnectedHint(onRetryLoad)
            }
        } else {
            // Simulator
            item(span = { GridItemSpan(2) }) {
                DeviceSimulatorCard(
                    file = state.selectedFile,
                    onCloseViewer = onDismissViewer
                )
            }

            // File List Header
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "文件列表 (${state.files.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // File List Content
            if (state.isLoading) {
                item(span = { GridItemSpan(2) }) { LoadingBanner() }
            } else if (state.visibleFiles.isEmpty()) {
                item(span = { GridItemSpan(2) }) { EmptyFilesCard() }
            } else {
                items(state.visibleFiles, key = { it.id }) { file ->
                    DeviceFileCard(
                        file = file,
                        isSelected = state.selectedFile?.id == file.id,
                        isApplying = state.applyInProgressId == file.id,
                        actionsEnabled = state.isConnected && !isBusy,
                        onSelect = { onSelectFile(file.id) },
                        onApply = { onApplyFile(file.id) },
                        onDelete = { deleteTarget = file }
                    )
                }
            }
        }
    }

    state.viewerFile?.let { file ->
        MediaViewerDialog(
            file = file,
            onDismiss = onDismissViewer
        )
    }
    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(text = "删除文件") },
            text = { Text(text = "确定删除 ${file.displayName} 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFile(file.id)
                        deleteTarget = null
                    }
                ) { Text(text = "删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(text = "取消") }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun DeviceSimulatorCard(
    file: DeviceFileUi?,
    onCloseViewer: () -> Unit
) {
    val targetKey = file?.id ?: "empty"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .testTag(DeviceManagerTestTags.PREVIEW_CARD),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            AnimatedContent(
                targetState = targetKey,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith
                        fadeOut(animationSpec = tween(180))
                },
                label = "device_simulator_animation"
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (file == null) 0.98f else 1f,
                    label = "device_simulator_scale"
                )
                if (file == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "选择文件预览",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val previewUrl = file.thumbnailUrl ?: file.mediaUrl
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = previewUrl,
                                contentDescription = "预览 ${file.displayName}",
                                modifier = Modifier.fillMaxSize()
                            )
                            if (file.mediaType == DeviceMediaTab.Videos) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                        .padding(6.dp)
                                )
                            }
                            file.durationText?.takeIf { it.isNotBlank() }?.let { duration ->
                                DurationBadge(
                                    duration = duration,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                )
                            }
                        }
                        Text(
                            text = file.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "修改时间：${file.modifiedAtText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (file != null) {
                IconButton(
                    onClick = onCloseViewer,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭预览"
                    )
                }
            }
            Text(
                text = "DEVICE SIMULATOR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun DeviceManagerHeader(
    state: DeviceManagerUiState,
    isBusy: Boolean,
    onRefresh: () -> Unit,
    onStartSetup: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onUseAutoBaseUrl: () -> Unit,
    showCloseButton: Boolean,
    onCloseRequested: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = when (val status = state.connectionStatus) {
                    is DeviceConnectionUiState.Disconnected -> status.reason ?: "设备未连接"
                    is DeviceConnectionUiState.Connecting -> status.detail ?: "连接中..."
                    is DeviceConnectionUiState.Connected -> status.deviceName?.let { "已连接 $it" } ?: "已连接"
                }
                val statusColor = when (state.connectionStatus) {
                    is DeviceConnectionUiState.Connected -> MaterialTheme.colorScheme.primary
                    is DeviceConnectionUiState.Connecting -> MaterialTheme.colorScheme.tertiary
                    is DeviceConnectionUiState.Disconnected -> MaterialTheme.colorScheme.error
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "设备管理", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "管理您的销售助手设备",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = onBaseUrlChange,
                        label = { Text(text = "媒体服务地址") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.autoDetectStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.autoDetectedBaseUrl?.takeIf { it.isNotBlank() }?.let {
                            TextButton(onClick = onUseAutoBaseUrl, enabled = !state.baseUrlWasManual) {
                                Text(text = "使用检测地址")
                            }
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isBusy,
                            modifier = Modifier.testTag(DeviceManagerTestTags.REFRESH_BUTTON)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "刷新"
                                )
                            }
                        }
                    }
                    Text(
                        text = if (state.isLoading) "刷新中..." else "刷新",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.canStartSetup) {
                        TextButton(
                            onClick = onStartSetup,
                            modifier = Modifier.testTag(DeviceManagerTestTags.START_SETUP_BUTTON)
                        ) {
                            Text(text = "开始配网")
                        }
                    }
                }
            }
            if (showCloseButton) {
                IconButton(
                    onClick = onCloseRequested,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭设备管理"
                    )
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
        Text(text = "正在同步设备文件列表...")
    }
}

@Composable
private fun DeviceFileCard(
    file: DeviceFileUi,
    isSelected: Boolean,
    isApplying: Boolean,
    actionsEnabled: Boolean,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val previewUrl = file.thumbnailUrl ?: file.mediaUrl
                if (previewUrl.isNotBlank()) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = "预览 ${file.displayName}",
                        modifier = Modifier.fillMaxSize()
                    )
                    TypeBadge(label = file.mediaLabel, modifier = Modifier.align(Alignment.TopStart))
                    if (file.mediaType == DeviceMediaTab.Videos) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                                .padding(6.dp)
                        )
                    }
                    file.durationText?.takeIf { it.isNotBlank() }?.let { duration ->
                        DurationBadge(
                            duration = duration,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        )
                    }
                } else {
                    Text(
                        text = "无预览",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${file.mimeType} · ${file.sizeText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "修改时间：${file.modifiedAtText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Only show "当前展示" for non-audio files (images/videos)
            if (file.isApplied && file.mediaType != com.smartsales.feature.media.devicemanager.DeviceMediaTab.Audio) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = "当前展示") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAudio = file.mediaType == com.smartsales.feature.media.devicemanager.DeviceMediaTab.Audio
                // Only show Apply button for images (not audio - use "下载录音" header button for WAV download)
                if (!isAudio) {
                    FilledIconButton(
                        onClick = onApply,
                        enabled = actionsEnabled && !isApplying,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(text = if (isApplying) "应用中..." else "应用")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(
                    onClick = onDelete,
                    enabled = actionsEnabled
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
private fun EmptyFilesCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .testTag(DeviceManagerTestTags.EMPTY_STATE),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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
            TextButton(onClick = onRefresh) {
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
    const val START_SETUP_BUTTON = "device_manager_start_setup_button"
    const val EMPTY_STATE = "device_manager_empty_state"
    const val ERROR_BANNER = "device_manager_error_banner"
    const val FILE_LIST = "device_manager_file_list"
    const val FILE_CARD_PREFIX = "device_manager_file_card_"
    const val SELECTED_FILE_CARD = "device_manager_selected_file"
    const val PREVIEW_CARD = "device_manager_preview_card"
    const val MEDIA_VIEWER = "device_manager_media_viewer"
}

object DeviceManagerRouteTestTags {
    const val ROOT = "device_manager_screen_root"
}

@Composable
private fun MediaViewerDialog(
    file: DeviceFileUi,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp)
                .testTag(DeviceManagerTestTags.MEDIA_VIEWER),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "媒体预览", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = file.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = "关闭")
                    }
                }
                when (file.mediaType) {
                    DeviceMediaTab.Videos -> VideoViewerContent(url = file.mediaUrl)
                    else -> ImageViewerContent(url = file.mediaUrl)
                }
            }
        }
    }
}

@Composable
private fun ImageViewerContent(url: String) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
    ) {
        if (url.isBlank()) {
            error = "无法加载媒体，请稍后重试"
            isLoading = false
        }
        AsyncImage(
            model = url,
            contentDescription = "媒体预览",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
            onLoading = {
                isLoading = true
                error = null
            },
            onSuccess = {
                isLoading = false
                error = null
            },
            onError = {
                isLoading = false
                error = "无法加载媒体，请稍后重试"
            }
        )
        if (isLoading && error == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        error?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun VideoViewerContent(url: String) {
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        if (url.isBlank()) {
            error = "无法加载媒体，请稍后重试"
            isPrepared = true
        }
        AndroidView(
            factory = { viewContext ->
                android.widget.VideoView(viewContext).apply {
                    setVideoURI(Uri.parse(url))
                    setOnPreparedListener { player ->
                        isPrepared = true
                        player.isLooping = true
                        if (isPlaying) start() else pause()
                    }
                    setOnErrorListener { _, _, _ ->
                        error = "无法加载媒体，请稍后重试"
                        true
                    }
                }
            },
            update = { view ->
                if (error != null) {
                    view.stopPlayback()
                } else if (isPrepared) {
                    if (isPlaying) {
                        if (!view.isPlaying) view.start()
                    } else {
                        if (view.isPlaying) view.pause()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (!isPrepared && error == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "播放切换",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        error?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun TypeBadge(label: String, modifier: Modifier = Modifier) {
    if (label.isBlank()) return
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = label) },
        modifier = modifier.padding(6.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun DurationBadge(duration: String, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = duration, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/**
 * Embedded BLE Traffic HUD for hardware team debugging.
 * Toggle to show/hide, collect TX/RX events, export via share sheet.
 */
@Composable
private fun BleTrafficHud() {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var isCollecting by remember { mutableStateOf(true) }
    val trafficEvents = remember { mutableStateListOf<BleTrafficEvent>() }

    // Collect BLE traffic events when expanded
    LaunchedEffect(isCollecting, isExpanded) {
        if (isCollecting && isExpanded) {
            BleTrafficObserver.events.collect { event ->
                trafficEvents.add(0, event)
                if (trafficEvents.size > 100) {
                    trafficEvents.removeAt(trafficEvents.lastIndex)
                }
            }
        }
    }

    // Export function
    fun exportLogs() {
        if (trafficEvents.isEmpty()) return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val header = buildString {
            appendLine("=== SmartSales BLE 流量日志 ===")
            appendLine("导出时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("记录数量: ${trafficEvents.size}")
            appendLine("=============================")
            appendLine()
        }
        val body = trafficEvents.reversed().joinToString("\n\n") { it.formatVerbose() }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "BLE_Traffic_$timestamp.txt")
            putExtra(Intent.EXTRA_TEXT, header + body)
        }
        context.startActivity(Intent.createChooser(intent, "导出 BLE 日志"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Toggle header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (isExpanded) "BLE 流量监控" else "BLE 调试",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (trafficEvents.isNotEmpty()) {
                        Text(
                            text = "(${trafficEvents.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = if (isExpanded) "收起 ▲" else "展开 ▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Collapsible content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isCollecting = !isCollecting },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isCollecting) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isCollecting) "暂停" else "继续")
                        }
                        OutlinedButton(
                            onClick = { trafficEvents.clear() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("清空")
                        }
                        FilledTonalButton(
                            onClick = { exportLogs() },
                            enabled = trafficEvents.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导出")
                        }
                    }

                    // Traffic log display
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 100.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (trafficEvents.isEmpty()) {
                                Text(
                                    text = if (isCollecting) "等待 BLE 通信..." else "已暂停监听",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = trafficEvents.joinToString("\n") { it.formatForHud() },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }

                    // Legend
                    Text(
                        text = "→ TX (App→设备)  ← RX (设备→App)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

