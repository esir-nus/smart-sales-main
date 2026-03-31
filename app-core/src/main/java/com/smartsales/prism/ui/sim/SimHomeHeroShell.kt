package com.smartsales.prism.ui.sim

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.core.content.ContextCompat
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.prismNavigationBarPadding
import com.smartsales.prism.ui.components.prismStatusBarPadding
import com.smartsales.prism.ui.components.resolveShellLayoutMode
import com.smartsales.prism.ui.components.ShellLayoutMode
import com.smartsales.prism.ui.sim.SimVerticalGestureDirection.DOWN
import com.smartsales.prism.ui.sim.SimVerticalGestureDirection.UP
import com.smartsales.prism.ui.theme.PrismThemeDefaults
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal const val SIM_EMPTY_HOME_GREETING = "你好, SmartSales 用户"

private data class SimHomeHeroPalette(
    val appBackground: Color,
    val monolithBackground: Color,
    val monolithStroke: Color,
    val monolithShadow: Color,
    val iconTint: Color,
    val islandIdleDot: Color,
    val greetingGradient: List<Color>,
    val greetingSubtitle: Color,
    val inputText: Color,
    val attachIcon: Color,
    val sendIconActive: Color,
    val sendIconInactive: Color,
    val progressIndicator: Color,
    val placeholderStart: Color,
    val placeholderCenter: Color,
    val auroraBlueCore: Color,
    val auroraBlueMid: Color,
    val auroraIndigoCore: Color,
    val auroraIndigoMid: Color,
    val auroraCyanCore: Color,
    val auroraCyanMid: Color
)

@Composable
private fun rememberSimHomeHeroPalette(
    forceDarkPalette: Boolean = false
): SimHomeHeroPalette {
    val prismColors = PrismThemeDefaults.colors
    val useDarkPalette = forceDarkPalette || PrismThemeDefaults.isDarkTheme
    return if (useDarkPalette) {
        SimHomeHeroPalette(
            appBackground = Color(0xFF0D0D12),
            monolithBackground = Color(0xFF020205),
            monolithStroke = Color.White.copy(alpha = 0.04f),
            monolithShadow = Color.Black.copy(alpha = 0.28f),
            iconTint = Color.White,
            islandIdleDot = Color.White.copy(alpha = 0.42f),
            greetingGradient = listOf(Color.White, Color(0xFFA0A0A5)),
            greetingSubtitle = Color(0xFF86868B),
            inputText = Color.White,
            attachIcon = Color.White,
            sendIconActive = Color.White,
            sendIconInactive = Color(0xFFAEAEB2),
            progressIndicator = Color.White,
            placeholderStart = Color.White.copy(alpha = 0.28f),
            placeholderCenter = Color.White.copy(alpha = 0.92f),
            auroraBlueCore = Color(0x470A84FF),
            auroraBlueMid = Color(0x140A84FF),
            auroraIndigoCore = Color(0x405E5CE6),
            auroraIndigoMid = Color(0x185E5CE6),
            auroraCyanCore = Color(0x3364D2FF),
            auroraCyanMid = Color(0x1264D2FF)
        )
    } else {
        SimHomeHeroPalette(
            appBackground = Color(0xFFF5F5F7),
            monolithBackground = Color.White.copy(alpha = 0.74f),
            monolithStroke = Color.Black.copy(alpha = 0.10f),
            monolithShadow = Color.Black.copy(alpha = 0.015f),
            iconTint = prismColors.textPrimary,
            islandIdleDot = prismColors.textSecondary.copy(alpha = 0.42f),
            greetingGradient = listOf(Color(0xFF4F64D8), Color(0xFF7081FF)),
            greetingSubtitle = prismColors.textSecondary.copy(alpha = 0.80f),
            inputText = prismColors.textPrimary,
            attachIcon = prismColors.textPrimary,
            sendIconActive = prismColors.accentBlue,
            sendIconInactive = prismColors.textMuted,
            progressIndicator = prismColors.accentBlue,
            placeholderStart = prismColors.textSecondary.copy(alpha = 0.32f),
            placeholderCenter = prismColors.textPrimary.copy(alpha = 0.88f),
            auroraBlueCore = Color(0x525E62B6),
            auroraBlueMid = Color(0x1A5E62B6),
            auroraIndigoCore = Color(0x3264D2FF),
            auroraIndigoMid = Color(0x1264D2FF),
            auroraCyanCore = Color(0x2E7AC1E4),
            auroraCyanMid = Color(0x117AC1E4)
        )
    }
}

