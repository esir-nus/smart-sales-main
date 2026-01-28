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

/**
 * Onboarding Coordinator (V15 Full Spectrum)
 * Manages the state and navigation of the 12-step "Golden Thread".
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }

    // Aurora Background (Shared)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1E2C), // Dark Blue/Purple
                        Color(0xFF000000)  // Black
                    )
                )
            ),
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
                    onCancel = { currentStep = OnboardingStep.HARDWARE_WAKE },
                    onFound = { currentStep = OnboardingStep.DEVICE_FOUND }
                )
                OnboardingStep.DEVICE_FOUND -> DeviceFoundStep(
                    onConnect = { currentStep = OnboardingStep.WIFI_CREDS }
                )
                OnboardingStep.WIFI_CREDS -> WifiCredsStep(
                    onConnect = { currentStep = OnboardingStep.FIRMWARE_CHECK }
                )
                OnboardingStep.FIRMWARE_CHECK -> FirmwareCheckStep(
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

// --- Placeholder Components for Scaffolding ---
// These will be moved to ui/onboarding/steps/ later if they grow

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Logo Placeholder
        Box(Modifier.size(100.dp).background(Color.White.copy(alpha=0.1f), MaterialTheme.shapes.extraLarge))
        Spacer(Modifier.height(32.dp))
        Text("SmartSales", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("您的 AI 销售教练", fontSize = 16.sp, color = Color.Gray)
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text("开启旅程")
        }
    }
}

@Composable
private fun PermissionsStep(onAllow: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("需要一些权限", fontSize = 24.sp, color = Color.White)
        Spacer(Modifier.height(32.dp))
        // Mock Glass Cards
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha=0.1f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("🎙️ 麦克风权限", color = Color.White, fontWeight = FontWeight.Bold)
                Text("为了分析您的销售对话...", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha=0.1f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📡 蓝牙权限", color = Color.White, fontWeight = FontWeight.Bold)
                Text("为了连接 SmartBadge...", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text("允许访问") }
    }
}

@Composable
private fun VoiceHandshakeStep(onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("让我们先认识一下", fontSize = 24.sp, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("试着说：“你好，帮我搞定这个客户”", color = Color.Gray)
        Spacer(Modifier.height(64.dp))
        // Mock Waveform
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(10) {
                Box(Modifier.width(4.dp).height(32.dp).background(Color(0xFF2196F3)))
            }
        }
        Spacer(Modifier.height(64.dp))
        Button(onClick = onContinue) { Text("继续 (Mock)") }
    }
}

@Composable
private fun HardwareWakeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("启动您的 SmartBadge", fontSize = 24.sp, color = Color.White)
        Text("长按中间按钮 3 秒，直到蓝灯闪烁", color = Color.Gray)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext) { Text("灯已经在闪了") }
    }
}

@Composable
private fun ScanStep(
    viewModel: OnboardingViewModel,
    onCancel: () -> Unit,
    onFound: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.scanForDevices() // Fake I/O: 延迟在FakeOnboardingService
        onFound()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("正在搜索设备...", fontSize = 20.sp, color = Color.White)
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(color = Color.White)
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = onCancel) { Text("取消", color = Color.Gray) }
    }
}

@Composable
private fun DeviceFoundStep(onConnect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha=0.1f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📱 SmartBadge (Frank's)", color = Color.White, fontWeight = FontWeight.Bold)
                Text("ID: FF:23:44:A1 • -42dBm", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                    Text("连接")
                }
            }
        }
    }
}

@Composable
private fun WifiCredsStep(onConnect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("配置网络", fontSize = 24.sp, color = Color.White)
        Text("让徽章独立工作", color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("WiFi SSID") })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("密码") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
        Spacer(Modifier.height(32.dp))
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text("连接网络") }
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
        Text("正在检查固件版本...", fontSize = 20.sp, color = Color.White)
        Text("v1.0.2 -> v1.2.0 (必需)", color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(0.7f))
        Spacer(Modifier.height(16.dp))
        Text("${(progress * 100).toInt()}%", color = Color.White)
    }
}

@Composable
private fun DeviceNamingStep(onConfirm: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("给它起个名字", fontSize = 24.sp, color = Color.White)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = "Frank's Badge", onValueChange = {})
        Spacer(Modifier.height(32.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("确定") }
    }
}

@Composable
private fun AccountGateStep(onLogin: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("保存您的设置", fontSize = 24.sp, color = Color.White)
        Text("登录以绑定", color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("邮箱/手机号") })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("密码") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
        Spacer(Modifier.height(32.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("登录并绑定") }
    }
}

@Composable
private fun ProfileStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("让我更好地帮助你", fontSize = 24.sp, color = Color.White)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("姓名") })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("行业/角色") })
        Spacer(Modifier.height(32.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("完成") }
    }
}

@Composable
private fun CompleteStep(onGoHome: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(32.dp))
        Text("一切就绪！", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(64.dp))
        Button(onClick = onGoHome) { Text("进入首页") }
    }
}
