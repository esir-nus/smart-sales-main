package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/setup/DeviceSetupScreen.kt
// 模块：:feature:connectivity
// 说明：设备配网的步骤化 UI，展示扫描→配对→配网→等待上线→就绪
// 作者：创建于 2025-11-21

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object DeviceSetupTestTags {
    const val STEP_TEXT = "device_setup_step_text"
    const val STATUS_TEXT = "device_setup_status_text"
    const val PRIMARY_BUTTON = "device_setup_primary_button"
    const val ERROR_BANNER = "device_setup_error_banner"
    const val TO_DEVICE_MANAGER = "device_setup_go_device_manager"
}

/** 轻量调色板，贴合 React 叠层样式。 */
private object SetupPalette {
    val BackgroundStart = Color(0xFFF7F9FF)
    val BackgroundEnd = Color(0xFFEFF2F7)
    val Card = Color(0xFFFFFFFF)
    val CardMuted = Color(0xFFF8F9FB)
    val Border = Color(0xFFE5E5EA)
    val Accent = Color(0xFF4B7BEC)
    val Warning = Color(0xFFFFB020)
    val Success = Color(0xFF34C759)
}

private object SetupShapes {
    val Card = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(14.dp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(SetupPalette.BackgroundStart, SetupPalette.BackgroundEnd)
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stepText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.testTag(DeviceSetupTestTags.STEP_TEXT)
        )
        Text(
            text = "按提示完成扫描、配网与上线，与 React 叠层保持一致。",
            style = MaterialTheme.typography.bodySmall,
            color = SetupPalette.Border
        )
    }
}

@Composable
private fun StatusCard(
    message: String,
    step: DeviceSetupStep,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(6.dp, shape = SetupShapes.Card, clip = false),
        shape = SetupShapes.Card,
        border = BorderStroke(1.dp, SetupPalette.Border),
        colors = CardDefaults.cardColors(containerColor = SetupPalette.CardMuted)
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
                DeviceSetupStep.Scanning -> "请确保设备已开机，靠近手机。"
                DeviceSetupStep.Pairing -> "配对时请保持设备和手机距离在 1 米内。"
                DeviceSetupStep.WifiProvisioning -> "填写正确的 Wi-Fi 名称和密码，设备会自动联网。"
                DeviceSetupStep.WaitingForDeviceOnline -> "设备联网后会自动进入下一步，如超过 5 秒请重试。"
                DeviceSetupStep.Ready -> "设备已准备就绪，可前往设备文件或音频列表。"
                DeviceSetupStep.Error -> "可重试或返回第一步重新扫描。"
                else -> "点击下方按钮开始扫描。"
            }
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = SetupPalette.Border)
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
    Card(
        shape = SetupShapes.Card,
        colors = CardDefaults.cardColors(containerColor = SetupPalette.Card),
        border = BorderStroke(1.dp, SetupPalette.Border),
        modifier = Modifier.shadow(4.dp, shape = SetupShapes.Card, clip = false)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "网络信息", style = MaterialTheme.typography.titleMedium, color = Color.Black)
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
            Text(
                text = "填写后点击配置，系统会自动推送到设备。",
                style = MaterialTheme.typography.bodySmall,
                color = SetupPalette.Border
            )
        }
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
        DeviceSetupStep.Scanning -> "扫描设备" to onStartScan

        DeviceSetupStep.Pairing -> "配置 Wi-Fi" to { onProvisionWifi(ssid.trim(), password.trim()) }
        DeviceSetupStep.WifiProvisioning,
        DeviceSetupStep.WaitingForDeviceOnline -> "等待设备上线" to {}
        DeviceSetupStep.Error -> "重试" to onRetry
        DeviceSetupStep.Ready -> "前往设备文件" to onOpenDeviceManager
    }
    Card(
        modifier = Modifier.shadow(4.dp, shape = SetupShapes.Card, clip = false),
        shape = SetupShapes.Card,
        colors = CardDefaults.cardColors(containerColor = SetupPalette.Card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                contentPadding = PaddingValues(vertical = 12.dp),
                shape = SetupShapes.Button
            ) { Text(text = label) }

            if (state.step == DeviceSetupStep.Ready) {
                TextButton(
                    onClick = onOpenDeviceManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DeviceSetupTestTags.TO_DEVICE_MANAGER)
                ) { Text(text = "前往音频列表") }
            }
        }
    }
}
