package com.smartsales.prism.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.data.connectivity.registry.RegisteredDevice
import com.smartsales.prism.ui.components.connectivity.ConnectivityManagerState
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.components.connectivity.WifiRepairState
import com.smartsales.prism.ui.components.connectivity.WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR
import kotlinx.coroutines.delay

// ── Design Tokens ─────────────────────────────────────────────
// Matching SimHomeHeroTokens and onboarding theme for consistency

private val ModalBackground = Color(0xFF0D0D12)
private val ModalSurface = Color(0xFF14141A)
private val CardFrost = Color(0x14FFFFFF)
private val CardBorder = Color(0x12FFFFFF)
private val TextPrimary = Color(0xFFF3F7FF)
private val TextSecondary = Color(0xFF86868B)
private val TextMuted = Color(0xFFAEAEB2)
private val AccentBlue = Color(0xFF0A84FF)
private val ConnectedGreen = Color(0xFF34C759)
private val DisconnectedGrey = Color(0xFF86868B)
private val ReconnectingAmber = Color(0xFFFF9F0A)
private val DangerRed = Color(0xFFFF453A)

// ── Entry Points ──────────────────────────────────────────────

/**
 * Evolved Connectivity Modal — 设备状态 + 设备管理 Hub
 *
 * 统一的单一界面：活跃设备状态 + 其他已注册设备列表 + 管理操作。
 * 设计语言与 SimHomeHero / Onboarding frosted glass 保持一致。
 */
