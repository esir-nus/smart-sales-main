package com.smartsales.prism.ui.onboarding

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.ErrorReason
import com.smartsales.prism.domain.pairing.PairingState
import com.smartsales.prism.domain.pairing.WifiCredentials
import com.smartsales.prism.ui.components.VoiceHandshakeBars
import com.smartsales.prism.ui.components.prismTopSafeBandPadding
import com.smartsales.prism.ui.components.rememberVoiceHandshakeWaveProgress
import com.smartsales.prism.ui.components.prismTopSafeBandPadding
import com.smartsales.prism.ui.sim.SimSharedAuroraBackground
import kotlinx.coroutines.delay

private val OnboardingBackground = Color(0xFF05060A)
private val OnboardingCard = Color(0x14FFFFFF)
private val OnboardingCardBorder = Color(0x26FFFFFF)
private val OnboardingCardSoft = Color(0x12FFFFFF)
private val OnboardingMuted = Color(0xFF9CA3AF)
private val OnboardingText = Color(0xFFF3F7FF)
private val OnboardingBlue = Color(0xFF38BDF8)
private val OnboardingIndigo = Color(0xFF818CF8)
private val OnboardingMint = Color(0xFF34D399)
private val OnboardingAmber = Color(0xFFF59E0B)
private val OnboardingField = Color(0x0DFFFFFF)
private val OnboardingErrorSurface = Color(0x14F59E0B)
private val OnboardingPrimarySurface = Color.White
private val OnboardingPrimaryText = Color(0xFF05060A)
private val OnboardingLogoTile = Color(0xFF12161E)

internal const val ONBOARDING_MIC_BUTTON_TEST_TAG = "onboarding_mic_button"

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

internal data class OnboardingVisualCaptureState(
    val host: OnboardingHost,
    val step: OnboardingStep,
    val pairingState: PairingState = PairingState.Idle,
    val badge: DiscoveredBadge? = null,
    val ssid: String = "",
    val password: String = "",
    val permissionDenied: Boolean = false,
    val showProvisioningForm: Boolean = true,
    val consultationCaptureState: OnboardingConsultationCaptureState =
        OnboardingConsultationCaptureState.COMPLETE,
    val profileCaptureState: OnboardingProfileCaptureState =
        OnboardingProfileCaptureState.EXTRACTED
)

enum class OnboardingExitPolicy {
    ALLOW_EXIT,
    BLOCK_EXIT
}

internal fun shouldShowOnboardingExitAction(exitPolicy: OnboardingExitPolicy): Boolean {
    return exitPolicy == OnboardingExitPolicy.ALLOW_EXIT
}

internal fun shouldBlockOnboardingSystemBack(exitPolicy: OnboardingExitPolicy): Boolean {
    return exitPolicy == OnboardingExitPolicy.BLOCK_EXIT
}

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

@Composable
private fun OnboardingFrame(
    host: OnboardingHost,
    currentStep: OnboardingStep,
    exitPolicy: OnboardingExitPolicy,
    onExit: () -> Unit,
    animateStepContent: Boolean = true,
    content: @Composable (OnboardingStep) -> Unit
) {
    BackHandler(enabled = shouldBlockOnboardingSystemBack(exitPolicy)) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
    ) {
        OnboardingDarkSystemBarsEffect()

        SimSharedAuroraBackground(
            modifier = Modifier.fillMaxSize(),
            forceDarkPalette = true
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (shouldShowOnboardingExitAction(exitPolicy)) {
                TextButton(
                    onClick = onExit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .prismTopSafeBandPadding()
                ) {
                    Text(
                        text = if (host == OnboardingHost.SIM_CONNECTIVITY) "关闭" else "跳过",
                        color = OnboardingMuted
                    )
                }
            }

            if (animateStepContent) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280)) + slideInVertically(animationSpec = tween(280)) { it / 8 }) togetherWith
                            (fadeOut(animationSpec = tween(220)) + slideOutVertically(animationSpec = tween(220)) { -it / 10 })
                    },
                    label = "OnboardingCoordinator"
                ) { step ->
                    OnboardingStepContainer(
                        step = step,
                        content = content
                    )
                }
            } else {
                OnboardingStepContainer(
                    step = currentStep,
                    content = content
                )
            }
        }
    }
}

