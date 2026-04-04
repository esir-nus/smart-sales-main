package com.smartsales.prism.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp

@Composable
internal fun VoiceHandshakeConsultationStep(
    viewModel: OnboardingInteractionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.consultationState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onConsultationMicPermissionResult(granted)
    }
    ObserveOnboardingMicSession(viewModel = viewModel, shouldCancel = state.isRecording)

    VoiceHandshakeConsultationContent(
        state = state,
        onContinue = onContinue,
        onPressStart = {
            val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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
internal fun VoiceHandshakeConsultationStaticStep(captureState: OnboardingConsultationCaptureState) {
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
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 200.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = !state.hasStartedInteracting,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TitleBlock("初次沟通体验", "点击下方麦克风开始说话，再次点击结束并提交这次销售咨询。")
                }
            }
            items(state.messages) { message -> OnboardingMessageBubble(message = message) }
            if (state.isCompleted) {
                item { OnboardingSuccessNote(title = "体验完成", subtitle = "完美！这就是与 AI 沟通的方式。") }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.errorMessage?.let {
                OnboardingInlineNotice(text = it)
                Spacer(Modifier.height(12.dp))
            } ?: state.guidanceMessage?.let {
                OnboardingInlineGuidance(text = it)
                Spacer(Modifier.height(12.dp))
            }
            if (state.isCompleted) {
                PrimaryPillButton("继续下一步", onContinue, modifier = Modifier.fillMaxWidth())
            } else {
                OnboardingMicFooter(
                    isRecording = state.isRecording,
                    isProcessing = state.isProcessing,
                    interactionMode = state.micInteractionMode,
                    handshakeHint = when {
                        (state.isRecording || state.isProcessing) && state.liveTranscript.isNotBlank() -> state.liveTranscript
                        state.isProcessing -> ""
                        else -> "试试说：“帮我搞定这个客户”"
                    },
                    processingLabel = consultationProcessingLabel(state.processingPhase),
                    onPressStart = onPressStart,
                    onPressEnd = onPressEnd,
                    onPressCancel = onPressCancel
                )
            }
        }
    }
}
