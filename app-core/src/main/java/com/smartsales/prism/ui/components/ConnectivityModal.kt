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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.data.connectivity.registry.RegisteredDevice
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext
import com.smartsales.prism.ui.components.connectivity.ConnectivityManagerState
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.components.connectivity.WifiRepairState
import com.smartsales.prism.ui.components.connectivity.WIFI_MISMATCH_EMPTY_CREDENTIALS_ERROR
import kotlinx.coroutines.delay

// ── Design Tokens ─────────────────────────────────────────────
private val ModalSurface = Color(0xFF14141A)
private val CardFrost = Color(0x14FFFFFF)
private val CardBorder = Color(0x12FFFFFF)
private val TextPrimary = Color(0xFFF3F7FF)
private val TextSecondary = Color(0xFF86868B)
private val TextMuted = Color(0xFFAEAEB2)
private val AccentBlue = Color(0xFF0A84FF)
private val ConnectedGreen = Color(0xFF34C759)
private val DisconnectedGrey = Color(0xFF86868B)
private val CyanDetected = Color(0xFF32D6E0)  // BLE 检测到（在范围内）
private val ReconnectingAmber = Color(0xFFFF9F0A)
private val DangerRed = Color(0xFFFF453A)
private const val ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS"

// ── Entry Points ──────────────────────────────────────────────

/**
 * Cards-First Connectivity Modal — 所有设备以等级卡片展示
 *
 * 已连接设备卡片展开显示电量、固件版本和操作按钮。
 * Wi-Fi 修复表单内嵌于活跃设备卡片下方，不再替换整个界面。
 */
@Composable
fun ConnectivityModal(
    onDismiss: () -> Unit,
    onNavigateToSetup: () -> Unit = {},
    onNavigateToAddDevice: () -> Unit = {},
    viewModel: ConnectivityViewModel = hiltViewModel()
) {
    val managerState by viewModel.managerState.collectAsState()
    val deviceCardPresentations by viewModel.deviceCardPresentations.collectAsState()
    val wifiMismatchSuggestedSsid by viewModel.wifiMismatchSuggestedSsid.collectAsState()
    val wifiMismatchErrorMessage by viewModel.wifiMismatchErrorMessage.collectAsState()
    val repairState by viewModel.repairState.collectAsState()
    val isolationBadgeIp by viewModel.isolationBadgeIp.collectAsState()
    val isolationTriggerContext by viewModel.isolationTriggerContext.collectAsState()
    val debugProbeText by viewModel.debugProbeText.collectAsState()
    val debugModeEnabled by viewModel.debugModeEnabled.collectAsState()
    val isWifiMismatchActive = managerState == ConnectivityManagerState.WIFI_MISMATCH

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                }

                // Empty state
                if (deviceCardPresentations.isEmpty()) {
                    item {
                        NeedsSetupCard(onStartSetup = {
                            viewModel.resetTransientState()
                            onDismiss()
                            onNavigateToSetup()
                        })
                    }
                }

                // Device cards — active device first, expanded
                items(deviceCardPresentations, key = { it.device.macAddress }) { card ->
                    DeviceCard(
                        device = card.device,
                        isActive = card.isActive,
                        managerState = card.managerState,
                        batteryLevel = card.batteryLevel,
                        firmwareVersion = card.firmwareVersion,
                        onConnect = { viewModel.connectDevice(card.device.macAddress) },
                        onDisconnect = viewModel::disconnect,
                        onCheckUpdate = viewModel::checkForUpdate,
                        onSetDefault = { viewModel.setDefault(card.device.macAddress) },
                        onRemove = { viewModel.removeDevice(card.device.macAddress) },
                        onRename = { newName -> viewModel.renameDevice(card.device.macAddress, newName) }
                    )
                    // WiFi mismatch inline below active card
                    if (card.isActive && isWifiMismatchActive) {
                        Spacer(modifier = Modifier.height(0.dp))
                        WifiMismatchCard(
                            repairState = repairState,
                            suggestedSsid = wifiMismatchSuggestedSsid,
                            errorMessage = wifiMismatchErrorMessage,
                            onUpdate = viewModel::updateWifiConfig,
                            onInputChanged = viewModel::clearWifiMismatchError,
                            onIgnore = viewModel::cancel,
                            onStartWifiRepair = viewModel::startIsolationWifiRepair,
                            isolationBadgeIp = isolationBadgeIp,
                            isolationTriggerContext = isolationTriggerContext
                        )
                    }
                }

                if (BuildConfig.DEBUG && debugModeEnabled) {
                    item {
                        ConnectivityDebugProbeCard(
                            debugProbeText = debugProbeText,
                            onProbeMedia = viewModel::debugProbeMediaReadiness,
                            onListRecordings = viewModel::debugListRecordings,
                            onDebugRec = viewModel::debugEmitRecNotification,
                            onReconnect = viewModel::reconnect,
                            onDebugSeedDefault = viewModel::debugSeedDefaultPriorityScenario,
                            onDebugDefaultDetect = viewModel::debugSimulateDefaultPriorityDetection,
                            onDebugManualDefault = viewModel::debugSimulateManualDefaultSuppression
                        )
                    }
                }

                // Add device button
                item {
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

// ── NeedsSetup Card ───────────────────────────────────────────

@Composable
private fun NeedsSetupCard(onStartSetup: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardFrost)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text("暂无设备", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("需要完成初始化设置才能连接", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            ModalActionButton(
                text = "开始配网",
                color = AccentBlue,
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartSetup
            )
        }
    }
}

