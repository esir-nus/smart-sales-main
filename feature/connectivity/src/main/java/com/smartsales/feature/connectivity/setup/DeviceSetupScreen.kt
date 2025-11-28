package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/setup/DeviceSetupScreen.kt
// 模块：:feature:connectivity
// 说明：设备配网的步骤化 UI，展示扫描→配对→配网→等待上线→就绪
// 作者：创建于 2025-11-21

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object DeviceSetupTestTags {
    const val STEP_TEXT = "device_setup_step_text"
    const val STATUS_TEXT = "device_setup_status_text"
    const val PRIMARY_BUTTON = "device_setup_primary_button"
    const val ERROR_BANNER = "device_setup_error_banner"
    const val TO_DEVICE_MANAGER = "device_setup_go_device_manager"
}

@Composable
fun DeviceSetupScreen(
    state: DeviceSetupUiState,
    onStartScan: () -> Unit,
    onProvisionWifi: (ssid: String, password: String) -> Unit,
    onRetry: () -> Unit,
    onOpenDeviceManager: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ssid by remember { mutableStateOf(state.wifiSsid.orEmpty()) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.wifiSsid) {
        ssid = state.wifiSsid.orEmpty()
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StepHeader(state.step)
                StatusCard(state.progressMessage, state.step, modifier = Modifier.fillMaxWidth())
                WiFiForm(
                    ssid = ssid,
                    password = password,
                    onSsidChange = { ssid = it },
                    onPasswordChange = { password = it },
                    enabled = state.step == DeviceSetupStep.Pairing || state.step == DeviceSetupStep.WifiProvisioning
                )
                if (state.errorMessage != null) {
                    ErrorBanner(
                        message = state.errorMessage,
                        onDismiss = onDismissError,
                        onRetry = onRetry
                    )
                }
                PrimaryActions(
                    state = state,
                    ssid = ssid,
                    password = password,
                    onStartScan = onStartScan,
                    onProvisionWifi = onProvisionWifi,
                    onRetry = onRetry,
                    onOpenDeviceManager = onOpenDeviceManager
                )
            }
        }
    }
}

@Composable
private fun StepHeader(step: DeviceSetupStep) {
    val stepText = when (step) {
        DeviceSetupStep.Idle, DeviceSetupStep.Scanning -> "步骤 1/4：扫描设备"
        DeviceSetupStep.Pairing -> "步骤 2/4：蓝牙配对"
        DeviceSetupStep.WifiProvisioning -> "步骤 3/4：配置 Wi-Fi"
        DeviceSetupStep.WaitingForDeviceOnline -> "步骤 4/4：等待设备上线"
        DeviceSetupStep.Ready -> "已完成"
        DeviceSetupStep.Error -> "需要重试"
    }
    Text(
        text = stepText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag(DeviceSetupTestTags.STEP_TEXT)
    )
}

@Composable
private fun StatusCard(
    message: String,
    step: DeviceSetupStep,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(DeviceSetupTestTags.STATUS_TEXT)
            )
            val hint = when (step) {
                DeviceSetupStep.Scanning -> "靠近设备并保持开机，扫描后会自动进入配对。"
                DeviceSetupStep.Pairing -> "发现设备，正在蓝牙配对并同步设备标识。"
                DeviceSetupStep.WifiProvisioning -> "填写 Wi-Fi 信息并下发，确保手机与设备在同一网络。"
                DeviceSetupStep.WaitingForDeviceOnline -> "等待设备上线，通常 5 秒内完成；如未上线请检查路由器。"
                DeviceSetupStep.Ready -> "配网完成，去设备管理查看文件或继续上传素材。"
                DeviceSetupStep.Error -> "连接异常，可重试扫描，或检查电源与网络后再试。"
                else -> "点击下方按钮开始扫描。"
            }
            Text(text = hint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WiFiForm(
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = ssid,
            onValueChange = onSsidChange,
            label = { Text("Wi-Fi 名称") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Wi-Fi 密码") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DeviceSetupTestTags.ERROR_BANNER),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(12.dp))
            TextButton(onClick = onRetry) { Text(text = "重试") }
            TextButton(onClick = onDismiss) { Text(text = "知道了") }
        }
    }
}

@Composable
private fun PrimaryActions(
    state: DeviceSetupUiState,
    ssid: String,
    password: String,
    onStartScan: () -> Unit,
    onProvisionWifi: (String, String) -> Unit,
    onRetry: () -> Unit,
    onOpenDeviceManager: () -> Unit
) {
    val (label, action) = when (state.step) {
        DeviceSetupStep.Idle,
        DeviceSetupStep.Scanning -> "开始扫描" to onStartScan

        DeviceSetupStep.Pairing -> "下发 Wi-Fi 并继续" to { onProvisionWifi(ssid.trim(), password.trim()) }
        DeviceSetupStep.WifiProvisioning,
        DeviceSetupStep.WaitingForDeviceOnline -> "等待设备上线" to {}
        DeviceSetupStep.Error -> "重新开始配网" to onRetry
        DeviceSetupStep.Ready -> "前往设备管理" to onOpenDeviceManager
    }
    Button(
        onClick = action,
        enabled = when (state.step) {
            DeviceSetupStep.WifiProvisioning,
            DeviceSetupStep.WaitingForDeviceOnline -> false
            DeviceSetupStep.Pairing -> ssid.isNotBlank() && !state.isActionInProgress && !state.isSubmittingWifi
            DeviceSetupStep.Ready -> true
            DeviceSetupStep.Scanning -> !state.isScanning
            else -> !state.isActionInProgress
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DeviceSetupTestTags.PRIMARY_BUTTON),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) { Text(text = label) }

    if (state.step == DeviceSetupStep.Ready) {
        TextButton(
            onClick = onOpenDeviceManager,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DeviceSetupTestTags.TO_DEVICE_MANAGER)
        ) { Text(text = "查看设备管理") }
    }
}
