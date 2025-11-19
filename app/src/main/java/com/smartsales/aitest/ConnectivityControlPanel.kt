package com.smartsales.aitest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.connectivity.WifiCredentials

// 文件：aiFeatureTestApp/src/main/java/com/smartsales/aitest/ConnectivityControlPanel.kt
// 模块：:aiFeatureTestApp
// 说明：测试 App 中的 BLE 扫描 + Wi-Fi 配网控制面板
// 作者：创建于 2025-11-18
@Composable
fun ConnectivityControlRoute(
    modifier: Modifier = Modifier,
    viewModel: ConnectivityControlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectivityControlPanel(
        state = state,
        modifier = modifier,
        onScan = viewModel::startScan,
        onPeripheralSelected = viewModel::selectPeripheral,
        onSsidChanged = viewModel::updateWifiSsid,
        onPasswordChanged = viewModel::updateWifiPassword,
        onSecurityChanged = viewModel::setSecurity,
        onSendCredentials = viewModel::sendCredentials,
        onRequestHotspot = viewModel::requestHotspot
    )
}

@Composable
fun ConnectivityControlPanel(
    state: ConnectivityControlState,
    modifier: Modifier = Modifier,
    onScan: () -> Unit,
    onPeripheralSelected: (String) -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSecurityChanged: (WifiCredentials.Security) -> Unit,
    onSendCredentials: () -> Unit,
    onRequestHotspot: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderRow(
                title = "当前状态",
                actionLabel = if (state.isScanning) "扫描中..." else "开始扫描",
                enabled = !state.isScanning,
                onAction = onScan
            )
            Text(
                text = state.connectionSummary ?: "等待连接",
                style = MaterialTheme.typography.bodyMedium
            )
            if (state.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.peripherals.forEach { peripheral ->
                    FilterChip(
                        selected = peripheral.isSelected,
                        onClick = { onPeripheralSelected(peripheral.id) },
                        label = {
                            Text("${peripheral.name} (${peripheral.signalStrengthDbm} dBm)")
                        },
                        leadingIcon = if (peripheral.isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
                if (state.peripherals.isEmpty() && !state.isScanning) {
                    Text(text = "尚未发现设备，点击开始扫描寻找 BT311 / Demo Pod。")
                }
            }
            Text(text = "Wi-Fi 名称配置 (wifi#connect#name#password)", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = state.wifiSsid,
                onValueChange = onSsidChanged,
                label = { Text("WiFi 名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.wifiPassword,
                onValueChange = onPasswordChanged,
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WifiCredentials.Security.values().forEach { security ->
                    FilterChip(
                        selected = state.wifiSecurity == security,
                        onClick = { onSecurityChanged(security) },
                        label = { Text(security.name) },
                        leadingIcon = if (state.wifiSecurity == security) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
            val preview = buildPreviewCommand(state)
            Text(
                text = "发送格式（wifi#connect#name#password）：$preview",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
            Button(
                onClick = onSendCredentials,
                enabled = state.canSendCredentials,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (state.isTransferring) "发送中..." else "发送到设备")
            }
            OutlinedButton(
                onClick = onRequestHotspot,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "读取设备热点信息")
            }
            state.transferStatus?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            if (state.hotspotSsid != null && state.hotspotPassword != null) {
                Text(
                    text = "热点：${state.hotspotSsid} / 密码：${state.hotspotPassword}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(
    title: String,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAction, enabled = enabled) {
            Text(text = actionLabel)
        }
    }
}

private fun buildPreviewCommand(state: ConnectivityControlState): String {
    val ssid = sanitizeSegment(state.wifiSsid.ifBlank { "<ssid>" })
    val password = sanitizeSegment(state.wifiPassword.ifBlank { "<password>" })
    return "wifi#connect#$ssid#$password"
}

private fun sanitizeSegment(value: String): String = value.replace("#", "-")
