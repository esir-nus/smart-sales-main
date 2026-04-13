package com.smartsales.prism.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.PairingState
import com.smartsales.prism.domain.pairing.WifiCredentials
import kotlinx.coroutines.delay

@Composable
internal fun ProvisioningStep(
    viewModel: PairingFlowViewModel,
    badge: DiscoveredBadge?,
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onRetryScan: () -> Unit,
    onSkipToQuickStart: () -> Unit,
    skipButtonText: String = "跳过，直接体验日程",
    onComplete: () -> Unit
) {
    val pairingState by viewModel.pairingState.collectAsState()
    var showProvisioningForm by remember(badge?.id) { mutableStateOf(true) }

    if (badge == null) {
        PairingErrorStep("缺少目标设备", "请回到扫描页重新选择设备。", "重新扫描", onRetryScan, "返回上一步", onBack)
        return
    }

    LaunchedEffect(pairingState) {
        if (pairingState is PairingState.Success) {
            delay(700)
            onComplete()
        }
    }

    ProvisioningStepContent(
        pairingState = pairingState,
        badge = badge,
        ssid = ssid,
        password = password,
        showProvisioningForm = showProvisioningForm,
        onSsidChange = onSsidChange,
        onPasswordChange = onPasswordChange,
        onBack = onBack,
        onRetryScan = onRetryScan,
        onSkipToQuickStart = onSkipToQuickStart,
        skipButtonText = skipButtonText,
        onSubmit = {
            showProvisioningForm = false
            viewModel.pairBadge(badge = badge, wifiCreds = WifiCredentials(ssid, password))
        },
        onRetryProvisioning = { showProvisioningForm = true }
    )
}

@Composable
internal fun ProvisioningStepContent(
    pairingState: PairingState,
    badge: DiscoveredBadge?,
    ssid: String,
    password: String,
    showProvisioningForm: Boolean,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onRetryScan: () -> Unit,
    onSkipToQuickStart: () -> Unit,
    skipButtonText: String = "跳过，直接体验日程",
    onSubmit: () -> Unit,
    onRetryProvisioning: () -> Unit
) {
    if (badge == null) {
        PairingErrorStep("缺少目标设备", "请回到扫描页重新选择设备。", "重新扫描", onRetryScan, "返回上一步", onBack)
        return
    }

    when (pairingState) {
        is PairingState.Error -> {
            if (showProvisioningForm) {
                ProvisioningForm(
                    ssid = ssid,
                    password = password,
                    onSsidChange = onSsidChange,
                    onPasswordChange = onPasswordChange,
                    onBack = onBack,
                    onSubmit = onSubmit,
                    onSkipToQuickStart = onSkipToQuickStart,
                    skipButtonText = skipButtonText
                )
            } else {
                val presentation = resolveConnectivityPairingErrorUiModel(OnboardingStep.PROVISIONING, pairingState)
                PairingErrorStep(
                    title = presentation.title,
                    description = presentation.description,
                    primaryLabel = presentation.primaryLabel,
                    onPrimary = {
                        when (presentation.retryAction) {
                            ConnectivityPairingRetryAction.RETRY_SCAN -> onRetryScan()
                            ConnectivityPairingRetryAction.RETRY_PROVISIONING -> onRetryProvisioning()
                        }
                    },
                    secondaryLabel = presentation.secondaryLabel,
                    onSecondary = onBack,
                    tertiaryLabel = skipButtonText,
                    onTertiary = onSkipToQuickStart
                )
            }
        }
        is PairingState.Pairing, is PairingState.Success -> ProvisioningProgressContent(pairingState, badge)
        else -> ProvisioningForm(
            ssid = ssid,
            password = password,
            onSsidChange = onSsidChange,
            onPasswordChange = onPasswordChange,
            onBack = onBack,
            onSubmit = onSubmit,
            onSkipToQuickStart = onSkipToQuickStart,
            skipButtonText = skipButtonText
        )
    }
}

