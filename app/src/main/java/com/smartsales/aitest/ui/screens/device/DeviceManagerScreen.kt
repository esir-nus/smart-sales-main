package com.smartsales.aitest.ui.screens.device

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/device/DeviceManagerScreen.kt
// 模块：:app
// 说明：底部导航设备管理页（本地模拟），支持连接状态与文件列表展示
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.DeviceConnectionCard
import com.smartsales.aitest.ui.components.DeviceFileItem
import com.smartsales.aitest.ui.screens.device.model.ConnectionStatus
import com.smartsales.aitest.ui.screens.device.model.DeviceFile
import com.smartsales.aitest.ui.screens.device.model.DeviceInfo
import com.smartsales.aitest.ui.screens.device.model.FileType
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerScreen() {
    var connectionStatus by rememberSaveable { mutableStateOf(ConnectionStatus.DISCONNECTED) }
    var deviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }
    var deviceFiles by remember { mutableStateOf<List<DeviceFile>>(emptyList()) }
    var selectedFileId by rememberSaveable { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "设备管理") },
                actions = {
                    if (connectionStatus == ConnectionStatus.CONNECTED) {
                        IconButton(onClick = {
                            isRefreshing = true
                            scope.launch {
                                delay(1_000)
                                deviceFiles = getMockDeviceFiles()
                                selectedFileId = deviceFiles.firstOrNull()?.id
                                isRefreshing = false
                            }
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (connectionStatus == ConnectionStatus.CONNECTED) {
                FloatingActionButton(onClick = { /* TODO: Open file picker */ }) {
                    Icon(Icons.Filled.Add, contentDescription = "上传文件")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("page_device_manager"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeviceConnectionCard(
                connectionStatus = connectionStatus,
                deviceInfo = deviceInfo,
                onConnectClick = {
                    connectionStatus = ConnectionStatus.CONNECTING
                    deviceInfo = DeviceInfo(
                        id = UUID.randomUUID().toString(),
                        name = "连接中...",
                        model = "",
                        batteryLevel = 0,
                        lastConnected = null
                    )
                    scope.launch {
                        delay(2_000)
                        connectionStatus = ConnectionStatus.CONNECTED
                        deviceInfo = DeviceInfo(
                            id = UUID.randomUUID().toString(),
                            name = "SmartSales 设备",
                            model = "SS-100",
                            batteryLevel = 85,
                            lastConnected = System.currentTimeMillis()
                        )
                        deviceFiles = getMockDeviceFiles()
                        selectedFileId = deviceFiles.firstOrNull()?.id
                    }
                },
                onDisconnectClick = {
                    connectionStatus = ConnectionStatus.DISCONNECTED
                    deviceInfo = null
                    deviceFiles = emptyList()
                    selectedFileId = null
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (connectionStatus == ConnectionStatus.CONNECTED) {
                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                }
                if (deviceFiles.isEmpty()) {
                    EmptyDeviceFilesState(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deviceFiles, key = { it.id }) { file ->
                            DeviceFileItem(
                                file = file,
                                isSelected = selectedFileId == file.id,
                                onClick = { selectedFileId = file.id },
                                onApplyClick = {
                                    deviceFiles = deviceFiles.map { current ->
                                        current.copy(isApplied = current.id == file.id)
                                    }
                                    selectedFileId = file.id
                                },
                                onDeleteClick = {
                                    deviceFiles = deviceFiles.filterNot { it.id == file.id }
                                    if (selectedFileId == file.id) {
                                        selectedFileId = deviceFiles.firstOrNull()?.id
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (connectionStatus == ConnectionStatus.CONNECTING) {
                BoxLoading()
            }
        }
    }
}

@Composable
private fun BoxLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "设备连接中，请稍候...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyDeviceFilesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "设备中暂无文件",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "上传或刷新后将显示最新内容",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getMockDeviceFiles(): List<DeviceFile> = listOf(
    DeviceFile(
        id = "file_001",
        fileName = "产品介绍.jpg",
        fileType = FileType.IMAGE,
        fileSizeBytes = 2_048_000L,
        isApplied = true
    ),
    DeviceFile(
        id = "file_002",
        fileName = "演示视频.mp4",
        fileType = FileType.VIDEO,
        durationSeconds = 120,
        fileSizeBytes = 15_360_000L
    ),
    DeviceFile(
        id = "file_003",
        fileName = "动画效果.gif",
        fileType = FileType.GIF,
        fileSizeBytes = 512_000L
    )
)