@Composable
private fun OnboardingDarkSystemBarsEffect() {
    val view = LocalView.current
    val activity = view.context.findComponentActivity() ?: return

    SideEffect {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}

@Composable
private fun OnboardingStepContainer(
    step: OnboardingStep,
    content: @Composable (OnboardingStep) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        content(step)
    }
}

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
    OnboardingStep.SCAN -> {
        val title = if (error.reason == ErrorReason.PERMISSION_DENIED) {
            "需要蓝牙权限"
        } else {
            "未发现设备"
        }
        val primaryLabel = if (error.reason == ErrorReason.PERMISSION_DENIED) {
            "授权并重试"
        } else {
            "重新扫描"
        }
        ConnectivityPairingErrorUiModel(
            title = title,
            description = error.message,
            primaryLabel = primaryLabel,
            secondaryLabel = "返回上一步",
            retryAction = ConnectivityPairingRetryAction.RETRY_SCAN
        )
    }

    OnboardingStep.PROVISIONING -> {
        val title = when (error.reason) {
            ErrorReason.DEVICE_NOT_FOUND -> "设备已不可用"
            ErrorReason.NETWORK_CHECK_FAILED -> "设备尚未上线"
            ErrorReason.WIFI_PROVISIONING_FAILED -> "配网失败"
            else -> "连接失败"
        }
        ConnectivityPairingErrorUiModel(
            title = title,
            description = error.message,
            primaryLabel = if (error.reason == ErrorReason.DEVICE_NOT_FOUND) "重新扫描" else "重试配网",
            secondaryLabel = "返回上一步",
            retryAction = if (error.reason == ErrorReason.DEVICE_NOT_FOUND) {
                ConnectivityPairingRetryAction.RETRY_SCAN
            } else {
                ConnectivityPairingRetryAction.RETRY_PROVISIONING
            }
        )
    }

    else -> ConnectivityPairingErrorUiModel(
        title = "连接失败",
        description = error.message,
        primaryLabel = "重试",
        secondaryLabel = "返回上一步",
        retryAction = ConnectivityPairingRetryAction.RETRY_SCAN
    )
}

@Composable
private fun StatusOrb(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = tint.copy(alpha = 0.08f),
    borderColor: Color = tint.copy(alpha = 0.20f),
    iconSize: Int = 32
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(OnboardingLogoTile)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("SS", color = OnboardingText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(28.dp))
        Text("SmartSales", color = OnboardingText, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "您的 AI 销售教练",
            color = OnboardingMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        PrimaryPillButton(
            text = "开启旅程",
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun PermissionsPrimerStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "需要一些权限",
            color = OnboardingText,
            fontSize = 33.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "为了提供完整体验，我们需要以下基础访问权限。系统会在您即将使用相关功能时，再按需发起授权请求。",
            color = OnboardingMuted,
            fontSize = 15.sp,
            lineHeight = 23.sp
        )
        Spacer(Modifier.height(30.dp))
        PermissionPrimerCard(
            icon = Icons.Default.Mic,
            title = "麦克风",
            description = "开始语音互动时请求，用于收音、转写和持续语音体验。"
        )
        Spacer(Modifier.height(16.dp))
        PermissionPrimerCard(
            icon = Icons.Default.Bluetooth,
            title = "蓝牙",
            description = "搜索并连接 SmartBadge 时请求，用于发现附近设备与建立连接。"
        )
        Spacer(Modifier.height(16.dp))
        PermissionPrimerCard(
            icon = Icons.Default.Alarm,
            title = "闹钟与提醒",
            description = "创建提醒与日程通知时使用，确保关键节点能按时触达。"
        )
        Spacer(Modifier.weight(1f))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.White.copy(alpha = 0.05f),
            borderColor = Color.White.copy(alpha = 0.10f)
        ) {
            Text(
                text = "说明：这一页只做提前说明，不会立刻弹出系统授权。后续在您准备使用相关功能时，Android 才会按需询问。",
                color = OnboardingMuted.copy(alpha = 0.96f),
                fontSize = 13.sp,
                lineHeight = 21.sp
            )
        }
        Spacer(Modifier.height(18.dp))
        PrimaryPillButton(
            text = "继续",
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun VoiceHandshakeConsultationStep(
    viewModel: OnboardingInteractionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.consultationState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onConsultationMicPermissionResult(granted)
    }
    ObserveOnboardingMicSession(
        viewModel = viewModel,
        shouldCancel = state.isRecording || state.awaitingMicPermission
    )

    VoiceHandshakeConsultationContent(
        state = state,
        onContinue = onContinue,
        onPressStart = {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMicPermission) {
                viewModel.onConsultationMicPermissionRequested()
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                false
            } else {
                viewModel.startConsultationRecording()
            }
        },
        onPressEnd = { viewModel.finishConsultationRecording() },
        onPressCancel = { viewModel.cancelActiveRecording() }
    )
}

