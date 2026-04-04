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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
    WELCOME("Welcome"),
    PERMISSIONS("Permissions"),
    CONSULTATION_IDLE("Consultation Idle"),
    CONSULTATION_RECORDING("Consultation Recording"),
    CONSULTATION_PROCESSING("Consultation Processing"),
    CONSULTATION_COMPLETE("Consultation Complete"),
    PROFILE_IDLE("Profile Idle"),
    PROFILE_RECORDING("Profile Recording"),
    PROFILE_PROCESSING("Profile Processing"),
    PROFILE("Profile"),
    QUICK_START_LIST("Quick Start List"),
    QUICK_START_APPEND("Quick Start Append"),
    QUICK_START_UPDATE("Quick Start Update"),
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
            host = host,
            step = OnboardingStep.WELCOME,
            badge = null
        )

        PERMISSIONS -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.PERMISSIONS_PRIMER,
            badge = null
        )

        CONSULTATION_IDLE -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.IDLE
        )

        CONSULTATION_RECORDING -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.RECORDING
        )

        CONSULTATION_PROCESSING -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.PROCESSING
        )

        CONSULTATION_COMPLETE -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.COMPLETE
        )

        PROFILE_IDLE -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.IDLE
        )

        PROFILE_RECORDING -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.RECORDING
        )

        PROFILE_PROCESSING -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.PROCESSING
        )

        PROFILE -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.EXTRACTED
        )

        QUICK_START_LIST -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.SCHEDULER_QUICK_START,
            badge = null,
            quickStartCaptureState = OnboardingQuickStartCaptureState.INITIAL_LIST
        )

        QUICK_START_APPEND -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.SCHEDULER_QUICK_START,
            badge = null,
            quickStartCaptureState = OnboardingQuickStartCaptureState.APPENDED
        )

        QUICK_START_UPDATE -> OnboardingVisualCaptureState(
            host = host,
            step = OnboardingStep.SCHEDULER_QUICK_START,
            badge = null,
            quickStartCaptureState = OnboardingQuickStartCaptureState.UPDATED
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
internal fun OnboardingStaticPreviewOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val reviewPages = remember {
        listOf(
            OnboardingDesignPreset.PERMISSIONS.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.CONSULTATION_COMPLETE.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.PROFILE.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.HARDWARE_WAKE.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.SCAN.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.DEVICE_FOUND.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.WIFI_FORM.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.COMPLETE.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.SCAN_TIMEOUT.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.SCAN_PERMISSION.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.WIFI_PROGRESS.toState(OnboardingHost.SIM_CONNECTIVITY),
            OnboardingDesignPreset.WIFI_FAILURE.toState(OnboardingHost.SIM_CONNECTIVITY)
        )
    }
    val pagerState = rememberPagerState(pageCount = { reviewPages.size })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingStaticScreen(
                state = reviewPages[page],
                onExit = onDismiss
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(reviewPages.size) { pageIndex ->
                val isSelected = pageIndex == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) {
                                Color.White.copy(alpha = 0.86f)
                            } else {
                                Color.White.copy(alpha = 0.24f)
                            }
                        )
                )
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