@Composable
fun ConnectivityModal(
    onDismiss: () -> Unit,
    onNavigateToSetup: () -> Unit = {},
    onNavigateToAddDevice: () -> Unit = {},
    viewModel: ConnectivityViewModel = hiltViewModel()
) {
    val state by viewModel.managerState.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val activeDevice by viewModel.activeDevice.collectAsState()
    val registeredDevices by viewModel.registeredDevices.collectAsState()
    val wifiMismatchSuggestedSsid by viewModel.wifiMismatchSuggestedSsid.collectAsState()
    val wifiMismatchErrorMessage by viewModel.wifiMismatchErrorMessage.collectAsState()
    val repairState by viewModel.repairState.collectAsState()

    val otherDevices = registeredDevices.filter { it.macAddress != activeDevice?.macAddress }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 48.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(ModalSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(26.dp))
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Close button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CardFrost)
                                .clickable {
                                    viewModel.resetTransientState()
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Active device section
                item {
                    ActiveDeviceSection(
                        device = activeDevice,
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
                        onRename = viewModel::renameDevice,
                        wifiMismatchSuggestedSsid = wifiMismatchSuggestedSsid,
                        wifiMismatchErrorMessage = wifiMismatchErrorMessage,
                        onWifiMismatchInputChanged = viewModel::clearWifiMismatchError,
                        repairState = repairState
                    )
                }

                // Other devices section
                if (otherDevices.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        OtherDevicesDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(otherDevices, key = { it.macAddress }) { device ->
                        OtherDeviceCard(
                            device = device,
                            onSwitch = { viewModel.switchToDevice(device.macAddress) },
                            onSetDefault = { viewModel.setDefault(device.macAddress) },
                            onRemove = { viewModel.removeDevice(device.macAddress) },
                            onRename = { newName -> viewModel.renameDevice(device.macAddress, newName) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Add device button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AddDeviceButton(onClick = {
                        viewModel.resetTransientState()
                        onDismiss()
                        onNavigateToAddDevice()
                    })
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Full-screen variant (same content, used in manager navigation).
 */
@Composable
fun ConnectivityManagerScreen(
    onClose: () -> Unit,
    onNavigateToSetup: () -> Unit = {},
    onNavigateToAddDevice: () -> Unit = {},
    viewModel: ConnectivityViewModel = hiltViewModel()
) {
    ConnectivityModal(
        onDismiss = onClose,
        onNavigateToSetup = onNavigateToSetup,
        onNavigateToAddDevice = onNavigateToAddDevice,
        viewModel = viewModel
    )
}

// ── Active Device Section ─────────────────────────────────────

@Composable
private fun ActiveDeviceSection(
    device: RegisteredDevice?,
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
    onRename: (String, String) -> Unit,
    wifiMismatchSuggestedSsid: String?,
    wifiMismatchErrorMessage: String?,
    onWifiMismatchInputChanged: () -> Unit,
    repairState: WifiRepairState = WifiRepairState.Idle,
) {
    AnimatedContent(targetState = state, label = "ActiveDeviceState") { currentState ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (currentState) {
                ConnectivityManagerState.CONNECTED ->
                    ConnectedView(device, batteryLevel, onDisconnect, onCheckUpdate, onRename)
                ConnectivityManagerState.DISCONNECTED ->
                    DisconnectedView(device, onReconnect)
                ConnectivityManagerState.BLE_PAIRED_NETWORK_UNKNOWN ->
                    BlePairedView(device, "蓝牙已连接，正在确认网络状态", onReconnect, onDisconnect)
                ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE ->
                    BlePairedView(device, "蓝牙已连接，但设备未接入网络", onReconnect, onDisconnect)
                ConnectivityManagerState.NEEDS_SETUP ->
                    NeedsSetupView(onStartSetup)
                ConnectivityManagerState.CHECKING_UPDATE ->
                    TransientStateView("正在检查更新...", "连接服务器中")
                ConnectivityManagerState.UPDATE_FOUND ->
                    UpdateFoundView(onStartUpdate, onCancel)
                ConnectivityManagerState.UPDATING ->
                    UpdatingView(onCompleteUpdate)
                ConnectivityManagerState.RECONNECTING ->
                    TransientStateView("正在重新连接...", "搜索附近设备")
                ConnectivityManagerState.WIFI_MISMATCH ->
                    WifiMismatchView(
                        repairState = repairState,
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

// ── Sub-Views ─────────────────────────────────────────────────

@Composable
private fun ConnectedView(
    device: RegisteredDevice?,
    batteryLevel: Int,
    onDisconnect: () -> Unit,
    onCheckUpdate: () -> Unit,
    onRename: (String, String) -> Unit
) {
    val deviceName = device?.displayName ?: "Badge"
    val macSuffix = device?.macSuffix ?: ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        DeviceHeader(
            name = deviceName,
            macSuffix = macSuffix,
            statusDot = ConnectedGreen,
            statusText = buildString {
                if (device?.isDefault == true) append("默认 · ")
                append("已连接")
            },
            onRename = device?.let { d -> { newName: String -> onRename(d.macAddress, newName) } }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$batteryLevel%", fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.width(12.dp))
            Text("v1.2.0", fontSize = 12.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModalActionButton(
                text = "断开连接",
                color = DangerRed,
                modifier = Modifier.weight(1f),
                onClick = onDisconnect
            )
            ModalActionButton(
                text = "检查更新",
                color = AccentBlue,
                modifier = Modifier.weight(1f),
                onClick = onCheckUpdate
            )
        }
    }
}

@Composable
private fun DisconnectedView(
    device: RegisteredDevice?,
    onReconnect: () -> Unit
) {
    val deviceName = device?.displayName ?: "Badge"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        DeviceHeader(
            name = deviceName,
            macSuffix = device?.macSuffix ?: "",
            statusDot = DisconnectedGrey,
            statusText = buildString {
                if (device?.isDefault == true) append("默认 · ")
                append("已断开")
            }
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text("请确保设备在附近并已开机", fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(20.dp))

        ModalActionButton(
            text = "重试连接",
            color = AccentBlue,
            modifier = Modifier.fillMaxWidth(),
            onClick = onReconnect
        )
    }
}

@Composable
private fun BlePairedView(
    device: RegisteredDevice?,
    message: String,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        DeviceHeader(
            name = device?.displayName ?: "Badge",
            macSuffix = device?.macSuffix ?: "",
            statusDot = ReconnectingAmber,
            statusText = "蓝牙已连接"
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(message, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (BuildConfig.DEBUG) {
                ModalActionButton(
                    text = "断开",
                    color = DangerRed,
                    modifier = Modifier.weight(1f),
                    onClick = onDisconnect
                )
            }
            ModalActionButton(
                text = "重试连接",
                color = AccentBlue,
                modifier = Modifier.weight(1f),
                onClick = onReconnect
            )
        }
    }
}

@Composable
private fun NeedsSetupView(onStartSetup: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text("暂无设备", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("需要完成初始化设置才能连接", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(20.dp))

        ModalActionButton(
            text = "开始配网",
            color = AccentBlue,
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartSetup
        )
    }
}

@Composable
private fun TransientStateView(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = AccentBlue,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun UpdateFoundView(onSync: () -> Unit, onLater: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text("发现新版本", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("包含重要安全修复与性能提升", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModalActionButton(
                text = "稍后",
                color = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = onLater
            )
            ModalActionButton(
                text = "立即同步",
                color = AccentBlue,
                modifier = Modifier.weight(1f),
                onClick = onSync
            )
        }
    }
}

@Composable
private fun UpdatingView(onComplete: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(50)
            progress += 0.02f
        }
        onComplete()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(36.dp),
            color = AccentBlue,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text("正在安装... ${(progress * 100).toInt()}%", fontSize = 16.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("请保持设备连接", fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Device Header ─────────────────────────────────────────────

@Composable
private fun DeviceHeader(
    name: String,
    macSuffix: String,
    statusDot: Color,
    statusText: String,
    onRename: ((String) -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(name) }
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Status dot
        Canvas(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(8.dp)
        ) {
            drawCircle(color = statusDot)
        }
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (isEditing && onRename != null) {
                BasicTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(AccentBlue),
                    singleLine = true
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "取消",
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.clickable {
                            editText = name
                            isEditing = false
                        }
                    )
                    Text(
                        "确认",
                        fontSize = 13.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            val trimmed = editText.trim()
                            if (trimmed.isNotEmpty()) {
                                onRename(trimmed)
                            }
                            isEditing = false
                        }
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (onRename != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    editText = name
                                    isEditing = true
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(statusText, fontSize = 13.sp, color = TextMuted)
                if (macSuffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(macSuffix, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

// ── Other Devices ─────────────────────────────────────────────

@Composable
private fun OtherDevicesDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = CardBorder
        )
        Text(
            "其他设备",
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 12.sp,
            color = TextSecondary
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = CardBorder
        )
    }
}

@Composable
private fun OtherDeviceCard(
    device: RegisteredDevice,
    onSwitch: () -> Unit,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
    onRename: (String) -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(device.displayName) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardFrost)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { showActions = !showActions }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = DisconnectedGrey)
                }
                Spacer(modifier = Modifier.width(8.dp))

                if (isRenaming) {
                    BasicTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(AccentBlue),
                        singleLine = true
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                device.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (device.isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "默认",
                                    fontSize = 10.sp,
                                    color = AccentBlue,
                                    modifier = Modifier
                                        .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(device.macSuffix, fontSize = 12.sp, color = TextSecondary)
                    }
                }

                if (isRenaming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "取消",
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.clickable {
                                renameText = device.displayName
                                isRenaming = false
                            }
                        )
                        Text(
                            "确认",
                            fontSize = 13.sp,
                            color = AccentBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                val trimmed = renameText.trim()
                                if (trimmed.isNotEmpty()) onRename(trimmed)
                                isRenaming = false
                            }
                        )
                    }
                } else {
                    Text(
                        "切换",
                        fontSize = 13.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onSwitch() }
                    )
                }
            }

            // Expandable action bar
            AnimatedVisibility(visible = showActions && !isRenaming) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "重命名",
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.clickable {
                            renameText = device.displayName
                            isRenaming = true
                        }
                    )
                    if (!device.isDefault) {
                        Text(
                            "设为默认",
                            fontSize = 13.sp,
                            color = AccentBlue,
                            modifier = Modifier.clickable { onSetDefault() }
                        )
                    }
                    Text(
                        "移除",
                        fontSize = 13.sp,
                        color = DangerRed,
                        modifier = Modifier.clickable { showRemoveDialog = true }
                    )
                }
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("移除设备", color = TextPrimary) },
            text = {
                Text(
                    "确定要将「${device.displayName}」从注册列表中移除？",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemove()
                }) {
                    Text("移除", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("取消", color = TextMuted)
                }
            },
            containerColor = ModalSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted
        )
    }
}