@Composable
private fun VoiceHandshakeConsultationStaticStep(
    captureState: OnboardingConsultationCaptureState
) {
    VoiceHandshakeConsultationContent(
        state = consultationStateForCapture(captureState),
        onContinue = {},
        onPressStart = { false },
        onPressEnd = {},
        onPressCancel = {}
    )
}

@Composable
private fun VoiceHandshakeConsultationContent(
    state: OnboardingConsultationUiState,
    onContinue: () -> Unit,
    onPressStart: () -> Boolean,
    onPressEnd: () -> Unit,
    onPressCancel: () -> Unit
) {
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + if (state.isCompleted) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 18.dp,
                bottom = 200.dp
            )
        ) {
            item {
                AnimatedVisibility(
                    visible = !state.hasStartedInteracting,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TitleBlock(
                        title = "初次沟通体验",
                        subtitle = "按住下方麦克风，用真实语音试着开启一次销售咨询。"
                    )
                }
            }
            items(state.messages) { message ->
                OnboardingMessageBubble(message = message)
            }
            if (state.isCompleted) {
                item {
                    OnboardingSuccessNote(
                        title = "体验完成",
                        subtitle = "完美！这就是与 AI 沟通的方式。"
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.errorMessage?.let {
                OnboardingInlineNotice(text = it)
                Spacer(Modifier.height(12.dp))
            }
            if (state.isCompleted) {
                PrimaryPillButton(
                    text = "继续下一步",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OnboardingMicFooter(
                    isRecording = state.isRecording,
                    isProcessing = state.isProcessing,
                    interactionMode = state.micInteractionMode,
                    handshakeHint = "试试说：“帮我搞定这个客户”",
                    processingLabel = consultationProcessingLabel(state.processingPhase),
                    onPressStart = onPressStart,
                    onPressEnd = onPressEnd,
                    onPressCancel = onPressCancel
                )
            }
        }
    }
}

@Composable
private fun VoiceHandshakeProfileStep(
    viewModel: OnboardingInteractionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.profileState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onProfileMicPermissionResult(granted)
    }
    ObserveOnboardingMicSession(
        viewModel = viewModel,
        shouldCancel = state.isRecording || state.awaitingMicPermission
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is OnboardingInteractionEffect.AdvanceProfileStep) {
                onContinue()
            }
        }
    }

    VoiceHandshakeProfileContent(
        state = state,
        onSave = { viewModel.saveProfileDraft() },
        onSkip = { viewModel.skipProfileSave() },
        onPressStart = {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMicPermission) {
                viewModel.onProfileMicPermissionRequested()
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                false
            } else {
                viewModel.startProfileRecording()
            }
        },
        onPressEnd = { viewModel.finishProfileRecording() },
        onPressCancel = { viewModel.cancelActiveRecording() }
    )
}

@Composable
private fun VoiceHandshakeProfileStaticStep(
    captureState: OnboardingProfileCaptureState
) {
    VoiceHandshakeProfileContent(
        state = profileStateForCapture(captureState),
        onSave = {},
        onSkip = {},
        onPressStart = { false },
        onPressEnd = {},
        onPressCancel = {}
    )
}

