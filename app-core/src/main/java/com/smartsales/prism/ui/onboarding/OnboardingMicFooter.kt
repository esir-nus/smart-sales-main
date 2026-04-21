package com.smartsales.prism.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smartsales.prism.ui.components.VoiceHandshakeBars
import com.smartsales.prism.ui.components.rememberVoiceHandshakeWaveProgress
import kotlinx.coroutines.delay

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
    val haptic = LocalHapticFeedback.current
    var handshakeVisible by remember { mutableStateOf(false) }
    val motion = rememberInfiniteTransition(label = "onboardingMicMotion")
    val waveProgress = rememberVoiceHandshakeWaveProgress(isRecording = isRecording, labelPrefix = "onboarding")
    val rippleProgress by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "onboardingMicRippleProgress"
    )
    val showHandshake = handshakeVisible || isRecording || isProcessing

    LaunchedEffect(Unit) {
        delay(600)
        handshakeVisible = true
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = showHandshake, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OnboardingVoiceHandshake(isRecording = isRecording, waveProgress = waveProgress)
                if (handshakeHint.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = handshakeHint,
                        color = if (isRecording) OnboardingBlue.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.height(if (showHandshake) 18.dp else 0.dp))
        Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
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
                    .pointerInput(currentInteractionMode) {
                        detectTapGestures(
                            onTap = {
                                if (currentIsProcessing) return@detectTapGestures
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentIsRecording) {
                                    currentOnPressEnd()
                                } else {
                                    currentOnPressStart()
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
        // 静默态不显示提示文案，保持录音/处理态反馈；用 alpha 占位避免布局跳动
        val showLabel = isRecording || isProcessing
        Text(
            text = if (isRecording) "正在聆听...再次点击结束" else processingLabel,
            color = OnboardingBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.alpha(if (showLabel) 1f else 0f)
        )
    }
}

@Composable
private fun OnboardingVoiceHandshake(isRecording: Boolean, waveProgress: Float) {
    VoiceHandshakeBars(isRecording = isRecording, waveProgress = waveProgress, barColor = OnboardingBlue)
}

@Composable
internal fun ObserveOnboardingMicSession(
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
            if (currentShouldCancel) viewModel.cancelActiveRecording()
        }
    }
}