// ── Add Device Button ─────────────────────────────────────────

@Composable
private fun AddDeviceButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardFrost)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add",
            tint = AccentBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("添加新设备", fontSize = 14.sp, color = AccentBlue, fontWeight = FontWeight.Medium)
    }
}

// ── Action Button ─────────────────────────────────────────────

@Composable
private fun ModalActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

// ── WiFi Mismatch View ────────────────────────────────────────

@Composable
internal fun WifiMismatchView(
    repairState: WifiRepairState,
    suggestedSsid: String?,
    errorMessage: String?,
    onUpdate: (String, String) -> Unit,
    onInputChanged: () -> Unit,
    onIgnore: () -> Unit
) {
    when (repairState) {
        is WifiRepairState.Idle,
        is WifiRepairState.EditCredentials,
        is WifiRepairState.RetryableFailure ->
            WifiCredentialFormContent(
                suggestedSsid = suggestedSsid,
                errorMessage = errorMessage,
                onUpdate = onUpdate,
                onInputChanged = onInputChanged,
                onIgnore = onIgnore
            )

        is WifiRepairState.SendingCredentials,
        is WifiRepairState.WaitingForBadgeNetworkSwitch,
        is WifiRepairState.TransportConfirmed,
        is WifiRepairState.HttpCheckPending,
        is WifiRepairState.HttpReady ->
            WifiRepairProgressContent(repairState = repairState, onIgnore = onIgnore)

        is WifiRepairState.HttpDelayed ->
            WifiRepairHttpDelayedContent(
                badgeSsid = repairState.badgeSsid,
                onDismiss = onIgnore
            )

        is WifiRepairState.HardFailure ->
            WifiRepairHardFailureContent(
                reason = repairState.reason,
                onRetry = onIgnore
            )
    }
}

