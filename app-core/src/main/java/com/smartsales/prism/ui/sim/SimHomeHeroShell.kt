package com.smartsales.prism.ui.sim

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.smartsales.prism.ui.SIM_ATTACH_BUTTON_TEST_TAG
import com.smartsales.prism.ui.SIM_INPUT_BAR_TEST_TAG
import com.smartsales.prism.ui.SIM_INPUT_FIELD_TEST_TAG
import com.smartsales.prism.ui.SIM_SEND_BUTTON_TEST_TAG
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.sim.SimVerticalGestureDirection.DOWN
import com.smartsales.prism.ui.sim.SimVerticalGestureDirection.UP
import com.smartsales.prism.ui.resolveSimDynamicIslandIndex
import kotlinx.coroutines.delay

@Composable
internal fun SimEmptyHomeHeroShell(
    greeting: String,
    inputText: String,
    isSending: Boolean,
    dynamicIslandItems: List<DynamicIslandItem>,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onSchedulerClick: (DynamicIslandTapAction) -> Unit,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    enableSchedulerPullGesture: Boolean = false,
    enableAudioPullGesture: Boolean = false,
    onSchedulerPullOpen: () -> Unit = {},
    onAudioPullOpen: () -> Unit = {}
) {
    SimHomeHeroShellFrame(
        inputText = inputText,
        isSending = isSending,
        dynamicIslandItems = dynamicIslandItems,
        onMenuClick = onMenuClick,
        onNewSessionClick = onNewSessionClick,
        onSchedulerClick = onSchedulerClick,
        onTextChanged = onTextChanged,
        onSend = onSend,
        onAttachClick = onAttachClick,
        enableSchedulerPullGesture = enableSchedulerPullGesture,
        enableAudioPullGesture = enableAudioPullGesture,
        onSchedulerPullOpen = onSchedulerPullOpen,
        onAudioPullOpen = onAudioPullOpen
    ) { modifier ->
        SimHomeHeroGreetingStage(
            modifier = modifier,
            greeting = greeting
        )
    }
}

@Composable
internal fun SimHomeHeroShellFrame(
    inputText: String,
    isSending: Boolean,
    dynamicIslandItems: List<DynamicIslandItem>,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onSchedulerClick: (DynamicIslandTapAction) -> Unit,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    enableSchedulerPullGesture: Boolean = false,
    enableAudioPullGesture: Boolean = false,
    onSchedulerPullOpen: () -> Unit = {},
    onAudioPullOpen: () -> Unit = {},
    centerContent: @Composable (Modifier) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SimHomeHeroTokens.AppBackground)
    ) {
        SimHomeHeroAuroraFloor()

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SimHomeHeroTopCap(
                dynamicIslandItems = dynamicIslandItems,
                onMenuClick = onMenuClick,
                onNewSessionClick = onNewSessionClick,
                onSchedulerClick = onSchedulerClick,
                enablePullGesture = enableSchedulerPullGesture,
                onPullOpen = onSchedulerPullOpen
            )

            centerContent(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            SimHomeHeroBottomMonolith(
                text = inputText,
                isSending = isSending,
                onTextChanged = onTextChanged,
                onSend = onSend,
                onAttachClick = onAttachClick,
                enablePullGesture = enableAudioPullGesture,
                onPullOpen = onAudioPullOpen
            )
        }
    }
}

@Composable
internal fun SimHomeHeroCenterStage(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SimHomeHeroTokens.CenterCanvasHorizontalPadding,
                vertical = SimHomeHeroTokens.CenterCanvasVerticalPadding
            ),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
internal fun SimHomeHeroGreetingStage(
    modifier: Modifier = Modifier,
    greeting: String
) {
    SimHomeHeroCenterStage(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        SimHomeHeroGreetingCanvas(
            modifier = Modifier.fillMaxSize(),
            greeting = greeting
        )
    }
}

