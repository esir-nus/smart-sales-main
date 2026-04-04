package com.smartsales.prism.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.smartsales.prism.data.notification.ReminderReliabilityAdvisor
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import kotlinx.coroutines.delay

@Composable
internal fun SchedulerQuickStartStep(
    viewModel: OnboardingInteractionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.quickStartState.collectAsState()
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onQuickStartMicPermissionResult(granted)
    }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onQuickStartCalendarPermissionResult(results.values.all { it })
    }
    ObserveOnboardingMicSession(viewModel = viewModel, shouldCancel = state.isRecording)

    LaunchedEffect(state.calendarPermissionRequestToken) {
        if (state.calendarPermissionRequestToken <= 0) return@LaunchedEffect
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.onQuickStartCalendarPermissionResult(granted = true)
        } else {
            calendarPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    SchedulerQuickStartContent(
        state = state,
        onContinue = onContinue,
        onDismissTransientNotice = viewModel::clearQuickStartTransientNotice,
        onDismissReminderGuide = viewModel::dismissQuickStartReminderGuide,
        onOpenReminderGuideAction = viewModel::openQuickStartReminderAction,
        onPressStart = {
            val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!hasMicPermission) {
                viewModel.onQuickStartMicPermissionRequested()
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                false
            } else {
                viewModel.startQuickStartRecording()
            }
        },
        onPressEnd = { viewModel.finishQuickStartRecording() },
        onPressCancel = { viewModel.cancelActiveRecording() }
    )
}

@Composable
internal fun SchedulerQuickStartStaticStep(captureState: OnboardingQuickStartCaptureState) {
    SchedulerQuickStartContent(
        state = quickStartStateForCapture(captureState),
        onContinue = {},
        onDismissTransientNotice = {},
        onDismissReminderGuide = {},
        onOpenReminderGuideAction = {},
        onPressStart = { false },
        onPressEnd = {},
        onPressCancel = {}
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SchedulerQuickStartContent(
    state: OnboardingQuickStartUiState,
    onContinue: () -> Unit,
    onDismissTransientNotice: () -> Unit,
    onDismissReminderGuide: () -> Unit,
    onOpenReminderGuideAction: (ReminderReliabilityAdvisor.Action) -> Unit,
    onPressStart: () -> Boolean,
    onPressEnd: () -> Unit,
    onPressCancel: () -> Unit
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val successNoteBringIntoViewRequester = remember { BringIntoViewRequester() }
    var footerHeightPx by remember { mutableIntStateOf(0) }
    val footerPadding = with(density) { footerHeightPx.toDp() } + 24.dp
    val previewRevealSignal = remember(state.items) {
        state.items.map { item -> item.stableId to item.highlightToken }
    }

    LaunchedEffect(previewRevealSignal) {
        if (previewRevealSignal.isEmpty()) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        successNoteBringIntoViewRequester.bringIntoView()
    }

    LaunchedEffect(state.transientNoticeToken) {
        if (state.transientNoticeToken <= 0 || state.transientNoticeMessage == null) return@LaunchedEffect
        delay(2_500L)
        onDismissTransientNotice()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = footerPadding)
        ) {
            item {
                TitleBlock(
                    title = "快速体验：日程",
                    subtitle = "试试用语音安排接下来几天的日程。体验完毕后，刚才的安排将保留到您的主界面。"
                )
            }
            item {
                AnimatedVisibility(
                    visible = state.items.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OnboardingQuickStartCard(state = state)
                }
            }
            if (state.items.isNotEmpty()) {
                item {
                    OnboardingSuccessNote(
                        title = "体验已就绪",
                        modifier = Modifier
                            .testTag(ONBOARDING_QUICK_START_SUCCESS_NOTE_TEST_TAG)
                            .bringIntoViewRequester(successNoteBringIntoViewRequester)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .onSizeChanged { footerHeightPx = it.height },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.errorMessage?.let {
                OnboardingInlineNotice(it)
                Spacer(Modifier.height(12.dp))
            } ?: state.transientNoticeMessage?.let {
                OnboardingInlineGuidance(it)
                Spacer(Modifier.height(12.dp))
            } ?: state.guidanceMessage?.let {
                OnboardingInlineGuidance(it)
                Spacer(Modifier.height(12.dp))
            }

            if (state.canContinue) {
                PrimaryPillButton(
                    text = "继续下一步",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }
            OnboardingMicFooter(
                isRecording = state.isRecording,
                isProcessing = state.isProcessing,
                interactionMode = state.micInteractionMode,
                handshakeHint = quickStartHandshakeHint(state),
                processingLabel = quickStartProcessingLabel(state.processingPhase),
                onPressStart = onPressStart,
                onPressEnd = onPressEnd,
                onPressCancel = onPressCancel
            )
        }
    }

    state.reminderGuide?.let { guide ->
        AlertDialog(
            onDismissRequest = onDismissReminderGuide,
            title = { Text(guide.title) },
            text = {
                Text(
                    buildString {
                        append(guide.message)
                        if (guide.checklist.isNotEmpty()) {
                            append("\n\n")
                            guide.checklist.forEach { item ->
                                append("• ")
                                append(item)
                                append('\n')
                            }
                        }
                    }.trim()
                )
            },
            confirmButton = {
                Row {
                    guide.secondaryAction?.let { secondaryAction ->
                        TextButton(onClick = { onOpenReminderGuideAction(secondaryAction) }) {
                            Text(guide.secondaryLabel ?: "更多设置")
                        }
                    }
                    TextButton(onClick = { onOpenReminderGuideAction(guide.primaryAction) }) {
                        Text(guide.primaryLabel)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissReminderGuide) {
                    Text("稍后")
                }
            }
        )
    }
}

@Composable
private fun OnboardingQuickStartCard(state: OnboardingQuickStartUiState) {
    FrostedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ONBOARDING_QUICK_START_CARD_TEST_TAG),
        containerColor = Color.White.copy(alpha = 0.03f),
        borderColor = Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = OnboardingBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "你的日程",
                color = OnboardingText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 0.dp,
                modifier = Modifier.testTag(ONBOARDING_QUICK_START_COUNT_BADGE_TEST_TAG)
            ) {
                Text(
                    text = "${state.items.size} 项",
                    color = OnboardingText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.items.forEachIndexed { index, item ->
                OnboardingQuickStartRow(index = index, item = item)
            }
        }
    }
}

@Composable
private fun OnboardingQuickStartRow(
    index: Int,
    item: OnboardingQuickStartItem
) {
    var visible by remember(item.stableId) { mutableStateOf(false) }
    var highlightVisible by remember(item.stableId, item.highlightToken) { mutableStateOf(item.highlightToken > 0) }
    val highlightColor by animateColorAsState(
        targetValue = if (highlightVisible) Color(0x33FFD60A) else Color.Transparent,
        label = "quickStartHighlight"
    )

    LaunchedEffect(item.stableId) {
        delay(index * 100L)
        visible = true
    }
    LaunchedEffect(item.highlightToken) {
        if (item.highlightToken > 0) {
            highlightVisible = true
            delay(1_200L)
            highlightVisible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 8 }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 8 })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(highlightColor)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(quickStartUrgencyColor(item.urgencyLevel))
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = item.timeLabel,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
                modifier = Modifier
                    .width(42.dp)
                    .testTag("$ONBOARDING_QUICK_START_ROW_TIME_TEST_TAG:${item.stableId}")
            )
            Text(
                text = item.title,
                color = OnboardingText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("$ONBOARDING_QUICK_START_ROW_DESC_TEST_TAG:${item.stableId}")
            )
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.testTag("$ONBOARDING_QUICK_START_ROW_BELLS_TEST_TAG:${item.stableId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(quickStartBellCount(item.urgencyLevel)) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = quickStartUrgencyColor(item.urgencyLevel),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.dateLabel,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("$ONBOARDING_QUICK_START_ROW_DATE_TEST_TAG:${item.stableId}")
                )
            }
        }
    }
}

