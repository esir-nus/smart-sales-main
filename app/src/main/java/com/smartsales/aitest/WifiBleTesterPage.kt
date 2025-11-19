package com.smartsales.aitest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

// 文件：aiFeatureTestApp/src/main/java/com/smartsales/aitest/WifiBleTesterPage.kt
// 模块：:aiFeatureTestApp
// 说明：仿 WiFi & BLE Tester 独立页面
// 作者：创建于 2025-11-18
@Composable
fun WifiBleTesterRoute(
    modifier: Modifier = Modifier,
    mediaServerClient: MediaServerClient,
    onShowMessage: (String) -> Unit,
    viewModel: ConnectivityControlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedFiles = remember { mutableStateMapOf<String, File>() }
    WifiBleTesterPage(
        state = state,
        modifier = modifier.verticalScroll(rememberScrollState()),
        onStartScan = viewModel::startScan,
        onStopScan = viewModel::stopScan,
        onStopConnection = viewModel::stopConnection,
        onRescan = viewModel::startScan,
        onPeripheralSelected = viewModel::selectPeripheral,
        onWifiNameChanged = viewModel::updateWifiSsid,
        onWifiPasswordChanged = viewModel::updateWifiPassword,
        onSendCredentials = viewModel::sendCredentials,
        onQueryNetwork = viewModel::queryNetworkStatus,
        onOpenConsole = viewModel::openWebConsole,
        onOpenMediaManager = viewModel::openMediaManager,
        onRequestHotspot = viewModel::requestHotspot,
        onServiceAddressChange = viewModel::updateMediaServerBaseUrl,
        mediaPanel = {
            MediaServerPanel(
                baseUrl = state.mediaServerBaseUrl,
                onBaseUrlChange = viewModel::updateMediaServerBaseUrl,
                client = mediaServerClient,
                onShowMessage = onShowMessage,
                onFilesUpdated = {},
                onFileDownloaded = { remote, local ->
                    downloadedFiles[remote.name] = local
                }
            )
        }
    )
}

@Composable
fun WifiBleTesterPage(
    state: ConnectivityControlState,
    modifier: Modifier = Modifier,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onStopConnection: () -> Unit,
    onRescan: () -> Unit,
    onPeripheralSelected: (String) -> Unit,
    onWifiNameChanged: (String) -> Unit,
    onWifiPasswordChanged: (String) -> Unit,
    onSendCredentials: () -> Unit,
    onQueryNetwork: () -> Unit,
    onOpenConsole: () -> Unit,
    onOpenMediaManager: () -> Unit,
    onRequestHotspot: () -> Unit,
    onServiceAddressChange: (String) -> Unit,
    mediaPanel: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = "当前状态") {
            Text(text = state.connectionSummary ?: "Not connected", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartScan, modifier = Modifier.weight(1f)) { Text("开始扫描") }
                OutlinedButton(onClick = onStopScan, modifier = Modifier.weight(1f)) { Text("停止扫描") }
                OutlinedButton(onClick = onStopConnection, modifier = Modifier.weight(1f)) { Text("停止连接") }
            }
        }
        SectionCard(title = "BT311 扫描状态") {
            Text(
                text = state.matchingStatus ?: "点击“开始扫描”以寻找目标设备。",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRescan, modifier = Modifier.fillMaxWidth()) { Text("重新扫描") }
            Spacer(modifier = Modifier.height(8.dp))
            state.peripherals.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${item.name} (${item.signalStrengthDbm} dBm)")
                        if (item.isSelected) {
                            Text(
                                text = "已连接",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Button(
                        onClick = { onPeripheralSelected(item.id) },
                        enabled = !item.isSelected
                    ) {
                        Text(if (item.isSelected) "已连接" else "连接")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (state.peripherals.isEmpty() && !state.isScanning) {
                Text(text = "尚未发现 BT311，系统会持续扫描。", style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionCard(title = "WiFi 名称配置 (wifi#connect#name#password)") {
            OutlinedTextField(
                value = state.wifiSsid,
                onValueChange = onWifiNameChanged,
                label = { Text("WiFi 名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.wifiPassword,
                onValueChange = onWifiPasswordChanged,
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onSendCredentials()
                },
                enabled = state.canSendCredentials,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("发送到设备")
            }
            state.transferStatus?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onRequestHotspot, modifier = Modifier.fillMaxWidth()) {
                Text("读取热点信息")
            }
            if (state.hotspotSsid != null && state.hotspotPassword != null) {
                Text(text = "热点：${state.hotspotSsid} / ${state.hotspotPassword}")
            }
        }
        SectionCard(title = "网络状态查询") {
            Text(text = "HTTP IP(自动)：${state.httpIp ?: "未知"}")
            Text(text = "WiFi 名称(设备)：${state.deviceWifiName ?: "未知"}")
            Text(text = "WiFi 名称(手机)：${state.phoneWifiName ?: "未知"}")
            Text(text = "匹配状态：${state.matchingStatus ?: "等待设备回复名称"}")
            state.networkMatchMessage?.let { msg ->
                val color = if (state.networkMatched == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                Text(text = msg, color = color, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onQueryNetwork, modifier = Modifier.fillMaxWidth()) {
                Text("查询设备网络")
            }
            Text(
                text = "提示：查询结果会自动更新 HTTP IP，并附带 WiFi 名称 (wifi#address#ip#name)。",
                style = MaterialTheme.typography.bodySmall
            )
        }
        SectionCard(title = "设备 Web 控制台") {
            OutlinedTextField(
                value = state.mediaServerBaseUrl,
                onValueChange = onServiceAddressChange,
                label = { Text("服务地址 (http://ip:port)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(text = "配网成功后会自动写入设备返回的 HTTP 地址，可手动覆盖。")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onOpenConsole, modifier = Modifier.weight(1f)) { Text("打开全屏 Web 控制台") }
                OutlinedButton(onClick = onOpenMediaManager, modifier = Modifier.weight(1f)) { Text("媒资管理") }
            }
            state.consoleStatusMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
        mediaPanel()
        SectionCard(title = "BLE 数据窗口") {
            val logs = state.bleLogs
            if (logs.isEmpty()) {
                Text(text = "暂无数据", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = logs.joinToString(separator = "\n"),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