@Composable
private fun SimHomeHeroAuroraFloor() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = SimHomeHeroTokens.AppBackground)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SimHomeHeroTokens.AuroraBlueCore,
                    SimHomeHeroTokens.AuroraBlueMid,
                    Color.Transparent
                ),
                center = Offset(
                    x = size.width * SimHomeHeroTokens.AuroraBlueCenterX,
                    y = size.height * SimHomeHeroTokens.AuroraBlueCenterY
                ),
                radius = size.minDimension * (SimHomeHeroTokens.AuroraBlueRadius - 0.02f)
            ),
            radius = size.minDimension * SimHomeHeroTokens.AuroraBlueRadius,
            center = Offset(
                x = size.width * SimHomeHeroTokens.AuroraBlueCenterX,
                y = size.height * SimHomeHeroTokens.AuroraBlueCenterY
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SimHomeHeroTokens.AuroraIndigoCore,
                    SimHomeHeroTokens.AuroraIndigoMid,
                    Color.Transparent
                ),
                center = Offset(
                    x = size.width * SimHomeHeroTokens.AuroraIndigoCenterX,
                    y = size.height * SimHomeHeroTokens.AuroraIndigoCenterY
                ),
                radius = size.minDimension * (SimHomeHeroTokens.AuroraIndigoRadius - 0.02f)
            ),
            radius = size.minDimension * SimHomeHeroTokens.AuroraIndigoRadius,
            center = Offset(
                x = size.width * SimHomeHeroTokens.AuroraIndigoCenterX,
                y = size.height * SimHomeHeroTokens.AuroraIndigoCenterY
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SimHomeHeroTokens.AuroraCyanCore,
                    SimHomeHeroTokens.AuroraCyanMid,
                    Color.Transparent
                ),
                center = Offset(
                    x = size.width * SimHomeHeroTokens.AuroraCyanCenterX,
                    y = size.height * SimHomeHeroTokens.AuroraCyanCenterY
                ),
                radius = size.minDimension * (SimHomeHeroTokens.AuroraCyanRadius - 0.02f)
            ),
            radius = size.minDimension * SimHomeHeroTokens.AuroraCyanRadius,
            center = Offset(
                x = size.width * SimHomeHeroTokens.AuroraCyanCenterX,
                y = size.height * SimHomeHeroTokens.AuroraCyanCenterY
            )
        )
    }
}

@Composable
private fun SimHomeHeroTopCap(
    dynamicIslandItems: List<DynamicIslandItem>,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onSchedulerClick: (DynamicIslandTapAction) -> Unit,
    enablePullGesture: Boolean,
    onPullOpen: () -> Unit
) {
    SimVerticalDragTrigger(
        modifier = Modifier
            .fillMaxWidth()
            .background(SimHomeHeroTokens.MonolithBackground),
        direction = DOWN,
        threshold = 40.dp,
        velocityThreshold = 1100.dp,
        enabled = enablePullGesture,
        onTriggered = onPullOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(SimHomeHeroTokens.HeaderHeight)
                .padding(horizontal = SimHomeHeroTokens.HeaderHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimHomeHeroIconButton(
                icon = Icons.Filled.Menu,
                contentDescription = "Open menu",
                onClick = onMenuClick
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                SimHomeHeroDynamicIsland(
                    items = dynamicIslandItems,
                    onTap = onSchedulerClick
                )
            }
            SimHomeHeroIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "Start new chat",
                onClick = onNewSessionClick
            )
        }
    }
}

@Composable
private fun SimHomeHeroDynamicIsland(
    items: List<DynamicIslandItem>,
    onTap: (DynamicIslandTapAction) -> Unit
) {
    if (items.isEmpty()) return

    val itemKeys = remember(items) { items.map(DynamicIslandItem::stableKey) }
    var currentItemKey by remember { mutableStateOf<String?>(null) }
    val currentIndex = resolveSimDynamicIslandIndex(
        items = items,
        currentItemKey = currentItemKey
    )
    val currentItem = items[currentIndex]
    val chroma = SimHomeHeroIslandChroma.from(currentItem)
    val pulse by rememberInfiniteTransition(label = "sim_home_hero_island_pulse").animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sim_home_hero_island_pulse_alpha"
    )

    LaunchedEffect(itemKeys) {
        currentItemKey = items[currentIndex].stableKey
    }

    LaunchedEffect(itemKeys, currentItem.stableKey) {
        if (items.size <= 1) return@LaunchedEffect
        delay(5000L)
        val nextIndex = (currentIndex + 1) % items.size
        currentItemKey = items[nextIndex].stableKey
    }

    Row(
        modifier = Modifier
            .widthIn(max = SimHomeHeroTokens.IslandMaxWidth)
            .defaultMinSize(minHeight = 24.dp)
            .clickable { onTap(currentItem.tapAction) }
            .padding(
                horizontal = SimHomeHeroTokens.IslandHorizontalPadding,
                vertical = SimHomeHeroTokens.IslandVerticalPadding
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(SimHomeHeroTokens.IslandDotCanvasSize)) {
            drawCircle(
                color = chroma.dot.copy(alpha = if (currentItem.isConflict) 0.32f * pulse else 0.18f),
                radius = size.minDimension * 0.5f
            )
            drawCircle(
                color = chroma.dot.copy(alpha = if (currentItem.isConflict) pulse else 1f),
                radius = size.minDimension * 0.30f
            )
        }
        Box(modifier = Modifier.width(SimHomeHeroTokens.IslandDotGap))
        Text(
            text = currentItem.displayText,
            style = TextStyle(
                brush = Brush.linearGradient(chroma.textGradient),
                fontSize = SimHomeHeroTokens.IslandTextSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = SimHomeHeroTokens.IslandLetterSpacing
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class SimHomeHeroIslandChroma(
    val dot: Color,
    val textGradient: List<Color>
) {
    companion object {
        fun from(item: DynamicIslandItem): SimHomeHeroIslandChroma {
            return when {
                item.isConflict -> SimHomeHeroIslandChroma(
                    dot = Color(0xFFFFD60A),
                    textGradient = listOf(Color(0xFFFFEB85), Color(0xFFFFD60A))
                )
                item.isIdleEntry -> SimHomeHeroIslandChroma(
                    dot = SimHomeHeroTokens.IslandIdleDot,
                    textGradient = listOf(Color.White, Color(0xFFA0A0A5))
                )
                else -> SimHomeHeroIslandChroma(
                    dot = Color(0xFFFF453A),
                    textGradient = listOf(Color(0xFFFF8A84), Color(0xFFFF453A))
                )
            }
        }
    }
}

@Composable
private fun SimHomeHeroIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(SimHomeHeroTokens.HeaderIconTouchSize)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(SimHomeHeroTokens.HeaderIconSize)
        )
    }
}