@Composable
private fun WifiCredentialFormContent(
    suggestedSsid: String?,
    errorMessage: String?,
    onUpdate: (String, String) -> Unit,
    onInputChanged: () -> Unit,
    onIgnore: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text("网络环境已变更", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("检测到徽章 WiFi 与当前网络不匹配", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(18.dp))

        var ssid by remember(suggestedSsid) { mutableStateOf(resolveWifiMismatchInitialSsid(suggestedSsid)) }
        var password by remember { mutableStateOf("") }
        var localValidationError by remember { mutableStateOf<String?>(null) }
        var pendingConfirmation by remember { mutableStateOf<WifiMismatchConfirmationPayload?>(null) }

        val displayedError = localValidationError ?: errorMessage

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardFrost)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text("WiFi 名称 (SSID)", fontSize = 12.sp, color = TextSecondary)
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
                    placeholder = { Text("请输入当前 WiFi 名称", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary
                    )
                )
            }

            Column {
                Text("密码", fontSize = 12.sp, color = TextSecondary)
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
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("请输入密码", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary
                    )
                )
            }

            if (displayedError != null) {
                Text(
                    text = displayedError,
                    color = DangerRed,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag(WIFI_MISMATCH_ERROR_TEST_TAG)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModalActionButton(
                text = "忽略",
                color = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = onIgnore
            )
            ModalActionButton(
                text = "更新配置",
                color = AccentBlue,
                modifier = Modifier.weight(1f),
                onClick = {
                    val normalizedSsid = ssid.trim()
                    val normalizedPassword = password.trim()
                    if (normalizedSsid.isEmpty() || normalizedPassword.isEmpty()) {
                        localValidationError = WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR
                        pendingConfirmation = null
                    } else {
                        localValidationError = null
                        pendingConfirmation = WifiMismatchConfirmationPayload(normalizedSsid, normalizedPassword)
                    }
                }
            )
        }

        pendingConfirmation?.let { confirmation ->
            AlertDialog(
                modifier = Modifier.testTag(WIFI_MISMATCH_CONFIRM_DIALOG_TEST_TAG),
                onDismissRequest = { pendingConfirmation = null },
                title = { Text("确认更新 Wi-Fi", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("即将把以下网络配置发送到徽章：", color = TextMuted)
                        Text(
                            text = "Wi-Fi：${confirmation.ssid}",
                            color = TextPrimary,
                            modifier = Modifier.testTag(WIFI_MISMATCH_CONFIRM_SSID_TEST_TAG)
                        )
                        Text("密码：已隐藏", color = TextSecondary)
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
                        Text("确认发送", color = AccentBlue)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingConfirmation = null },
                        modifier = Modifier.testTag(WIFI_MISMATCH_CONFIRM_CANCEL_BUTTON_TEST_TAG)
                    ) {
                        Text("取消", color = TextMuted)
                    }
                },
                containerColor = ModalSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextMuted
            )
        }
    }
}

