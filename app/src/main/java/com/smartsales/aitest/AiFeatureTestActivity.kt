package com.smartsales.aitest

// 文件：app/src/main/java/com/smartsales/aitest/AiFeatureTestActivity.kt
// 模块：:app
// 说明：AI 功能、媒体与配网测试壳 Activity
// 作者：创建于 2025-11-20

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.smartsales.aitest.audio.AudioFilesRoute
import com.smartsales.aitest.devicemanager.DeviceManagerRoute
import com.smartsales.aitest.setup.DeviceSetupRoute
import com.smartsales.aitest.usercenter.UserCenterRoute
import com.smartsales.aitest.ui.theme.AppTheme
import com.smartsales.core.util.Result
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.DeviceConnectionStateUi
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeScreenRoute
import com.smartsales.feature.chat.home.HomeViewModel
import com.smartsales.feature.chat.home.TranscriptionChatRequest
import com.smartsales.feature.chat.history.ChatHistoryRoute
import com.smartsales.feature.media.audio.AudioFilesEvent
import com.smartsales.feature.media.audio.AudioFilesViewModel
import com.smartsales.core.util.AppDesignTokens
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Divider
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt
import com.smartsales.aitest.onboarding.OnboardingGateViewModel
import com.smartsales.aitest.onboarding.OnboardingPersonalInfoScreen
import com.smartsales.aitest.onboarding.OnboardingTestTags
import com.smartsales.aitest.onboarding.OnboardingUiState
import com.smartsales.aitest.onboarding.OnboardingViewModel
import com.smartsales.aitest.onboarding.OnboardingWelcomeScreen

@AndroidEntryPoint
class AiFeatureTestActivity : ComponentActivity() {
    @VisibleForTesting
    val overlayTestState: MutableStateFlow<TestHomePage?> = MutableStateFlow(null)

    @VisibleForTesting
    fun setOverlayForTest(page: TestHomePage) {
        overlayTestState.value = page
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiFeatureTestApp(overlayTestFlow = overlayTestState)
        }
    }
}