@Composable
private fun SimHomeHeroGreetingCanvas(
    modifier: Modifier = Modifier,
    greeting: String
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SimHomeHeroTokens.GreetingHorizontalPadding,
                vertical = SimHomeHeroTokens.GreetingVerticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        val upwardOffset = (maxHeight * SimHomeHeroTokens.GreetingUpwardOffsetRatio)
            .coerceAtMost(SimHomeHeroTokens.GreetingMaxUpwardOffset)
        Column(
            modifier = Modifier.offset(y = -upwardOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = greeting,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, Color(0xFFA0A0A5))
                    ),
                    fontSize = SimHomeHeroTokens.GreetingTitleSize,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "我是您的销售助手",
                color = SimHomeHeroTokens.TextSecondary,
                fontSize = SimHomeHeroTokens.GreetingSubtitleSize,
                modifier = Modifier.padding(top = SimHomeHeroTokens.GreetingSubtitleTopPadding)
            )
        }
    }
}

@Composable
private fun SimHomeHeroBottomMonolith(
    text: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    enablePullGesture: Boolean,
    onPullOpen: () -> Unit
) {
    SimVerticalDragTrigger(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .background(SimHomeHeroTokens.MonolithBackground)
            .testTag(SIM_INPUT_BAR_TEST_TAG),
        direction = UP,
        threshold = 40.dp,
        velocityThreshold = 1100.dp,
        enabled = enablePullGesture,
        onTriggered = onPullOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SimHomeHeroTokens.BottomMonolithHeight)
                .padding(
                    start = SimHomeHeroTokens.BottomHorizontalPadding,
                    top = SimHomeHeroTokens.BottomTopPadding,
                    end = SimHomeHeroTokens.BottomHorizontalPadding,
                    bottom = SimHomeHeroTokens.BottomBottomPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(SimHomeHeroTokens.BottomIconTouchSize)
                    .testTag(SIM_ATTACH_BUTTON_TEST_TAG)
                    .clickable(onClick = onAttachClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = "Attach audio",
                    tint = Color.White,
                    modifier = Modifier.size(SimHomeHeroTokens.BottomIconSize)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = SimHomeHeroTokens.BottomContentGap),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SIM_INPUT_FIELD_TEST_TAG),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = SimHomeHeroTokens.BottomInputTextSize,
                        lineHeight = SimHomeHeroTokens.BottomInputLineHeight
                    ),
                    cursorBrush = SolidColor(SimHomeHeroTokens.OutgoingBlue),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isBlank()) {
                                Text(
                                    text = "输入消息...",
                                    style = TextStyle(
                                        brush = simHomeHeroPlaceholderBrush(),
                                        fontSize = SimHomeHeroTokens.BottomInputTextSize,
                                        lineHeight = SimHomeHeroTokens.BottomInputLineHeight
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .size(SimHomeHeroTokens.BottomIconTouchSize)
                    .testTag(SIM_SEND_BUTTON_TEST_TAG)
                    .clickable(
                        enabled = text.isNotBlank() && !isSending,
                        onClick = onSend
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SimHomeHeroTokens.BottomProgressSize),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) Color.White else SimHomeHeroTokens.TextMuted,
                        modifier = Modifier.size(SimHomeHeroTokens.BottomSendIconSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun simHomeHeroPlaceholderBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "sim_home_hero_placeholder_shimmer")
    val shimmerOffset = transition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sim_home_hero_placeholder_offset"
    )

    return Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f to Color.White.copy(alpha = 0.28f),
            0.5f to Color.White.copy(alpha = 0.92f),
            1.0f to Color.White.copy(alpha = 0.28f)
        ),
        startX = shimmerOffset.value,
        endX = shimmerOffset.value + 180f
    )
}
