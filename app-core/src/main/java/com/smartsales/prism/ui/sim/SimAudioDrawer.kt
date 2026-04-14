package com.smartsales.prism.ui.sim

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.components.connectivity.ConnectionState
import com.smartsales.prism.ui.components.prismNavigationBarPadding
import com.smartsales.prism.ui.theme.BackdropScrim
import kotlinx.coroutines.launch

internal const val SIM_AUDIO_BADGE_DELETE_DIALOG_TEST_TAG = "sim_audio_badge_delete_dialog"
internal const val SIM_AUDIO_BADGE_DELETE_CONFIRM_TEST_TAG = "sim_audio_badge_delete_confirm"
internal const val SIM_AUDIO_BADGE_DELETE_DISMISS_TEST_TAG = "sim_audio_badge_delete_dismiss"
internal const val SIM_AUDIO_BADGE_DELETE_OPT_OUT_TEST_TAG = "sim_audio_badge_delete_opt_out"

@Composable
fun SimAudioDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAskAi: (SimAudioDiscussion) -> Unit,
    onSelectForChat: (SimChatAudioSelection) -> Unit,
    connectionState: ConnectionState,
    onSyncFromBadge: () -> Unit = {},
    onOpenConnectivity: () -> Unit = {},
    onArtifactOpened: (String, String) -> Unit = { _, _ -> },
    onImportTestAudio: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onDeleteAudio: (String) -> Unit = {},
    mode: RuntimeAudioDrawerMode = RuntimeAudioDrawerMode.BROWSE,
    currentChatAudioId: String? = null,
    showTestImportAction: Boolean = false,
    showDebugScenarioActions: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: SimAudioDrawerViewModel
) {
    val entries = viewModel.entries.collectAsStateWithLifecycle()
    val expandedAudioIds = viewModel.expandedAudioIds.collectAsStateWithLifecycle()
    val isSyncing = viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncFeedback = viewModel.syncFeedback.collectAsStateWithLifecycle()
    val lastSyncTimestamp = viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val pendingBadgeDeleteConfirmation =
        viewModel.pendingBadgeDeleteConfirmation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val browsePullOffsetPx = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

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
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    }
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
                    .fillMaxHeight(0.9f)
                    .graphicsLayer {
                        translationY = -browsePullOffsetPx.value
                    },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                backgroundColor = SimDrawerSurface,
                elevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .prismNavigationBarPadding()
                ) {
                    SimAudioDrawerContent(
                        entries = entries.value,
                        viewModel = viewModel,
                        mode = mode,
                        expandedAudioIds = expandedAudioIds.value,
                        currentChatAudioId = currentChatAudioId,
                        onDismiss = onDismiss,
                        connectionState = connectionState,
                        isSyncing = isSyncing.value,
                        syncFeedback = syncFeedback.value,
                        lastSyncTimestamp = lastSyncTimestamp.value,
                        onSyncFromBadge = onSyncFromBadge,
                        onOpenConnectivity = onOpenConnectivity,
                        onArtifactOpened = onArtifactOpened,
                        onAskAi = onAskAi,
                        onDeleteAudio = onDeleteAudio,
                        onSelectForChat = onSelectForChat,
                        onImportTestAudio = onImportTestAudio,
                        onReplayOnboarding = onReplayOnboarding,
                        onBrowsePullOffsetChanged = { pullOffset ->
                            coroutineScope.launch {
                                browsePullOffsetPx.snapTo(pullOffset)
                            }
                        },
                        onBrowsePullSettled = {
                            coroutineScope.launch {
                                browsePullOffsetPx.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.5f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        showTestImportAction = showTestImportAction,
                        showDebugScenarioActions = showDebugScenarioActions
                    )
                }
            }
        }
    }

    pendingBadgeDeleteConfirmation.value?.let { pendingDelete ->
        var optOutChecked by remember { mutableStateOf(false) }
        AlertDialog(
            modifier = Modifier.testTag(SIM_AUDIO_BADGE_DELETE_DIALOG_TEST_TAG),
            onDismissRequest = viewModel::dismissBadgeDeleteConfirmation,
            title = { Text("删除徽章录音") },
            text = {
                Column {
                    Text(
                        "\u201C${pendingDelete.filename}\u201D会从当前抽屉中删除，并同步删除徽章上的原始录音。删除后，同步不会再把它带回当前列表。"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { optOutChecked = !optOutChecked }
                    ) {
                        Checkbox(
                            checked = optOutChecked,
                            onCheckedChange = { optOutChecked = it },
                            modifier = Modifier.testTag(SIM_AUDIO_BADGE_DELETE_OPT_OUT_TEST_TAG)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "不再提示",
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(SIM_AUDIO_BADGE_DELETE_CONFIRM_TEST_TAG),
                    onClick = { viewModel.confirmBadgeDelete(optOutChecked) }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag(SIM_AUDIO_BADGE_DELETE_DISMISS_TEST_TAG),
                    onClick = viewModel::dismissBadgeDeleteConfirmation
                ) {
                    Text("取消")
                }
            }
        )
    }
}
