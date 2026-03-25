package com.smartsales.prism.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.ErrorReason
import com.smartsales.prism.domain.pairing.PairingState
import com.smartsales.prism.domain.pairing.WifiCredentials
import kotlinx.coroutines.delay

private val OnboardingBackground = Color(0xFF05060A)
private val OnboardingCard = Color(0x14FFFFFF)
private val OnboardingCardBorder = Color(0x26FFFFFF)
private val OnboardingMuted = Color(0xFF9CA3AF)
private val OnboardingText = Color(0xFFF3F7FF)
private val OnboardingBlue = Color(0xFF38BDF8)
private val OnboardingIndigo = Color(0xFF818CF8)
private val OnboardingMint = Color(0xFF34D399)
private val OnboardingAmber = Color(0xFFF59E0B)
private val OnboardingField = Color(0x0DFFFFFF)
private val OnboardingErrorSurface = Color(0x14F59E0B)
private val OnboardingPrimarySurface = Color.White
private val OnboardingPrimaryText = Color(0xFF05060A)
private val OnboardingGhostSurface = Color(0x0DFFFFFF)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    pairingViewModel: PairingFlowViewModel = hiltViewModel()
) {
    OnboardingCoordinator(
        host = OnboardingHost.FULL_APP,
        onComplete = onComplete,
        onExit = onComplete,
        pairingViewModel = pairingViewModel
    )
}

