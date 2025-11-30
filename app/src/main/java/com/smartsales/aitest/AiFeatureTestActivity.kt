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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.smartsales.core.util.Result
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.DeviceConnectionStateUi
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeScreenRoute
import com.smartsales.feature.chat.home.HomeScreenViewModel
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

@AndroidEntryPoint
class AiFeatureTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiFeatureTestApp()
        }
    }
}

@Composable
private fun AiFeatureTestApp() {
    val context = LocalContext.current
    val mediaServerClient = remember { MediaServerClient(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val homeViewModel: HomeScreenViewModel = hiltViewModel()
    val audioFilesViewModel: AudioFilesViewModel = hiltViewModel()
    var currentPage by rememberSaveable { mutableStateOf(TestHomePage.Home) }
    var latestDeviceSnapshot by remember { mutableStateOf<DeviceSnapshotUi?>(null) }
    var pendingSessionId by remember { mutableStateOf<String?>(null) }
    val goHome: () -> Unit = {
        currentPage = TestHomePage.Home
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { granted -> !granted }) {
            scope.launch { snackbarHostState.showSnackbar("缺少 BLE 或定位权限，无法扫描 BT311。") }
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
                    val sessionId = "session-${event.jobId}"
                    homeViewModel.onTranscriptionRequested(
                        TranscriptionChatRequest(
                            jobId = event.jobId,
                            fileName = event.fileName,
                            recordingId = event.recordingId,
                            sessionId = sessionId,
                            transcriptPreview = event.transcriptPreview,
                            transcriptMarkdown = event.fullTranscriptMarkdown
                        )
                    )
                    pendingSessionId = sessionId
                }
            }
        }
    }

    fun overlayFor(page: TestHomePage): HomeOverlay = when (page) {
        TestHomePage.DeviceManager,
        TestHomePage.DeviceSetup -> HomeOverlay.Device
        TestHomePage.AudioFiles -> HomeOverlay.Audio
        else -> HomeOverlay.Home
    }

    fun setPage(page: TestHomePage) {
        currentPage = page
    }

    fun openHomeOverlay() {
        goHome()
    }

    fun openAudioOverlay() {
        setPage(TestHomePage.AudioFiles)
    }

    fun openDeviceSection(forceSetup: Boolean = false) {
        val destination = when {
            forceSetup -> TestHomePage.DeviceSetup
            latestDeviceSnapshot?.connectionState == DeviceConnectionStateUi.CONNECTED -> TestHomePage.DeviceManager
            else -> TestHomePage.DeviceSetup
        }
        setPage(destination)
    }

    val currentOverlay = overlayFor(currentPage)

    BackHandler(enabled = currentPage != TestHomePage.Home) {
        goHome()
    }

    MaterialTheme {
        val designTokens = AppDesignTokens.current()
        val showSnackbar: (String) -> Unit = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
        Scaffold(
            containerColor = designTokens.appBackground,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
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
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DraggableOverlayStack(
                currentOverlay = currentOverlay,
                onSelectDevice = { setPage(TestHomePage.DeviceManager) },
                onSelectHome = { openHomeOverlay() },
                onSelectAudio = { openAudioOverlay() },
                showTags = currentPage in setOf(
                    TestHomePage.Home,
                    TestHomePage.AudioFiles,
                    TestHomePage.DeviceManager,
                    TestHomePage.DeviceSetup
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (currentPage) {
                                TestHomePage.Home -> {
                                    // Home 页：加载聊天 HomeScreen 并把回调映射到对应 Tab。
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(AiFeatureTestTags.PAGE_HOME)
                                            .testTag(HomeScreenTestTags.ROOT)
                                    ) {
                                        HomeScreenRoute(
                                            modifier = Modifier.fillMaxSize(),
                                            viewModel = homeViewModel,
                                            sessionId = pendingSessionId,
                                            selectedSessionId = pendingSessionId,
                                            onSessionSelectionConsumed = { pendingSessionId = null },
                                            onNavigateToDeviceManager = { openDeviceSection() },
                                        onNavigateToDeviceSetup = { openDeviceSection(forceSetup = true) },
                                        onNavigateToAudioFiles = { setPage(TestHomePage.AudioFiles) },
                                        onNavigateToUserCenter = { setPage(TestHomePage.UserCenter) },
                                        onNavigateToChatHistory = { setPage(TestHomePage.ChatHistory) },
                                        onDeviceSnapshotChanged = { latestDeviceSnapshot = it }
                                    )
                                }
                                }

                                TestHomePage.WifiBleTester -> {
                                    WifiBleTesterRoute(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(AiFeatureTestTags.PAGE_WIFI),
                                        mediaServerClient = mediaServerClient,
                                        onShowMessage = showSnackbar
                                    )
                                }

                                TestHomePage.DeviceManager -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER)
                                    ) {
                                        DeviceManagerRoute(
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                TestHomePage.DeviceSetup -> {
                                    DeviceSetupRoute(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(AiFeatureTestTags.PAGE_DEVICE_SETUP),
                                        onCompleted = { setPage(TestHomePage.DeviceManager) },
                                        onBackToHome = { goHome() }
                                    )
                                }

                                TestHomePage.AudioFiles -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(AiFeatureTestTags.PAGE_AUDIO_FILES)
                                    ) {
                                        AudioFilesRoute(
                                            modifier = Modifier.fillMaxSize(),
                                            viewModel = audioFilesViewModel,
                                            onAskAiAboutTranscript = { recordingId, fileName, jobId, preview, full ->
                                                val resolvedJobId = jobId ?: "transcription-$recordingId"
                                                val sessionId = "session-$resolvedJobId"
                                                homeViewModel.onTranscriptionRequested(
                                                    TranscriptionChatRequest(
                                                        jobId = resolvedJobId,
                                                        fileName = fileName,
                                                        recordingId = recordingId,
                                                        sessionId = sessionId,
                                                        transcriptPreview = preview,
                                                        transcriptMarkdown = full
                                                    )
                                                )
                                                pendingSessionId = sessionId
                                                setPage(TestHomePage.Home)
                                            }
                                        )
                                    }
                                }

                                TestHomePage.ChatHistory -> {
                                    ChatHistoryRoute(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(AiFeatureTestTags.PAGE_CHAT_HISTORY),
                                        onBackClick = { goHome() },
                                        onSessionSelected = { sessionId ->
                                            pendingSessionId = sessionId
                                            goHome()
                                        }
                                    )
                                }

                                TestHomePage.UserCenter -> {
                                    UserCenterRoute(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(AiFeatureTestTags.PAGE_USER_CENTER),
                                        onLogout = { goHome() },
                                        onOpenDeviceManager = { setPage(TestHomePage.DeviceManager) },
                                        onOpenSubscription = { goHome() },
                                        onOpenPrivacy = { goHome() },
                                        onOpenGeneral = { goHome() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableOverlayStack(
    currentOverlay: HomeOverlay,
    onSelectDevice: () -> Unit,
    onSelectHome: () -> Unit,
    onSelectAudio: () -> Unit,
    showTags: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val designTokens = AppDesignTokens.current()
    val dragRangePx = with(density) { 240.dp.toPx() }
    val offset = remember { Animatable(overlayToPosition(currentOverlay)) }
    val target = overlayToPosition(currentOverlay)

    LaunchedEffect(target) {
        offset.animateTo(
            targetValue = target,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )
    }

    val dragState = rememberDraggableState { delta ->
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
        when (destination) {
            HomeOverlay.Device -> onSelectDevice()
            HomeOverlay.Audio -> onSelectAudio()
            HomeOverlay.Home -> onSelectHome()
        }
    }

    Surface(
        modifier = modifier
            .width(designTokens.overlayRailWidth)
            .fillMaxHeight()
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> settle(velocity) }
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
                onClick = onSelectAudio,
                tag = if (showTags) AiFeatureTestTags.OVERLAY_AUDIO_HANDLE else null
            )
            OverlayCard(
                label = "Home",
                overlay = HomeOverlay.Home,
                currentOffset = offset.value,
                onClick = onSelectHome,
                tag = if (showTags) AiFeatureTestTags.OVERLAY_HOME else null
            )
            OverlayCard(
                label = "设备",
                overlay = HomeOverlay.Device,
                currentOffset = offset.value,
                onClick = onSelectDevice,
                tag = if (showTags) AiFeatureTestTags.OVERLAY_DEVICE_HANDLE else null
            )
        }
    }
}

@Composable
private fun OverlayCard(
    label: String,
    overlay: HomeOverlay,
    currentOffset: Float,
    onClick: () -> Unit,
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

private fun titleForPage(page: TestHomePage): String = when (page) {
    TestHomePage.Home -> "新对话"
    TestHomePage.AudioFiles -> "录音文件"
    TestHomePage.DeviceManager -> "设备管理"
    TestHomePage.DeviceSetup -> "设备初始化"
    TestHomePage.ChatHistory -> "聊天记录"
    TestHomePage.UserCenter -> "用户中心"
    TestHomePage.WifiBleTester -> "连接测试"
}

private enum class TestHomePage {
    Home,
    WifiBleTester,
    DeviceManager,
    DeviceSetup,
    ChatHistory,
    AudioFiles,
    UserCenter
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