@Composable
private fun AiFeatureTestApp(
    overlayTestFlow: MutableStateFlow<TestHomePage?>? = null
) {
    val context = LocalContext.current
    val mediaServerClient = remember { MediaServerClient(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle
    val scope = lifecycleOwner.lifecycleScope
    val homeViewModel: HomeViewModel = hiltViewModel()
    val audioFilesViewModel: AudioFilesViewModel = hiltViewModel()
    var currentPage by rememberSaveable { mutableStateOf(TestHomePage.Home) }
    var latestDeviceSnapshot by remember { mutableStateOf<DeviceSnapshotUi?>(null) }
    var pendingSessionId by remember { mutableStateOf<String?>(null) }
    var overlayState by rememberSaveable { mutableStateOf(HomeOverlay.Home) }
    val goHome: () -> Unit = {
        currentPage = TestHomePage.Home
    }
    val overlayTestOverride = overlayTestFlow?.collectAsState()?.value
    LaunchedEffect(overlayTestOverride) {
        overlayTestOverride?.let { page ->
            when (page) {
                TestHomePage.AudioFiles -> {
                    currentPage = TestHomePage.Home
                    overlayState = HomeOverlay.Audio
                }
                TestHomePage.DeviceManager -> {
                    currentPage = TestHomePage.Home
                    overlayState = HomeOverlay.Device
                }
                else -> {
                    currentPage = page
                    if (page != TestHomePage.Home) {
                        overlayState = HomeOverlay.Home
                    }
                }
            }
            overlayTestFlow.value = null
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { granted -> !granted }) {
            // 使用 lifecycle state guard 防止在 Activity 销毁后更新 UI
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                Log.w("AiFeatureTestActivity", "忽略 snackbar：Activity 已销毁")
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                try {
                    snackbarHostState.showSnackbar("缺少 BLE 或定位权限，无法扫描 CHLE。")
                } catch (e: Exception) {
                    // 忽略已取消的协程异常，避免崩溃
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val missingPermissions = REQUIRED_BLE_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    LaunchedEffect(audioFilesViewModel) {
        audioFilesViewModel.events.collect { event ->
            when (event) {
                is AudioFilesEvent.TranscriptReady -> {
                    val sessionId = event.sessionId
                    homeViewModel.onTranscriptionRequested(
                        TranscriptionChatRequest(
                            jobId = event.jobId,
                            fileName = event.fileName,
                            recordingId = event.recordingId,
                            sessionId = sessionId,
                            transcriptPreview = event.transcriptPreview,
                            transcriptMarkdown = event.fullTranscriptMarkdown,
                            isFromCache = event.isFromCache
                        )
                    )
                    pendingSessionId = sessionId
                }
            }
        }
    }

    fun setPage(page: TestHomePage) {
        currentPage = page
        if (page != TestHomePage.Home) {
            overlayState = HomeOverlay.Home
        }
    }

    fun openDeviceSection(forceSetup: Boolean = false) {
        val destination = when {
            forceSetup -> TestHomePage.DeviceSetup
            latestDeviceSnapshot?.connectionState == DeviceConnectionStateUi.CONNECTED -> TestHomePage.DeviceManager
            else -> TestHomePage.DeviceSetup
        }
        setPage(destination)
        overlayState = if (destination == TestHomePage.DeviceManager) HomeOverlay.Device else HomeOverlay.Home
    }

    BackHandler(enabled = currentPage != TestHomePage.Home || overlayState != HomeOverlay.Home) {
        if (overlayState != HomeOverlay.Home) {
            overlayState = HomeOverlay.Home
        } else {
            goHome()
        }
    }

    val onboardingGateViewModel: OnboardingGateViewModel = hiltViewModel()
    val onboardingCompleted by onboardingGateViewModel.completed.collectAsState()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingUiState by onboardingViewModel.uiState.collectAsState()

    AppTheme {
        val designTokens = AppDesignTokens.current()
        val showSnackbar: (String) -> Unit = { message ->
            // 使用 lifecycle state guard 防止在 Activity 销毁后更新 UI
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                scope.launch {
                    try {
                        snackbarHostState.showSnackbar(message)
                    } catch (e: Exception) {
                        // 忽略已取消的协程异常，避免 Handler 线程崩溃
                    }
                }
            } else {
                Log.w("AiFeatureTestActivity", "忽略 snackbar：Activity 已销毁 - $message")
            }
        }
        if (false && !onboardingCompleted) {
            currentPage = TestHomePage.Home
            overlayState = HomeOverlay.Home
            OnboardingHost(
                uiState = onboardingUiState,
                onDisplayNameChange = onboardingViewModel::onDisplayNameChange,
                onRoleChange = onboardingViewModel::onRoleChange,
                onIndustryChange = onboardingViewModel::onIndustryChange,
                onMainChannelChange = onboardingViewModel::onMainChannelChange,
                onExperienceLevelChange = onboardingViewModel::onExperienceLevelChange,
                onStylePreferenceChange = onboardingViewModel::onStylePreferenceChange,
                onSubmit = {
                    onboardingViewModel.onSubmit {
                        currentPage = TestHomePage.Home
                    }
                }
            )
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (currentPage != TestHomePage.Home) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = titleForPage(currentPage),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = designTokens.appBackground
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        when (currentPage) {
                            TestHomePage.Home -> {
                                // NEW: Sleek Glass Home Layout
                                com.smartsales.aitest.ui.home.HomeLayout()
                                
                                /* LEGACY: OverlayScaffold
                                OverlayScaffold(
                                currentOverlay = overlayState,
                                onOverlayChange = { overlayState = it },
                                mediaServerClient = mediaServerClient,
                                homeViewModel = homeViewModel,
                                audioFilesViewModel = audioFilesViewModel,
                                setPage = ::setPage,
                                goHome = goHome,
                                pendingSessionId = pendingSessionId,
                                onPendingSessionConsumed = { pendingSessionId = null },
                                latestDeviceSnapshot = latestDeviceSnapshot,
                                onDeviceSnapshotChanged = { latestDeviceSnapshot = it },
                                showSnackbar = showSnackbar
                            ) */
                            }

                            TestHomePage.WifiBleTester -> WifiBleTesterRoute(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(AiFeatureTestTags.PAGE_WIFI),
                                mediaServerClient = mediaServerClient,
                                onShowMessage = showSnackbar
                            )

                            TestHomePage.DeviceManager -> DeviceManagerRoute(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER),
                                onNavigateToDeviceSetup = { setPage(TestHomePage.DeviceSetup) },
                                onNavigateToGifUpload = { setPage(TestHomePage.GifUpload) },
                                onNavigateToWavDownload = { setPage(TestHomePage.WavDownload) }
                            )

                            TestHomePage.DeviceSetup -> DeviceSetupRoute(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(AiFeatureTestTags.PAGE_DEVICE_SETUP),
                                onCompleted = { setPage(TestHomePage.DeviceManager) },
                                onBackToHome = { goHome() }
                            )

                            TestHomePage.AudioFiles -> AudioFilesRoute(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(AiFeatureTestTags.PAGE_AUDIO_FILES),
                                viewModel = audioFilesViewModel,
                                onAskAiAboutTranscript = { recordingId, fileName, jobId, sessionId, preview, full ->
                                    val resolvedJobId = jobId ?: "transcription-$recordingId"
                                    val resolvedSessionId = sessionId ?: "session-$resolvedJobId"
                                    homeViewModel.onTranscriptionRequested(
                                        TranscriptionChatRequest(
                                            jobId = resolvedJobId,
                                            fileName = fileName,
                                            recordingId = recordingId,
                                            sessionId = resolvedSessionId,
                                            transcriptPreview = preview,
                                            transcriptMarkdown = full
                                        )
                                    )
                                    pendingSessionId = resolvedSessionId
                                    setPage(TestHomePage.Home)
                                }
                            )

                            TestHomePage.ChatHistory -> ChatHistoryRoute(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(AiFeatureTestTags.PAGE_CHAT_HISTORY),
                                onBackClick = { goHome() },
                                onSessionSelected = { sessionId ->
                                    pendingSessionId = sessionId
                                    goHome()
                                }
                            )

                            TestHomePage.UserCenter -> UserCenterRoute(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(AiFeatureTestTags.PAGE_USER_CENTER),
                                onLogout = { goHome() },
                                onOpenDeviceManager = { setPage(TestHomePage.DeviceManager) },
                                onOpenPrivacy = { goHome() }
                            )

                            TestHomePage.GifUpload -> { /* GIF pipeline removed */ }

                            TestHomePage.WavDownload -> com.smartsales.feature.media.WavDownloadScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHost(
    uiState: OnboardingUiState,
    onDisplayNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onMainChannelChange: (String) -> Unit,
    onExperienceLevelChange: (String) -> Unit,
    onStylePreferenceChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Welcome) }
    when (step) {
        OnboardingStep.Welcome -> OnboardingWelcomeScreen(onStart = {
            step = OnboardingStep.PersonalInfo
        })
        OnboardingStep.PersonalInfo -> OnboardingPersonalInfoScreen(
            state = uiState,
            onDisplayNameChange = onDisplayNameChange,
            onRoleChange = onRoleChange,
            onIndustryChange = onIndustryChange,
            onMainChannelChange = onMainChannelChange,
            onExperienceLevelChange = onExperienceLevelChange,
            onStylePreferenceChange = onStylePreferenceChange,
            onSave = onSubmit
        )
    }
}

private enum class OnboardingStep { Welcome, PersonalInfo }

@Composable
private fun OverlayScaffold(
    currentOverlay: HomeOverlay,
    onOverlayChange: (HomeOverlay) -> Unit,
    mediaServerClient: MediaServerClient,
    homeViewModel: HomeViewModel,
    audioFilesViewModel: AudioFilesViewModel,
    setPage: (TestHomePage) -> Unit,
    goHome: () -> Unit,
    pendingSessionId: String?,
    onPendingSessionConsumed: () -> Unit,
    latestDeviceSnapshot: DeviceSnapshotUi?,
    onDeviceSnapshotChanged: (DeviceSnapshotUi?) -> Unit,
    showSnackbar: (String) -> Unit
) {
    val designTokens = AppDesignTokens.current()
    var disableOverlayGestures by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        VerticalOverlayLayout(
            currentOverlay = currentOverlay,
            onOverlayChange = onOverlayChange,
            backgroundColor = designTokens.appBackground,
            disableOverlayGestures = disableOverlayGestures,
            home = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(AiFeatureTestTags.OVERLAY_HOME_LAYER)
                        .testTag(HomeScreenTestTags.ROOT)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        HomeScreenRoute(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = homeViewModel,
                            sessionId = pendingSessionId,
                            selectedSessionId = pendingSessionId,
                            onSessionSelectionConsumed = onPendingSessionConsumed,
                            onNavigateToDeviceManager = { onOverlayChange(HomeOverlay.Device) },
                            onNavigateToDeviceSetup = {
                                setPage(TestHomePage.DeviceSetup)
                                onOverlayChange(HomeOverlay.Home)
                            },
                            onNavigateToAudioFiles = { onOverlayChange(HomeOverlay.Audio) },
                            onNavigateToUserCenter = { setPage(TestHomePage.UserCenter) },
                            onNavigateToChatHistory = { setPage(TestHomePage.ChatHistory) },
                            onDeviceSnapshotChanged = onDeviceSnapshotChanged,
                            onInputFocusChanged = { focused -> disableOverlayGestures = focused }
                        )
                    }
                }
            },
            audio = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(AiFeatureTestTags.OVERLAY_AUDIO_LAYER)
                        .testTag(AiFeatureTestTags.PAGE_AUDIO_FILES)
                ) {
                    AudioFilesRoute(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = audioFilesViewModel,
                        onAskAiAboutTranscript = { recordingId, fileName, jobId, sessionId, preview, full ->
                            val resolvedJobId = jobId ?: "transcription-$recordingId"
                            val resolvedSessionId = sessionId ?: "session-$resolvedJobId"
                            homeViewModel.onTranscriptionRequested(
                                TranscriptionChatRequest(
                                    jobId = resolvedJobId,
                                    fileName = fileName,
                                    recordingId = recordingId,
                                    sessionId = resolvedSessionId,
                                    transcriptPreview = preview,
                                    transcriptMarkdown = full
                                )
                            )
                            onOverlayChange(HomeOverlay.Home)
                        }
                    )
                }
            },
            device = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(AiFeatureTestTags.OVERLAY_DEVICE_LAYER)
                ) {
                    DeviceManagerRoute(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToDeviceSetup = {
                            setPage(TestHomePage.DeviceSetup)
                            onOverlayChange(HomeOverlay.Home)
                        },
                        onNavigateToGifUpload = {
                            setPage(TestHomePage.GifUpload)
                            onOverlayChange(HomeOverlay.Home)
                        },
                        onNavigateToWavDownload = {
                            setPage(TestHomePage.WavDownload)
                            onOverlayChange(HomeOverlay.Home)
                        }
                    )
                }
            }
        )
    }
}