@Composable
internal fun SimEmptyHomeHeroShell(
    greeting: String,
    inputText: String,
    isSending: Boolean,
    dynamicIslandItems: List<DynamicIslandItem>,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onSchedulerClick: (DynamicIslandTapAction) -> Unit,
    showMenuButton: Boolean = true,
    showNewSessionButton: Boolean = true,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    showIdleComposerHint: Boolean = false,
    voiceDraftState: SimVoiceDraftUiState = SimVoiceDraftUiState(),
    voiceDraftEnabled: Boolean = false,
    onVoiceDraftPermissionRequested: () -> Unit = {},
    onVoiceDraftPermissionResult: (Boolean) -> Unit = {},
    onVoiceDraftStart: () -> Boolean = { false },
    onVoiceDraftFinish: () -> Unit = {},
    onVoiceDraftCancel: () -> Unit = {},
    showBottomComposer: Boolean = true,
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
        showMenuButton = showMenuButton,
        showNewSessionButton = showNewSessionButton,
        onTextChanged = onTextChanged,
        onSend = onSend,
        onAttachClick = onAttachClick,
        showIdleComposerHint = showIdleComposerHint,
        voiceDraftState = voiceDraftState,
        voiceDraftEnabled = voiceDraftEnabled,
        onVoiceDraftPermissionRequested = onVoiceDraftPermissionRequested,
        onVoiceDraftPermissionResult = onVoiceDraftPermissionResult,
        onVoiceDraftStart = onVoiceDraftStart,
        onVoiceDraftFinish = onVoiceDraftFinish,
        onVoiceDraftCancel = onVoiceDraftCancel,
        showBottomComposer = showBottomComposer,
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
    showMenuButton: Boolean = true,
    showNewSessionButton: Boolean = true,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    showIdleComposerHint: Boolean = false,
    voiceDraftState: SimVoiceDraftUiState = SimVoiceDraftUiState(),
    voiceDraftEnabled: Boolean = false,
    onVoiceDraftPermissionRequested: () -> Unit = {},
    onVoiceDraftPermissionResult: (Boolean) -> Unit = {},
    onVoiceDraftStart: () -> Boolean = { false },
    onVoiceDraftFinish: () -> Unit = {},
    onVoiceDraftCancel: () -> Unit = {},
    showBottomComposer: Boolean = true,
    enableSchedulerPullGesture: Boolean = false,
    enableAudioPullGesture: Boolean = false,
    onSchedulerPullOpen: () -> Unit = {},
    onAudioPullOpen: () -> Unit = {},
    centerContent: @Composable (Modifier) -> Unit
) {
    val palette = rememberSimHomeHeroPalette()
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
    var rootTopInRoot by remember { mutableStateOf(0f) }
    var topMonolithBottomInRoot by remember { mutableStateOf<Float?>(null) }
    var bottomMonolithTopInRoot by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.appBackground)
            .onGloballyPositioned { coordinates ->
                rootTopInRoot = coordinates.boundsInRoot().top
            }
    ) {
        SimHomeHeroAuroraFloor(palette = palette)

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SimHomeHeroTopCap(
                dynamicIslandItems = dynamicIslandItems,
                onMenuClick = onMenuClick,
                onNewSessionClick = onNewSessionClick,
                onSchedulerClick = onSchedulerClick,
                showMenuButton = showMenuButton,
                showNewSessionButton = showNewSessionButton,
                enablePullGesture = enableSchedulerPullGesture,
                onPullOpen = onSchedulerPullOpen,
                onBoundsChanged = { bounds ->
                    topMonolithBottomInRoot = bounds.bottom
                }
            )

            centerContent(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            if (showBottomComposer) {
                SimHomeHeroBottomMonolith(
                    text = inputText,
                    isSending = isSending,
                    onTextChanged = onTextChanged,
                    onSend = onSend,
                    onAttachClick = onAttachClick,
                    showIdleComposerHint = showIdleComposerHint,
                    voiceDraftState = voiceDraftState,
                    voiceDraftEnabled = voiceDraftEnabled,
                    onVoiceDraftPermissionRequested = onVoiceDraftPermissionRequested,
                    onVoiceDraftPermissionResult = onVoiceDraftPermissionResult,
                    onVoiceDraftStart = onVoiceDraftStart,
                    onVoiceDraftFinish = onVoiceDraftFinish,
                    onVoiceDraftCancel = onVoiceDraftCancel,
                    enablePullGesture = enableAudioPullGesture,
                    onPullOpen = onAudioPullOpen,
                    onBoundsChanged = { bounds ->
                        bottomMonolithTopInRoot = bounds.top
                    }
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SimHomeHeroTokens.BottomMonolithHeight)
                        .prismNavigationBarPadding()
                        .imePadding()
                )
            }
        }

        val topMonolithBottom = topMonolithBottomInRoot?.minus(rootTopInRoot)
        if (topMonolithBottom != null) {
            SimMonolithSeamOverlay(
                anchorY = topMonolithBottom,
                direction = SimMonolithSeamDirection.TOP,
                insideHeight = SimHomeHeroTokens.TopSeamInsideSofteningHeight,
                outsideHeight = if (isDarkTheme) {
                    SimHomeHeroTokens.TopSeamOutsideFeatherHeight
                } else {
                    5.dp
                },
                innerHighlightAlpha = SimHomeHeroTokens.TopSeamInnerHighlightAlpha,
                outsideHazeAlpha = if (isDarkTheme) {
                    SimHomeHeroTokens.TopSeamOutsideHazeAlpha
                } else {
                    0.015f
                },
                isDarkTheme = isDarkTheme
            )
        }

        val bottomMonolithTop = bottomMonolithTopInRoot?.minus(rootTopInRoot)
        if (showBottomComposer && bottomMonolithTop != null) {
            SimMonolithSeamOverlay(
                anchorY = bottomMonolithTop,
                direction = SimMonolithSeamDirection.BOTTOM,
                insideHeight = SimHomeHeroTokens.BottomSeamInsideSofteningHeight,
                outsideHeight = if (isDarkTheme) {
                    SimHomeHeroTokens.BottomSeamOutsideFeatherHeight
                } else {
                    8.dp
                },
                innerHighlightAlpha = SimHomeHeroTokens.BottomSeamInnerHighlightAlpha,
                outsideHazeAlpha = if (isDarkTheme) {
                    SimHomeHeroTokens.BottomSeamOutsideHazeAlpha
                } else {
                    0.028f
                },
                isDarkTheme = isDarkTheme
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
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val layoutMode = resolveShellLayoutMode(
            availableWidth = maxWidth,
            availableHeight = maxHeight
        )
        val metrics = SimHomeHeroTokens.layoutMetrics(layoutMode)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = SimHomeHeroTokens.CenterCanvasHorizontalPadding,
                    vertical = metrics.centerCanvasVerticalPadding
                ),
            contentAlignment = contentAlignment,
            content = content
        )
    }
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
internal fun SimSharedAuroraBackground(
    modifier: Modifier = Modifier,
    forceDarkPalette: Boolean = false
) {
    val palette = rememberSimHomeHeroPalette(forceDarkPalette = forceDarkPalette)
    SimHomeHeroAuroraFloor(
        palette = palette,
        modifier = modifier
    )
}

