package com.smartsales.prism.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.pairing.ErrorReason
import com.smartsales.prism.domain.pairing.PairingState

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
    OnboardingStep.SCAN -> ConnectivityPairingErrorUiModel(
        title = if (error.reason == ErrorReason.PERMISSION_DENIED) "需要蓝牙权限" else "未发现设备",
        description = error.message,
        primaryLabel = if (error.reason == ErrorReason.PERMISSION_DENIED) "授权并重试" else "重新扫描",
        secondaryLabel = "返回上一步",
        retryAction = ConnectivityPairingRetryAction.RETRY_SCAN
    )
    OnboardingStep.PROVISIONING -> ConnectivityPairingErrorUiModel(
        title = when (error.reason) {
            ErrorReason.DEVICE_NOT_FOUND -> "设备已不可用"
            ErrorReason.NETWORK_CHECK_FAILED -> "设备尚未上线"
            ErrorReason.WIFI_PROVISIONING_FAILED -> "配网失败"
            else -> "连接失败"
        },
        description = error.message,
        primaryLabel = if (error.reason == ErrorReason.DEVICE_NOT_FOUND) "重新扫描" else "重试配网",
        secondaryLabel = "返回上一步",
        retryAction = if (error.reason == ErrorReason.DEVICE_NOT_FOUND) ConnectivityPairingRetryAction.RETRY_SCAN else ConnectivityPairingRetryAction.RETRY_PROVISIONING
    )
    else -> ConnectivityPairingErrorUiModel("连接失败", error.message, "重试", "返回上一步", ConnectivityPairingRetryAction.RETRY_SCAN)
}

@Composable
internal fun PairingErrorStep(
    title: String,
    description: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusOrb(icon = Icons.Default.Warning, tint = OnboardingAmber, iconSize = 32)
        Spacer(Modifier.height(20.dp))
        Text(title, color = OnboardingText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = OnboardingErrorSurface,
            borderColor = OnboardingAmber.copy(alpha = 0.35f)
        ) {
            Text(text = description, color = OnboardingText, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryPillButton(primaryLabel, onPrimary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryPillButton(secondaryLabel, onSecondary, modifier = Modifier.fillMaxWidth())
        if (tertiaryLabel != null && onTertiary != null) {
            Spacer(Modifier.height(10.dp))
            QuietGhostButton(tertiaryLabel, onTertiary, modifier = Modifier.fillMaxWidth())
        }
    }
}
