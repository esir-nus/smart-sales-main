package com.smartsales.aitest.setup

// 文件：app/src/main/java/com/smartsales/aitest/setup/DeviceSetupScreen.kt
// 模块：:app
// 说明：交互式设备配网界面，驱动 DeviceSetupViewModel 状态机
// 作者：创建于 2025-11-20

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.connectivity.setup.DeviceSetupDeviceUi
import com.smartsales.feature.connectivity.setup.DeviceSetupStep
import com.smartsales.feature.connectivity.setup.DeviceSetupUiState
import com.smartsales.feature.connectivity.setup.DeviceSetupViewModel

@Composable
fun DeviceSetupRoute(
    modifier: Modifier = Modifier,
    onCompleted: () -> Unit,
    viewModel: DeviceSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onStartScan() }
    DeviceSetupScreen(
        state = state,
        onStartScan = viewModel::onStartScan,
        onSelectDevice = viewModel::onSelectDevice,
        onWifiSsidChanged = viewModel::onWifiSsidChanged,
        onWifiPasswordChanged = viewModel::onWifiPasswordChanged,
        onSubmitWifiCredentials = viewModel::onSubmitWifiCredentials,
        onRetry = viewModel::onRetry,
        onCompleted = onCompleted,
        modifier = modifier
    )
}

@Composable
fun DeviceSetupScreen(
    state: DeviceSetupUiState,
    onStartScan: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onWifiSsidChanged: (String) -> Unit,
    onWifiPasswordChanged: (String) -> Unit,
    onSubmitWifiCredentials: () -> Unit,
    onRetry: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state.step) {
                DeviceSetupStep.SCAN, DeviceSetupStep.SELECT_DEVICE -> {
                    DeviceScanSection(
                        devices = state.devices,
                        isScanning = state.isScanning,
                        onStartScan = onStartScan,
                        onSelectDevice = onSelectDevice
                    )
                }

                DeviceSetupStep.ENTER_WIFI -> {
                    WifiCredentialSection(
                        ssid = state.wifiSsid,
                        password = state.wifiPassword,
                        enabled = !state.isSubmitting,
                        onSsidChanged = onWifiSsidChanged,
                        onPasswordChanged = onWifiPasswordChanged,
                        onSubmit = onSubmitWifiCredentials
                    )
                }

                DeviceSetupStep.PROVISIONING, DeviceSetupStep.VERIFYING -> {
                    ProgressSection(
                        title = if (state.step == DeviceSetupStep.PROVISIONING) "正在配网" else "正在验证网络"
                    )
                }

                DeviceSetupStep.COMPLETED -> {
                    CompletedSection(
                        networkSummary = state.networkStatus?.ipAddress ?: "",
                        onDone = onCompleted
                    )
                }

                DeviceSetupStep.FAILED -> {
                    FailedSection(
                        message = state.errorMessage ?: "配网失败",
                        onRetry = onRetry
                    )
                }
            }
            state.errorMessage?.takeIf { state.step != DeviceSetupStep.FAILED }?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }
            state.networkStatus?.takeIf { state.step == DeviceSetupStep.COMPLETED }?.let { status ->
                Text(text = "设备 IP：${status.ipAddress}")
            }
        }
    }
}

@Composable
private fun DeviceScanSection(
    devices: List<DeviceSetupDeviceUi>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onSelectDevice: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "扫描并选择设备", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onStartScan, enabled = !isScanning) {
            Text(text = if (isScanning) "扫描中..." else "开始扫描")
        }
        if (devices.isEmpty() && !isScanning) {
            Text(text = "尚未发现目标设备。请确认电源开启，并重新扫描。")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices, key = { it.id }) { device ->
                Card(onClick = { onSelectDevice(device.id) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = device.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = "信号 ${device.signalStrengthDbm} dBm")
                        }
                        if (device.isSelected) {
                            Text(text = "已选择", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiCredentialSection(
    ssid: String,
    password: String,
    enabled: Boolean,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "输入 Wi-Fi 凭据", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = ssid,
            onValueChange = onSsidChanged,
            label = { Text(text = "Wi-Fi 名称") },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = { Text(text = "Wi-Fi 密码") },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Button(
            onClick = onSubmit,
            enabled = enabled && ssid.isNotBlank() && password.length >= 8,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "提交凭据")
        }
    }
}

@Composable
private fun ProgressSection(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text(text = title)
    }
}

@Composable
private fun CompletedSection(networkSummary: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = "设备已成功上线，IP：$networkSummary")
        Button(onClick = onDone) {
            Text(text = "返回")
        }
    }
}

@Composable
private fun FailedSection(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry) {
            Text(text = "重试")
        }
    }
}