private const val OVERLAY_PEEK_THRESHOLD_FRACTION = 0.15f

@Composable
private fun DraggableOverlayStack(
    currentOverlay: HomeOverlay,
    onSelectDevice: () -> Unit,
    onSelectHome: () -> Unit,
    onSelectAudio: () -> Unit,
    showTags: Boolean,
    disableOverlayGestures: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val designTokens = AppDesignTokens.current()
    val dragRangePx = with(density) { 240.dp.toPx() }
    val offset = remember { Animatable(overlayToPosition(currentOverlay)) }
    val target = overlayToPosition(currentOverlay)
    val dragEnabled = !disableOverlayGestures

    LaunchedEffect(target) {
        offset.animateTo(
            targetValue = target,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )
    }

    val dragState = rememberDraggableState { delta ->
        if (!dragEnabled) return@rememberDraggableState
        val next = (offset.value + delta / dragRangePx).coerceIn(-1f, 1f)
        scope.launch { offset.snapTo(next) }
    }

    fun settle(velocity: Float) {
        val adjusted = offset.value + (velocity / 4_000f).coerceIn(-0.2f, 0.2f)
        val destination = when {
            adjusted > 0.25f -> HomeOverlay.Device
            adjusted < -0.25f -> HomeOverlay.Audio
            else -> HomeOverlay.Home
        }
        if (destination == currentOverlay) {
            scope.launch {
                offset.animateTo(
                    targetValue = overlayToPosition(currentOverlay),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
            }
        } else {
            when (destination) {
                HomeOverlay.Device -> onSelectDevice()
                HomeOverlay.Audio -> onSelectAudio()
                HomeOverlay.Home -> onSelectHome()
            }
        }
    }

    Surface(
        modifier = modifier
            .width(designTokens.overlayRailWidth)
            .fillMaxHeight()
            .draggable(
                state = dragState,
                enabled = dragEnabled,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> if (dragEnabled) settle(velocity) }
            )
            .testTag(AiFeatureTestTags.OVERLAY_STACK),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OverlayCard(
                label = "音频",
                overlay = HomeOverlay.Audio,
                currentOffset = offset.value,
                onClick = {
                    if (!disableOverlayGestures) onSelectAudio()
                },
                enabled = !disableOverlayGestures,
                tag = if (showTags) AiFeatureTestTags.OVERLAY_AUDIO_HANDLE else null
            )
            OverlayCard(
                label = "Home",
                overlay = HomeOverlay.Home,
                currentOffset = offset.value,
                onClick = {
                    if (!disableOverlayGestures) onSelectHome()
                },
                enabled = !disableOverlayGestures,
                tag = if (showTags) AiFeatureTestTags.OVERLAY_HOME else null
            )
            OverlayCard(
                label = "设备",
                overlay = HomeOverlay.Device,
                currentOffset = offset.value,
                onClick = {
                    if (!disableOverlayGestures) onSelectDevice()
                },
                enabled = !disableOverlayGestures,
                tag = if (showTags) AiFeatureTestTags.OVERLAY_DEVICE_HANDLE else null
            )
        }
    }
}

