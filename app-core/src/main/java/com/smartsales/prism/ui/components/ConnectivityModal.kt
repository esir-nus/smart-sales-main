package com.smartsales.prism.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.ui.components.connectivity.ConnectivityManagerState
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.components.connectivity.WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR
import kotlinx.coroutines.delay

/**
 * Connectivity Modal (Truncated Onboarding)
 * @see prism-ui-ux-contract.md §1.6.1
 * 
 * 重构: 使用 ViewModel 管理状态，而非直接注入 Service。
 */
@Composable
fun ConnectivityModal(
    onDismiss: () -> Unit,
    onNavigateToSetup: () -> Unit = {},
    viewModel: ConnectivityViewModel = hiltViewModel()
) {
    // Manager/modal 使用 richer managerState；shell 路由仍使用 shared connectionState
    val state by viewModel.managerState.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val wifiMismatchSuggestedSsid by viewModel.wifiMismatchSuggestedSsid.collectAsState()
    val wifiMismatchErrorMessage by viewModel.wifiMismatchErrorMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {}, // 拦截点击
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            viewModel.resetTransientState()
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ConnectivityStateContent(
                    state = state,
                    batteryLevel = batteryLevel,
                    onDisconnect = viewModel::disconnect,
                    onCheckUpdate = viewModel::checkForUpdate,
                    onReconnect = viewModel::reconnect,
                    onStartSetup = {
                        viewModel.resetTransientState()
                        onDismiss()
                        onNavigateToSetup()
                    },
                    onStartUpdate = viewModel::startUpdate,
                    onCancel = viewModel::cancel,
                    onCompleteUpdate = viewModel::completeUpdate,
                    onUpdateWifi = viewModel::updateWifiConfig,
                    wifiMismatchSuggestedSsid = wifiMismatchSuggestedSsid,
                    wifiMismatchErrorMessage = wifiMismatchErrorMessage,
                    onWifiMismatchInputChanged = viewModel::clearWifiMismatchError
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ConnectivityManagerScreen(
    onClose: () -> Unit,
    onNavigateToSetup: () -> Unit = {},
    viewModel: ConnectivityViewModel = hiltViewModel()
) {
    val state by viewModel.managerState.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val wifiMismatchSuggestedSsid by viewModel.wifiMismatchSuggestedSsid.collectAsState()
    val wifiMismatchErrorMessage by viewModel.wifiMismatchErrorMessage.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            viewModel.resetTransientState()
                            onClose()
                        }
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ConnectivityStateContent(
                    state = state,
                    batteryLevel = batteryLevel,
                    onDisconnect = viewModel::disconnect,
                    onCheckUpdate = viewModel::checkForUpdate,
                    onReconnect = viewModel::reconnect,
                    onStartSetup = {
                        viewModel.resetTransientState()
                        onNavigateToSetup()
                    },
                    onStartUpdate = viewModel::startUpdate,
                    onCancel = viewModel::cancel,
                    onCompleteUpdate = viewModel::completeUpdate,
                    onUpdateWifi = viewModel::updateWifiConfig,
                    wifiMismatchSuggestedSsid = wifiMismatchSuggestedSsid,
                    wifiMismatchErrorMessage = wifiMismatchErrorMessage,
                    onWifiMismatchInputChanged = viewModel::clearWifiMismatchError
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ConnectivityStateContent(
    state: ConnectivityManagerState,
    batteryLevel: Int,
    onDisconnect: () -> Unit,
    onCheckUpdate: () -> Unit,
    onReconnect: () -> Unit,
    onStartSetup: () -> Unit,
    onStartUpdate: () -> Unit,
    onCancel: () -> Unit,
    onCompleteUpdate: () -> Unit,
    onUpdateWifi: (String, String) -> Unit,
    wifiMismatchSuggestedSsid: String?,
    wifiMismatchErrorMessage: String?,
    onWifiMismatchInputChanged: () -> Unit
) {
    AnimatedContent(targetState = state, label = "StateTransition") { currentState ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (currentState) {
                ConnectivityManagerState.CONNECTED -> ConnectedView(
                    battery = batteryLevel,
                    onUnbind = onDisconnect,
                    onCheckUpdate = onCheckUpdate
                )
                ConnectivityManagerState.DISCONNECTED -> DisconnectedView(
                    onReconnect = onReconnect
                )
                ConnectivityManagerState.BLE_PAIRED_NETWORK_UNKNOWN -> BlePairedPendingNetworkView(
                    onReconnect = onReconnect,
                    onDisconnect = onDisconnect,
                    showDebugDisconnect = BuildConfig.DEBUG
                )
                ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE -> BlePairedOfflineView(
                    onReconnect = onReconnect,
                    onDisconnect = onDisconnect,
                    showDebugDisconnect = BuildConfig.DEBUG
                )
                ConnectivityManagerState.NEEDS_SETUP -> NeedsSetupView(
                    onStartSetup = onStartSetup
                )
                ConnectivityManagerState.CHECKING_UPDATE -> CheckingUpdateView()
                ConnectivityManagerState.UPDATE_FOUND -> UpdateFoundView(
                    onSync = onStartUpdate,
                    onLater = onCancel
                )
                ConnectivityManagerState.UPDATING -> UpdatingView(
                    onComplete = onCompleteUpdate
                )
                ConnectivityManagerState.RECONNECTING -> ReconnectingView()
                ConnectivityManagerState.WIFI_MISMATCH -> WifiMismatchView(
                    suggestedSsid = wifiMismatchSuggestedSsid,
                    errorMessage = wifiMismatchErrorMessage,
                    onUpdate = onUpdateWifi,
                    onInputChanged = onWifiMismatchInputChanged,
                    onIgnore = onCancel
                )
            }
        }
    }
}

// --- Sub-Views ---

@Composable
private fun ConnectedView(
    battery: Int,
    onUnbind: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Icon(
        imageVector = Icons.Default.Bluetooth,
        contentDescription = "Connected",
        tint = Color(0xFF4CAF50).copy(alpha = alpha),
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("SmartBadge Pro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("ID: 8842", fontSize = 14.sp, color = Color(0xFFAAAAAA))
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.BatteryFull,
            contentDescription = null,
            tint = Color(0xFFAAAAAA),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("$battery%", fontSize = 14.sp, color = Color(0xFFAAAAAA))
    }
    Text("v1.2.0", fontSize = 12.sp, color = Color.Gray)

    Spacer(modifier = Modifier.height(32.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(
            onClick = onUnbind,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
        ) {
            Text("断开连接")
        }

        Button(
            onClick = onCheckUpdate,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text("检查更新")
        }
    }
}

@Composable
private fun DisconnectedView(
    onReconnect: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.BluetoothDisabled,
        contentDescription = "Disconnected",
        tint = Color.Gray,
        modifier = Modifier
            .size(64.dp)
            .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("设备离线", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text(
        "请确保设备在附近并已开机",
        fontSize = 14.sp,
        color = Color(0xFFAAAAAA),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onReconnect,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("重试连接")
    }
}

@Composable
private fun BlePairedPendingNetworkView(
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    showDebugDisconnect: Boolean
) {
    Icon(
        imageVector = Icons.Default.Bluetooth,
        contentDescription = "Ble paired",
        tint = Color(0xFF4FC3F7),
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFF4FC3F7).copy(alpha = 0.12f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("已连接设备", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text(
        "蓝牙已连接，正在确认设备网络状态",
        fontSize = 14.sp,
        color = Color(0xFFAAAAAA),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "如果长时间停留在此状态，请重试连接并检查徽章 Wi‑Fi 状态",
        fontSize = 12.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    if (showDebugDisconnect) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) {
                Text("断开连接")
            }

            Button(
                onClick = onReconnect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("重试连接")
            }
        }
    } else {
        Button(
            onClick = onReconnect,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("重试连接")
        }
    }
}

@Composable
private fun BlePairedOfflineView(
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    showDebugDisconnect: Boolean
) {
    Icon(
        imageVector = Icons.Default.Bluetooth,
        contentDescription = "Ble paired offline",
        tint = Color(0xFF4FC3F7),
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFF4FC3F7).copy(alpha = 0.12f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("已连接设备", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text(
        "蓝牙已连接，但设备当前未接入可用网络",
        fontSize = 14.sp,
        color = Color(0xFFAAAAAA),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "请确认设备附近已开机，并检查徽章 Wi‑Fi 状态",
        fontSize = 12.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    if (showDebugDisconnect) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) {
                Text("断开连接")
            }

            Button(
                onClick = onReconnect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("重试连接")
            }
        }
    } else {
        Button(
            onClick = onReconnect,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("重试连接")
        }
    }
}

@Composable
private fun NeedsSetupView(
    onStartSetup: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "Needs Setup",
        tint = Color(0xFFFFC107),
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFFFFC107).copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("设备未配网", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text("需要完成初始化设置才能连接", fontSize = 14.sp, color = Color(0xFFAAAAAA))

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onStartSetup,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("开始配网")
    }
}

@Composable
private fun UpdateFoundView(
    onSync: () -> Unit,
    onLater: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Download,
        contentDescription = "Update",
        tint = Color(0xFFFFC107),
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFFFFC107).copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("发现新版本 v1.3", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text("包含重要安全修复与性能提升", fontSize = 14.sp, color = Color(0xFFAAAAAA))

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onSync,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("立即同步")
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    TextButton(onClick = onLater) {
        Text("稍后", color = Color.Gray)
    }
}

@Composable
private fun UpdatingView(
    onComplete: () -> Unit
) {
    // 模拟进度
    var progress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(50)
            progress += 0.02f
        }
        onComplete()
    }

    CircularProgressIndicator(
        progress = { progress },
        modifier = Modifier.size(64.dp),
        color = Color(0xFF2196F3),
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("正在安装... ${(progress * 100).toInt()}%", fontSize = 16.sp, color = Color.White)
    Text("请保持设备连接", fontSize = 12.sp, color = Color.Gray)
}

@Composable
private fun CheckingUpdateView() {
    CircularProgressIndicator(
        modifier = Modifier.size(64.dp),
        color = Color(0xFF2196F3),
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("正在检查更新...", fontSize = 16.sp, color = Color.White)
    Text("连接服务器中", fontSize = 12.sp, color = Color.Gray)
}

@Composable
private fun ReconnectingView() {
    CircularProgressIndicator(
        modifier = Modifier.size(64.dp),
        color = Color(0xFF2196F3),
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("正在重新连接...", fontSize = 16.sp, color = Color.White)
    Text("搜索附近设备", fontSize = 12.sp, color = Color.Gray)
}

@Composable
internal fun WifiMismatchView(
    suggestedSsid: String?,
    errorMessage: String?,
    onUpdate: (String, String) -> Unit,
    onInputChanged: () -> Unit,
    onIgnore: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "WiFi Mismatch",
        tint = Color(0xFFFFC107), // Amber
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFFFFC107).copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("网络环境已变更", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text("检测到徽章 WiFi 与当前网络不匹配", fontSize = 14.sp, color = Color(0xFFAAAAAA))

    Spacer(modifier = Modifier.height(24.dp))

    // 输入状态
    var ssid by remember(suggestedSsid) { mutableStateOf(resolveWifiMismatchInitialSsid(suggestedSsid)) }
    var password by remember { mutableStateOf("") }
    var localValidationError by remember { mutableStateOf<String?>(null) }
    var pendingConfirmation by remember { mutableStateOf<WifiMismatchConfirmationPayload?>(null) }

    val displayedError = localValidationError ?: errorMessage

    // WiFi 输入框
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2B38), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SSID 输入
        Column {
            Text("WiFi 名称 (SSID)", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = ssid,
                onValueChange = {
                    ssid = it
                    localValidationError = null
                    pendingConfirmation = null
                    onInputChanged()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WIFI_MISMATCH_SSID_INPUT_TEST_TAG),
                singleLine = true,
                isError = displayedError != null,
                placeholder = { Text("请输入当前 WiFi 名称", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color.Gray
                )
            )
        }
        
        // 密码输入
        Column {
            Text("密码", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localValidationError = null
                    pendingConfirmation = null
                    onInputChanged()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WIFI_MISMATCH_PASSWORD_INPUT_TEST_TAG),
                singleLine = true,
                isError = displayedError != null,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                placeholder = { Text("请输入密码", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color.Gray
                )
            )
        }

        if (displayedError != null) {
            Text(
                text = displayedError,
                color = Color(0xFFFF8A80),
                fontSize = 12.sp,
                modifier = Modifier.testTag(WIFI_MISMATCH_ERROR_TEST_TAG)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(
            onClick = onIgnore,
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
        ) {
            Text("忽略")
        }

        Button(
            onClick = {
                val normalizedSsid = ssid.trim()
                val normalizedPassword = password.trim()
                if (normalizedSsid.isEmpty() || normalizedPassword.isEmpty()) {
                    localValidationError = WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR
                    pendingConfirmation = null
                } else {
                    localValidationError = null
                    pendingConfirmation = WifiMismatchConfirmationPayload(
                        ssid = normalizedSsid,
                        password = normalizedPassword
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            modifier = Modifier.weight(1f)
        ) {
            Text("更新配置")
        }
    }

    pendingConfirmation?.let { confirmation ->
        AlertDialog(
            modifier = Modifier.testTag(WIFI_MISMATCH_CONFIRM_DIALOG_TEST_TAG),
            onDismissRequest = { pendingConfirmation = null },
            title = { Text("确认更新 Wi-Fi", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("即将把以下网络配置发送到徽章：", color = Color(0xFFDDDDDD))
                    Text(
                        text = "Wi-Fi：${confirmation.ssid}",
                        color = Color.White,
                        modifier = Modifier.testTag(WIFI_MISMATCH_CONFIRM_SSID_TEST_TAG)
                    )
                    Text("密码：已隐藏", color = Color(0xFFAAAAAA))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingConfirmation = null
                        onUpdate(confirmation.ssid, confirmation.password)
                    },
                    modifier = Modifier.testTag(WIFI_MISMATCH_CONFIRM_BUTTON_TEST_TAG)
                ) {
                    Text("确认发送")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingConfirmation = null },
                    modifier = Modifier.testTag(WIFI_MISMATCH_CONFIRM_CANCEL_BUTTON_TEST_TAG)
                ) {
                    Text("取消")
                }
            },
            containerColor = Color(0xFF2B2B38),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFDDDDDD)
        )
    }
}

internal fun resolveWifiMismatchInitialSsid(suggestedSsid: String?): String = suggestedSsid.orEmpty()

internal const val WIFI_MISMATCH_SSID_INPUT_TEST_TAG = "wifi_mismatch_ssid_input"
internal const val WIFI_MISMATCH_PASSWORD_INPUT_TEST_TAG = "wifi_mismatch_password_input"
internal const val WIFI_MISMATCH_ERROR_TEST_TAG = "wifi_mismatch_error"
internal const val WIFI_MISMATCH_CONFIRM_DIALOG_TEST_TAG = "wifi_mismatch_confirm_dialog"
internal const val WIFI_MISMATCH_CONFIRM_BUTTON_TEST_TAG = "wifi_mismatch_confirm_button"
internal const val WIFI_MISMATCH_CONFIRM_CANCEL_BUTTON_TEST_TAG = "wifi_mismatch_confirm_cancel_button"
internal const val WIFI_MISMATCH_CONFIRM_SSID_TEST_TAG = "wifi_mismatch_confirm_ssid"

private data class WifiMismatchConfirmationPayload(
    val ssid: String,
    val password: String
)
