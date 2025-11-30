package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/setup/DeviceSetupScreen.kt
// 模块：:feature:connectivity
// 说明：对齐 React 的设备配网界面，基于连接状态展示开始/配网中/成功/失败布局
// 作者：创建于 2025-11-30

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object DeviceSetupTestTags {
    const val TITLE = "device_setup_title"
    const val DESCRIPTION = "device_setup_description"
    const val PRIMARY_BUTTON = "device_setup_primary_button"
    const val SECONDARY_BUTTON = "device_setup_secondary_button"
    const val WIFI_SSID = "device_setup_wifi_ssid"
    const val WIFI_PASSWORD = "device_setup_wifi_password"
    const val ERROR_BANNER = "device_setup_error_banner"
}

@Composable
fun DeviceSetupScreen(
    state: DeviceSetupUiState,
    onStartScan: () -> Unit,
    onProvisionWifi: (ssid: String, password: String) -> Unit,
    onRetry: () -> Unit,
    onOpenDeviceManager: () -> Unit,
    onDismissError: () -> Unit,
    onBackToHome: () -> Unit,
    onWifiSsidChanged: (String) -> Unit,
    onWifiPasswordChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag(DeviceSetupTestTags.TITLE)
                    )
                    Text(
                        text = state.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(DeviceSetupTestTags.DESCRIPTION)
                    )
                    if (state.showWifiForm) {
                        WifiForm(
                            ssid = state.wifiSsid,
                            password = state.wifiPassword,
                            onSsidChange = onWifiSsidChanged,
                            onPasswordChange = onWifiPasswordChanged,
                            enabled = state.isPrimaryEnabled || state.step == DeviceSetupStep.Pairing
                        )
                    }
                    if (state.errorMessage != null && state.step == DeviceSetupStep.Error) {
                        ErrorBanner(
                            message = state.errorMessage,
                            onRetry = onRetry,
                            onBack = {
                                onDismissError()
                                onBackToHome()
                            }
                        )
                    }
                    PrimaryButtons(
                        state = state,
                        onStartScan = onStartScan,
                        onProvisionWifi = onProvisionWifi,
                        onRetry = onRetry,
                        onOpenDeviceManager = onOpenDeviceManager,
                        onBackToHome = onBackToHome
                    )
                }
            }
        }
    }
}

@Composable
private fun WifiForm(
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = ssid,
            onValueChange = onSsidChange,
            label = { Text("Wi-Fi 名称") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DeviceSetupTestTags.WIFI_SSID),
            enabled = enabled
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Wi-Fi 密码") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DeviceSetupTestTags.WIFI_PASSWORD),
            enabled = enabled
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DeviceSetupTestTags.ERROR_BANNER),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        RowActions(
            primaryLabel = "重试配网",
            secondaryLabel = "返回首页",
            onPrimary = onRetry,
            onSecondary = onBack
        )
    }
}
}

@Composable
private fun PrimaryButtons(
    state: DeviceSetupUiState,
    onStartScan: () -> Unit,
    onProvisionWifi: (String, String) -> Unit,
    onRetry: () -> Unit,
    onOpenDeviceManager: () -> Unit,
    onBackToHome: () -> Unit
) {
    val primaryAction = when (state.step) {
        DeviceSetupStep.Idle,
        DeviceSetupStep.Scanning -> onStartScan

        DeviceSetupStep.Pairing -> { { onProvisionWifi(state.wifiSsid.trim(), state.wifiPassword.trim()) } }
        DeviceSetupStep.WifiProvisioning,
        DeviceSetupStep.WaitingForDeviceOnline -> { {} }
        DeviceSetupStep.Error -> onRetry
        DeviceSetupStep.Ready -> onOpenDeviceManager
    }

    Button(
        onClick = primaryAction,
        enabled = state.isPrimaryEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DeviceSetupTestTags.PRIMARY_BUTTON)
    ) {
        Text(text = state.primaryLabel)
    }

    state.secondaryLabel?.let { secondary ->
        TextButton(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DeviceSetupTestTags.SECONDARY_BUTTON)
        ) {
            Text(text = secondary)
        }
    }
}

@Composable
private fun RowActions(
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
            Text(text = primaryLabel)
        }
        TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
            Text(text = secondaryLabel)
        }
    }
}