private fun quickStartHandshakeHint(state: OnboardingQuickStartUiState): String {
    return when {
        (state.isRecording || state.isProcessing) && state.liveTranscript.isNotBlank() -> state.liveTranscript
        state.isProcessing -> state.transcript
        state.items.isEmpty() -> "试试说：\"明天早上7点叫我起床，9点要带合同去见老板...\""
        else -> "修改试试：\"把最后一项推迟到大后天\""
    }
}

private fun quickStartProcessingLabel(phase: OnboardingProcessingPhase): String {
    return when (phase) {
        OnboardingProcessingPhase.RECOGNIZING -> "正在识别您的语音..."
        OnboardingProcessingPhase.BUILDING_QUICK_START_RESULT -> "正在整理您的日程..."
        else -> "正在处理您的日程..."
    }
}

private fun quickStartBellCount(level: UrgencyLevel): Int {
    return when (level) {
        UrgencyLevel.L1_CRITICAL -> 3
        UrgencyLevel.L2_IMPORTANT -> 2
        UrgencyLevel.L3_NORMAL,
        UrgencyLevel.FIRE_OFF -> 1
    }
}

private fun quickStartUrgencyColor(level: UrgencyLevel): Color {
    return when (level) {
        UrgencyLevel.L1_CRITICAL -> Color(0xFFFF453A)
        UrgencyLevel.L2_IMPORTANT -> Color(0xFFFF9F0A)
        UrgencyLevel.L3_NORMAL,
        UrgencyLevel.FIRE_OFF -> Color(0xFF0A84FF)
    }
}
