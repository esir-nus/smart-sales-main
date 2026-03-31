package com.smartsales.prism.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.domain.pairing.DiscoveredBadge

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    pairingViewModel: PairingFlowViewModel = hiltViewModel(),
    interactionViewModel: OnboardingInteractionViewModel = hiltViewModel()
) {
    OnboardingCoordinator(
        host = OnboardingHost.FULL_APP,
        onComplete = onComplete,
        onExit = onComplete,
        exitPolicy = OnboardingExitPolicy.ALLOW_EXIT,
        pairingViewModel = pairingViewModel,
        interactionViewModel = interactionViewModel
    )
}

@Composable
fun OnboardingCoordinator(
    host: OnboardingHost,
    onComplete: () -> Unit,
    onExit: () -> Unit,
    exitPolicy: OnboardingExitPolicy = OnboardingExitPolicy.ALLOW_EXIT,
    pairingViewModel: PairingFlowViewModel = hiltViewModel(),
    interactionViewModel: OnboardingInteractionViewModel = hiltViewModel()
) {
    var currentStep by remember(host) { mutableStateOf(initialOnboardingStep(host)) }
    var discoveredBadge by remember(host) { mutableStateOf<DiscoveredBadge?>(null) }
    var wifiSsid by remember(host) { mutableStateOf("") }
    var wifiPassword by remember(host) { mutableStateOf("") }

    LaunchedEffect(host) {
        interactionViewModel.bindHost(host)
        interactionViewModel.resetInteractionState()
    }

    OnboardingFrame(
        host = host,
        currentStep = currentStep,
        exitPolicy = exitPolicy,
        onExit = {
            interactionViewModel.cancelActiveRecording()
            pairingViewModel.cancelPairing()
            if (host == OnboardingHost.SIM_CONNECTIVITY) {
                onExit()
            } else {
                onComplete()
            }
        }
    ) {
        when (it) {
            OnboardingStep.WELCOME -> WelcomeStep(
                onStart = { currentStep = nextOnboardingStep(it, host) }
            )

            OnboardingStep.PERMISSIONS_PRIMER -> PermissionsPrimerStep(
                onContinue = { currentStep = nextOnboardingStep(it, host) }
            )

            OnboardingStep.VOICE_HANDSHAKE_CONSULTATION -> VoiceHandshakeConsultationStep(
                viewModel = interactionViewModel,
                onContinue = { currentStep = nextOnboardingStep(it, host) }
            )

            OnboardingStep.VOICE_HANDSHAKE_PROFILE -> VoiceHandshakeProfileStep(
                viewModel = interactionViewModel,
                onContinue = { currentStep = nextOnboardingStep(it, host) }
            )

            OnboardingStep.HARDWARE_WAKE -> HardwareWakeStep(
                onContinue = { currentStep = nextOnboardingStep(it, host) }
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
                    currentStep = nextOnboardingStep(it, host)
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
                    currentStep = nextOnboardingStep(it, host)
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

@Composable
internal fun OnboardingStaticScreen(
    state: OnboardingVisualCaptureState,
    onExit: () -> Unit = {}
) {
    OnboardingFrame(
        host = state.host,
        currentStep = state.step,
        exitPolicy = OnboardingExitPolicy.ALLOW_EXIT,
        animateStepContent = false,
        onExit = onExit
    ) {
        when (it) {
            OnboardingStep.WELCOME -> WelcomeStep(onStart = {})
            OnboardingStep.PERMISSIONS_PRIMER -> PermissionsPrimerStep(onContinue = {})
            OnboardingStep.VOICE_HANDSHAKE_CONSULTATION -> VoiceHandshakeConsultationStaticStep(
                captureState = state.consultationCaptureState
            )
            OnboardingStep.VOICE_HANDSHAKE_PROFILE -> VoiceHandshakeProfileStaticStep(
                captureState = state.profileCaptureState
            )
            OnboardingStep.HARDWARE_WAKE -> HardwareWakeStep(onContinue = {})
            OnboardingStep.SCAN -> ScanStepContent(
                pairingState = state.pairingState,
                permissionDenied = state.permissionDenied,
                onRequestPermissions = {},
                onRetryScan = {},
                onCancel = {}
            )
            OnboardingStep.DEVICE_FOUND -> DeviceFoundStep(
                badge = state.badge,
                onRescan = {},
                onConnect = {}
            )
            OnboardingStep.PROVISIONING -> ProvisioningStepContent(
                pairingState = state.pairingState,
                badge = state.badge,
                ssid = state.ssid,
                password = state.password,
                showProvisioningForm = state.showProvisioningForm,
                onSsidChange = {},
                onPasswordChange = {},
                onBack = {},
                onRetryScan = {},
                onSubmit = {},
                onRetryProvisioning = {}
            )
            OnboardingStep.COMPLETE -> CompleteStep(
                host = state.host,
                onAcknowledge = {}
            )
        }
    }
}