@Composable
private fun WifiRepairProgressContent(
    repairState: WifiRepairState,
    onIgnore: () -> Unit
) {
    val phaseLabel = when (repairState) {
        is WifiRepairState.SendingCredentials -> "正在发送 Wi-Fi 配置..."
        is WifiRepairState.WaitingForBadgeNetworkSwitch -> "等待设备切换网络..."
        is WifiRepairState.TransportConfirmed -> "网络切换成功，正在验证服务..."
        is WifiRepairState.HttpCheckPending -> "正在验证服务连通性..."
        is WifiRepairState.HttpReady -> "服务已就绪"
        else -> "正在处理..."
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(phaseLabel, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(24.dp))
        ModalActionButton(text = "取消", color = TextSecondary, onClick = onIgnore)
    }
}

@Composable
private fun WifiRepairHttpDelayedContent(
    badgeSsid: String?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "网络已切换成功",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = ConnectedGreen
        )
        Spacer(modifier = Modifier.height(6.dp))
        val detail = if (badgeSsid != null) {
            "设备已接入 $badgeSsid，服务仍在启动中（通常需要 10–30 秒）"
        } else {
            "设备网络切换成功，服务仍在启动中（通常需要 10–30 秒）"
        }
        Text(detail, fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        ModalActionButton(text = "关闭", color = AccentBlue, onClick = onDismiss)
    }
}

@Composable
private fun WifiRepairHardFailureContent(
    reason: WifiRepairState.HardFailure.HardFailureReason,
    onRetry: () -> Unit
) {
    val detail = when (reason) {
        WifiRepairState.HardFailure.HardFailureReason.SSID_MISMATCH ->
            "设备接入的 Wi-Fi 与输入不符，请重新检查凭据后重试"
        WifiRepairState.HardFailure.HardFailureReason.BADGE_OFFLINE ->
            "设备未能接入网络，请确认 Wi-Fi 密码后重试"
        WifiRepairState.HardFailure.HardFailureReason.CREDENTIAL_REPLAY_FAILED ->
            "已保存凭据重播失败，请重新输入"
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text("网络配置失败", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
        Spacer(modifier = Modifier.height(6.dp))
        Text(detail, fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        ModalActionButton(text = "重新输入", color = AccentBlue, onClick = onRetry)
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
