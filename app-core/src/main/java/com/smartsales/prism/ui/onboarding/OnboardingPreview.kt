package com.smartsales.prism.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.ErrorReason
import com.smartsales.prism.domain.pairing.PairingState

private val PreviewBadge = DiscoveredBadge(
    id = "88:7A:2D:4C:91:30",
    name = "SmartSales Badge",
    signalStrengthDbm = -54
)

private fun previewState(
    host: OnboardingHost,
    step: OnboardingStep,
    pairingState: PairingState = PairingState.Idle,
    badge: DiscoveredBadge? = PreviewBadge,
    ssid: String = "Office-WiFi",
    password: String = "wave-a-preview",
    permissionDenied: Boolean = false,
    showProvisioningForm: Boolean = true,
    consultationCaptureState: OnboardingConsultationCaptureState =
        OnboardingConsultationCaptureState.COMPLETE,
    profileCaptureState: OnboardingProfileCaptureState =
        OnboardingProfileCaptureState.EXTRACTED,
    quickStartCaptureState: OnboardingQuickStartCaptureState =
        OnboardingQuickStartCaptureState.UPDATED
): OnboardingVisualCaptureState = OnboardingVisualCaptureState(
    host = host,
    step = step,
    pairingState = pairingState,
    badge = badge,
    ssid = ssid,
    password = password,
    permissionDenied = permissionDenied,
    showProvisioningForm = showProvisioningForm,
    consultationCaptureState = consultationCaptureState,
    profileCaptureState = profileCaptureState,
    quickStartCaptureState = quickStartCaptureState
)

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 1. Welcome")
@Composable
fun PreviewOnboardingFullAppWelcome() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.WELCOME,
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 2. Permissions")
@Composable
fun PreviewOnboardingFullAppPermissionsPrimer() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.PERMISSIONS_PRIMER,
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 3A. Consultation Idle")
@Composable
fun PreviewOnboardingFullAppConsultationIdle() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.IDLE
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 3A. Consultation Recording")
@Composable
fun PreviewOnboardingFullAppConsultationRecording() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.RECORDING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 3A. Consultation Processing")
@Composable
fun PreviewOnboardingFullAppConsultationProcessing() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.PROCESSING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 3A. Consultation Complete")
@Composable
fun PreviewOnboardingFullAppConsultationComplete() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.COMPLETE
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 3B. Profile Recording")
@Composable
fun PreviewOnboardingFullAppProfileRecording() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.RECORDING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 3B. Profile Idle")
@Composable
fun PreviewOnboardingFullAppProfileIdle() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.IDLE
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 3B. Profile Processing")
@Composable
fun PreviewOnboardingFullAppProfileProcessing() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.PROCESSING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 3B. Profile Extracted")
@Composable
fun PreviewOnboardingFullAppProfileExtracted() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.EXTRACTED
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 4. Quick Start List")
@Composable
fun PreviewOnboardingFullAppQuickStartList() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.SCHEDULER_QUICK_START,
            badge = null,
            quickStartCaptureState = OnboardingQuickStartCaptureState.INITIAL_LIST
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 4. Quick Start Update")
@Composable
fun PreviewOnboardingFullAppQuickStartUpdate() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.SCHEDULER_QUICK_START,
            badge = null,
            quickStartCaptureState = OnboardingQuickStartCaptureState.UPDATED
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 5. Hardware Wake")
@Composable
fun PreviewOnboardingFullAppHardwareWake() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.HARDWARE_WAKE,
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 6. Scan")
@Composable
fun PreviewOnboardingFullAppScan() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.SCAN,
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 7. Device Found")
@Composable
fun PreviewOnboardingFullAppDeviceFound() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.DEVICE_FOUND
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 8. Provisioning Form")
@Composable
fun PreviewOnboardingFullAppProvisioningForm() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.PROVISIONING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Full App 9. Complete")
@Composable
fun PreviewOnboardingFullAppComplete() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.COMPLETE
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 1. Welcome")
@Composable
fun PreviewOnboardingSimWelcome() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.WELCOME,
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 2. Permissions")
@Composable
fun PreviewOnboardingSimPermissionsPrimer() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.PERMISSIONS_PRIMER,
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 3A. Consultation Idle")
@Composable
fun PreviewOnboardingSimConsultationIdle() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.IDLE
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 3A. Consultation Recording")
@Composable
fun PreviewOnboardingSimConsultationRecording() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.RECORDING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 3A. Consultation Processing")
@Composable
fun PreviewOnboardingSimConsultationProcessing() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.PROCESSING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 3A. Consultation Complete")
@Composable
fun PreviewOnboardingSimConsultationComplete() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.VOICE_HANDSHAKE_CONSULTATION,
            badge = null,
            consultationCaptureState = OnboardingConsultationCaptureState.COMPLETE
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 3B. Profile Recording")
@Composable
fun PreviewOnboardingSimProfileRecording() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.RECORDING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 3B. Profile Idle")
@Composable
fun PreviewOnboardingSimProfileIdle() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.IDLE
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 3B. Profile Processing")
@Composable
fun PreviewOnboardingSimProfileProcessing() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.PROCESSING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 3B. Profile Extracted")
@Composable
fun PreviewOnboardingSimProfileExtracted() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.VOICE_HANDSHAKE_PROFILE,
            badge = null,
            profileCaptureState = OnboardingProfileCaptureState.EXTRACTED
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 4. Hardware Wake")
@Composable
fun PreviewOnboardingSimHardwareWake() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.HARDWARE_WAKE,
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 5. Scan")
@Composable
fun PreviewOnboardingSimScan() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.SCAN,
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 6. Device Found")
@Composable
fun PreviewOnboardingSimDeviceFound() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.DEVICE_FOUND
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 7. Provisioning Form")
@Composable
fun PreviewOnboardingSimProvisioningForm() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.PROVISIONING
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "SIM 8. Complete")
@Composable
fun PreviewOnboardingSimComplete() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.SIM_CONNECTIVITY,
            step = OnboardingStep.COMPLETE
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Wave A Scan Failure")
@Composable
fun PreviewOnboardingScanFailure() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.SCAN,
            pairingState = PairingState.Error(
                message = "scan timeout",
                reason = ErrorReason.SCAN_TIMEOUT,
                canRetry = true
            ),
            badge = null
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Wave A Scan Permission Denied")
@Composable
fun PreviewOnboardingScanPermissionDenied() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.SCAN,
            badge = null,
            permissionDenied = true
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Wave A Provisioning Progress")
@Composable
fun PreviewOnboardingProvisioningProgress() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.PROVISIONING,
            pairingState = PairingState.Pairing(progress = 62),
            showProvisioningForm = false
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A, name = "Wave A Provisioning Failure")
@Composable
fun PreviewOnboardingProvisioningFailure() {
    OnboardingStaticScreen(
        state = previewState(
            host = OnboardingHost.FULL_APP,
            step = OnboardingStep.PROVISIONING,
            pairingState = PairingState.Error(
                message = "network check failed",
                reason = ErrorReason.NETWORK_CHECK_FAILED,
                canRetry = true
            ),
            showProvisioningForm = false
        )
    )
}
