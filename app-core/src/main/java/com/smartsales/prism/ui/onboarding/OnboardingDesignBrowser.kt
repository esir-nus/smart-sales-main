package com.smartsales.prism.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.ErrorReason
import com.smartsales.prism.domain.pairing.PairingState

private val DesignPreviewBadge = DiscoveredBadge(
    id = "FF:23:44:A1",
    name = "CHLE_Intelligent",
    signalStrengthDbm = -42
)

private enum class OnboardingDesignPreset(
    val label: String,
    val fullAppOnly: Boolean = false
) {
    WELCOME("Welcome", fullAppOnly = true),
    PERMISSIONS("Permissions"),
    HANDSHAKE("Handshake", fullAppOnly = true),
    HARDWARE_WAKE("Wake"),
    SCAN("Scan"),
    SCAN_TIMEOUT("Timeout"),
    SCAN_PERMISSION("Scan Permission"),
    DEVICE_FOUND("Found"),
    WIFI_FORM("Wi-Fi Form"),
    WIFI_PROGRESS("Wi-Fi Progress"),
    WIFI_FAILURE("Wi-Fi Failure"),
    COMPLETE("Complete");

    fun supports(host: OnboardingHost): Boolean = when {
        fullAppOnly -> host == OnboardingHost.FULL_APP
        else -> true
    }

    fun toState(host: OnboardingHost): OnboardingVisualCaptureState = when (this) {
        WELCOME -> OnboardingVisualCaptureState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.WELCOME,
            badge = null
        )

        PERMISSIONS -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.PERMISSIONS_PRIMER,
            badge = null
        )

        HANDSHAKE -> OnboardingVisualCaptureState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE,
            badge = null
        )

        HARDWARE_WAKE -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.HARDWARE_WAKE,
            badge = null
        )

        SCAN -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.SCAN,
            badge = null
        )

        SCAN_TIMEOUT -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.SCAN,
            pairingState = PairingState.Error(
                message = "未发现设备，请检查设备电源或蓝牙后重新扫描。",
                reason = ErrorReason.SCAN_TIMEOUT,
                canRetry = true
            ),
            badge = null
        )

        SCAN_PERMISSION -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.SCAN,
            permissionDenied = true,
            badge = null
        )

        DEVICE_FOUND -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.DEVICE_FOUND,
            badge = DesignPreviewBadge
        )

        WIFI_FORM -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.PROVISIONING,
            badge = DesignPreviewBadge,
            ssid = "Office_5G",
            password = "",
            showProvisioningForm = true
        )

        WIFI_PROGRESS -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.PROVISIONING,
            badge = DesignPreviewBadge,
            ssid = "Office_5G",
            password = "design-preview",
            pairingState = PairingState.Pairing(progress = 85),
            showProvisioningForm = false
        )

        WIFI_FAILURE -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.PROVISIONING,
            badge = DesignPreviewBadge,
            ssid = "Office_5G",
            password = "design-preview",
            pairingState = PairingState.Error(
                message = "设备尚未上线，请确认 Wi‑Fi 凭证后重试。",
                reason = ErrorReason.NETWORK_CHECK_FAILED,
                canRetry = true
            ),
            showProvisioningForm = false
        )

        COMPLETE -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.COMPLETE,
            badge = DesignPreviewBadge
        )
    }
}

@Composable
fun OnboardingDesignBrowser(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var host by remember { mutableStateOf(OnboardingHost.FULL_APP) }
    var preset by remember { mutableStateOf(OnboardingDesignPreset.WELCOME) }

    val availablePresets = remember(host) {
        OnboardingDesignPreset.entries.filter { it.supports(host) }
    }

    LaunchedEffect(host) {
        if (!preset.supports(host)) {
            preset = availablePresets.first()
        }
    }

    val previewState = remember(host, preset) { preset.toState(host) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Onboarding Design Browser",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Static review only — no gate, permission, or pairing side effects.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OnboardingDesignChipRow(
                label = "Host",
                options = OnboardingHost.entries,
                selected = host,
                labelFor = {
                    when (it) {
                        OnboardingHost.FULL_APP -> "Full App"
                        OnboardingHost.SIM_CONNECTIVITY -> "SIM"
                    }
                },
                onSelect = { host = it }
            )

            Spacer(Modifier.height(10.dp))

            OnboardingDesignChipRow(
                label = "State",
                options = availablePresets,
                selected = preset,
                labelFor = { it.label },
                onSelect = { preset = it }
            )

            Spacer(Modifier.height(16.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val widthBound: Dp = maxWidth - 12.dp
                val heightBound: Dp = maxHeight * (390f / 844f)
                val previewWidth: Dp = if (widthBound < heightBound) widthBound else heightBound
                Surface(
                    modifier = Modifier
                        .width(previewWidth)
                        .aspectRatio(390f / 844f)
                        .clip(RoundedCornerShape(34.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(34.dp)
                        ),
                    color = Color(0xFF05060A),
                    shape = RoundedCornerShape(34.dp)
                ) {
                    OnboardingStaticScreen(state = previewState)
                }
            }
        }
    }
}

@Composable
private fun <T> OnboardingDesignChipRow(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(labelFor(option)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color(0xFF05060A),
                        containerColor = Color.White.copy(alpha = 0.06f),
                        labelColor = Color.White.copy(alpha = 0.82f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = option == selected,
                        borderColor = Color.White.copy(alpha = 0.08f),
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}