// ── WiFi Mismatch Card (inline below active device) ────────────

@Composable
private fun WifiMismatchCard(
    repairState: WifiRepairState,
    suggestedSsid: String?,
    errorMessage: String?,
    onUpdate: (String, String) -> Unit,
    onInputChanged: () -> Unit,
    onIgnore: () -> Unit,
    onStartWifiRepair: () -> Unit,
    isolationBadgeIp: String?,
    isolationTriggerContext: IsolationTriggerContext?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardFrost)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        WifiMismatchView(
            repairState = repairState,
            suggestedSsid = suggestedSsid,
            errorMessage = errorMessage,
            onUpdate = onUpdate,
            onInputChanged = onInputChanged,
            onIgnore = onIgnore,
            onStartWifiRepair = onStartWifiRepair,
            isolationBadgeIp = isolationBadgeIp,
            isolationTriggerContext = isolationTriggerContext
        )
    }
}

@Composable
private fun ConnectivityDebugProbeCard(
    debugProbeText: String?,
    onProbeMedia: () -> Unit,
    onListRecordings: () -> Unit,
    onDebugRec: () -> Unit,
    onReconnect: () -> Unit,
    onDebugSeedDefault: () -> Unit,
    onDebugDefaultDetect: () -> Unit,
    onDebugManualDefault: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardFrost)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Debug probes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModalActionButton(
                text = "isReady",
                color = AccentBlue,
                modifier = Modifier.weight(1f),
                onClick = onProbeMedia
            )
            ModalActionButton(
                text = "/list",
                color = AccentBlue,
                modifier = Modifier.weight(1f),
                onClick = onListRecordings
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModalActionButton(
                text = "debug rec#",
                color = AccentBlue,
                modifier = Modifier.weight(1f),
                onClick = onDebugRec
            )
            ModalActionButton(
                text = "reconnect",
                color = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = onReconnect
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModalActionButton(
                text = "seed dual",
                color = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = onDebugSeedDefault
            )
            ModalActionButton(
                text = "L2.5 active-only",
                color = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = onDebugDefaultDetect
            )
        }
        ModalActionButton(
            text = "L2.5 manual default",
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            onClick = onDebugManualDefault
        )
        Text(
            text = debugProbeText ?: "No probe run",
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Device Card (expanded for connected, compact for others) ──────

internal fun connectivityDeviceCardSubtitle(
    isConnected: Boolean,
    batteryLevel: Int?,
    firmwareVersion: String?,
    isCheckingUpdate: Boolean,
    isUpdateFound: Boolean,
    isUpdating: Boolean,
    isReconnecting: Boolean,
    isBlePaired: Boolean,
    bleDetected: Boolean,
    manuallyDisconnected: Boolean,
    isActive: Boolean
): String = when {
    isConnected || isCheckingUpdate || isUpdateFound || isUpdating -> buildString {
        append("已连接")
        if (batteryLevel != null) append(" · $batteryLevel%")
        if (firmwareVersion != null) append(" · $firmwareVersion")
        else append(" · v?.?.?")
    }
    isReconnecting -> "重连中…"
    isBlePaired -> "蓝牙已连接，Wi-Fi 未就绪"
    manuallyDisconnected -> "已断开 · 点击重连"
    bleDetected -> if (isActive) "已断开 · 点击重连" else "已检测到 · 点击连接"
    isActive -> "不在范围内"
    else -> "点击连接"
}

@Composable
private fun DeviceCard(
    device: RegisteredDevice,
    isActive: Boolean,
    managerState: ConnectivityManagerState?,
    batteryLevel: Int?,
    firmwareVersion: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCheckUpdate: () -> Unit,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
    onRename: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var isRenaming by remember { mutableStateOf(device.displayName) }
    var renameText by remember(device.displayName) { mutableStateOf(device.displayName) }
    var renamingActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val isConnected = isActive && managerState == ConnectivityManagerState.CONNECTED
    val isReconnecting = isActive && managerState == ConnectivityManagerState.RECONNECTING
    val isBlePaired = isActive && (managerState == ConnectivityManagerState.BLE_PAIRED_NETWORK_UNKNOWN ||
        managerState == ConnectivityManagerState.BLE_PAIRED_NETWORK_OFFLINE)
    val isCheckingUpdate = isActive && managerState == ConnectivityManagerState.CHECKING_UPDATE
    val isUpdating = isActive && managerState == ConnectivityManagerState.UPDATING
    val isUpdateFound = isActive && managerState == ConnectivityManagerState.UPDATE_FOUND

    data class DotStyle(val color: Color, val filled: Boolean, val subtitle: String)
    val subtitle = connectivityDeviceCardSubtitle(
        isConnected = isConnected,
        batteryLevel = batteryLevel,
        firmwareVersion = firmwareVersion,
        isCheckingUpdate = isCheckingUpdate,
        isUpdateFound = isUpdateFound,
        isUpdating = isUpdating,
        isReconnecting = isReconnecting,
        isBlePaired = isBlePaired,
        bleDetected = device.bleDetected,
        manuallyDisconnected = device.manuallyDisconnected,
        isActive = isActive
    )
    val dot = when {
        isConnected || isCheckingUpdate || isUpdateFound || isUpdating ->
            DotStyle(ConnectedGreen, true, subtitle)
        isReconnecting ->
            DotStyle(ReconnectingAmber, false, subtitle)
        isBlePaired ->
            DotStyle(ReconnectingAmber, false, subtitle)
        device.manuallyDisconnected ->
            DotStyle(DisconnectedGrey, false, subtitle)
        device.bleDetected ->
            DotStyle(CyanDetected, false, subtitle)
        isActive ->
            DotStyle(DisconnectedGrey.copy(alpha = 0.5f), false, subtitle)
        else ->
            DotStyle(DisconnectedGrey.copy(alpha = 0.5f), false, subtitle)
    }

    val cardBorder = if (isActive) AccentBlue else CardBorder
    val cardBorderWidth = if (isActive) 1.5.dp else 1.dp
    val tappable = !isConnected && !isReconnecting && !isBlePaired && !isCheckingUpdate && !isUpdating && !isUpdateFound

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardFrost)
                .border(cardBorderWidth, cardBorder, RoundedCornerShape(14.dp))
                .then(if (tappable) Modifier.clickable { onConnect() } else Modifier)
                .padding(14.dp)
        ) {
            Column {
                // ─ Header row: dot + name + menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        if (dot.filled) {
                            drawCircle(color = dot.color)
                        } else {
                            drawCircle(color = dot.color, style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    if (renamingActive) {
                        BasicTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary),
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
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
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
                            val subtitleAnnotated = if (isConnected && batteryLevel != null) {
                                buildAnnotatedString {
                                    append("已连接 · ")
                                    withStyle(SpanStyle(color = batteryGlyphColor(batteryLevel))) {
                                        append("$batteryLevel%")
                                    }
                                    if (firmwareVersion != null) append(" · $firmwareVersion")
                                    else append(" · v?.?.?")
                                }
                            } else {
                                buildAnnotatedString { append(dot.subtitle) }
                            }
                            Text(subtitleAnnotated, fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    if (renamingActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("取消", fontSize = 13.sp, color = TextMuted, modifier = Modifier.clickable {
                                renameText = device.displayName; renamingActive = false
                            })
                            Text("确认", fontSize = 13.sp, color = AccentBlue, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    val t = renameText.trim()
                                    if (t.isNotEmpty()) onRename(t)
                                    renamingActive = false
                                })
                        }
                    } else {
                        Box {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Device options",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp).clickable { showMenu = true }
                            )
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = ModalSurface) {
                                DropdownMenuItem(
                                    text = { Text("重命名", color = TextPrimary, fontSize = 14.sp) },
                                    onClick = { showMenu = false; renameText = device.displayName; renamingActive = true }
                                )
                                if (!device.isDefault) {
                                    DropdownMenuItem(
                                        text = { Text("设为默认", color = AccentBlue, fontSize = 14.sp) },
                                        onClick = { showMenu = false; onSetDefault() }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("移除", color = DangerRed, fontSize = 14.sp) },
                                    onClick = { showMenu = false; showRemoveDialog = true }
                                )
                            }
                        }
                    }
                }

                // ─ Expanded footer: only for connected/transient active device
                if (isActive && (isConnected || isCheckingUpdate || isUpdating || isUpdateFound)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    when {
                        isUpdating -> {
                            var progress by remember { mutableFloatStateOf(0f) }
                            LaunchedEffect(Unit) {
                                while (progress < 1f) { delay(50); progress += 0.02f }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(18.dp), color = AccentBlue, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在安装… ${(progress * 100).toInt()}%", fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                        isCheckingUpdate -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentBlue, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在检查更新…", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                        isUpdateFound -> {
                            Column {
                                Text("发现新版本", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text("包含重要安全修复与性能提升", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ModalActionButton(text = "稍后", color = TextSecondary, modifier = Modifier.weight(1f), onClick = onConnect)
                                    ModalActionButton(text = "立即更新", color = AccentBlue, modifier = Modifier.weight(1f), onClick = onCheckUpdate)
                                }
                            }
                        }
                        else -> { // CONNECTED
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ModalActionButton(text = "断开连接", color = DangerRed, modifier = Modifier.weight(1f), onClick = onDisconnect)
                                ModalActionButton(text = "检查更新", color = AccentBlue, modifier = Modifier.weight(1f), onClick = onCheckUpdate)
                            }
                        }
                    }
                }

                // ─ Reconnecting progress indicator inline
                if (isReconnecting || isBlePaired) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = ReconnectingAmber, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isBlePaired) "蓝牙已配对，等待网络…" else "正在重新连接…",
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("移除设备", color = TextPrimary) },
            text = { Text("确定要将「${device.displayName}」从注册列表中移除？", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { showRemoveDialog = false; onRemove() }) { Text("移除", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("取消", color = TextMuted) }
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
    repairState: WifiRepairState = WifiRepairState.Idle,
    suggestedSsid: String?,
    errorMessage: String?,
    onUpdate: (String, String) -> Unit,
    onInputChanged: () -> Unit,
    onIgnore: () -> Unit,
    onStartWifiRepair: () -> Unit = onIgnore,
    isolationBadgeIp: String? = null,
    isolationTriggerContext: IsolationTriggerContext? = null,
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
            // 隔离场景路由到专用视图，其余原因沿用硬失败视图
            if (repairState.reason == WifiRepairState.HardFailure.HardFailureReason.SUSPECTED_ISOLATION) {
                WifiRepairIsolationContent(
                    badgeIp = isolationBadgeIp,
                    triggerContext = isolationTriggerContext,
                    onStartWifiRepair = onStartWifiRepair
                )
            } else {
                WifiRepairHardFailureContent(
                    reason = repairState.reason,
                    onRetry = onIgnore
                )
            }
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
private fun WifiRepairIsolationContent(
    badgeIp: String?,
    triggerContext: IsolationTriggerContext? = null,
    onStartWifiRepair: () -> Unit,
) {
    val context = LocalContext.current

    // 根据触发场景选择标题和说明文案
    val titleText = when (triggerContext) {
        IsolationTriggerContext.PRE_SYNC -> "同步暂停 — 网络可能隔离了设备"
        IsolationTriggerContext.ON_DISCONNECT -> "设备断开 — 网络可能正在隔离设备"
        IsolationTriggerContext.ON_CONNECT -> "连接后网络检测异常"
        else -> "网络可能隔离了设备"  // POST_PAIRING 或未知
    }
    val bodyText = when (triggerContext) {
        IsolationTriggerContext.PRE_SYNC ->
            "录音无法上传。手机与徽章均已接入网络，但无法互相通信。" +
            "尝试切换到个人热点后重新同步。"
        IsolationTriggerContext.ON_DISCONNECT ->
            "设备已断开连接，可能由网络隔离引起。" +
            "请切换到其他 Wi-Fi 或开启个人热点后重新连接。"
        IsolationTriggerContext.ON_CONNECT ->
            "徽章已接入网络，但无法通过 HTTP 访问。" +
            "可能是路由器隔离了设备间通信。尝试切换到个人热点。"
        else ->
            "手机和徽章均已接入同一网络，但无法互相通信。" +
            "部分访客网络和企业 Wi-Fi 会隔离设备间的直接通信。"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(titleText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = ReconnectingAmber)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            bodyText,
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 20.sp
        )
        if (badgeIp != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("徽章 IP：$badgeIp", fontSize = 12.sp, color = TextMuted)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ModalActionButton(
                text = "打开 WiFi 设置",
                color = AccentBlue,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_WIFI_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
            ModalActionButton(
                text = "打开热点设置",
                color = AccentBlue,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // 部分 OEM 隐藏 ACTION_TETHER_SETTINGS，回退到通用无线设置
                    runCatching {
                        context.startActivity(
                            Intent(ACTION_TETHER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onFailure {
                        context.startActivity(
                            Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
            ModalActionButton(
                text = "修复 Wi-Fi 配置",
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    Log.i("ConnectivityModal", "isolation wifi repair tapped")
                    onStartWifiRepair()
                }
            )
        }
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
        // SUSPECTED_ISOLATION 由 WifiMismatchView 路由到 WifiRepairIsolationContent，此处不可达
        WifiRepairState.HardFailure.HardFailureReason.SUSPECTED_ISOLATION ->
            "网络可能隔离了设备，请切换网络后重试"
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
