package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.smartsales.prism.ui.drawers.scheduler.ConflictVisual
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerVisualMode
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisualMode
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisuals
import com.smartsales.prism.ui.theme.AccentAmber

@Composable
fun SchedulerTaskCard(
    state: TimelineItem.Task,
    isExpanded: Boolean = false,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val visuals = currentSchedulerDrawerVisuals
    val isSimVisualMode = currentSchedulerDrawerVisualMode == SchedulerDrawerVisualMode.SIM
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(visuals.cardCornerRadius)
    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = if (isSimVisualMode && isPressed) 10.dp else 0.dp,
            shape = shape,
            ambientColor = visuals.cardHighlight,
            spotColor = visuals.cardHighlight
        )
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null
        ) { onClick() }
        .then(
            when {
                state.isVague -> Modifier.border(1.dp, com.smartsales.prism.ui.theme.AccentDanger.copy(alpha = 0.8f), shape)
                state.hasConflict || state.conflictVisual != ConflictVisual.NONE ->
                    Modifier
                        .background(visuals.cardConflictBackground, shape)
                        .border(0.75.dp, visuals.cardConflictBorder, shape)
                else -> Modifier
            }
        )

    GlassCard(
        modifier = cardModifier
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth()) {
            TaskCardIndicator(
                urgencyLevel = state.urgencyLevel,
                isDone = state.isDone
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = visuals.cardContentVerticalPadding,
                        horizontal = visuals.cardContentHorizontalPadding
                    )
            ) {
                // Wave 17: Caution Banner for Conflicts
                if (state.hasConflict) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(visuals.conflictBannerBackground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.conflictSummary ?: "发现冲突",
                            color = visuals.conflictBannerText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Header Component
                TaskCardHeader(state = state, isExpanded = isExpanded)
                
                // Expanded Area Component
                AnimatedVisibility(visible = isExpanded && !state.isDone) {
                    HorizontalDivider(
                        color = visuals.cardBorder,
                        modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                    )
                    if (state.clarificationState != null) {
                        TaskCardClarification(clarificationState = state.clarificationState)
                    } else {
                        TaskCardDetails(state = state)
                    }
                }
            }
        }

        // Processing Overlay Component
        if (state.processingStatus != null) {
            TaskCardProcessingOverlay(
                status = state.processingStatus,
                modifier = Modifier.zIndex(1f)
            )
        }
    }
}
