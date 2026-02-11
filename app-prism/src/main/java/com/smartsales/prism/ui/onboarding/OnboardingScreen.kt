package com.smartsales.prism.ui.onboarding

import android.content.Intent

import androidx.compose.animation.*
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.smartsales.prism.ui.components.PrismCard
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.theme.*

/**
 * Onboarding Coordinator (Sleek Glass Version)
 * Manages the state and navigation of the 12-step "Golden Thread".
 * 
 * Updates:
 * - Uses `BackgroundApp` (Aurora)
 * - `PrismCard` for all steps
 * - `PrismButton` for actions
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var discoveredBadge by remember { mutableStateOf<com.smartsales.prism.domain.pairing.DiscoveredBadge?>(null) }

    // Aurora Background (Shared) - Consistent with App
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundApp), // Global Aurora
        contentAlignment = Alignment.Center
    ) {
        // 跳过按钮 — 固定在右上角，所有步骤通用
        TextButton(
            onClick = onComplete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
                .statusBarsPadding()
        ) {
            Text("跳过", color = TextMuted, fontSize = 14.sp)
        }

        // Step Content with Animation
        AnimatedContent(
            targetState = currentStep,
            label = "OnboardingNav",
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            }
        ) { step ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        onStart = { currentStep = OnboardingStep.VOICE_HANDSHAKE }
                    )
                    OnboardingStep.VOICE_HANDSHAKE -> VoiceHandshakeStep(
                        onContinue = { currentStep = OnboardingStep.HARDWARE_WAKE }
                    )
                    OnboardingStep.HARDWARE_WAKE -> HardwareWakeStep(
                        onNext = { currentStep = OnboardingStep.SCAN }
                    )
                    OnboardingStep.SCAN -> ScanStep(
                        viewModel = viewModel,
                        onCancel = { 
                            viewModel.cancelPairing()
                            currentStep = OnboardingStep.HARDWARE_WAKE 
                        },
                        onFound = { badge -> 
                            discoveredBadge = badge
                            currentStep = OnboardingStep.DEVICE_FOUND 
                        }
                    )
                    OnboardingStep.DEVICE_FOUND -> DeviceFoundStep(
                        badge = discoveredBadge!!,
                        onConnect = { currentStep = OnboardingStep.BLE_CONNECTING }
                    )
                    OnboardingStep.BLE_CONNECTING -> BleConnectingStep(
                        onReady = { currentStep = OnboardingStep.WIFI_CREDS }
                    )
                    OnboardingStep.WIFI_CREDS -> WifiCredsStep(
                        viewModel = viewModel,
                        badge = discoveredBadge!!,
                        onConnect = { currentStep = OnboardingStep.FIRMWARE_CHECK }
                    )
                    OnboardingStep.FIRMWARE_CHECK -> PairingProgressStep(
                        viewModel = viewModel,
                        onComplete = { currentStep = OnboardingStep.DEVICE_NAMING }
                    )
                    OnboardingStep.DEVICE_NAMING -> DeviceNamingStep(
                        onConfirm = { currentStep = OnboardingStep.ACCOUNT_GATE }
                    )
                    OnboardingStep.ACCOUNT_GATE -> AccountGateStep(
                        onLogin = { currentStep = OnboardingStep.PROFILE }
                    )
                    OnboardingStep.PROFILE -> ProfileStep(
                        onFinish = { currentStep = OnboardingStep.NOTIFICATION_PERMISSION }
                    )
                    OnboardingStep.NOTIFICATION_PERMISSION -> NotificationPermissionStep(
                        onNext = { currentStep = OnboardingStep.COMPLETE }
                    )
                    OnboardingStep.COMPLETE -> CompleteStep(
                        onGoHome = onComplete
                    )
                }
            }
        }
    }
}

// --- Steps using Sleek Glass Components ---

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Logo Placeholder - Glass
        PrismSurface(
            modifier = Modifier.size(100.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
            backgroundColor = BackgroundSurface.copy(alpha = 0.5f)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🔮", fontSize = 48.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("SmartSales", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("您的 AI 销售教练", fontSize = 16.sp, color = TextSecondary)
        Spacer(Modifier.height(64.dp))
        PrismButton(
            text = "开启旅程",
            onClick = onStart,
            modifier = Modifier.width(200.dp)
        )
    }
}

@Composable
private fun VoiceHandshakeStep(onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("让我们先认识一下", fontSize = 24.sp, color = TextPrimary)
        Spacer(Modifier.height(16.dp))
        Text("试着说：“你好，帮我搞定这个客户”", color = TextSecondary)
        Spacer(Modifier.height(64.dp))
        // Mock Waveform - Blue Accent
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(10) {
                Box(Modifier.width(4.dp).height(32.dp).background(AccentBlue, androidx.compose.foundation.shape.CircleShape))
            }
        }
        Spacer(Modifier.height(64.dp))
        PrismButton(text = "继续 (Mock)", onClick = onContinue)
    }
}

@Composable
private fun HardwareWakeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("启动您的 SmartBadge", fontSize = 24.sp, color = TextPrimary)
        Text("长按中间按钮 3 秒，直到蓝灯闪烁", color = TextSecondary)
        Spacer(Modifier.height(48.dp))
        PrismButton(text = "灯已经在闪了", onClick = onNext)
    }
}

@Composable
private fun ScanStep(
    viewModel: OnboardingViewModel,
    onCancel: () -> Unit,
    onFound: (com.smartsales.prism.domain.pairing.DiscoveredBadge) -> Unit
) {
    val pairingState by viewModel.pairingState.collectAsState()
    
    // 权限请求 (Android 12+)
    val permissions = remember {
        buildList {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_SCAN)
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }
    
    var permissionsGranted by remember { mutableStateOf(permissions.isEmpty()) }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            viewModel.startScan()
        }
    }
    
    // 启动扫描或请求权限
    LaunchedEffect(Unit) {
        if (permissions.isEmpty() || permissionsGranted) {
            viewModel.startScan()
        } else {
            launcher.launch(permissions.toTypedArray())
        }
    }
    
    // 处理状态转换
    when (val state = pairingState) {
        is com.smartsales.prism.domain.pairing.PairingState.DeviceFound -> {
            LaunchedEffect(state.badge) {
                onFound(state.badge)
            }
        }
        is com.smartsales.prism.domain.pairing.PairingState.Error -> {
            // 显示错误（TODO: 添加 toast）
        }
        else -> {} // Scanning or Idle
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("正在搜索设备...", fontSize = 20.sp, color = TextPrimary)
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(color = AccentBlue)
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = onCancel) { Text("取消", color = TextMuted) }
    }
}

@Composable
private fun DeviceFoundStep(
    badge: com.smartsales.prism.domain.pairing.DiscoveredBadge,
    onConnect: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PrismCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📱 ${badge.name}", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("ID: ${badge.id} • ${badge.signalStrengthDbm}dBm", color = AccentSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                PrismButton(text = "连接", onClick = onConnect, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * BLE 配对冷却步骤 — 5秒动画缓冲
 * 
 * Makeshift: 给 BLE GATT 配对完成留出时间
 * 真正的修复应该是监听 PairingState 到达 BLE ready 状态
 */
