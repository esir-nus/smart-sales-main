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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
internal fun VoiceHandshakeProfileStep(
    viewModel: OnboardingInteractionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.profileState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onProfileMicPermissionResult(granted)
    }
    ObserveOnboardingMicSession(viewModel = viewModel, shouldCancel = state.isRecording)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is OnboardingInteractionEffect.AdvanceProfileStep) onContinue()
        }
    }

    VoiceHandshakeProfileContent(
        state = state,
        onSave = { viewModel.saveProfileDraft() },
        onSkip = { viewModel.skipProfileSave() },
        onPressStart = {
            val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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
internal fun VoiceHandshakeProfileStaticStep(captureState: OnboardingProfileCaptureState) {
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
    LaunchedEffect(itemCount) { if (itemCount > 0) listState.animateScrollToItem(itemCount - 1) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 220.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = !state.hasStartedInteracting,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TitleBlock("建立专属档案", "再用一次真实语音，让系统了解您的行业、角色与沟通习惯。")
                }
            }
            if (state.transcript.isNotBlank()) item { OnboardingMessageBubble(OnboardingInteractionMessage(OnboardingMessageRole.USER, state.transcript)) }
            if (state.acknowledgement.isNotBlank()) item { OnboardingMessageBubble(OnboardingInteractionMessage(OnboardingMessageRole.AI, state.acknowledgement)) }
            state.draft?.let { draft -> item { OnboardingProfileExtractionCard(draft) } }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.errorMessage?.let {
                OnboardingInlineNotice(it)
                Spacer(Modifier.height(12.dp))
            } ?: state.guidanceMessage?.let {
                OnboardingInlineGuidance(it)
                Spacer(Modifier.height(12.dp))
            }

            when {
                state.draft != null -> {
                    PrimaryPillButton("保存并继续", onSave, modifier = Modifier.fillMaxWidth())
                    if (state.canSkipAfterFailure) {
                        Spacer(Modifier.height(10.dp))
                        QuietGhostButton("跳过保存，继续下一步", onSkip, modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> {
                    OnboardingMicFooter(
                        isRecording = state.isRecording,
                        isProcessing = state.isProcessing,
                        interactionMode = state.micInteractionMode,
                        handshakeHint = when {
                            (state.isRecording || state.isProcessing) && state.liveTranscript.isNotBlank() -> state.liveTranscript
                            state.isProcessing -> ""
                            else -> "试试说：“我是做教育行业解决方案的销售经理”"
                        },
                        processingLabel = profileProcessingLabel(state.processingPhase),
                        onPressStart = onPressStart,
                        onPressEnd = onPressEnd,
                        onPressCancel = onPressCancel
                    )
                    if (state.canSkipAfterFailure) {
                        Spacer(Modifier.height(10.dp))
                        QuietGhostButton("跳过保存，继续下一步", onSkip, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
internal fun OnboardingMessageBubble(message: OnboardingInteractionMessage) {
    val isUser = message.role == OnboardingMessageRole.USER
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        FrostedCard(
            modifier = Modifier.fillMaxWidth(0.86f),
            containerColor = if (isUser) Color.White.copy(alpha = 0.08f) else Color(0x141A2337),
            borderColor = if (isUser) Color.White.copy(alpha = 0.15f) else OnboardingBlue.copy(alpha = 0.24f)
        ) {
            Column {
                if (!isUser) {
                    Text("AI 助手", color = OnboardingBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                }
                Text(message.text, color = OnboardingText, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
private fun OnboardingProfileExtractionCard(draft: OnboardingProfileDraft) {
    FrostedCard(modifier = Modifier.fillMaxWidth(), containerColor = OnboardingCard, borderColor = OnboardingCardBorder) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = OnboardingBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("信息已提取", color = OnboardingBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
private fun OnboardingExtractionRow(label: String, value: String, isLast: Boolean = false) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = OnboardingMuted, fontSize = 14.sp)
            Text(value, color = OnboardingText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        if (!isLast) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun OnboardingSuccessNote(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusOrb(Icons.Default.CheckCircle, tint = OnboardingMint, modifier = Modifier.size(64.dp), iconSize = 28)
        Spacer(Modifier.height(12.dp))
        Text(title, color = OnboardingText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = OnboardingMuted, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun OnboardingInlineNotice(text: String) {
    FrostedCard(modifier = Modifier.fillMaxWidth(), containerColor = OnboardingErrorSurface, borderColor = OnboardingAmber.copy(alpha = 0.28f)) {
        Text(text, color = OnboardingAmber, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun OnboardingInlineGuidance(text: String) {
    FrostedCard(modifier = Modifier.fillMaxWidth(), containerColor = OnboardingBlue.copy(alpha = 0.10f), borderColor = OnboardingBlue.copy(alpha = 0.22f)) {
        Text(text, color = OnboardingBlue.copy(alpha = 0.96f), fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
    }
}
