package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.components.PrismButtonStyle
import com.smartsales.prism.ui.theme.GlassCardShape
import com.smartsales.prism.ui.theme.TextPrimary

/**
 * 灵感箱可折叠面板
 *
 * - 标准模式保持现有标题/按钮风格
 * - SIM 模式按原型对齐为灵感记录卡片
 * - 空时：完全隐藏
 */
@Composable
fun CollapsibleInspirationShelf(
    items: List<TimelineItem.Inspiration>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: (String) -> Unit,
    onAskAI: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val visuals = currentSchedulerDrawerVisuals
    val isSimVisualMode = currentSchedulerDrawerVisualMode == SchedulerDrawerVisualMode.SIM

    if (items.isEmpty()) return

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "inspirationShelfChevronRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = visuals.drawerContentHorizontalPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggle() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSimVisualMode) "灵感记录" else "灵感箱 (${items.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = visuals.shelfHeaderText
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = visuals.shelfHeaderIcon,
                modifier = Modifier
                    .size(if (isSimVisualMode) 16.dp else 18.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)),
            exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    SwipeableShelfItem(
                        itemId = item.id,
                        onDelete = { onDelete(item.id) },
                        isSimVisualMode = isSimVisualMode
                    ) {
                        if (isSimVisualMode) {
                            SimInspirationShelfCard(
                                title = item.title,
                                onAskAI = onAskAI?.let { callback -> { callback(item.title) } }
                            )
                        } else {
                            StandardInspirationShelfCard(
                                title = item.title,
                                onAskAI = onAskAI?.let { callback -> { callback(item.title) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardInspirationShelfCard(
    title: String,
    onAskAI: (() -> Unit)?
) {
    val visuals = currentSchedulerDrawerVisuals

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassCardShape)
            .background(visuals.shelfCardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(visuals.shelfTagBackground)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AI",
                    fontSize = 10.sp,
                    color = visuals.shelfTagText,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onAskAI != null) {
            Spacer(modifier = Modifier.width(12.dp))
            PrismButton(
                text = "Ask AI",
                onClick = onAskAI,
                style = PrismButtonStyle.GHOST,
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

@Composable
private fun SimInspirationShelfCard(
    title: String,
    onAskAI: (() -> Unit)?
) {
    val visuals = currentSchedulerDrawerVisuals
    val cardShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(visuals.shelfCardBackground)
            .border(width = 0.5.dp, color = visuals.shelfCardBorder, shape = cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimInspirationLeadingTile()

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            lineHeight = 18.2.sp,
            color = visuals.taskTitleColor.copy(alpha = 0.9f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        if (onAskAI != null) {
            SimAskAiIconAction(onClick = onAskAI)
        }
    }
}

@Composable
private fun SimInspirationLeadingTile() {
    val visuals = currentSchedulerDrawerVisuals

    Box(modifier = Modifier.size(28.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(6.dp))
                .background(visuals.shelfLeadingTileBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = visuals.shelfLeadingTileIcon,
                modifier = Modifier.size(14.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(visuals.shelfAiBadgeBackground)
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = visuals.shelfAiBadgeText
            )
        }
    }
}

@Composable
private fun SimAskAiIconAction(onClick: () -> Unit) {
    val visuals = currentSchedulerDrawerVisuals
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = "Ask AI",
            tint = if (isPressed) visuals.shelfActionIconActive else visuals.shelfActionIcon,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableShelfItem(
    itemId: String,
    onDelete: () -> Unit,
    isSimVisualMode: Boolean,
    content: @Composable () -> Unit
) {
    val visuals = currentSchedulerDrawerVisuals
    val density = LocalDensity.current
    val positionalThresholdPx = remember(density, isSimVisualMode) {
        with(density) { if (isSimVisualMode) 80.dp.toPx() else 56.dp.toPx() }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { positionalThresholdPx },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    LaunchedEffect(itemId) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val isRevealVisible = dismissState.targetValue != SwipeToDismissBoxValue.Settled

            if (isSimVisualMode) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isRevealVisible) {
                        Row(
                            modifier = Modifier
                                .width(80.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                .background(visuals.shelfDeleteRevealBackground)
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = visuals.shelfDeleteRevealForeground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "删除",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = visuals.shelfDeleteRevealForeground
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isRevealVisible) {
                                Modifier.background(visuals.shelfDeleteRevealBackground, GlassCardShape)
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isRevealVisible) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = visuals.shelfDeleteRevealForeground
                        )
                    }
                }
            }
        },
        content = { content() }
    )
}
