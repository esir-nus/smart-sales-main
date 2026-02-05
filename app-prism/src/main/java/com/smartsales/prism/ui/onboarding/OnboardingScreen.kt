package com.smartsales.prism.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
                        onStart = { currentStep = OnboardingStep.PERMISSIONS }
                    )
                    OnboardingStep.PERMISSIONS -> PermissionsStep(
                        onAllow = { currentStep = OnboardingStep.VOICE_HANDSHAKE }
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
                        onConnect = { currentStep = OnboardingStep.WIFI_CREDS }
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
                        onFinish = { currentStep = OnboardingStep.COMPLETE }
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
private fun PermissionsStep(onAllow: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("需要一些权限", fontSize = 24.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(32.dp))
        
        PrismCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("🎙️ 麦克风权限", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("为了分析您的销售对话...", color = TextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrismCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📡 蓝牙权限", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("为了连接 SmartBadge...", color = TextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(48.dp))
        PrismButton(text = "允许访问", onClick = onAllow, modifier = Modifier.fillMaxWidth())
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
    onFound: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.startScan()
        onFound()
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
private fun DeviceFoundStep(onConnect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PrismCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📱 SmartBadge (Frank's)", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("ID: FF:23:44:A1 • -42dBm", color = AccentSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                PrismButton(text = "连接", onClick = onConnect, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun WifiCredsStep(onConnect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("配置网络", fontSize = 24.sp, color = TextPrimary)
        Text("让徽章独立工作", color = TextSecondary)
        Spacer(Modifier.height(32.dp))
        GlassTextField(value = "", onValueChange = {}, label = "WiFi SSID")
        Spacer(Modifier.height(16.dp))
        GlassTextField(value = "", onValueChange = {}, label = "密码", isPassword = true)
        Spacer(Modifier.height(32.dp))
        PrismButton(text = "连接网络", onClick = onConnect, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FirmwareCheckStep(onComplete: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(50)
            progress += 0.02f
        }
        delay(500)
        onComplete()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("正在检查固件版本...", fontSize = 20.sp, color = TextPrimary)
        Text("v1.0.2 -> v1.2.0 (必需)", color = TextSecondary)
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(0.7f),
            color = AccentBlue,
            trackColor = BackgroundSurfaceActive
        )
        Spacer(Modifier.height(16.dp))
        Text("${(progress * 100).toInt()}%", color = TextPrimary)
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