@Composable
private fun VerticalOverlayLayout(
    currentOverlay: HomeOverlay,
    onOverlayChange: (HomeOverlay) -> Unit,
    backgroundColor: Color,
    disableOverlayGestures: Boolean = false,
    home: @Composable () -> Unit,
    audio: @Composable () -> Unit,
    device: @Composable () -> Unit
    ) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val heightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        val openFraction = 0.85f
        val audioAnchor = heightPx * openFraction
        val homeAnchor = 0f
        val deviceAnchor = -heightPx * openFraction
        val dragEnabled = !disableOverlayGestures
        val offset = remember(heightPx) {
            Animatable(overlayToAnchor(currentOverlay, heightPx, openFraction))
        }

        LaunchedEffect(heightPx, currentOverlay) {
            val target = overlayToAnchor(currentOverlay, heightPx, openFraction)
            offset.animateTo(
                targetValue = target,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        }

        val dragState = rememberDraggableState { delta ->
            if (!dragEnabled) return@rememberDraggableState
            val next = (offset.value + delta).coerceIn(deviceAnchor, audioAnchor)
            scope.launch { offset.snapTo(next) }
        }

        fun settle(velocity: Float) {
            val projection = offset.value + velocity * 0.05f
            val threshold = heightPx * OVERLAY_PEEK_THRESHOLD_FRACTION
            val destination = when {
                projection > threshold -> HomeOverlay.Audio
                projection < -threshold -> HomeOverlay.Device
                else -> HomeOverlay.Home
            }
            val destinationAnchor = when (destination) {
                HomeOverlay.Audio -> audioAnchor
                HomeOverlay.Home -> homeAnchor
                HomeOverlay.Device -> deviceAnchor
            }
            if (destination == currentOverlay) {
                scope.launch {
                    offset.animateTo(
                        targetValue = destinationAnchor,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }
            } else {
                onOverlayChange(destination)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = dragState,
                    enabled = dragEnabled,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> if (dragEnabled) settle(velocity) }
                )
        ) {
            val stackOffset = offset.value
            val audioOffset = stackOffset - heightPx
            val deviceOffset = stackOffset + heightPx
            val isOverlayOpen = kotlin.math.abs(stackOffset - homeAnchor) > 1f

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) { home() }

            if (isOverlayOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(enabled = !disableOverlayGestures) {
                            if (!disableOverlayGestures) {
                                onOverlayChange(HomeOverlay.Home)
                            }
                        }
                        .testTag(AiFeatureTestTags.OVERLAY_BACKDROP)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, audioOffset.roundToInt()) }
                    .zIndex(0.8f)
            ) { audio() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, deviceOffset.roundToInt()) }
                    .zIndex(0.9f)
            ) { device() }
        }
    }
}

