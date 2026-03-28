package com.smartsales.prism.ui.sim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smartsales.prism.ui.components.VoiceHandshakeBars
import com.smartsales.prism.ui.components.rememberVoiceHandshakeWaveProgress
import androidx.compose.material3.Text

@Composable
internal fun ObserveSimVoiceDraftSession(
    shouldCancel: Boolean,
    onCancel: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentShouldCancel by rememberUpdatedState(shouldCancel)
    val currentOnCancel by rememberUpdatedState(onCancel)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && currentShouldCancel) {
                currentOnCancel()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (currentShouldCancel) {
                currentOnCancel()
            }
        }
    }
}

@Composable
internal fun SimVoiceDraftHandshake(
    state: SimVoiceDraftUiState,
    accentColor: Color,
    hintColor: Color,
    modifier: Modifier = Modifier,
    processingLabel: String = "正在识别..."
) {
    if (!state.isRecording && !state.isProcessing) return

    val waveProgress = rememberVoiceHandshakeWaveProgress(
        isRecording = state.isRecording,
        labelPrefix = "sim"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VoiceHandshakeBars(
            isRecording = state.isRecording,
            waveProgress = waveProgress,
            barColor = accentColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when {
                state.isRecording && state.interactionMode == SimVoiceDraftInteractionMode.TAP_TO_SEND -> "正在聆听...点击完成"
                state.isRecording -> "正在聆听...松开完成"
                else -> processingLabel
            },
            color = if (state.isRecording || state.isProcessing) accentColor else hintColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
