package com.smartsales.prism.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.PairingState

@Composable
internal fun ScanStep(
    viewModel: PairingFlowViewModel,
    onCancel: () -> Unit,
    onFound: (DiscoveredBadge) -> Unit
) {
    val pairingState by viewModel.pairingState.collectAsState()
    val discoveredBadge = (pairingState as? PairingState.DeviceFound)?.badge
    val permissions = remember {
        buildList {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_SCAN)
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            // Chinese OEMs (Xiaomi, Huawei, OPPO, Vivo) require location for BLE scan
            // even on Android 12+. Pre-12 devices always need it.
            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    var permissionsGranted by remember { mutableStateOf(permissions.isEmpty()) }
    var permissionDenied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
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

    LaunchedEffect(discoveredBadge?.id) {
        if (discoveredBadge != null) onFound(discoveredBadge)
    }

    ScanStepContent(
        pairingState = pairingState,
        permissionDenied = permissionDenied,
        onRequestPermissions = {
            permissionDenied = false
            launcher.launch(permissions.toTypedArray())
        },
        onRetryScan = {
            permissionDenied = false
            viewModel.startScan()
        },
        onCancel = onCancel
    )
}

@Composable
internal fun ScanStepContent(
    pairingState: PairingState,
    permissionDenied: Boolean,
    onRequestPermissions: () -> Unit,
    onRetryScan: () -> Unit,
    onCancel: () -> Unit
) {
    when {
        permissionDenied -> PairingErrorStep("需要蓝牙和位置权限", "请授权蓝牙搜索、连接和位置信息后继续扫描。", "授权并重试", onRequestPermissions, "返回上一步", onCancel)
        pairingState is PairingState.Error -> {
            val presentation = resolveConnectivityPairingErrorUiModel(OnboardingStep.SCAN, pairingState)
            PairingErrorStep(presentation.title, presentation.description, presentation.primaryLabel, onRetryScan, presentation.secondaryLabel, onCancel)
        }
        else -> ScanRadarContent(onCancel = onCancel)
    }
}

@Composable
private fun ScanRadarContent(onCancel: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "scanRadar")
    val pulse by transition.animateFloat(0.92f, 2.8f, infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Restart), label = "scanPulse")
    val pulseAlpha by transition.animateFloat(0.75f, 0f, infiniteRepeatable(animation = tween(2200), repeatMode = RepeatMode.Restart), label = "scanPulseAlpha")

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("添加新设备", color = OnboardingText, fontSize = 29.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.size(14.dp))
        Text("仅显示尚未注册的徽章，已添加设备会保留在设备管理里。", color = OnboardingMuted, fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.size(42.dp))
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(172.dp).clip(CircleShape).border(1.dp, OnboardingBlue.copy(alpha = 0.18f), CircleShape))
            Box(modifier = Modifier.size(172.dp).scale(pulse).alpha(pulseAlpha).clip(CircleShape).background(OnboardingBlue.copy(alpha = 0.09f)))
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.015f)).border(1.dp, OnboardingBlue.copy(alpha = 0.08f), CircleShape))
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(OnboardingBlue))
        }
        Spacer(Modifier.size(46.dp))
        QuietGhostButton("取消", onCancel, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun DeviceFoundStep(
    badge: DiscoveredBadge?,
    onRescan: () -> Unit,
    onConnect: () -> Unit
) {
    if (badge == null) {
        PairingErrorStep("设备已不可用", "请重新搜索设备后继续。", "重新扫描", onRescan, "返回上一步", onRescan)
        return
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TitleBlock("发现新设备", "系统不会自动连接，必须由您手动点按设备卡片确认。")
        Spacer(Modifier.size(26.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = OnboardingBlue.copy(alpha = 0.05f),
            borderColor = OnboardingBlue.copy(alpha = 0.20f)
        ) {
            Column {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(OnboardingBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = OnboardingBlue)
                    }
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(badge.name, color = OnboardingText, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(badge.id.toMacSuffix(), color = OnboardingMuted, fontSize = 13.sp)
                    }
                    Text("${badge.signalStrengthDbm} dBm", color = OnboardingBlue, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.size(18.dp))
                PrimaryPillButton("连接", onConnect, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.size(12.dp))
                SecondaryPillButton("重新扫描", onRescan, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun String.toMacSuffix(): String {
    val parts = split(":")
    return if (parts.size >= 2) "...${parts.takeLast(2).joinToString(":")}" else this
}