@Composable
private fun OverlayCard(
    label: String,
    overlay: HomeOverlay,
    currentOffset: Float,
    onClick: () -> Unit,
    enabled: Boolean,
    tag: String?
) {
    val designTokens = AppDesignTokens.current()
    val base = overlayToPosition(overlay)
    val distance = 1f - kotlin.math.abs(currentOffset - base).coerceIn(0f, 1f)
    val scale = 0.92f + distance * 0.12f
    val elevation = 1.dp + (distance * 2).dp
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "overlay_press_anim"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minWidth = 48.dp, minHeight = 56.dp)
            .graphicsLayer(
                scaleX = scale * pressedScale,
                scaleY = scale * pressedScale
            )
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(elevation.coerceAtMost(4.dp)),
        enabled = enabled,
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(designTokens.overlayHandleSize.height)
                    .width(designTokens.overlayHandleSize.width)
                    .background(designTokens.overlayHandleColor, shape = MaterialTheme.shapes.small)
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = when (overlay) {
                    HomeOverlay.Home -> "主页"
                    HomeOverlay.Audio -> "音频"
                    HomeOverlay.Device -> "设备"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class HomeOverlay {
    Device,
    Home,
    Audio
}

private fun overlayToPosition(overlay: HomeOverlay): Float = when (overlay) {
    HomeOverlay.Audio -> -1f
    HomeOverlay.Home -> 0f
    HomeOverlay.Device -> 1f
}

private fun overlayToAnchor(
    overlay: HomeOverlay,
    heightPx: Float,
    openFraction: Float = 1f
): Float = when (overlay) {
    HomeOverlay.Audio -> heightPx * openFraction
    HomeOverlay.Home -> 0f
    HomeOverlay.Device -> -heightPx * openFraction
}

private fun titleForPage(page: TestHomePage): String = when (page) {
    TestHomePage.Home -> ""
    TestHomePage.AudioFiles -> "录音文件"
    TestHomePage.DeviceManager -> "设备管理"
    TestHomePage.DeviceSetup -> "设备初始化"
    TestHomePage.ChatHistory -> "聊天记录"
    TestHomePage.UserCenter -> "用户中心"
    TestHomePage.WifiBleTester -> "连接测试"
    TestHomePage.GifUpload -> "发送图片"
    TestHomePage.WavDownload -> "下载录音"
}

enum class TestHomePage {
    Home,
    WifiBleTester,
    DeviceManager,
    DeviceSetup,
    ChatHistory,
    AudioFiles,
    UserCenter,
    GifUpload,
    WavDownload
}

@Composable
private fun MediaServerFileCard(
    file: MediaServerFile,
    imageLoader: ImageLoader,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
    onDownload: () -> Unit
) {
    val localContext = LocalContext.current
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (file.isImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(localContext)
                            .data(file.mediaUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxSize(),
                        imageLoader = imageLoader
                    )
                } else if (file.isVideo) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            VideoView(viewContext).apply {
                                setVideoURI(Uri.parse(file.mediaUrl))
                                setOnPreparedListener { player ->
                                    player.isLooping = true
                                    if (isPlaying) start() else pause()
                                }
                            }
                        },
                        update = { view ->
                            if (isPlaying) {
                                if (!view.isPlaying) {
                                    view.setVideoURI(Uri.parse(file.mediaUrl))
                                    view.start()
                                }
                            } else {
                                if (view.isPlaying) {
                                    view.pause()
                                }
                            }
                        }
                    )
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放切换",
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                } else {
                    Text(text = "不支持的格式", modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatSize(file.sizeBytes)} · ${formatRelativeTime(file.modifiedAtMillis)}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onDownload, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "下载")
                }
                Button(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "删除")
                }
                Button(onClick = onApply, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "应用")
                }
            }
        }
    }
}

