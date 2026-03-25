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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.theme.BackdropScrim

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
    onDeleteAudio: (String) -> Unit = {},
    mode: SimAudioDrawerMode = SimAudioDrawerMode.BROWSE,
    currentChatAudioId: String? = null,
    showTestImportAction: Boolean = false,
    showDebugScenarioActions: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: SimAudioDrawerViewModel
) {
    val entries = viewModel.entries.collectAsStateWithLifecycle()
    val expandedAudioIds = viewModel.expandedAudioIds.collectAsStateWithLifecycle()
    val isSyncing = viewModel.isSyncing.collectAsStateWithLifecycle()
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
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                backgroundColor = SimDrawerSurface,
                elevation = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
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
                        onSeedDebugFailureScenario = onSeedDebugFailureScenario,
                        onSeedDebugMissingSectionsScenario = onSeedDebugMissingSectionsScenario,
                        onSeedDebugFallbackScenario = onSeedDebugFallbackScenario,
                        showTestImportAction = showTestImportAction,
                        showDebugScenarioActions = showDebugScenarioActions
                    )
                }
            }
        }
    }
}