@Composable
private fun SimHomeHeroAuroraFloor(
    palette: SimHomeHeroPalette,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = palette.appBackground)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.auroraBlueCore,
                    palette.auroraBlueMid,
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
                    palette.auroraIndigoCore,
                    palette.auroraIndigoMid,
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
                    palette.auroraCyanCore,
                    palette.auroraCyanMid,
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
    showMenuButton: Boolean,
    showNewSessionButton: Boolean,
    enablePullGesture: Boolean,
    onPullOpen: () -> Unit,
    onBoundsChanged: (Rect) -> Unit
) {
    val palette = rememberSimHomeHeroPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enablePullGesture) {
                    Modifier.testTag(SIM_HEADER_TEST_TAG)
                } else {
                    Modifier
                }
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (PrismThemeDefaults.isDarkTheme) 0.dp else 1.dp,
                    shape = RectangleShape,
                    clip = false,
                    ambientColor = palette.monolithShadow,
                    spotColor = palette.monolithShadow
                )
                .background(palette.monolithBackground)
                .drawBehind {
                    drawLine(
                        color = palette.monolithStroke,
                        start = Offset(0f, size.height - 1f),
                        end = Offset(size.width, size.height - 1f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .onGloballyPositioned { coordinates ->
                    onBoundsChanged(coordinates.boundsInRoot())
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .prismStatusBarPadding()
                    .height(SimHomeHeroTokens.HeaderHeight)
                    .padding(horizontal = SimHomeHeroTokens.HeaderHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimHomeHeroHeaderSlot {
                    if (showMenuButton) {
                        SimHomeHeroIconButton(
                            icon = Icons.Filled.Menu,
                            contentDescription = "Open menu",
                            onClick = onMenuClick,
                            modifier = Modifier.testTag(SIM_HEADER_MENU_BUTTON_TEST_TAG)
                        )
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SimHomeHeroDynamicIsland(
                        items = dynamicIslandItems,
                        onTap = onSchedulerClick,
                        enablePullGesture = enablePullGesture,
                        onPullOpen = onPullOpen
                    )
                }
                SimHomeHeroHeaderSlot {
                    if (showNewSessionButton) {
                        SimHomeHeroIconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = "Start new chat",
                            onClick = onNewSessionClick,
                            modifier = Modifier.testTag(SIM_HEADER_NEW_CHAT_BUTTON_TEST_TAG)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimHomeHeroDynamicIsland(
    items: List<DynamicIslandItem>,
    onTap: (DynamicIslandTapAction) -> Unit,
    enablePullGesture: Boolean = false,
    onPullOpen: () -> Unit = {}
) {
    if (items.isEmpty()) return

    val palette = rememberSimHomeHeroPalette()
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
    val itemKeys = remember(items) { items.map(DynamicIslandItem::stableKey) }
    var currentItemKey by remember { mutableStateOf<String?>(null) }
    val currentIndex = resolveSimDynamicIslandIndex(
        items = items,
        currentItemKey = currentItemKey
    )
    val currentItem = items[currentIndex]
    val chroma = SimHomeHeroIslandChroma.from(
        item = currentItem,
        palette = palette,
        isDarkTheme = isDarkTheme
    )
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

    SimVerticalDragTrigger(
        modifier = Modifier
            .widthIn(max = SimHomeHeroTokens.IslandMaxWidth)
            .testTag(SIM_DYNAMIC_ISLAND_TEST_TAG),
        direction = DOWN,
        threshold = 40.dp,
        velocityThreshold = 1100.dp,
        enabled = enablePullGesture,
        onTriggered = onPullOpen
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = if (PrismThemeDefaults.isDarkTheme) 0.dp else 3.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = chroma.shadow,
                    spotColor = chroma.shadow
                )
                .background(chroma.surface, CircleShape)
                .border(1.dp, chroma.border, CircleShape)
                .defaultMinSize(minHeight = 28.dp)
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
}

private data class SimHomeHeroIslandChroma(
    val dot: Color,
    val textGradient: List<Color>,
    val surface: Color,
    val border: Color,
    val shadow: Color
) {
    companion object {
        fun from(
            item: DynamicIslandItem,
            palette: SimHomeHeroPalette,
            isDarkTheme: Boolean
        ): SimHomeHeroIslandChroma {
            return when {
                item.isConflict -> SimHomeHeroIslandChroma(
                    dot = Color(0xFFFFD60A),
                    textGradient = listOf(Color(0xFFC93400), Color(0xFFFF9500)),
                    surface = if (isDarkTheme) {
                        Color(0x29FF9500)
                    } else {
                        Color(0x1AFF9500)
                    },
                    border = Color(0x33FF9500),
                    shadow = Color(0x1FFF9500)
                )
                item.isIdleEntry -> SimHomeHeroIslandChroma(
                    dot = palette.islandIdleDot,
                    textGradient = listOf(palette.iconTint, palette.greetingSubtitle),
                    surface = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.Black.copy(alpha = 0.04f)
                    },
                    border = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.05f)
                    } else {
                        Color.Black.copy(alpha = 0.04f)
                    },
                    shadow = Color.Black.copy(alpha = if (isDarkTheme) 0f else 0.03f)
                )
                else -> SimHomeHeroIslandChroma(
                    dot = if (isDarkTheme) Color(0xFFFF453A) else Color(0xFFD70015),
                    textGradient = if (isDarkTheme) {
                        listOf(Color(0xFFFF8A84), Color(0xFFFF453A))
                    } else {
                        listOf(Color(0xFFD70015), Color(0xFFFF3B30))
                    },
                    surface = if (isDarkTheme) {
                        Color(0x1AFF453A)
                    } else {
                        Color(0x14FF3B30)
                    },
                    border = if (isDarkTheme) {
                        Color(0x26FF453A)
                    } else {
                        Color(0x26FF3B30)
                    },
                    shadow = Color(0x1FFF3B30)
                )
            }
        }
    }
}

@Composable
private fun SimHomeHeroIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberSimHomeHeroPalette()
    Box(
        modifier = modifier
            .size(SimHomeHeroTokens.HeaderIconTouchSize)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.iconTint,
            modifier = Modifier.size(SimHomeHeroTokens.HeaderIconSize)
        )
    }
}

@Composable
private fun SimHomeHeroHeaderSlot(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.size(SimHomeHeroTokens.HeaderIconTouchSize),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun SimHomeHeroGreetingCanvas(
    modifier: Modifier = Modifier,
    greeting: String
) {
    val palette = rememberSimHomeHeroPalette()
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val layoutMode = resolveShellLayoutMode(
            availableWidth = maxWidth,
            availableHeight = maxHeight
        )
        val metrics = SimHomeHeroTokens.layoutMetrics(layoutMode)
        val upwardOffset = (maxHeight * metrics.greetingUpwardOffsetRatio)
            .coerceAtMost(metrics.greetingMaxUpwardOffset)
        val greetingAlignment = if (layoutMode == ShellLayoutMode.TIGHT) {
            Alignment.TopCenter
        } else {
            Alignment.Center
        }
        Column(
            modifier = Modifier
                .align(greetingAlignment)
                .padding(
                    horizontal = metrics.greetingHorizontalPadding,
                    vertical = metrics.greetingVerticalPadding
                )
                .padding(top = metrics.greetingTopBiasPadding)
                .offset(y = -upwardOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = greeting,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = palette.greetingGradient
                    ),
                    fontSize = metrics.greetingTitleSize,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "我是您的销售助手",
                color = palette.greetingSubtitle,
                fontSize = metrics.greetingSubtitleSize,
                modifier = Modifier.padding(top = metrics.greetingSubtitleTopPadding)
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
    showIdleComposerHint: Boolean,
    voiceDraftState: SimVoiceDraftUiState,
    voiceDraftEnabled: Boolean,
    onVoiceDraftPermissionRequested: () -> Unit,
    onVoiceDraftPermissionResult: (Boolean) -> Unit,
    onVoiceDraftStart: () -> Boolean,
    onVoiceDraftFinish: () -> Unit,
    onVoiceDraftCancel: () -> Unit,
    enablePullGesture: Boolean,
    onPullOpen: () -> Unit,
    onBoundsChanged: (Rect) -> Unit
) {
    val palette = rememberSimHomeHeroPalette()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onVoiceDraftPermissionResult(granted)
    }
    val actionEnabled = text.isNotBlank() && !isSending
    val showVoiceMic = voiceDraftEnabled && text.isBlank()
    val displayText = if (text.isBlank() && voiceDraftState.liveTranscript.isNotBlank()) {
        voiceDraftState.liveTranscript
    } else {
        text
    }
    SimVerticalDragTrigger(
        modifier = Modifier
            .fillMaxWidth()
            .prismNavigationBarPadding()
            .imePadding()
            .then(
                if (enablePullGesture) {
                    Modifier.testTag(SIM_INPUT_BAR_TEST_TAG)
                } else {
                    Modifier
                }
            ),
        direction = UP,
        threshold = 40.dp,
        velocityThreshold = 1100.dp,
        enabled = enablePullGesture,
        onTriggered = onPullOpen
    ) {
        BoxWithConstraints {
            val layoutMode = resolveShellLayoutMode(
                availableWidth = maxWidth,
                availableHeight = maxHeight
            )
            val layoutMetrics = SimHomeHeroTokens.layoutMetrics(layoutMode)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (PrismThemeDefaults.isDarkTheme) 0.dp else 2.dp,
                        shape = RectangleShape,
                        clip = false,
                        ambientColor = palette.monolithShadow,
                        spotColor = palette.monolithShadow
                    )
                    .background(palette.monolithBackground)
                    .drawBehind {
                        drawLine(
                            color = palette.monolithStroke,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .onGloballyPositioned { coordinates ->
                        onBoundsChanged(coordinates.boundsInRoot())
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SimHomeHeroTokens.BottomHorizontalPadding,
                            top = layoutMetrics.bottomTopPadding,
                            end = SimHomeHeroTokens.BottomHorizontalPadding,
                            bottom = layoutMetrics.bottomBottomPadding
                        )
                ) {
                    if (voiceDraftState.isRecording) {
                        SimVoiceDraftHandshake(
                            state = voiceDraftState,
                            accentColor = palette.sendIconActive,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SimHomeHeroTokens.BottomMonolithHeight),
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
                                tint = palette.attachIcon,
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
                                value = displayText,
                                onValueChange = { updatedText ->
                                    if (!voiceDraftState.isRecording) {
                                        onTextChanged(updatedText)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SIM_INPUT_FIELD_TEST_TAG),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = palette.inputText,
                                    fontSize = SimHomeHeroTokens.BottomInputTextSize,
                                    lineHeight = SimHomeHeroTokens.BottomInputLineHeight
                                ),
                                cursorBrush = SolidColor(SimHomeHeroTokens.OutgoingBlue),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (displayText.isBlank()) {
                                            SimHomeHeroComposerRotatingHint(
                                                visible = !voiceDraftState.isRecording && !voiceDraftState.isProcessing,
                                                rotatingHints = SIM_IDLE_COMPOSER_ROTATING_HINTS,
                                                useFullRotation = showIdleComposerHint
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
                                .then(
                                    if (showVoiceMic) {
                                        // 使用 Unit 作为 key，避免 isRecording 变化时
                                        // 取消 pointerInput 协程导致 tryAwaitRelease 丢失。
                                        Modifier.pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    if (voiceDraftState.isProcessing) return@detectTapGestures
                                                    if (
                                                        voiceDraftState.isRecording &&
                                                        voiceDraftState.interactionMode == SimVoiceDraftInteractionMode.TAP_TO_SEND
                                                    ) {
                                                        val released = tryAwaitRelease()
                                                        if (released) {
                                                            onVoiceDraftFinish()
                                                        } else {
                                                            onVoiceDraftCancel()
                                                        }
                                                        return@detectTapGestures
                                                    }
                                                    val hasMicPermission = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.RECORD_AUDIO
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    if (!hasMicPermission) {
                                                        onVoiceDraftPermissionRequested()
                                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                        return@detectTapGestures
                                                    }
                                                    val started = onVoiceDraftStart()
                                                    if (!started) return@detectTapGestures
                                                    val released = tryAwaitRelease()
                                                    if (released) {
                                                        onVoiceDraftFinish()
                                                    } else {
                                                        onVoiceDraftCancel()
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        Modifier.clickable(
                                            enabled = actionEnabled,
                                            onClick = onSend
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isSending -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(SimHomeHeroTokens.BottomProgressSize),
                                        color = palette.progressIndicator,
                                        strokeWidth = 2.dp
                                    )
                                }

                                showVoiceMic -> {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = "Mic",
                                        tint = if (voiceDraftState.isRecording || voiceDraftState.isProcessing) {
                                            palette.sendIconActive
                                        } else {
                                            palette.iconTint
                                        },
                                        modifier = Modifier.size(SimHomeHeroTokens.BottomSendIconSize)
                                    )
                                }

                                else -> {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (actionEnabled) palette.sendIconActive else palette.sendIconInactive,
                                        modifier = Modifier.size(SimHomeHeroTokens.BottomSendIconSize)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class SimMonolithSeamDirection {
    TOP,
    BOTTOM
}

@Composable
private fun SimMonolithSeamOverlay(
    anchorY: Float,
    direction: SimMonolithSeamDirection,
    insideHeight: androidx.compose.ui.unit.Dp,
    outsideHeight: androidx.compose.ui.unit.Dp,
    innerHighlightAlpha: Float,
    outsideHazeAlpha: Float,
    isDarkTheme: Boolean
) {
    val density = LocalDensity.current
    val insideHeightPx = with(density) { insideHeight.roundToPx().toFloat() }
    val outsideHeightPx = with(density) { outsideHeight.roundToPx().toFloat() }
    val overlayOffsetY = when (direction) {
        SimMonolithSeamDirection.TOP -> anchorY - insideHeightPx
        SimMonolithSeamDirection.BOTTOM -> anchorY - outsideHeightPx
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(insideHeight + outsideHeight)
            .offset { IntOffset(x = 0, y = overlayOffsetY.roundToInt()) }
    ) {
        val boundaryY = when (direction) {
            SimMonolithSeamDirection.TOP -> insideHeightPx
            SimMonolithSeamDirection.BOTTOM -> outsideHeightPx
        }
        val outsideHazeColor = if (isDarkTheme) {
            Color.Black.copy(alpha = outsideHazeAlpha)
        } else {
            Color(0xFF6A6A6A).copy(alpha = outsideHazeAlpha)
        }

        when (direction) {
            SimMonolithSeamDirection.TOP -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = innerHighlightAlpha)
                        ),
                        startY = 0f,
                        endY = boundaryY
                    ),
                    size = Size(width = size.width, height = boundaryY)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            outsideHazeColor,
                            Color.Transparent
                        ),
                        startY = boundaryY,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, boundaryY),
                    size = Size(width = size.width, height = size.height - boundaryY)
                )
            }

            SimMonolithSeamDirection.BOTTOM -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            outsideHazeColor
                        ),
                        startY = 0f,
                        endY = boundaryY
                    ),
                    size = Size(width = size.width, height = boundaryY)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = innerHighlightAlpha),
                            Color.Transparent
                        ),
                        startY = boundaryY,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, boundaryY),
                    size = Size(width = size.width, height = size.height - boundaryY)
                )
            }
        }

        drawLine(
            color = Color.White.copy(alpha = innerHighlightAlpha * 0.85f),
            start = Offset(0f, boundaryY),
            end = Offset(size.width, boundaryY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun simHomeHeroPlaceholderBrush(): Brush {
    val palette = rememberSimHomeHeroPalette()
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
            0.0f to palette.placeholderStart,
            0.5f to palette.placeholderCenter,
            1.0f to palette.placeholderStart
        ),
        startX = shimmerOffset.value,
        endX = shimmerOffset.value + 180f
    )
}

@Composable
private fun SimHomeHeroComposerRotatingHint(
    visible: Boolean,
    rotatingHints: List<String>,
    useFullRotation: Boolean
) {
    if (!visible || rotatingHints.isEmpty()) return

    val displayedHints = if (useFullRotation) {
        rotatingHints
    } else {
        rotatingHints.take(1)
    }
    var currentHintIndex by remember(visible, displayedHints) { mutableStateOf(0) }

    LaunchedEffect(visible, displayedHints) {
        currentHintIndex = 0
        while (visible && displayedHints.size > 1) {
            delay(2600L)
            currentHintIndex = (currentHintIndex + 1) % displayedHints.size
        }
    }

    Text(
        text = displayedHints[currentHintIndex],
        style = TextStyle(
            brush = simHomeHeroPlaceholderBrush(),
            fontSize = SimHomeHeroTokens.BottomInputTextSize,
            lineHeight = SimHomeHeroTokens.BottomInputLineHeight
        )
    )
}