@Composable
private fun VoiceHandshakeProfileContent(
    state: OnboardingProfileUiState,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    onPressStart: () -> Boolean,
    onPressEnd: () -> Unit,
    onPressCancel: () -> Unit
) {
    val listState = rememberLazyListState()
    val itemCount = listOfNotNull(
        if (state.transcript.isNotBlank()) state.transcript else null,
        if (state.acknowledgement.isNotBlank()) state.acknowledgement else null,
        if (state.draft != null) "card" else null
    ).size + 1
    LaunchedEffect(itemCount) {
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 18.dp,
                bottom = 200.dp
            )
        ) {
            item {
                AnimatedVisibility(
                    visible = !state.hasStartedInteracting,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TitleBlock(
                        title = "认识一下您",
                        subtitle = "为了更好地配合您，请简单聊聊您的角色、行业或经验。"
                    )
                }
            }
            if (state.transcript.isNotBlank()) {
                item {
                    OnboardingMessageBubble(
                        message = OnboardingInteractionMessage(
                            role = OnboardingMessageRole.USER,
                            text = state.transcript
                        )
                    )
                }
            }
            if (state.acknowledgement.isNotBlank()) {
                item {
                    OnboardingMessageBubble(
                        message = OnboardingInteractionMessage(
                            role = OnboardingMessageRole.AI,
                            text = state.acknowledgement
                        )
                    )
                }
            }
            state.draft?.let { draft ->
                item {
                    OnboardingProfileExtractionCard(draft = draft)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.errorMessage?.let {
                OnboardingInlineNotice(text = it)
                Spacer(Modifier.height(12.dp))
            }
            when {
                state.hasExtractionResult -> {
                    PrimaryPillButton(
                        text = if (state.isSaving) "正在保存..." else "继续下一步",
                        onClick = onSave,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.canSkipAfterFailure) {
                        Spacer(Modifier.height(10.dp))
                        QuietGhostButton(
                            text = "跳过保存，继续下一步",
                            onClick = onSkip,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    OnboardingMicFooter(
                        isRecording = state.isRecording,
                        isProcessing = state.isProcessing,
                        interactionMode = state.micInteractionMode,
                        handshakeHint = "试试说：“我是王经理...”",
                        processingLabel = profileProcessingLabel(state.processingPhase),
                        onPressStart = onPressStart,
                        onPressEnd = onPressEnd,
                        onPressCancel = onPressCancel
                    )
                    if (state.canSkipAfterFailure) {
                        Spacer(Modifier.height(10.dp))
                        QuietGhostButton(
                            text = "跳过保存，继续下一步",
                            onClick = onSkip,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingMessageBubble(message: OnboardingInteractionMessage) {
    val isUser = message.role == OnboardingMessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        FrostedCard(
            modifier = Modifier.fillMaxWidth(0.86f),
            containerColor = if (isUser) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color(0x141A2337)
            },
            borderColor = if (isUser) {
                Color.White.copy(alpha = 0.15f)
            } else {
                OnboardingBlue.copy(alpha = 0.24f)
            }
        ) {
            Column {
                if (!isUser) {
                    Text(
                        text = "AI 助手",
                        color = OnboardingBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = message.text,
                    color = OnboardingText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingProfileExtractionCard(draft: OnboardingProfileDraft) {
    FrostedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = OnboardingCard,
        borderColor = OnboardingCardBorder
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = OnboardingBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "信息已提取",
                    color = OnboardingBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(12.dp))
            OnboardingExtractionRow("称呼", draft.displayName.ifBlank { "未识别" })
            OnboardingExtractionRow("角色", draft.role.ifBlank { "未识别" })
            OnboardingExtractionRow("行业领域", draft.industry.ifBlank { "未识别" })
            OnboardingExtractionRow("从业经验", draft.experienceYears.ifBlank { "未识别" })
            OnboardingExtractionRow("偏好联系", draft.communicationPlatform.ifBlank { "未识别" }, isLast = true)
        }
    }
}

@Composable
private fun OnboardingExtractionRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = OnboardingMuted,
                fontSize = 14.sp
            )
            Text(
                text = value,
                color = OnboardingText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (!isLast) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OnboardingSuccessNote(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusOrb(
            icon = Icons.Default.CheckCircle,
            tint = OnboardingMint,
            modifier = Modifier.size(64.dp),
            iconSize = 28
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = OnboardingText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = OnboardingMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingInlineNotice(text: String) {
    FrostedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = OnboardingErrorSurface,
        borderColor = OnboardingAmber.copy(alpha = 0.28f)
    ) {
        Text(
            text = text,
            color = OnboardingAmber,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun OnboardingMicFooter(
    isRecording: Boolean,
    isProcessing: Boolean,
    interactionMode: OnboardingMicInteractionMode,
    handshakeHint: String,
    processingLabel: String,
    onPressStart: () -> Boolean,
    onPressEnd: () -> Unit,
    onPressCancel: () -> Unit
) {
    val currentIsRecording by rememberUpdatedState(isRecording)
    val currentIsProcessing by rememberUpdatedState(isProcessing)
    val currentInteractionMode by rememberUpdatedState(interactionMode)
    val currentOnPressStart by rememberUpdatedState(onPressStart)
    val currentOnPressEnd by rememberUpdatedState(onPressEnd)
    val currentOnPressCancel by rememberUpdatedState(onPressCancel)
    var handshakeVisible by remember { mutableStateOf(false) }
    val motion = rememberInfiniteTransition(label = "onboardingMicMotion")
    val waveProgress = rememberVoiceHandshakeWaveProgress(
        isRecording = isRecording,
        labelPrefix = "onboarding"
    )
    val rippleProgress by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onboardingMicRippleProgress"
    )
    val showHandshake = handshakeVisible || isRecording || isProcessing

    LaunchedEffect(Unit) {
        delay(600)
        handshakeVisible = true
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = showHandshake,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OnboardingVoiceHandshake(
                    isRecording = isRecording,
                    waveProgress = waveProgress
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = handshakeHint,
                    color = if (isRecording) OnboardingBlue.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(if (showHandshake) 18.dp else 0.dp))
        Box(
            modifier = Modifier.size(86.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(1f + (1.08f * rippleProgress))
                        .alpha(0.8f - (0.8f * rippleProgress))
                        .clip(CircleShape)
                        .background(OnboardingBlue.copy(alpha = 0.4f))
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRecording -> OnboardingBlue.copy(alpha = 0.18f)
                            isProcessing -> OnboardingBlue.copy(alpha = 0.12f)
                            else -> Color.White.copy(alpha = 0.08f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isRecording -> OnboardingBlue.copy(alpha = 0.48f)
                            isProcessing -> OnboardingBlue.copy(alpha = 0.28f)
                            else -> Color.White.copy(alpha = 0.15f)
                        },
                        shape = CircleShape
                    )
                    .testTag(ONBOARDING_MIC_BUTTON_TEST_TAG)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (currentIsProcessing) return@detectTapGestures
                                if (
                                    currentIsRecording &&
                                    currentInteractionMode == OnboardingMicInteractionMode.TAP_TO_SEND
                                ) {
                                    val released = tryAwaitRelease()
                                    if (released) {
                                        currentOnPressEnd()
                                    } else {
                                        currentOnPressCancel()
                                    }
                                    return@detectTapGestures
                                }
                                val started = currentOnPressStart()
                                if (!started) return@detectTapGestures
                                val released = tryAwaitRelease()
                                if (released) {
                                    currentOnPressEnd()
                                } else {
                                    currentOnPressCancel()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isRecording || isProcessing) OnboardingBlue else OnboardingText,
                    modifier = Modifier.size(if (isRecording) 30.dp else 28.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                isRecording && interactionMode == OnboardingMicInteractionMode.TAP_TO_SEND -> "正在聆听...点击发送"
                isRecording -> "正在聆听...松开发送"
                isProcessing -> processingLabel
                else -> "按住说话"
            },
            color = if (isRecording || isProcessing) OnboardingBlue else OnboardingMuted,
            fontSize = 13.sp,
            fontWeight = if (isRecording || isProcessing) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun OnboardingVoiceHandshake(
    isRecording: Boolean,
    waveProgress: Float
) {
    VoiceHandshakeBars(
        isRecording = isRecording,
        waveProgress = waveProgress,
        barColor = OnboardingBlue
    )
}

@Composable
private fun ObserveOnboardingMicSession(
    viewModel: OnboardingInteractionViewModel,
    shouldCancel: Boolean
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentShouldCancel by rememberUpdatedState(shouldCancel)
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && currentShouldCancel) {
                viewModel.cancelActiveRecording()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (currentShouldCancel) {
                viewModel.cancelActiveRecording()
            }
        }
    }
}

private fun consultationStateForCapture(
    captureState: OnboardingConsultationCaptureState
): OnboardingConsultationUiState = when (captureState) {
    OnboardingConsultationCaptureState.IDLE -> OnboardingConsultationUiState()
    OnboardingConsultationCaptureState.RECORDING -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        isRecording = true
    )
    OnboardingConsultationCaptureState.PROCESSING -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        isProcessing = true,
        processingPhase = OnboardingProcessingPhase.RECOGNIZING
    )
    OnboardingConsultationCaptureState.ONE_TURN_REVEALED -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        messages = listOf(
            OnboardingInteractionMessage(OnboardingMessageRole.USER, "帮我搞定这个客户"),
            OnboardingInteractionMessage(OnboardingMessageRole.AI, "好的，先告诉我这位客户的情况，遇到了什么卡点？")
        ),
        completedRounds = 1
    )
    OnboardingConsultationCaptureState.COMPLETE -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        messages = listOf(
            OnboardingInteractionMessage(OnboardingMessageRole.USER, "帮我搞定这个客户"),
            OnboardingInteractionMessage(OnboardingMessageRole.AI, "好的，先告诉我这位客户的情况，遇到了什么卡点？"),
            OnboardingInteractionMessage(OnboardingMessageRole.USER, "客户觉得我们的价格比竞品高，预算一直批不下来"),
            OnboardingInteractionMessage(OnboardingMessageRole.AI, "这个阶段可以尝试分享一些同行的成功案例作为切入点，证明长期 ROI。您做得很好！")
        ),
        completedRounds = 2
    )
    OnboardingConsultationCaptureState.ERROR -> OnboardingConsultationUiState(
        hasStartedInteracting = true,
        errorMessage = "网络连接波动，请重试"
    )
}

private fun profileStateForCapture(
    captureState: OnboardingProfileCaptureState
): OnboardingProfileUiState = when (captureState) {
    OnboardingProfileCaptureState.IDLE -> OnboardingProfileUiState()
    OnboardingProfileCaptureState.RECORDING -> OnboardingProfileUiState(
        hasStartedInteracting = true,
        isRecording = true
    )
    OnboardingProfileCaptureState.PROCESSING -> OnboardingProfileUiState(
        hasStartedInteracting = true,
        isProcessing = true,
        processingPhase = OnboardingProcessingPhase.RECOGNIZING
    )
    OnboardingProfileCaptureState.EXTRACTED -> OnboardingProfileUiState(
        hasStartedInteracting = true,
        transcript = "我是王经理，做 SaaS 软件销售总监已经 8 年了。平时主要用微信和电话联系客户。",
        acknowledgement = "谢谢您的分享，我已经为您建立好了专属档案。",
        draft = OnboardingProfileDraft(
            displayName = "王经理",
            role = "销售总监",
            industry = "SaaS 软件服务",
            experienceYears = "8年",
            communicationPlatform = "微信 / 电话"
        )
    )
    OnboardingProfileCaptureState.ERROR -> OnboardingProfileUiState(
        hasStartedInteracting = true,
        errorMessage = "资料提取结果暂时不可用，请重试。"
    )
}

private fun consultationProcessingLabel(phase: OnboardingProcessingPhase): String {
    return when (phase) {
        OnboardingProcessingPhase.RECOGNIZING -> "正在识别语音..."
        OnboardingProcessingPhase.BUILDING_CONSULTATION_REPLY -> "正在整理建议..."
        OnboardingProcessingPhase.DETERMINISTIC_FALLBACK -> "正在准备体验..."
        else -> "正在整理建议..."
    }
}

private fun profileProcessingLabel(phase: OnboardingProcessingPhase): String {
    return when (phase) {
        OnboardingProcessingPhase.RECOGNIZING -> "正在识别语音..."
        OnboardingProcessingPhase.BUILDING_PROFILE_RESULT -> "正在整理资料..."
        OnboardingProcessingPhase.DETERMINISTIC_FALLBACK -> "正在准备资料..."
        else -> "正在整理资料..."
    }
}

@Composable
private fun HardwareWakeStep(onContinue: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "hardwareWake")
    val glow by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hardwareGlow"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "唤醒您的 SmartBadge",
                color = OnboardingText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "长按中间按钮 3 秒，直到中央蓝灯开始呼吸闪烁。",
                color = OnboardingMuted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(28.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = OnboardingCardSoft,
            borderColor = Color.White.copy(alpha = 0.12f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(228.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0x100D1118)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(156.dp)
                            .clip(RoundedCornerShape(38.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.07f),
                                        Color.White.copy(alpha = 0.025f)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(38.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .alpha(glow)
                                    .clip(CircleShape)
                                    .background(OnboardingBlue)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "3 秒中心长按",
                    color = OnboardingMuted.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        PrimaryPillButton(
            text = "蓝灯已经在闪了",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScanStep(
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
        }
    }
    var permissionsGranted by remember { mutableStateOf(permissions.isEmpty()) }
    var permissionDenied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
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
        if (discoveredBadge != null) {
            onFound(discoveredBadge)
        }
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
private fun ScanStepContent(
    pairingState: PairingState,
    permissionDenied: Boolean,
    onRequestPermissions: () -> Unit,
    onRetryScan: () -> Unit,
    onCancel: () -> Unit
) {
    when {
        permissionDenied -> {
            PairingErrorStep(
                title = "需要蓝牙权限",
                description = "请授权蓝牙搜索与连接后继续扫描。",
                primaryLabel = "授权并重试",
                onPrimary = onRequestPermissions,
                secondaryLabel = "返回上一步",
                onSecondary = onCancel
            )
        }

        pairingState is PairingState.Error -> {
            val presentation = resolveConnectivityPairingErrorUiModel(
                OnboardingStep.SCAN,
                pairingState
            )
            PairingErrorStep(
                title = presentation.title,
                description = presentation.description,
                primaryLabel = presentation.primaryLabel,
                onPrimary = onRetryScan,
                secondaryLabel = presentation.secondaryLabel,
                onSecondary = onCancel
            )
        }

        else -> {
            val transition = rememberInfiniteTransition(label = "scanRadar")
            val pulse by transition.animateFloat(
                initialValue = 0.92f,
                targetValue = 2.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scanPulse"
            )
            val pulseAlpha by transition.animateFloat(
                initialValue = 0.75f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scanPulseAlpha"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "正在搜索设备",
                    color = OnboardingText,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "保持徽章靠近手机，连接将始终要求您手动确认。",
                    color = OnboardingMuted,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(42.dp))
                Box(
                    modifier = Modifier
                        .size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(172.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = OnboardingBlue.copy(alpha = 0.18f),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(172.dp)
                            .scale(pulse)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(OnboardingBlue.copy(alpha = 0.09f))
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.015f))
                            .border(1.dp, OnboardingBlue.copy(alpha = 0.08f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(OnboardingBlue)
                    )
                }
                Spacer(Modifier.height(46.dp))
                QuietGhostButton(
                    text = "取消",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DeviceFoundStep(
    badge: DiscoveredBadge?,
    onRescan: () -> Unit,
    onConnect: () -> Unit
) {
    if (badge == null) {
        PairingErrorStep(
            title = "设备已不可用",
            description = "请重新搜索设备后继续。",
            primaryLabel = "重新扫描",
            onPrimary = onRescan,
            secondaryLabel = "返回上一步",
            onSecondary = onRescan
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleBlock(
            title = "发现设备",
            subtitle = "系统不会自动连接，必须由您手动点按设备卡片确认。"
        )
        Spacer(Modifier.height(26.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = OnboardingBlue.copy(alpha = 0.05f),
            borderColor = OnboardingBlue.copy(alpha = 0.20f)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OnboardingBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = OnboardingBlue
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.name,
                            color = OnboardingText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "ID ${badge.id}",
                            color = OnboardingMuted,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "${badge.signalStrengthDbm} dBm",
                        color = OnboardingBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(18.dp))
                PrimaryPillButton(
                    text = "连接",
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                SecondaryPillButton(
                    text = "重新扫描",
                    onClick = onRescan,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProvisioningStep(
    viewModel: PairingFlowViewModel,
    badge: DiscoveredBadge?,
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onRetryScan: () -> Unit,
    onComplete: () -> Unit
) {
    val pairingState by viewModel.pairingState.collectAsState()
    var showProvisioningForm by remember(badge?.id) { mutableStateOf(true) }

    if (badge == null) {
        PairingErrorStep(
            title = "缺少目标设备",
            description = "请回到扫描页重新选择设备。",
            primaryLabel = "重新扫描",
            onPrimary = onRetryScan,
            secondaryLabel = "返回上一步",
            onSecondary = onBack
        )
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
        onSubmit = {
            showProvisioningForm = false
            viewModel.pairBadge(
                badge = badge,
                wifiCreds = WifiCredentials(ssid, password)
            )
        },
        onRetryProvisioning = {
            showProvisioningForm = true
        }
    )
}

@Composable
private fun ProvisioningStepContent(
    pairingState: PairingState,
    badge: DiscoveredBadge?,
    ssid: String,
    password: String,
    showProvisioningForm: Boolean,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onRetryScan: () -> Unit,
    onSubmit: () -> Unit,
    onRetryProvisioning: () -> Unit
) {
    if (badge == null) {
        PairingErrorStep(
            title = "缺少目标设备",
            description = "请回到扫描页重新选择设备。",
            primaryLabel = "重新扫描",
            onPrimary = onRetryScan,
            secondaryLabel = "返回上一步",
            onSecondary = onBack
        )
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
                    onSubmit = onSubmit
                )
            } else {
                val presentation = resolveConnectivityPairingErrorUiModel(
                    OnboardingStep.PROVISIONING,
                    pairingState
                )
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
                    onSecondary = onBack
                )
            }
        }

        is PairingState.Pairing,
        is PairingState.Success -> {
            val progress = when (pairingState) {
                is PairingState.Pairing -> pairingState.progress / 100f
                is PairingState.Success -> 1f
                else -> 0f
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusOrb(
                    icon = Icons.Default.Bluetooth,
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
                        Text(
                            text = "设备：${badge.name}",
                            color = OnboardingText,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (pairingState is PairingState.Success) OnboardingMint else OnboardingBlue,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (pairingState is PairingState.Success) {
                                "100%"
                            } else {
                                "${(progress * 100).toInt()}%"
                            },
                            color = OnboardingText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (pairingState is PairingState.Success) {
                                "设备已完成上线校验。"
                            } else {
                                "请保持设备通电，并保持手机与设备靠近。"
                            },
                            color = OnboardingMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        else -> {
            ProvisioningForm(
                ssid = ssid,
                password = password,
                onSsidChange = onSsidChange,
                onPasswordChange = onPasswordChange,
                onBack = onBack,
                onSubmit = onSubmit
            )
        }
    }
}

@Composable
private fun ProvisioningForm(
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleBlock(
            title = "配置网络",
            subtitle = "输入 Wi‑Fi 信息后，设备会完成写入、联网检查与最终准备。"
        )
        Spacer(Modifier.height(28.dp))
        FrostedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                OnboardingTextField(
                    value = ssid,
                    onValueChange = onSsidChange,
                    label = "Wi‑Fi 名称"
                )
                Spacer(Modifier.height(16.dp))
                OnboardingTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "密码",
                    isPassword = true
                )
                Spacer(Modifier.height(20.dp))
                PrimaryPillButton(
                    text = "开始写入",
                    onClick = onSubmit,
                    enabled = ssid.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                SecondaryPillButton(
                    text = "返回设备卡片",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CompleteStep(
    host: OnboardingHost,
    onAcknowledge: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusOrb(
            icon = Icons.Default.CheckCircle,
            tint = OnboardingMint,
            modifier = Modifier.size(80.dp),
            iconSize = 36
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "一切就绪！",
            color = OnboardingText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (host == OnboardingHost.SIM_CONNECTIVITY) {
                "连接已完成，下一步进入设备连接管理。"
            } else {
                "欢迎进入 SmartSales 主界面。"
            },
            color = OnboardingMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = if (host == OnboardingHost.FULL_APP) {
                OnboardingBlue.copy(alpha = 0.05f)
            } else {
                OnboardingCard
            },
            borderColor = if (host == OnboardingHost.FULL_APP) {
                OnboardingBlue.copy(alpha = 0.20f)
            } else {
                OnboardingCardBorder
            }
        ) {
            Text(
                text = if (host == OnboardingHost.FULL_APP) {
                    "FULL APP HOST"
                } else {
                    "SIM CONNECTIVITY HOST"
                },
                color = OnboardingMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (host == OnboardingHost.SIM_CONNECTIVITY) {
                    "完成当前配对后，流转进入连接管理稳态。"
                } else {
                    "完成当前引导后，流转进入 SmartSales 主界面。"
                },
                color = OnboardingText,
                lineHeight = 22.sp
            )
        }
        Spacer(Modifier.height(36.dp))
        PrimaryPillButton(
            text = if (host == OnboardingHost.SIM_CONNECTIVITY) "进入连接管理" else "进入首页",
            onClick = onAcknowledge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PairingErrorStep(
    title: String,
    description: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusOrb(
            icon = Icons.Default.Warning,
            tint = OnboardingAmber,
            iconSize = 32
        )
        Spacer(Modifier.height(20.dp))
        Text(title, color = OnboardingText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        FrostedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = OnboardingErrorSurface,
            borderColor = OnboardingAmber.copy(alpha = 0.35f)
        ) {
            Text(
                text = description,
                color = OnboardingText,
                lineHeight = 22.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        PrimaryPillButton(
            text = primaryLabel,
            onClick = onPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        SecondaryPillButton(
            text = secondaryLabel,
            onClick = onSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionPrimerCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(30.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OnboardingText,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = OnboardingText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = description,
                        color = OnboardingMuted,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBlock(
    title: String,
    subtitle: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = OnboardingText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = subtitle,
            color = OnboardingMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun FrostedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = OnboardingCard,
    borderColor: Color = OnboardingCardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = OnboardingPrimarySurface,
    textColor: Color = OnboardingPrimaryText,
    borderColor: Color? = null
) {
    Surface(
        modifier = modifier,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.45f),
        shape = RoundedCornerShape(999.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = if (borderColor != null) 1.dp else 0.dp,
                    color = borderColor ?: Color.Transparent,
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 20.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(999.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, OnboardingCardBorder, RoundedCornerShape(999.dp))
                .padding(horizontal = 20.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = OnboardingText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuietGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(999.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                .padding(horizontal = 20.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = OnboardingText.copy(alpha = 0.88f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SecondaryNote(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.10f)
        )
        Text(
            text = text,
            color = OnboardingMuted,
            fontSize = 12.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.10f)
        )
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = OnboardingMuted) },
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OnboardingField,
            unfocusedContainerColor = OnboardingField,
            focusedBorderColor = OnboardingBlue,
            unfocusedBorderColor = OnboardingCardBorder,
            focusedTextColor = OnboardingText,
            unfocusedTextColor = OnboardingText,
            cursorColor = OnboardingBlue,
            focusedLabelColor = OnboardingBlue,
            unfocusedLabelColor = OnboardingMuted
        ),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }
    )
}
