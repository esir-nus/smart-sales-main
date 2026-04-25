package com.smartsales.prism.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.smartsales.prism.ui.components.prismTopSafeBandPadding
import com.smartsales.prism.ui.sim.SimSharedAuroraBackground

@Composable
internal fun OnboardingFrame(
    host: OnboardingHost,
    currentStep: OnboardingStep,
    exitPolicy: OnboardingExitPolicy,
    onExit: () -> Unit,
    showExitAction: Boolean = true,
    animateStepContent: Boolean = true,
    content: @Composable (OnboardingStep) -> Unit
) {
    BackHandler(enabled = true) {
        if (!shouldBlockOnboardingSystemBack(exitPolicy)) {
            onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
    ) {
        OnboardingDarkSystemBarsEffect()

        SimSharedAuroraBackground(
            modifier = Modifier.fillMaxSize(),
            forceDarkPalette = true
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (showExitAction && shouldShowOnboardingExitAction(exitPolicy)) {
                val compactSimSkipAction = host == OnboardingHost.SIM_CONNECTIVITY &&
                    exitPolicy == OnboardingExitPolicy.EXPLICIT_ACTION_ONLY
                TextButton(
                    onClick = onExit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .prismTopSafeBandPadding()
                        .padding(top = if (compactSimSkipAction) 2.dp else 0.dp),
                    contentPadding = if (compactSimSkipAction) {
                        PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    } else {
                        ButtonDefaults.TextButtonContentPadding
                    }
                ) {
                    Text(
                        text = resolveOnboardingExitActionLabel(host, exitPolicy),
                        color = OnboardingMuted,
                        fontSize = if (compactSimSkipAction) 12.sp else 14.sp
                    )
                }
            }

            if (animateStepContent) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280)) + slideInVertically(animationSpec = tween(280)) { it / 8 }) togetherWith
                            (fadeOut(animationSpec = tween(220)) + slideOutVertically(animationSpec = tween(220)) { -it / 10 })
                    },
                    label = "OnboardingCoordinator"
                ) { step ->
                    OnboardingStepContainer(step = step, content = content)
                }
            } else {
                OnboardingStepContainer(step = currentStep, content = content)
            }
        }
    }
}

@Composable
private fun OnboardingDarkSystemBarsEffect() {
    val view = LocalView.current
    val activity = view.context.findComponentActivity() ?: return

    SideEffect {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}

@Composable
private fun OnboardingStepContainer(
    step: OnboardingStep,
    content: @Composable (OnboardingStep) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        content(step)
    }
}