@Composable
private fun BleConnectingStep(onReady: () -> Unit) {
    // 5秒后自动前进
    LaunchedEffect(Unit) {
        delay(5000)
        onReady()
    }
    
    // 进度动画
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed / 5000f).coerceAtMost(1f)
            delay(50)
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📡", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text("正在连接设备...", fontSize = 22.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("建立蓝牙连接中，请稍候", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(0.6f),
            color = AccentBlue,
            trackColor = BackgroundSurfaceActive
        )
    }
}

@Composable
private fun WifiCredsStep(
    viewModel: OnboardingViewModel,
    badge: com.smartsales.prism.domain.pairing.DiscoveredBadge,
    onConnect: () -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("配置网络", fontSize = 24.sp, color = TextPrimary)
        Text("让徽章独立工作", color = TextSecondary)
        Spacer(Modifier.height(32.dp))
        GlassTextField(value = ssid, onValueChange = { ssid = it }, label = "WiFi SSID")
        Spacer(Modifier.height(16.dp))
        GlassTextField(value = password, onValueChange = { password = it }, label = "密码", isPassword = true)
        Spacer(Modifier.height(32.dp))
        PrismButton(
            text = "连接网络", 
            onClick = {
                viewModel.pairBadge(
                    badge = badge,
                    wifiCreds = com.smartsales.prism.domain.pairing.WifiCredentials(ssid, password)
                )
                onConnect()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PairingProgressStep(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val pairingState by viewModel.pairingState.collectAsState()
    
    // 监听配对完成
    when (val state = pairingState) {
        is com.smartsales.prism.domain.pairing.PairingState.Success -> {
            LaunchedEffect(Unit) {
                delay(500)
                onComplete()
            }
        }
        else -> {}
    }
    
    val currentProgress = when (val state = pairingState) {
        is com.smartsales.prism.domain.pairing.PairingState.Pairing -> state.progress / 100f
        is com.smartsales.prism.domain.pairing.PairingState.Success -> 1f
        else -> 0f
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("正在配对设备...", fontSize = 20.sp, color = TextPrimary)
        Text("WiFi 配网 + 网络检查", color = TextSecondary)
        Spacer(modifier = Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier.fillMaxWidth(0.7f),
            color = AccentBlue,
            trackColor = BackgroundSurfaceActive
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("${(currentProgress * 100).toInt()}%", color = TextPrimary)
    }
}

@Composable
private fun DeviceNamingStep(onConfirm: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("给它起个名字", fontSize = 24.sp, color = TextPrimary)
        Spacer(Modifier.height(32.dp))
        GlassTextField(value = "Frank's Badge", onValueChange = {}, label = "设备名称")
        Spacer(Modifier.height(32.dp))
        PrismButton(text = "确定", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AccountGateStep(onLogin: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("保存您的设置", fontSize = 24.sp, color = TextPrimary)
        Text("登录以绑定", color = TextSecondary)
        Spacer(Modifier.height(32.dp))
        GlassTextField(value = "", onValueChange = {}, label = "邮箱/手机号")
        Spacer(Modifier.height(16.dp))
        GlassTextField(value = "", onValueChange = {}, label = "密码", isPassword = true)
        Spacer(Modifier.height(32.dp))
        PrismButton(text = "登录并绑定", onClick = onLogin, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProfileStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("让我更好地帮助你", fontSize = 24.sp, color = TextPrimary)
        Spacer(Modifier.height(32.dp))
        GlassTextField(value = "", onValueChange = {}, label = "姓名")
        Spacer(Modifier.height(16.dp))
        GlassTextField(value = "", onValueChange = {}, label = "行业/角色")
        Spacer(Modifier.height(32.dp))
        PrismButton(text = "完成", onClick = onFinish, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CompleteStep(onGoHome: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, null, tint = AccentSecondary, modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(32.dp))
        Text("一切就绪！", fontSize = 32.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(64.dp))
        PrismButton(text = "进入首页", onClick = onGoHome)
    }
}

// Helper: Glass Text Field
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = BorderSubtle,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AccentBlue
        ),
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    )
}

@Composable
private fun NotificationPermissionStep(onNext: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isChineseOem = com.smartsales.prism.data.notification.OemCompat.isChineseOem

    // Android 13+ 需要运行时权限；旧版本直接进入通知设置引导或跳过
    val needsPostNotification = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU

    // 两阶段: Phase 1 = POST_NOTIFICATIONS, Phase 2 = 通知设置页（中国 OEM 需要手动开启锁屏通知等）
    var phase by remember { mutableIntStateOf(if (needsPostNotification) 1 else if (isChineseOem) 2 else 0) }

    if (phase == 0) {
        // 不需要任何权限引导，跳过
        LaunchedEffect(Unit) { onNext() }
        return
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ ->
        // POST_NOTIFICATIONS 完成，进入 Phase 2 (中国 OEM) 或结束
        if (isChineseOem) phase = 2 else onNext()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(32.dp))

        when (phase) {
            // Phase 1: POST_NOTIFICATIONS (Android 13+)
            1 -> {
                Text("开启通知提醒", fontSize = 24.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text(
                    "在任务开始前收到精准提醒\n不错过任何重要安排",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(48.dp))
                PrismButton(
                    text = "开启通知",
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else if (isChineseOem) {
                            phase = 2
                        } else {
                            onNext()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Phase 2: 通知设置页 — 一站式开启锁屏通知、悬浮通知、振动等
            2 -> {
                Text("检查通知设置", fontSize = 24.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text(
                    "请确认以下选项已开启：\n锁屏通知 · 悬浮通知 · 振动 · 后台发送本地通知",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(48.dp))
                PrismButton(
                    text = "打开通知设置",
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                PrismButton(
                    text = "已设置完成",
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNext) {
            Text("稍后再说", color = TextMuted)
        }
    }
}