@Composable
fun OnboardingCoordinator(
    host: OnboardingHost,
    onComplete: () -> Unit,
    onExit: () -> Unit,
    pairingViewModel: PairingFlowViewModel = hiltViewModel()
) {
    var currentStep by remember(host) { mutableStateOf(initialOnboardingStep(host)) }
    var discoveredBadge by remember(host) { mutableStateOf<DiscoveredBadge?>(null) }
    var wifiSsid by remember(host) { mutableStateOf("") }
    var wifiPassword by remember(host) { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OnboardingBackground,
                        Color(0xFF07111A),
                        OnboardingBackground
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        OnboardingAuroraBackground()

        TextButton(
            onClick = {
                pairingViewModel.cancelPairing()
                if (host == OnboardingHost.SIM_CONNECTIVITY) {
                    onExit()
                } else {
                    onComplete()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
        ) {
            Text(
                text = if (host == OnboardingHost.SIM_CONNECTIVITY) "关闭" else "跳过",
                color = OnboardingMuted
            )
        }

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) + slideInVertically(animationSpec = tween(280)) { it / 8 }) togetherWith
                    (fadeOut(animationSpec = tween(220)) + slideOutVertically(animationSpec = tween(220)) { -it / 10 })
            },
            label = "OnboardingCoordinator"
        ) { step ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        onStart = { currentStep = nextOnboardingStep(step, host) }
                    )

                    OnboardingStep.PERMISSIONS_PRIMER -> PermissionsPrimerStep(
                        onContinue = { currentStep = nextOnboardingStep(step, host) }
                    )

                    OnboardingStep.VOICE_HANDSHAKE -> VoiceHandshakeStep(
                        onContinue = { currentStep = nextOnboardingStep(step, host) }
                    )

                    OnboardingStep.HARDWARE_WAKE -> HardwareWakeStep(
                        onContinue = { currentStep = nextOnboardingStep(step, host) }
                    )

                    OnboardingStep.SCAN -> ScanStep(
                        viewModel = pairingViewModel,
                        onCancel = {
                            pairingViewModel.cancelPairing()
                            currentStep = OnboardingStep.HARDWARE_WAKE
                        },
                        onFound = { badge ->
                            discoveredBadge = badge
                            currentStep = OnboardingStep.DEVICE_FOUND
                        }
                    )

                    OnboardingStep.DEVICE_FOUND -> DeviceFoundStep(
                        badge = discoveredBadge,
                        onRescan = {
                            pairingViewModel.cancelPairing()
                            discoveredBadge = null
                            currentStep = OnboardingStep.SCAN
                        },
                        onConnect = {
                            currentStep = nextOnboardingStep(step, host)
                        }
                    )

                    OnboardingStep.PROVISIONING -> ProvisioningStep(
                        viewModel = pairingViewModel,
                        badge = discoveredBadge,
                        ssid = wifiSsid,
                        password = wifiPassword,
                        onSsidChange = { wifiSsid = it },
                        onPasswordChange = { wifiPassword = it },
                        onBack = {
                            currentStep = OnboardingStep.DEVICE_FOUND
                        },
                        onRetryScan = {
                            pairingViewModel.cancelPairing()
                            discoveredBadge = null
                            currentStep = OnboardingStep.SCAN
                        },
                        onComplete = {
                            currentStep = nextOnboardingStep(step, host)
                        }
                    )

                    OnboardingStep.COMPLETE -> CompleteStep(
                        host = host,
                        onAcknowledge = {
                            pairingViewModel.cancelPairing()
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

internal data class ConnectivityPairingErrorUiModel(
    val title: String,
    val description: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val retryAction: ConnectivityPairingRetryAction
)

internal enum class ConnectivityPairingRetryAction {
    RETRY_SCAN,
    RETRY_PROVISIONING
}

internal fun resolveConnectivityPairingErrorUiModel(
    step: OnboardingStep,
    error: PairingState.Error
): ConnectivityPairingErrorUiModel = when (step) {
    OnboardingStep.SCAN -> {
        val title = if (error.reason == ErrorReason.PERMISSION_DENIED) {
            "需要蓝牙权限"
        } else {
            "未发现设备"
        }
        val primaryLabel = if (error.reason == ErrorReason.PERMISSION_DENIED) {
            "授权并重试"
        } else {
            "重新扫描"
        }
        ConnectivityPairingErrorUiModel(
            title = title,
            description = error.message,
            primaryLabel = primaryLabel,
            secondaryLabel = "返回上一步",
            retryAction = ConnectivityPairingRetryAction.RETRY_SCAN
        )
    }

    OnboardingStep.PROVISIONING -> {
        val title = when (error.reason) {
            ErrorReason.DEVICE_NOT_FOUND -> "设备已不可用"
            ErrorReason.NETWORK_CHECK_FAILED -> "设备尚未上线"
            ErrorReason.WIFI_PROVISIONING_FAILED -> "配网失败"
            else -> "连接失败"
        }
        ConnectivityPairingErrorUiModel(
            title = title,
            description = error.message,
            primaryLabel = if (error.reason == ErrorReason.DEVICE_NOT_FOUND) "重新扫描" else "重试配网",
            secondaryLabel = "返回上一步",
            retryAction = if (error.reason == ErrorReason.DEVICE_NOT_FOUND) {
                ConnectivityPairingRetryAction.RETRY_SCAN
            } else {
                ConnectivityPairingRetryAction.RETRY_PROVISIONING
            }
        )
    }

    else -> ConnectivityPairingErrorUiModel(
        title = "连接失败",
        description = error.message,
        primaryLabel = "重试",
        secondaryLabel = "返回上一步",
        retryAction = ConnectivityPairingRetryAction.RETRY_SCAN
    )
}

@Composable
private fun OnboardingAuroraBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(280.dp)
                .clip(CircleShape)
                .background(OnboardingBlue.copy(alpha = 0.10f))
                .alpha(0.65f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(300.dp)
                .clip(CircleShape)
                .background(OnboardingIndigo.copy(alpha = 0.10f))
                .alpha(0.55f)
        )
    }
}

