package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/setup/DeviceSetupScreen.kt
// 模块：:feature:connectivity
// 说明：对齐 React 的设备配网界面，基于连接状态展示开始/配网中/成功/失败布局
// 作者：创建于 2025-11-30

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
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
            // Branch: Device Found state gets specialized UI
            if (state.step == DeviceSetupStep.Found) {
                DeviceFoundCard(
                    deviceName = state.deviceName,
                    onConnect = onPrimaryClick,
                    onRescan = onSecondaryClick
                )
            } else {
                // Default generic card for all other states
                GenericSetupCard(
                    state = state,
                    onPrimaryClick = onPrimaryClick,
                    onSecondaryClick = onSecondaryClick,
                    onRetry = onRetry,
                    onDismissError = onDismissError,
                    onWifiSsidChanged = onWifiSsidChanged,
                    onWifiPasswordChanged = onWifiPasswordChanged
                )
            }
        }
    }
}

/**
 * Specialized card for the "Device Found" state.
 * 
 * Distinct layout with:
 * - Large device icon
 * - Prominent device name
 * - Signal strength indicator (mock)
 * - Connect/Rescan actions
 */
@Composable
private fun DeviceFoundCard(
    deviceName: String?,
    onConnect: () -> Unit,
    onRescan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large device icon
            Icon(
                imageVector = Icons.Default.DeviceHub,
                contentDescription = "Device found",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            
            // Device name
            Text(
                text = deviceName ?: "未知设备",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(DeviceSetupTestTags.TITLE)
            )
            
            // Signal strength indicator (mock for now)
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = "Signal strength",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "信号良好",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Primary action: Connect
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DeviceSetupTestTags.PRIMARY_BUTTON)
            ) {
                Text("连接设备")
            }
            
            // Secondary action: Rescan
            TextButton(
                onClick = onRescan,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DeviceSetupTestTags.SECONDARY_BUTTON)
            ) {
                Text("重新扫描")
            }
        }
    }
}

/**
 * Generic setup card for all states except "Found".
 * 
 * Preserves the original layout logic.
 */
@Composable
private fun GenericSetupCard(
    state: DeviceSetupUiState,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onWifiSsidChanged: (String) -> Unit,
    onWifiPasswordChanged: (String) -> Unit
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
            // State icon + loading indicator
            StateIcon(step = state.step)
            
            if (state.isActionInProgress) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
            if (state.showWifiForm || state.setupStep == SetupStep.WifiForm) {
                WifiForm(
                    ssid = state.wifiSsid,
                    password = state.wifiPassword,
                    onSsidChange = onWifiSsidChanged,
                    onPasswordChange = onWifiPasswordChanged,
                    // Enable input when waiting for user input, disable during provisioning
                    enabled = state.step == DeviceSetupStep.WifiInput
                )
            }
            if (state.errorMessage != null && (state.step == DeviceSetupStep.Error || state.setupStep == SetupStep.Error)) {
                ErrorBanner(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    onBack = {
                        onDismissError()
                        onSecondaryClick()
                    }
                )
            }
            PrimaryButtons(
                state = state,
                onPrimaryClick = onPrimaryClick,
                onSecondaryClick = onSecondaryClick
            )
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
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    Button(
        onClick = onPrimaryClick,
        enabled = state.isPrimaryEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DeviceSetupTestTags.PRIMARY_BUTTON)
    ) {
        Text(text = state.primaryLabel)
    }

    state.secondaryLabel?.let { secondary ->
        TextButton(
            onClick = onSecondaryClick,
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

@Composable
private fun StateIcon(step: DeviceSetupStep) {
    val (icon, tint) = when (step) {
        DeviceSetupStep.Idle,
        DeviceSetupStep.Scanning,
        DeviceSetupStep.Found,
        DeviceSetupStep.Pairing -> Icons.Default.Bluetooth to MaterialTheme.colorScheme.primary
        DeviceSetupStep.CheckingNetwork,
        DeviceSetupStep.WifiInput,
        DeviceSetupStep.WifiProvisioning,
        DeviceSetupStep.WaitingForDeviceOnline -> Icons.Default.Wifi to MaterialTheme.colorScheme.secondary
        DeviceSetupStep.Ready -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        DeviceSetupStep.Error -> Icons.Default.Close to MaterialTheme.colorScheme.error
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(56.dp)
    )
}
