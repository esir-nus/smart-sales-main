package com.smartsales.prism.ui.sim

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.components.MarkdownText
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.components.PrismButtonStyle
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import com.smartsales.prism.ui.theme.AccentSecondary
import com.smartsales.prism.ui.theme.BackgroundSurface
import com.smartsales.prism.ui.theme.BackdropScrim
import com.smartsales.prism.ui.theme.BorderSubtle
import com.smartsales.prism.ui.theme.TextPrimary
import com.smartsales.prism.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun SimAudioDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAskAi: (SimAudioDiscussion) -> Unit,
    onSelectForChat: (SimChatAudioSelection) -> Unit,
    onSyncFromBadge: () -> Unit = {},
    onOpenConnectivity: () -> Unit = {},
    onArtifactOpened: (String, String) -> Unit = { _, _ -> },
    onImportTestAudio: () -> Unit = {},
    onSeedDebugFailureScenario: () -> Unit = {},
    onSeedDebugMissingSectionsScenario: () -> Unit = {},
    onSeedDebugFallbackScenario: () -> Unit = {},
    mode: SimAudioDrawerMode = SimAudioDrawerMode.BROWSE,
    currentChatAudioId: String? = null,
    showTestImportAction: Boolean = false,
    showDebugScenarioActions: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: SimAudioDrawerViewModel
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val expandedAudioIds by viewModel.expandedAudioIds.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isOpen) {
        if (!isOpen) {
            viewModel.resetExpandedCards()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackdropScrim)
                    .clickable { onDismiss() }
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            PrismSurface(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                backgroundColor = BackgroundSurface.copy(alpha = 0.96f),
                elevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SimDrawerHandle(
                        dismissDirection = SimVerticalGestureDirection.DOWN,
                        onDismiss = onDismiss,
                        testTag = SIM_AUDIO_HANDLE_TEST_TAG,
                        dismissOnTap = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mode == SimAudioDrawerMode.CHAT_RESELECT) "选择要讨论的录音" else "录音文件",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (mode == SimAudioDrawerMode.BROWSE) {
                                PrismButton(
                                    text = "工牌连接",
                                    onClick = onOpenConnectivity,
                                    style = PrismButtonStyle.GHOST
                                )
                                PrismButton(
                                    text = if (isSyncing) "同步中..." else "同步徽章",
                                    onClick = onSyncFromBadge,
                                    style = PrismButtonStyle.GHOST,
                                    enabled = !isSyncing
                                )
                            }

                            Surface(
                                color = AccentSecondary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Smartphone,
                                        contentDescription = null,
                                        tint = AccentSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SIM 本地样本",
                                        color = AccentSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (mode == SimAudioDrawerMode.CHAT_RESELECT) {
                        Text(
                            text = "点击录音卡片切换当前聊天",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(entries, key = { it.item.id }) { entry ->
                            SimAudioCard(
                                entry = entry,
                                viewModel = viewModel,
                                mode = mode,
                                expanded = expandedAudioIds.contains(entry.item.id),
                                currentChatAudioId = currentChatAudioId,
                                onToggleExpanded = { viewModel.toggleExpanded(entry.item.id) },
                                onToggleStar = { viewModel.toggleStar(entry.item.id) },
                                onTranscribe = { viewModel.startTranscription(entry.item.id) },
                                onArtifactOpened = onArtifactOpened,
                                onAskAi = {
                                    viewModel.createDiscussion(entry.item.id)?.let(onAskAi)
                                },
                                onSelectForChat = {
                                    onSelectForChat(
                                        SimChatAudioSelection(
                                            audioId = entry.item.id,
                                            title = entry.item.filename,
                                            summary = entry.item.summary ?: entry.preview,
                                            status = entry.item.status
                                        )
                                    )
                                }
                            )
                        }
                        if (showTestImportAction && mode == SimAudioDrawerMode.BROWSE) {
                            item {
                                SimTestImportButton(onClick = onImportTestAudio)
                            }
                        }
                        if (showDebugScenarioActions && mode == SimAudioDrawerMode.BROWSE) {
                            item {
                                SimDebugScenarioPanel(
                                    onSeedFailureScenario = onSeedDebugFailureScenario,
                                    onSeedMissingSectionsScenario = onSeedDebugMissingSectionsScenario,
                                    onSeedFallbackScenario = onSeedDebugFallbackScenario
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SimAudioCard(
    entry: SimAudioEntry,
    viewModel: SimAudioDrawerViewModel,
    mode: SimAudioDrawerMode,
    expanded: Boolean,
    currentChatAudioId: String?,
    onToggleExpanded: () -> Unit,
    onToggleStar: () -> Unit,
    onTranscribe: () -> Unit,
    onArtifactOpened: (String, String) -> Unit,
    onAskAi: () -> Unit,
    onSelectForChat: () -> Unit
) {
    var artifacts by remember(entry.item.id) { mutableStateOf<TingwuJobArtifacts?>(null) }
    var isArtifactLoading by remember(entry.item.id) { mutableStateOf(false) }
    var hasReportedArtifactOpen by remember(entry.item.id, expanded) { mutableStateOf(false) }
    var transcriptPreview by remember(entry.item.id) { mutableStateOf<String?>(null) }
    val isCurrentChatAudio = currentChatAudioId == entry.item.id
    val isBrowseMode = mode == SimAudioDrawerMode.BROWSE
    val canExpandInBrowseMode = isBrowseMode && entry.item.status == AudioStatus.TRANSCRIBED
    val scope = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.25f },
        confirmValueChange = { value ->
            if (
                value == SwipeToDismissBoxValue.StartToEnd &&
                isBrowseMode &&
                entry.item.status == AudioStatus.PENDING
            ) {
                onTranscribe()
            }
            false
        }
    )

    LaunchedEffect(entry.item.id, entry.item.status, expanded) {
        if (expanded && entry.item.status == AudioStatus.TRANSCRIBED && isBrowseMode) {
            isArtifactLoading = true
            artifacts = viewModel.getArtifacts(entry.item.id)
            isArtifactLoading = false
        }
    }

    LaunchedEffect(entry.item.id, entry.item.status, mode) {
        if (mode != SimAudioDrawerMode.CHAT_RESELECT || entry.item.status != AudioStatus.TRANSCRIBED) {
            transcriptPreview = null
            return@LaunchedEffect
        }

        transcriptPreview = buildSimAudioTranscriptPreview(viewModel.getArtifacts(entry.item.id))
    }

    LaunchedEffect(expanded, entry.item.status, artifacts) {
        if (!expanded || entry.item.status != AudioStatus.TRANSCRIBED) {
            hasReportedArtifactOpen = false
            return@LaunchedEffect
        }
        if (artifacts != null && !hasReportedArtifactOpen) {
            onArtifactOpened(entry.item.id, entry.item.filename)
            hasReportedArtifactOpen = true
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = isBrowseMode && entry.item.status == AudioStatus.PENDING,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val active = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (active) AccentSecondary.copy(alpha = 0.12f) else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (active) AccentSecondary else Color.Transparent
                )
            }
        }
    ) {
        PrismSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = if (mode == SimAudioDrawerMode.CHAT_RESELECT) !isCurrentChatAudio else true
                ) {
                    when (mode) {
                        SimAudioDrawerMode.CHAT_RESELECT -> {
                            if (!isCurrentChatAudio) onSelectForChat()
                        }

                        SimAudioDrawerMode.BROWSE -> {
                            if (canExpandInBrowseMode) {
                                onToggleExpanded()
                            } else {
                                coroutineScope.launch {
                                    scope.snapTo(0f)
                                    scope.animateTo(
                                        targetValue = 0f,
                                        animationSpec = androidx.compose.animation.core.keyframes {
                                            durationMillis = 260
                                            0f at 0
                                            (-8f) at 40
                                            8f at 80
                                            (-6f) at 120
                                            4f at 160
                                            0f at 220
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = if (isCurrentChatAudio && mode == SimAudioDrawerMode.CHAT_RESELECT) {
                Color(0xFFF7F9FB)
            } else {
                Color.White.copy(alpha = 0.92f)
            },
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .offset(x = scope.value.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.item.filename,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append(entry.item.timeDisplay)
                                append(" • ")
                                append(
                                    when (entry.item.source) {
                                        AudioSource.SMARTBADGE -> "SmartBadge"
                                        AudioSource.PHONE -> "Phone"
                                    }
                                )
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        if (entry.isTestImport) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = AccentSecondary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "测试导入",
                                    color = AccentSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (entry.item.isStarred) Color(0xFFF59E0B) else Color(0xFFB0BEC5),
                        modifier = Modifier.clickable(onClick = onToggleStar)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFFF3F7FA),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = buildSimAudioStatusLabel(
                            status = entry.item.status,
                            mode = mode,
                            isCurrentChatAudio = isCurrentChatAudio
                        ),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (mode == SimAudioDrawerMode.CHAT_RESELECT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = buildSimAudioSelectPreview(
                            entry = entry,
                            transcriptPreview = transcriptPreview
                        ),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    buildSimAudioSelectHelperText(
                        status = entry.item.status,
                        isCurrentChatAudio = isCurrentChatAudio
                    )?.let { helperText ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = helperText,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }

                    if (entry.item.status == AudioStatus.TRANSCRIBING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (entry.item.progress ?: 0f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = AccentSecondary
                        )
                    }
                } else if (expanded && entry.item.status == AudioStatus.TRANSCRIBED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = entry.item.summary ?: entry.preview,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        isArtifactLoading -> {
                            Text(
                                text = "正在读取转写结果...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        artifacts == null -> {
                            Text(
                                text = "当前未读取到转写结果，请稍后重试。",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        else -> {
                            SimArtifactContent(artifacts = artifacts!!)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                        PrismButton(
                            text = "Ask AI",
                            onClick = onAskAi,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    when (entry.item.status) {
                        AudioStatus.PENDING -> SimBrowseModeSwipePrompt()
                        AudioStatus.TRANSCRIBING -> {
                            val progress = entry.item.progress ?: 0f
                            Text(
                                text = buildTransparentStateLabel(progress),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = AccentSecondary
                            )
                        }

                        AudioStatus.TRANSCRIBED -> {
                            Text(
                                text = entry.item.summary ?: entry.preview,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimBrowseModeSwipePrompt() {
    val infiniteTransition = rememberInfiniteTransition(label = "sim_audio_swipe_prompt")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sim_audio_swipe_prompt_alpha"
    )

    Text(
        text = "右滑开始转写 >>>",
        color = AccentSecondary.copy(alpha = alpha),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun SimTestImportButton(onClick: () -> Unit) {
    val stroke = Stroke(
        width = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color.LightGray,
                    style = stroke,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .background(Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+",
                fontSize = 18.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "导入测试音频",
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SimDebugScenarioPanel(
    onSeedFailureScenario: () -> Unit,
    onSeedMissingSectionsScenario: () -> Unit,
    onSeedFallbackScenario: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "调试验证场景",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Text(
            text = "仅调试版可见。点击后会在当前 SIM 录音列表中生成对应验证卡片。",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        PrismButton(
            text = "Seed Failure Scenario",
            onClick = onSeedFailureScenario,
            style = PrismButtonStyle.GHOST,
            modifier = Modifier.fillMaxWidth()
        )
        PrismButton(
            text = "Seed Missing Sections Scenario",
            onClick = onSeedMissingSectionsScenario,
            style = PrismButtonStyle.GHOST,
            modifier = Modifier.fillMaxWidth()
        )
        PrismButton(
            text = "Seed Fallback Scenario",
            onClick = onSeedFallbackScenario,
            style = PrismButtonStyle.GHOST,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

internal fun buildSimAudioStatusLabel(
    status: AudioStatus,
    mode: SimAudioDrawerMode,
    isCurrentChatAudio: Boolean
): String {
    if (mode == SimAudioDrawerMode.CHAT_RESELECT && isCurrentChatAudio) {
        return "当前讨论中"
    }

    return when (status) {
        AudioStatus.TRANSCRIBED -> "已转写"
        AudioStatus.TRANSCRIBING -> "转写中"
        AudioStatus.PENDING -> "待处理"
    }
}

internal fun buildSimAudioSelectHelperText(
    status: AudioStatus,
    isCurrentChatAudio: Boolean
): String? {
    if (isCurrentChatAudio) return null

    return when (status) {
        AudioStatus.TRANSCRIBED -> null
        AudioStatus.TRANSCRIBING -> "将在聊天中继续处理"
        AudioStatus.PENDING -> "可在当前聊天中继续处理"
    }
}

internal fun buildSimAudioSelectPreview(
    entry: SimAudioEntry,
    transcriptPreview: String?
): String {
    return when (entry.item.status) {
        AudioStatus.TRANSCRIBED -> {
            transcriptPreview
                ?.takeIf { it.isNotBlank() }
                ?: entry.item.summary
                ?: entry.preview
        }

        AudioStatus.TRANSCRIBING,
        AudioStatus.PENDING -> entry.preview
    }
}

internal fun buildSimAudioTranscriptPreview(artifacts: TingwuJobArtifacts?): String? {
    val markdown = artifacts?.transcriptMarkdown?.takeIf { it.isNotBlank() } ?: return null
    return markdown
        .replace(Regex("""[#>*`_\-\[\]\(\)]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .takeIf { it.isNotBlank() }
}

data class SimChatAudioSelection(
    val audioId: String,
    val title: String,
    val summary: String?,
    val status: AudioStatus
)

@Composable
private fun SimArtifactSections(artifacts: TingwuJobArtifacts) {
    val transcript = artifacts.transcriptMarkdown?.takeIf { it.isNotBlank() }
    val summary = artifacts.smartSummary?.summary?.takeIf { it.isNotBlank() }
    val highlights = artifacts.smartSummary?.keyPoints?.takeIf { it.isNotEmpty() }
        ?.joinToString("\n") { "• $it" }
    val chapters = artifacts.chapters?.takeIf { it.isNotEmpty() }
        ?.joinToString("\n\n") { chapter ->
            buildString {
                append(formatChapterTime(chapter.startMs))
                append("  ")
                append(chapter.title)
                chapter.summary?.takeIf { it.isNotBlank() }?.let {
                    append("\n")
                    append(it)
                }
            }
        }
    val speakers = buildSpeakerSection(artifacts)
    val providerAdjacent = buildProviderAdjacentSection(artifacts)
    val links = artifacts.resultLinks.takeIf { it.isNotEmpty() }
        ?.joinToString("\n") { "- ${it.label}: ${it.url}" }

    transcript?.let {
        SimArtifactSection(
            title = "转写",
            text = it,
            initiallyExpanded = true,
            useMarkdown = true,
            enablePseudoStreaming = true
        )
    }
    summary?.let {
        Divider(color = BorderSubtle)
        SimArtifactSection(title = "摘要", text = it, useMarkdown = true)
    }
    highlights?.let {
        Divider(color = BorderSubtle)
        SimArtifactSection(title = "重点", text = it)
    }
    chapters?.let {
        Divider(color = BorderSubtle)
        SimArtifactSection(title = "章节", text = it)
    }
    speakers?.let {
        Divider(color = BorderSubtle)
        SimArtifactSection(title = "说话人", text = it)
    }
    providerAdjacent?.let {
        Divider(color = BorderSubtle)
        SimArtifactSection(title = "附加结果", text = it)
    }
    links?.let {
        Divider(color = BorderSubtle)
        SimArtifactSection(title = "结果链接", text = links)
    }
}

@Composable
private fun SimArtifactSection(
    title: String,
    text: String,
    initiallyExpanded: Boolean = false,
    useMarkdown: Boolean = false,
    enablePseudoStreaming: Boolean = false
) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    var displayedText by remember(title, text) { mutableStateOf(if (enablePseudoStreaming) "" else text) }

    LaunchedEffect(text, enablePseudoStreaming) {
        if (!enablePseudoStreaming) {
            displayedText = text
            return@LaunchedEffect
        }

        displayedText = ""
        while (displayedText.length < text.length) {
            val next = minOf(displayedText.length + 8, text.length)
            displayedText = text.substring(0, next)
            kotlinx.coroutines.delay(14)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Box(modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)) {
                if (useMarkdown) {
                    MarkdownText(
                        text = displayedText.ifBlank { text },
                        color = TextSecondary,
                        lineHeight = 21.sp
                    )
                } else {
                    Text(
                        text = displayedText.ifBlank { text },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

private fun buildTransparentStateLabel(progress: Float): String {
    return when {
        progress < 0.35f -> "SIM 展示状态：正在整理转写"
        progress < 0.7f -> "SIM 展示状态：正在整理摘要与重点"
        else -> "SIM 展示状态：正在梳理章节与说话人"
    }
}

private fun formatChapterTime(startMs: Long): String {
    val totalSeconds = (startMs / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun buildSpeakerSection(artifacts: TingwuJobArtifacts): String? {
    if (artifacts.speakerLabels.isNotEmpty()) {
        return artifacts.speakerLabels.entries.joinToString("\n") { (speakerId, label) ->
            "- ${label.ifBlank { speakerId }} (${speakerId})"
        }
    }

    val diarized = artifacts.diarizedSegments?.takeIf { it.isNotEmpty() } ?: return null
    return diarized
        .groupBy { it.speakerId ?: "speaker_${it.speakerIndex}" }
        .entries
        .joinToString("\n") { (speakerId, segments) ->
            "- $speakerId: ${segments.size} 段"
        }
}

private fun buildProviderAdjacentSection(artifacts: TingwuJobArtifacts): String? {
    val raw = artifacts.meetingAssistanceRaw ?: return null

    return runCatching {
        val root = Json.parseToJsonElement(raw) as? JsonObject ?: return null
        val meetingAssistance = root["MeetingAssistance"] as? JsonObject ?: root

        val keywords = (meetingAssistance["Keywords"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.takeIf { it.isNotEmpty() }

        val actions = (meetingAssistance["Actions"] as? JsonArray)
            ?.mapNotNull { element ->
                when (element) {
                    is JsonPrimitive -> element.contentOrNull
                    is JsonObject -> (element["Text"] as? JsonPrimitive)?.contentOrNull
                    else -> null
                }
            }
            ?.takeIf { it.isNotEmpty() }

        val keySentences = (meetingAssistance["KeySentences"] as? JsonArray)
            ?.mapNotNull { element ->
                (element as? JsonObject)?.get("Text")
                    ?.let { it as? JsonPrimitive }
                    ?.contentOrNull
            }
            ?.takeIf { it.isNotEmpty() }

        val classifications = (meetingAssistance["Classifications"] as? JsonObject)
            ?.entries
            ?.mapNotNull { (label, value) ->
                val score = (value as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() ?: return@mapNotNull null
                "$label: ${(score * 100).toInt()}%"
            }
            ?.takeIf { it.isNotEmpty() }

        buildString {
            actions?.let {
                appendLine("待办事项")
                it.forEach { action -> appendLine("- $action") }
                appendLine()
            }
            keywords?.let {
                appendLine("关键词")
                appendLine(it.joinToString(" • "))
                appendLine()
            }
            keySentences?.let {
                appendLine("重点内容")
                it.forEach { sentence -> appendLine("- $sentence") }
                appendLine()
            }
            classifications?.let {
                appendLine("分类")
                it.forEach { item -> appendLine("- $item") }
            }
        }.trim().ifBlank { null }
    }.getOrNull()
}

private val JsonPrimitive.contentOrNull: String?
    get() = runCatching { content }.getOrNull()