@Composable
private fun StatusOrb(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = tint.copy(alpha = 0.08f),
    borderColor: Color = tint.copy(alpha = 0.20f),
    iconSize: Int = 32
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x14FFFFFF),
                            Color(0x1F38BDF8)
                        )
                    )
                )
                .border(1.dp, OnboardingCardBorder, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("SS", color = OnboardingText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
        Text("SmartSales", color = OnboardingText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("您的 AI 销售教练", color = OnboardingMuted, fontSize = 17.sp)
        Spacer(Modifier.height(64.dp))
        Text(
            text = "更安静的引导，更可靠的连接。",
            color = OnboardingMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        PrimaryPillButton(
            text = "开启旅程",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionsPrimerStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleBlock(
            title = "先确认这三项准备",
            subtitle = "这里只做说明，不会立刻弹出系统授权。真正请求会在对应操作发生时再出现。"
        )
        Spacer(Modifier.height(24.dp))
        PermissionPrimerCard(
            icon = Icons.Default.Mic,
            title = "麦克风",
            description = "后续语音互动会在真正使用手机麦克风时再请求权限。"
        )
        Spacer(Modifier.height(14.dp))
        PermissionPrimerCard(
            icon = Icons.Default.Bluetooth,
            title = "蓝牙",
            description = "扫描和连接徽章时才会触发系统授权，避免提前打断。"
        )
        Spacer(Modifier.height(14.dp))
        PermissionPrimerCard(
            icon = Icons.Default.Alarm,
            title = "闹钟与提醒",
            description = "排程提醒依赖精确闹钟能力，本轮仅做能力说明。"
        )
        Spacer(Modifier.height(28.dp))
        SecondaryNote(text = "权限仍然按需请求")
        Spacer(Modifier.height(28.dp))
        PrimaryPillButton(
            text = "继续",
            onClick = onContinue,
            containerColor = OnboardingGhostSurface,
            textColor = OnboardingText,
            borderColor = OnboardingCardBorder,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun VoiceHandshakeStep(onContinue: () -> Unit) {
    var replyVisible by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "voiceHandshake")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(1500)
        replyVisible = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleBlock(
            title = "语音握手",
            subtitle = "抽象声波只表达“正在聆听”，不伪装聊天气泡。"
        )
        Spacer(Modifier.height(40.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barHeights = listOf(20.dp, 40.dp, 60.dp, 80.dp, 60.dp, 40.dp, 20.dp)
            val barColors = listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.68f),
                OnboardingBlue,
                OnboardingIndigo,
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.45f)
            )
            barHeights.forEachIndexed { index, height ->
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = height)
                        .scale(if (index % 2 == 0) pulse else 1.15f - (pulse * 0.2f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColors[index])
                )
            }
        }
        Spacer(Modifier.height(36.dp))
        if (replyVisible) {
            FrostedCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = OnboardingBlue.copy(alpha = 0.20f)
            ) {
                Text(
                    text = "语音通道已确认，接下来进入设备唤醒。",
                    color = OnboardingText,
                    lineHeight = 22.sp
                )
            }
            Spacer(Modifier.height(22.dp))
        }
        PrimaryPillButton(
            text = if (replyVisible) "继续" else "等待识别…",
            onClick = onContinue,
            enabled = replyVisible,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HardwareWakeStep(onContinue: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "hardwareWake")
    val glow by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hardwareGlow"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleBlock(
            title = "唤醒您的 SmartBadge",
            subtitle = "长按中间按钮 3 秒，直到中央蓝灯开始呼吸闪烁。"
        )
        Spacer(Modifier.height(32.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0x12000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.06f),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                )
                            )
                            .border(1.dp, OnboardingCardBorder, RoundedCornerShape(36.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .alpha(glow)
                                    .clip(CircleShape)
                                    .background(OnboardingBlue)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                SecondaryNote(text = "3 秒中心长按")
            }
        }
        Spacer(Modifier.height(26.dp))
        PrimaryPillButton(
            text = "蓝灯已经在闪了",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScanStep(
    viewModel: PairingFlowViewModel,
    onCancel: () -> Unit,
    onFound: (DiscoveredBadge) -> Unit
) {
    val pairingState by viewModel.pairingState.collectAsState()
    val permissions = remember {
        buildList {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_SCAN)
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }
    var permissionsGranted by remember { mutableStateOf(permissions.isEmpty()) }
    var permissionDenied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            permissionDenied = false
            viewModel.startScan()
        } else {
            permissionDenied = true
        }
    }

    LaunchedEffect(Unit) {
        if (permissions.isEmpty() || permissionsGranted) {
            permissionDenied = false
            viewModel.startScan()
        } else {
            launcher.launch(permissions.toTypedArray())
        }
    }

    when {
        permissionDenied -> {
            PairingErrorStep(
                title = "需要蓝牙权限",
                description = "请授权蓝牙搜索与连接后继续扫描。",
                primaryLabel = "授权并重试",
                onPrimary = {
                    permissionDenied = false
                    launcher.launch(permissions.toTypedArray())
                },
                secondaryLabel = "返回上一步",
                onSecondary = onCancel
            )
        }

        pairingState is PairingState.Error -> {
            val presentation = resolveConnectivityPairingErrorUiModel(
                OnboardingStep.SCAN,
                pairingState as PairingState.Error
            )
            PairingErrorStep(
                title = presentation.title,
                description = presentation.description,
                primaryLabel = presentation.primaryLabel,
                onPrimary = {
                    permissionDenied = false
                    viewModel.startScan()
                },
                secondaryLabel = presentation.secondaryLabel,
                onSecondary = onCancel
            )
        }

        else -> {
            if (pairingState is PairingState.DeviceFound) {
                val badge = (pairingState as PairingState.DeviceFound).badge
                LaunchedEffect(badge.id) {
                    onFound(badge)
                }
            }

            val transition = rememberInfiniteTransition(label = "scanRadar")
            val pulse by transition.animateFloat(
                initialValue = 0.8f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scanPulse"
            )
            val pulseAlpha by transition.animateFloat(
                initialValue = 0.8f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scanPulseAlpha"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TitleBlock(
                    title = "正在搜索设备",
                    subtitle = "保持徽章靠近手机，连接将始终要求您手动确认。"
                )
                Spacer(Modifier.height(34.dp))
                Box(
                    modifier = Modifier
                        .size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = OnboardingBlue.copy(alpha = 0.20f),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulse)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(OnboardingBlue.copy(alpha = 0.10f))
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(OnboardingBlue)
                    )
                }
                Spacer(Modifier.height(24.dp))
                SecondaryPillButton(
                    text = "取消",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DeviceFoundStep(
    badge: DiscoveredBadge?,
    onRescan: () -> Unit,
    onConnect: () -> Unit
) {
    if (badge == null) {
        PairingErrorStep(
            title = "设备已不可用",
            description = "请重新搜索设备后继续。",
            primaryLabel = "重新扫描",
            onPrimary = onRescan,
            secondaryLabel = "返回上一步",
            onSecondary = onRescan
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleBlock(
            title = "发现设备",
            subtitle = "系统不会自动连接，必须由您手动点按设备卡片确认。"
        )
        Spacer(Modifier.height(26.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = OnboardingBlue.copy(alpha = 0.05f),
            borderColor = OnboardingBlue.copy(alpha = 0.20f)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OnboardingBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = OnboardingBlue
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.name,
                            color = OnboardingText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "ID ${badge.id}",
                            color = OnboardingMuted,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "${badge.signalStrengthDbm} dBm",
                        color = OnboardingBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(18.dp))
                PrimaryPillButton(
                    text = "连接",
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                SecondaryPillButton(
                    text = "重新扫描",
                    onClick = onRescan,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProvisioningStep(
    viewModel: PairingFlowViewModel,
    badge: DiscoveredBadge?,
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onRetryScan: () -> Unit,
    onComplete: () -> Unit
) {
    val pairingState by viewModel.pairingState.collectAsState()
    var showProvisioningForm by remember(badge?.id) { mutableStateOf(true) }

    if (badge == null) {
        PairingErrorStep(
            title = "缺少目标设备",
            description = "请回到扫描页重新选择设备。",
            primaryLabel = "重新扫描",
            onPrimary = onRetryScan,
            secondaryLabel = "返回上一步",
            onSecondary = onBack
        )
        return
    }

    LaunchedEffect(pairingState) {
        if (pairingState is PairingState.Success) {
            delay(700)
            onComplete()
        }
    }

    when (val state = pairingState) {
        is PairingState.Error -> {
            if (showProvisioningForm) {
                ProvisioningForm(
                    ssid = ssid,
                    password = password,
                    onSsidChange = onSsidChange,
                    onPasswordChange = onPasswordChange,
                    onBack = onBack,
                    onSubmit = {
                        showProvisioningForm = false
                        viewModel.pairBadge(
                            badge = badge,
                            wifiCreds = WifiCredentials(ssid, password)
                        )
                    }
                )
            } else {
                val presentation = resolveConnectivityPairingErrorUiModel(OnboardingStep.PROVISIONING, state)
                PairingErrorStep(
                    title = presentation.title,
                    description = presentation.description,
                    primaryLabel = presentation.primaryLabel,
                    onPrimary = {
                        when (presentation.retryAction) {
                            ConnectivityPairingRetryAction.RETRY_SCAN -> onRetryScan()
                            ConnectivityPairingRetryAction.RETRY_PROVISIONING -> showProvisioningForm = true
                        }
                    },
                    secondaryLabel = presentation.secondaryLabel,
                    onSecondary = onBack
                )
            }
        }

        is PairingState.Pairing,
        is PairingState.Success -> {
            showProvisioningForm = false
            val progress = when (state) {
                is PairingState.Pairing -> state.progress / 100f
                is PairingState.Success -> 1f
                else -> 0f
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusOrb(
                    icon = Icons.Default.Bluetooth,
                    tint = if (state is PairingState.Success) OnboardingMint else OnboardingBlue,
                    modifier = Modifier.size(72.dp),
                    iconSize = 24
                )
                Spacer(Modifier.height(24.dp))
                TitleBlock(
                    title = if (state is PairingState.Success) "配网完成" else "正在写入设备配置",
                    subtitle = "Wi‑Fi 信息与设备上线校验在这里汇合，过程保持可见。"
                )
                Spacer(Modifier.height(32.dp))
                FrostedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "设备：${badge.name}",
                            color = OnboardingText,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (state is PairingState.Success) OnboardingMint else OnboardingBlue,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (state is PairingState.Success) {
                                "100%"
                            } else {
                                "${(progress * 100).toInt()}%"
                            },
                            color = OnboardingText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state is PairingState.Success) {
                                "设备已完成上线校验。"
                            } else {
                                "请保持设备通电，并保持手机与设备靠近。"
                            },
                            color = OnboardingMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        else -> {
            showProvisioningForm = true
            ProvisioningForm(
                ssid = ssid,
                password = password,
                onSsidChange = onSsidChange,
                onPasswordChange = onPasswordChange,
                onBack = onBack,
                onSubmit = {
                    showProvisioningForm = false
                    viewModel.pairBadge(
                        badge = badge,
                        wifiCreds = WifiCredentials(ssid, password)
                    )
                }
            )
        }
    }
}

@Composable
private fun ProvisioningForm(
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleBlock(
            title = "配置网络",
            subtitle = "输入 Wi‑Fi 信息后，设备会完成写入、联网检查与最终准备。"
        )
        Spacer(Modifier.height(28.dp))
        FrostedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                OnboardingTextField(
                    value = ssid,
                    onValueChange = onSsidChange,
                    label = "Wi‑Fi 名称"
                )
                Spacer(Modifier.height(16.dp))
                OnboardingTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "密码",
                    isPassword = true
                )
                Spacer(Modifier.height(20.dp))
                PrimaryPillButton(
                    text = "开始写入",
                    onClick = onSubmit,
                    enabled = ssid.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                SecondaryPillButton(
                    text = "返回设备卡片",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CompleteStep(
    host: OnboardingHost,
    onAcknowledge: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusOrb(
            icon = Icons.Default.CheckCircle,
            tint = OnboardingMint,
            modifier = Modifier.size(80.dp),
            iconSize = 36
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "一切就绪！",
            color = OnboardingText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (host == OnboardingHost.SIM_CONNECTIVITY) {
                "连接已完成，下一步进入设备连接管理。"
            } else {
                "欢迎进入 SmartSales 主界面。"
            },
            color = OnboardingMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = if (host == OnboardingHost.FULL_APP) {
                OnboardingBlue.copy(alpha = 0.05f)
            } else {
                OnboardingCard
            },
            borderColor = if (host == OnboardingHost.FULL_APP) {
                OnboardingBlue.copy(alpha = 0.20f)
            } else {
                OnboardingCardBorder
            }
        ) {
            Text(
                text = if (host == OnboardingHost.FULL_APP) {
                    "FULL APP HOST"
                } else {
                    "SIM CONNECTIVITY HOST"
                },
                color = OnboardingMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (host == OnboardingHost.SIM_CONNECTIVITY) {
                    "完成当前配对后，流转进入连接管理稳态。"
                } else {
                    "完成当前引导后，流转进入 SmartSales 主界面。"
                },
                color = OnboardingText,
                lineHeight = 22.sp
            )
        }
        Spacer(Modifier.height(36.dp))
        PrimaryPillButton(
            text = if (host == OnboardingHost.SIM_CONNECTIVITY) "进入连接管理" else "进入首页",
            onClick = onAcknowledge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PairingErrorStep(
    title: String,
    description: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusOrb(
            icon = Icons.Default.Warning,
            tint = OnboardingAmber,
            iconSize = 32
        )
        Spacer(Modifier.height(20.dp))
        Text(title, color = OnboardingText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = OnboardingErrorSurface,
            borderColor = OnboardingAmber.copy(alpha = 0.35f)
        ) {
            Text(
                text = description,
                color = OnboardingText,
                lineHeight = 22.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        PrimaryPillButton(
            text = primaryLabel,
            onClick = onPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        SecondaryPillButton(
            text = secondaryLabel,
            onClick = onSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionPrimerCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    FrostedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OnboardingText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = OnboardingText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = OnboardingMuted,
                        lineHeight = 21.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "按需请求",
                        color = OnboardingMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBlock(
    title: String,
    subtitle: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = OnboardingText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = subtitle,
            color = OnboardingMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun FrostedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = OnboardingCard,
    borderColor: Color = OnboardingCardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = OnboardingPrimarySurface,
    textColor: Color = OnboardingPrimaryText,
    borderColor: Color? = null
) {
    Surface(
        modifier = modifier,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.45f),
        shape = RoundedCornerShape(999.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = if (borderColor != null) 1.dp else 0.dp,
                    color = borderColor ?: Color.Transparent,
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 20.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(999.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, OnboardingCardBorder, RoundedCornerShape(999.dp))
                .padding(horizontal = 20.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = OnboardingText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SecondaryNote(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.10f)
        )
        Text(
            text = text,
            color = OnboardingMuted,
            fontSize = 12.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.10f)
        )
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = OnboardingMuted) },
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OnboardingField,
            unfocusedContainerColor = OnboardingField,
            focusedBorderColor = OnboardingBlue,
            unfocusedBorderColor = OnboardingCardBorder,
            focusedTextColor = OnboardingText,
            unfocusedTextColor = OnboardingText,
            cursorColor = OnboardingBlue,
            focusedLabelColor = OnboardingBlue,
            unfocusedLabelColor = OnboardingMuted
        ),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }
    )
}