@Composable
private fun ProvisioningForm(
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onSkipToQuickStart: () -> Unit,
    skipButtonText: String
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TitleBlock("配置网络", "输入 Wi‑Fi 信息后，设备会完成写入、联网检查与最终准备。")
        Spacer(Modifier.height(28.dp))
        FrostedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                OnboardingTextField(value = ssid, onValueChange = onSsidChange, label = "Wi‑Fi 名称")
                Spacer(Modifier.height(16.dp))
                OnboardingTextField(value = password, onValueChange = onPasswordChange, label = "密码", isPassword = true)
                Spacer(Modifier.height(20.dp))
                PrimaryPillButton("开始写入", onSubmit, modifier = Modifier.fillMaxWidth(), enabled = ssid.isNotBlank() && password.isNotBlank())
                Spacer(Modifier.height(10.dp))
                SecondaryPillButton("返回设备卡片", onBack, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                QuietGhostButton(skipButtonText, onSkipToQuickStart, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ProvisioningProgressContent(pairingState: PairingState, badge: DiscoveredBadge) {
    val progress = when (pairingState) {
        is PairingState.Pairing -> pairingState.progress / 100f
        is PairingState.Success -> 1f
        else -> 0f
    }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusOrb(
            icon = if (pairingState is PairingState.Success) Icons.Default.CheckCircle else Icons.Default.Bluetooth,
            tint = if (pairingState is PairingState.Success) OnboardingMint else OnboardingBlue,
            modifier = Modifier.size(72.dp),
            iconSize = 24
        )
        Spacer(Modifier.height(24.dp))
        TitleBlock(
            title = if (pairingState is PairingState.Success) "配网完成" else "正在写入设备配置",
            subtitle = "Wi‑Fi 信息与设备上线校验在这里汇合，过程保持可见。"
        )
        Spacer(Modifier.height(32.dp))
        FrostedCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("设备：${badge.name}", color = OnboardingText, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (pairingState is PairingState.Success) OnboardingMint else OnboardingBlue,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                Spacer(Modifier.height(16.dp))
                Text(if (pairingState is PairingState.Success) "100%" else "${(progress * 100).toInt()}%", color = OnboardingText, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (pairingState is PairingState.Success) "设备已完成上线校验。" else "请保持设备通电，并保持手机与设备靠近。",
                    color = OnboardingMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun CompleteStep(
    isFinalizing: Boolean = false,
    errorMessage: String? = null,
    showSchedulerHandoff: Boolean = true,
    onAcknowledge: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusOrb(icon = Icons.Default.CheckCircle, tint = OnboardingMint, modifier = Modifier.size(80.dp), iconSize = 36)
        Spacer(Modifier.height(24.dp))
        Text("一切就绪！", color = OnboardingText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "欢迎进入 SmartSales 主界面。",
            color = OnboardingMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = OnboardingBlue.copy(alpha = 0.05f),
            borderColor = OnboardingBlue.copy(alpha = 0.20f)
        ) {
            Text(
                text = "SMARTSALES HOME HANDOFF",
                color = OnboardingMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (showSchedulerHandoff) {
                    "完成当前引导后，流转进入 SmartSales 主界面，并通过真实抽屉动效展开日程。"
                } else {
                    "完成当前引导后，直接进入 SmartSales 主界面，不再显示日程教学或日程抽屉。"
                },
                color = OnboardingText,
                lineHeight = 22.sp
            )
        }
        Spacer(Modifier.height(36.dp))
        errorMessage?.let {
            OnboardingInlineNotice(it)
            Spacer(Modifier.height(16.dp))
        }
        PrimaryPillButton(
            text = when {
                isFinalizing && showSchedulerHandoff -> "正在同步体验日程..."
                isFinalizing -> "正在完成引导..."
                else -> "进入首页"
            },
            onClick = onAcknowledge,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isFinalizing
        )
    }
}
