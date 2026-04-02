package com.smartsales.prism.ui.sim

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.components.prismNavigationBarPadding
import com.smartsales.prism.ui.theme.BackdropScrim

internal const val SIM_AUDIO_BADGE_DELETE_DIALOG_TEST_TAG = "sim_audio_badge_delete_dialog"
internal const val SIM_AUDIO_BADGE_DELETE_CONFIRM_TEST_TAG = "sim_audio_badge_delete_confirm"
internal const val SIM_AUDIO_BADGE_DELETE_DISMISS_TEST_TAG = "sim_audio_badge_delete_dismiss"

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
    val pendingBadgeDeleteConfirmation =
        viewModel.pendingBadgeDeleteConfirmation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isOpen) {
        if (!isOpen) {
            viewModel.resetExpandedCards()
            viewModel.resetDeleteConfirmationSession()
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
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                backgroundColor = SimDrawerSurface,
                elevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .prismNavigationBarPadding()
                ) {
                    SimDrawerHandle(
                        dismissDirection = SimVerticalGestureDirection.DOWN,
                        onDismiss = onDismiss,
                        testTag = SIM_AUDIO_HANDLE_TEST_TAG,
                        dismissOnTap = true
                    )

                    SimAudioDrawerContent(
                        entries = entries.value,
                        viewModel = viewModel,
                        mode = mode,
                        expandedAudioIds = expandedAudioIds.value,
                        currentChatAudioId = currentChatAudioId,
                        isSyncing = isSyncing.value,
                        onSyncFromBadge = onSyncFromBadge,
                        onOpenConnectivity = onOpenConnectivity,
                        onArtifactOpened = onArtifactOpened,
                        onAskAi = onAskAi,
                        onDeleteAudio = onDeleteAudio,
                        onSelectForChat = onSelectForChat,
                        onImportTestAudio = onImportTestAudio,
                        onReplayOnboarding = onReplayOnboarding,
                        showTestImportAction = showTestImportAction,
                        showDebugScenarioActions = showDebugScenarioActions
                    )
                }
            }
        }
    }

    pendingBadgeDeleteConfirmation.value?.let { pendingDelete ->
        AlertDialog(
            modifier = Modifier.testTag(SIM_AUDIO_BADGE_DELETE_DIALOG_TEST_TAG),
            onDismissRequest = viewModel::dismissBadgeDeleteConfirmation,
            title = { Text("删除徽章录音") },
            text = {
                Text(
                    "“${pendingDelete.filename}”会从当前抽屉中删除，并同步删除徽章上的原始录音。删除后，同步不会再把它带回当前列表。"
                )
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(SIM_AUDIO_BADGE_DELETE_CONFIRM_TEST_TAG),
                    onClick = viewModel::confirmBadgeDelete
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