@Composable
fun MediaServerPanel(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    client: MediaServerClient,
    onShowMessage: (String) -> Unit,
    onFilesUpdated: (List<MediaServerFile>) -> Unit,
    onFileDownloaded: (MediaServerFile, File) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<MediaServerFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var playingFile by remember { mutableStateOf<String?>(null) }
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                when (val result = client.uploadFile(baseUrl, uri)) {
                    is Result.Success -> {
                        onShowMessage("上传成功")
                        when (val listResult = client.fetchFiles(baseUrl)) {
                            is Result.Success -> {
                                files = listResult.data
                                onFilesUpdated(listResult.data)
                                errorMessage = null
                            }
                            is Result.Error -> {
                                errorMessage = listResult.throwable.message
                            }
                        }
                    }
                    is Result.Error -> {
                        onShowMessage(result.throwable.message ?: "上传失败")
                    }
                }
                isLoading = false
            }
        }
    }

    fun triggerRefresh() {
        scope.launch {
            isLoading = true
            when (val result = client.fetchFiles(baseUrl)) {
                is Result.Success -> {
                    files = result.data
                    onFilesUpdated(result.data)
                    errorMessage = null
                    onShowMessage("共 ${result.data.size} 个文件")
                }
                is Result.Error -> {
                    val msg = result.throwable.message ?: "刷新失败"
                    errorMessage = msg
                    onShowMessage(msg)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(baseUrl) {
        triggerRefresh()
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "硬件媒体库", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { uploadLauncher.launch(arrayOf("image/*", "video/*")) }) {
                        Icon(Icons.Default.Upload, contentDescription = "上传文件")
                    }
                    IconButton(onClick = { triggerRefresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新列表")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text(text = "服务地址") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(text = "示例：192.168.0.109:8000（默认设备）/ 10.0.2.2:8000（模拟器）")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (files.isEmpty()) {
                Text(text = "暂无媒体文件，点击上传或刷新。")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(files, key = { it.name }) { file ->
                        MediaServerFileCard(
                            file = file,
                            imageLoader = imageLoader,
                            isPlaying = playingFile == file.name,
                            onTogglePlay = {
                                playingFile = if (playingFile == file.name) null else file.name
                            },
                            onDelete = {
                                scope.launch {
                                    when (val result = client.deleteFile(baseUrl, file.name)) {
                                        is Result.Success -> {
                                            onShowMessage("已删除 ${file.name}")
                                            triggerRefresh()
                                        }
                                        is Result.Error -> {
                                            onShowMessage(result.throwable.message ?: "删除失败")
                                        }
                                    }
                                }
                            },
                            onApply = {
                                scope.launch {
                                    when (val result = client.applyFile(baseUrl, file.name)) {
                                        is Result.Success -> onShowMessage("已应用 ${file.name}")
                                        is Result.Error -> onShowMessage(result.throwable.message ?: "应用失败")
                                    }
                                }
                            },
                            onDownload = {
                                scope.launch {
                                    when (val result = client.downloadFile(baseUrl, file)) {
                                        is Result.Success -> {
                                            onFileDownloaded(file, result.data)
                                            onShowMessage("已下载至 ${result.data.absolutePath}")
                                        }
                                        is Result.Error -> onShowMessage(result.throwable.message ?: "下载失败")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val seconds = ((System.currentTimeMillis() - timestamp) / 1000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "${seconds}s 前"
        seconds < 3_600 -> "${seconds / 60} 分钟前"
        seconds < 86_400 -> "${seconds / 3_600} 小时前"
        else -> "${seconds / 86_400} 天前"
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format("%.1f%s", value, units[index])
}

private val REQUIRED_BLE_PERMISSIONS = listOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.ACCESS_FINE_LOCATION
)

object AiFeatureTestTags {
    const val PAGE_HOME = "page_home"
    const val PAGE_WIFI = "page_wifi_ble"
    const val PAGE_DEVICE_MANAGER = "page_device_manager"
    const val PAGE_DEVICE_SETUP = "page_device_setup"
    const val PAGE_AUDIO_FILES = "page_audio_files"
    const val PAGE_USER_CENTER = "page_user_center"
    const val PAGE_CHAT_HISTORY = "page_chat_history"
    const val CHIP_WIFI = "chip_wifi_ble"
    const val CHIP_DEVICE_MANAGER = "chip_device_manager"
    const val CHIP_DEVICE_SETUP = "chip_device_setup"
    const val CHIP_AUDIO_FILES = "chip_audio_files"
    const val CHIP_CHAT_HISTORY = "chip_chat_history"
    const val CHIP_USER_CENTER = "chip_user_center"
    const val OVERLAY_STACK = "overlay_stack"
    const val OVERLAY_HOME = "overlay_home"
    const val OVERLAY_DEVICE = "overlay_device"
    const val OVERLAY_AUDIO = "overlay_audio"

    // 垂直 overlay shell 相关 testTag（用于 NavigationSmokeTest 等）
    const val OVERLAY_SHELL = "overlay_shell"
    const val OVERLAY_HOME_LAYER = "overlay_home_layer"
    const val OVERLAY_AUDIO_LAYER = "overlay_audio_layer"
    const val OVERLAY_DEVICE_LAYER = "overlay_device_layer"
    const val OVERLAY_AUDIO_HANDLE = "overlay_audio_handle"
    const val OVERLAY_DEVICE_HANDLE = "overlay_device_handle"
    const val OVERLAY_BACKDROP = "overlay_backdrop"
}
