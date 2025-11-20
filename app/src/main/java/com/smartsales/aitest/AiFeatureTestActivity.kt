package com.smartsales.aitest

// 文件：app/src/main/java/com/smartsales/aitest/AiFeatureTestActivity.kt
// 模块：:app
// 说明：AI 功能、媒体与配网测试壳 Activity
// 作者：创建于 2025-11-20

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.smartsales.aitest.devicemanager.DeviceManagerRoute
import com.smartsales.aitest.setup.DeviceSetupRoute
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.OssUploadClient
import com.smartsales.data.aicore.OssUploadRequest
import com.smartsales.data.aicore.OssUploadResult
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.feature.chat.ChatController
import com.smartsales.feature.chat.ChatExportState
import com.smartsales.feature.chat.ChatState
import com.smartsales.feature.chat.ui.ChatPanel
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.media.MediaClip
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.tingwutest.TingwuTestUiState
import com.smartsales.tingwutest.TingwuTestViewModel
import com.smartsales.tingwutest.cacheAudioFromUri
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AiFeatureTestActivity : ComponentActivity() {
    @Inject lateinit var chatController: ChatController
    @Inject lateinit var mediaSyncCoordinator: MediaSyncCoordinator
    @Inject lateinit var ossUploadClient: OssUploadClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AiFeatureTestApp(chatController, mediaSyncCoordinator, ossUploadClient) }
    }
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
private fun AiFeatureTestApp(
    chatController: ChatController,
    mediaSyncCoordinator: MediaSyncCoordinator,
    ossUploadClient: OssUploadClient
) {
    val tingwuViewModel: TingwuTestViewModel = hiltViewModel()
    val chatState by chatController.state.collectAsState()
    val mediaState by mediaSyncCoordinator.state.collectAsState(initial = mediaSyncCoordinator.state.value)
    val tingwuState by tingwuViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val mediaServerClient = remember { MediaServerClient(context) }
    var mediaServerBaseUrl by rememberSaveable { mutableStateOf("http://192.168.0.109:8000") }
    val serverFiles = remember { mutableStateMapOf<String, MediaServerFile>() }
    val downloadedFiles = remember { mutableStateMapOf<String, File>() }
    val uploadedObjects = remember { mutableStateMapOf<String, OssUploadResult>() }
    var lastInjectedTranscript by remember { mutableStateOf<String?>(null) }
    val showSnackbar: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { granted -> !granted }) {
            showSnackbar("缺少 BLE 或定位权限，无法扫描 BT311。")
        }
    }

    LaunchedEffect(Unit) {
        val missingPermissions = REQUIRED_BLE_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    val tingwuAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = cacheAudioFromUri(context, uri)
                if (cached == null) {
                    showSnackbar("无法读取音频文件")
                    return@launch
                }
                showSnackbar("已读取 ${cached.name}，开始上传并触发转写")
                tingwuViewModel.uploadLocalAudioAndSubmit(cached)
            }
        }
    }

    LaunchedEffect(tingwuState.jobs) {
        val completed = tingwuState.jobs.firstOrNull { !it.transcriptMarkdown.isNullOrBlank() }
        if (completed != null) {
            val transcript = completed.transcriptMarkdown ?: return@LaunchedEffect
            val fingerprint = "${completed.jobId}:${transcript.hashCode()}"
            if (fingerprint != lastInjectedTranscript) {
                lastInjectedTranscript = fingerprint
                chatController.importTranscript(
                    markdown = transcript,
                    sourceName = completed.jobId.ifBlank { tingwuState.uploadedFileName ?: "Tingwu" }
                )
                snackbarHostState.showSnackbar("已同步转写结果到聊天记录")
            }
        }
    }

    LaunchedEffect(chatState.clipboardMessage) {
        val clipboard = chatState.clipboardMessage
        if (!clipboard.isNullOrBlank()) {
            snackbarHostState.showSnackbar(clipboard)
            chatController.clearClipboardMessage()
        }
    }

    LaunchedEffect(chatState.errorMessage) {
        val error = chatState.errorMessage
        if (!error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(error)
            chatController.clearError()
        }
    }

    LaunchedEffect(chatState.exportState) {
        val exportState = chatState.exportState
        if (exportState is ChatExportState.Completed) {
            snackbarHostState.showSnackbar("已生成 ${exportState.result.fileName}")
        }
    }

    val handleSend: (String) -> Unit = { prompt ->
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            showSnackbar("请输入聊天内容")
        } else {
            scope.launch { chatController.send(trimmed) }
        }
    }

    suspend fun downloadClipFromServer(clip: MediaClip): File? {
        val targetName = clip.mediaFileName
        if (targetName.isNullOrBlank()) {
            showSnackbar("没有可用的媒体文件名，无法下载。")
            return null
        }
        downloadedFiles[targetName]?.takeIf { it.exists() }?.let { return it }
        val serverFile = serverFiles[targetName]
        if (serverFile == null) {
            showSnackbar("硬件媒体库中找不到 $targetName，请先刷新列表。")
            return null
        }
        showSnackbar("正在下载 $targetName ...")
        return when (val result = mediaServerClient.downloadFile(mediaServerBaseUrl, serverFile)) {
            is Result.Success -> {
                downloadedFiles[targetName] = result.data
                showSnackbar("已下载 $targetName")
                result.data
            }
            is Result.Error -> {
                showSnackbar(result.throwable.message ?: "下载失败")
                null
            }
        }
    }

    fun resolveLocalFile(clip: MediaClip): File? {
        val targetName = clip.mediaFileName ?: return null
        downloadedFiles[targetName]?.takeIf { it.exists() }?.let { return it }
        val mediaDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "device-media")
        val cachedFile = File(mediaDir, targetName)
        return cachedFile.takeIf { it.exists() }?.also { downloadedFiles[targetName] = it }
    }

    suspend fun ensureClipUpload(clip: MediaClip): ClipUploadContext? {
        clip.transcriptSource?.let { return ClipUploadContext(it, null) }
        uploadedObjects[clip.id]?.let { return ClipUploadContext(it.objectKey, it.presignedUrl) }
        val localFile = resolveLocalFile(clip) ?: downloadClipFromServer(clip) ?: return null
        showSnackbar("正在上传 ${clip.title} 至 OSS ...")
        return when (val upload = ossUploadClient.uploadAudio(OssUploadRequest(localFile))) {
            is Result.Success -> {
                uploadedObjects[clip.id] = upload.data
                showSnackbar("上传成功，生成 OSS Key")
                ClipUploadContext(upload.data.objectKey, upload.data.presignedUrl)
            }
            is Result.Error -> {
                showSnackbar(upload.throwable.message ?: "OSS 上传失败")
                null
            }
        }
    }

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Surface(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    var currentPage by rememberSaveable { mutableStateOf(TestHomePage.AiFeatures) }
                    PageSelector(currentPage = currentPage, onPageSelected = { currentPage = it })
                    Spacer(modifier = Modifier.height(16.dp))
                    when (currentPage) {
                        TestHomePage.AiFeatures -> AiFeatureColumn(
                            chatState = chatState,
                            tingwuState = tingwuState,
                            mediaState = mediaState,
                            onPickAudio = { tingwuAudioPicker.launch(arrayOf("audio/*")) },
                            onSend = handleSend,
                            onShowSnackbar = showSnackbar,
                            ensureClipUpload = { clip -> ensureClipUpload(clip) },
                            mediaSyncCoordinator = mediaSyncCoordinator,
                            chatController = chatController,
                            scope = scope,
                            scrollState = scrollState,
                            mediaServerBaseUrl = mediaServerBaseUrl,
                            onBaseUrlChange = { mediaServerBaseUrl = it },
                            mediaServerClient = mediaServerClient,
                            serverFiles = serverFiles,
                            downloadedFiles = downloadedFiles
                        )
                        TestHomePage.WifiBleTester -> WifiBleTesterRoute(
                            modifier = Modifier.weight(1f),
                            mediaServerClient = mediaServerClient,
                            onShowMessage = showSnackbar
                        )
                        TestHomePage.DeviceManager -> DeviceManagerRoute(
                            modifier = Modifier.weight(1f)
                        )
                        TestHomePage.DeviceSetup -> DeviceSetupRoute(
                            modifier = Modifier.weight(1f),
                            onCompleted = { currentPage = TestHomePage.AiFeatures }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageSelector(currentPage: TestHomePage, onPageSelected: (TestHomePage) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = currentPage == TestHomePage.AiFeatures,
            onClick = { onPageSelected(TestHomePage.AiFeatures) },
            label = { Text("AI & 媒体") }
        )
        FilterChip(
            selected = currentPage == TestHomePage.WifiBleTester,
            onClick = { onPageSelected(TestHomePage.WifiBleTester) },
            label = { Text("WiFi & BLE Tester") }
        )
        FilterChip(
            selected = currentPage == TestHomePage.DeviceManager,
            onClick = { onPageSelected(TestHomePage.DeviceManager) },
            label = { Text("设备文件") }
        )
        FilterChip(
            selected = currentPage == TestHomePage.DeviceSetup,
            onClick = { onPageSelected(TestHomePage.DeviceSetup) },
            label = { Text("设备配网") }
        )
    }
}

enum class TestHomePage { AiFeatures, WifiBleTester, DeviceManager, DeviceSetup }

@Composable
private fun AiFeatureColumn(
    chatState: ChatState,
    tingwuState: TingwuTestUiState,
    mediaState: MediaSyncState,
    onPickAudio: () -> Unit,
    onSend: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    ensureClipUpload: suspend (MediaClip) -> ClipUploadContext?,
    mediaSyncCoordinator: MediaSyncCoordinator,
    chatController: ChatController,
    scope: kotlinx.coroutines.CoroutineScope,
    scrollState: androidx.compose.foundation.ScrollState,
    mediaServerBaseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    mediaServerClient: MediaServerClient,
    serverFiles: MutableMap<String, MediaServerFile>,
    downloadedFiles: MutableMap<String, File>
) {
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxWidth()
    ) {
        ChatPanel(
            state = chatState,
            onDraftChange = chatController::updateDraft,
            onSend = onSend,
            onSkillToggle = chatController::toggleSkill,
            onCopyMarkdown = chatController::copyMarkdown,
            onExport = { format ->
                scope.launch { chatController.requestExport(format) }
            },
            onTranscriptRequest = { source ->
                scope.launch {
                    chatController.startTranscriptJob(
                        TingwuRequest(audioAssetName = source)
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        LocalAudioTranscriptionPanel(
            state = tingwuState,
            onPickAudio = onPickAudio
        )
        Spacer(modifier = Modifier.height(16.dp))
        MediaPreview(
            state = mediaState,
            onMediaSync = { scope.launch { mediaSyncCoordinator.triggerSync() } },
            onStartTranscript = { clip ->
                scope.launch {
                    val uploadContext = ensureClipUpload(clip)
                    if (uploadContext == null) {
                        onShowSnackbar("无法找到可上传的音频文件。")
                        return@launch
                    }
                    val request = TingwuRequest(
                        audioAssetName = clip.mediaFileName ?: clip.title,
                        ossObjectKey = uploadContext.objectKey,
                        fileUrl = uploadContext.presignedUrl
                    )
                    chatController.startTranscriptJob(request)
                    onShowSnackbar("已提交 ${clip.title} 的转写请求")
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        MediaServerPanel(
            baseUrl = mediaServerBaseUrl,
            onBaseUrlChange = onBaseUrlChange,
            client = mediaServerClient,
            onShowMessage = onShowSnackbar,
            onFilesUpdated = { files ->
                serverFiles.clear()
                files.forEach { serverFiles[it.name] = it }
            },
            onFileDownloaded = { remote, local ->
                downloadedFiles[remote.name] = local
            }
        )
    }
}

@Composable
private fun MediaPreview(
    state: MediaSyncState,
    onMediaSync: () -> Unit,
    onStartTranscript: (MediaClip) -> Unit
) {
    val canSync = state.connectionState is ConnectionState.WifiProvisioned ||
        state.connectionState is ConnectionState.Syncing
    val error = state.errorMessage
    val lastSynced = state.lastSyncedAtMillis
    val statusText = when {
        state.syncing -> "正在同步..."
        error != null -> error
        lastSynced != null -> "上次同步：${formatRelativeTime(lastSynced)}"
        state.connectionState is ConnectionState.Disconnected -> "等待设备连接"
        state.connectionState is ConnectionState.Pairing -> "配对中..."
        else -> "尚未同步"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "媒体同步", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onMediaSync, enabled = canSync && !state.syncing) {
                    Text(text = if (state.syncing) "同步中..." else "触发同步")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "连接状态：${describeConnection(state.connectionState)}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = statusText, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
            if (state.items.isEmpty()) {
                Text(text = "尚未同步媒体。")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 220.dp)
                ) {
                    items(state.items) { item ->
                        MediaClipRow(
                            clip = item,
                            onStartTranscript = onStartTranscript
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaClipRow(
    clip: MediaClip,
    onStartTranscript: (MediaClip) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = clip.title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${clip.customer} · ${formatRelativeTime(clip.recordedAtMillis)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "来自 ${clip.sourceDeviceName} · 时长 ${clip.durationSeconds}s",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (clip.transcriptSource.isNullOrBlank()) {
                Text(
                    text = "首次点击将自动上传至 OSS。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(onClick = { onStartTranscript(clip) }) {
                Text(text = "转写此音频")
            }
        }
    }
}

@Composable
private fun LocalAudioTranscriptionPanel(
    state: TingwuTestUiState,
    onPickAudio: () -> Unit
) {
    val latestJob = state.jobs.firstOrNull()
    val isBusy = state.isUploading || state.isSubmitting
    val transcriptMarkdown = latestJob?.transcriptMarkdown
    val errorMessage = latestJob?.errorMessage
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "本地音频转写（绕过媒体库）", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "单击下方按钮：系统会缓存音频、上传 OSS 并直接触发 Tingwu，结果展示在此区域并自动同步到聊天记录。",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onPickAudio, enabled = !isBusy) {
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.width(80.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (state.isUploading) "上传中..." else "提交中...")
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "选择音频文件")
                }
            }
            state.uploadedFileName?.let {
                Text(text = "最近音频：$it", style = MaterialTheme.typography.bodySmall)
            }
            state.lastSubmittedJobId?.let {
                Text(text = "最近任务 ID：$it", style = MaterialTheme.typography.bodySmall)
            }
            when {
                !transcriptMarkdown.isNullOrBlank() -> {
                    Text(text = "转写结果", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = transcriptMarkdown,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 12,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                !errorMessage.isNullOrBlank() -> {
                    Text(
                        text = "转写失败：$errorMessage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    Text(
                        text = "尚未生成转写结果，选择音频即可开始调试。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class ClipUploadContext(
    val objectKey: String,
    val presignedUrl: String?
)

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

private fun describeConnection(state: ConnectionState): String = when (state) {
    ConnectionState.Disconnected -> "未连接"
    is ConnectionState.Pairing -> "配对 ${state.deviceName} (${state.progressPercent}%)"
    is ConnectionState.WifiProvisioned -> "已连 ${state.session.peripheralName}"
    is ConnectionState.Syncing -> "同步中 ${state.session.peripheralName}"
    is ConnectionState.Error -> when (val err = state.error) {
        is ConnectivityError.PairingInProgress -> "冲突：${err.deviceName}"
        is ConnectivityError.ProvisioningFailed -> "配网失败：${err.reason}"
        is ConnectivityError.PermissionDenied -> "权限不足：${err.permissions.joinToString()}"
        is ConnectivityError.Timeout -> "超时等待重试"
        is ConnectivityError.Transport -> "传输失败：${err.reason}"
        ConnectivityError.MissingSession -> "缺少会话"
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
