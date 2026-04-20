package com.smartsales.prism.ui.sim

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.core.content.ContextCompat
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandUiState
import com.smartsales.prism.ui.components.DynamicIslandVisualState
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
internal const val SIM_HEADER_LEFT_AMBIENT_ICON_TEST_TAG = "sim_header_left_ambient_icon"
internal const val SIM_HEADER_RIGHT_AMBIENT_ICON_TEST_TAG = "sim_header_right_ambient_icon"

private val SimHomeHeroAmbientEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

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
    dynamicIslandState: DynamicIslandUiState,
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
        dynamicIslandState = dynamicIslandState,
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
    dynamicIslandState: DynamicIslandUiState,
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
                dynamicIslandState = dynamicIslandState,
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
    dynamicIslandState: DynamicIslandUiState,
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
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
    val visibleItem = (dynamicIslandState as? DynamicIslandUiState.Visible)?.item
    val showAmbientFlanks = visibleItem?.visualState == DynamicIslandVisualState.CONNECTIVITY_CONNECTED ||
        visibleItem?.visualState == DynamicIslandVisualState.CONNECTIVITY_PARTIAL
    val wifiConnected = visibleItem?.visualState == DynamicIslandVisualState.CONNECTIVITY_CONNECTED
    val bleTint = if (showAmbientFlanks) Color(0xFF34C759) else Color.White.copy(alpha = 0.30f)
    val ambientBatteryPercentage = visibleItem?.batteryPercentage ?: 78
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
                    elevation = if (isDarkTheme) 0.dp else 1.dp,
                    shape = RectangleShape,
                    clip = false,
                    ambientColor = palette.monolithShadow,
                    spotColor = palette.monolithShadow
                )
                .background(palette.monolithBackground)
                .drawBehind {
                    if (!isDarkTheme) {
                        drawLine(
                            color = palette.monolithStroke,
                            start = Offset(0f, size.height - 1f),
                            end = Offset(size.width, size.height - 1f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                .onGloballyPositioned { coordinates ->
                    onBoundsChanged(coordinates.boundsInRoot())
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .prismStatusBarPadding()
                    .height(SimHomeHeroTokens.HeaderHeight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(SimHomeHeroTokens.HeaderCenterClusterFillFraction)
                                .widthIn(max = SimHomeHeroTokens.HeaderCenterClusterMaxWidth)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            SimHomeHeroDynamicIsland(
                                state = dynamicIslandState,
                                onTap = onSchedulerClick,
                                enablePullGesture = enablePullGesture,
                                onPullOpen = onPullOpen
                            )

                            SimHomeHeroAmbientFlankIcon(
                                visible = showAmbientFlanks,
                                alignment = Alignment.CenterStart,
                                offsetX = SimHomeHeroTokens.AmbientClusterInset,
                                delayMillis = 100,
                                testTag = SIM_HEADER_LEFT_AMBIENT_ICON_TEST_TAG
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = bleTint,
                                    modifier = Modifier.size(SimHomeHeroTokens.AmbientLinkIconSize)
                                )
                            }

                            SimHomeHeroAmbientFlankIcon(
                                visible = showAmbientFlanks,
                                alignment = Alignment.CenterEnd,
                                offsetX = -SimHomeHeroTokens.AmbientClusterInset,
                                delayMillis = 0,
                                testTag = SIM_HEADER_RIGHT_AMBIENT_ICON_TEST_TAG
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (wifiConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                        contentDescription = null,
                                        tint = if (wifiConnected) Color(0xFF34C759) else Color(0xFFFF453A),
                                        modifier = Modifier.size(SimHomeHeroTokens.AmbientLinkIconSize)
                                    )
                                    SimHomeHeroAmbientBatteryGlyph(
                                        percentage = ambientBatteryPercentage,
                                        accentColor = Color(0xFF34C759)
                                    )
                                }
                            }
                        }
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
}

@Composable
private fun BoxScope.SimHomeHeroAmbientFlankIcon(
    visible: Boolean,
    alignment: Alignment,
    offsetX: androidx.compose.ui.unit.Dp,
    delayMillis: Int,
    testTag: String,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX),
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 400,
                delayMillis = delayMillis,
                easing = SimHomeHeroAmbientEasing
            )
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = 400,
                delayMillis = delayMillis,
                easing = SimHomeHeroAmbientEasing
            ),
            initialScale = 0.95f
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 300,
                easing = SimHomeHeroAmbientEasing
            )
        ) + scaleOut(
            animationSpec = tween(
                durationMillis = 300,
                easing = SimHomeHeroAmbientEasing
            ),
            targetScale = 0.95f
        )
    ) {
        val transition = rememberInfiniteTransition(label = "sim_home_hero_ambient_breathe")
        val breatheScale by transition.animateFloat(
            initialValue = SimHomeHeroTokens.AmbientBreatheMinScale,
            targetValue = SimHomeHeroTokens.AmbientBreatheMaxScale,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = SimHomeHeroTokens.AmbientBreatheDurationMillis,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sim_home_hero_ambient_scale"
        )
        val breatheAlpha by transition.animateFloat(
            initialValue = SimHomeHeroTokens.AmbientBreatheMinAlpha,
            targetValue = SimHomeHeroTokens.AmbientBreatheMaxAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = SimHomeHeroTokens.AmbientBreatheDurationMillis,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sim_home_hero_ambient_alpha"
        )

        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = breatheAlpha
                    scaleX = breatheScale
                    scaleY = breatheScale
                }
                .testTag(testTag)
        ) {
            content()
        }
    }
}

@Composable
private fun SimHomeHeroDynamicIsland(
    state: DynamicIslandUiState,
    onTap: (DynamicIslandTapAction) -> Unit,
    enablePullGesture: Boolean = false,
    onPullOpen: () -> Unit = {}
) {
    val currentItem = (state as? DynamicIslandUiState.Visible)?.item ?: return
    val palette = rememberSimHomeHeroPalette()
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
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
    val breathe by rememberInfiniteTransition(label = "sim_home_hero_island_breathe").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sim_home_hero_island_breathe_alpha"
    )
    var visibleText by remember(currentItem.stableKey) {
        mutableStateOf(if (currentItem.isSessionTitleItem) "" else currentItem.displayText)
    }
    LaunchedEffect(currentItem.stableKey) {
        if (!currentItem.isSessionTitleItem) {
            visibleText = currentItem.displayText
            return@LaunchedEffect
        }
        visibleText = ""
        currentItem.displayText.forEachIndexed { index, _ ->
            delay(28L)
            visibleText = currentItem.displayText.take(index + 1)
        }
    }

    SimVerticalDragTrigger(
        modifier = Modifier
            .widthIn(
                min = SimHomeHeroTokens.IslandMinWidth,
                max = SimHomeHeroTokens.IslandMaxWidth
            )
            .testTag(SIM_DYNAMIC_ISLAND_TEST_TAG),
        direction = DOWN,
        threshold = 40.dp,
        velocityThreshold = 1100.dp,
        enabled = enablePullGesture,
        onTriggered = onPullOpen
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 28.dp)
                .clickable { onTap(currentItem.tapAction) }
                .padding(
                    horizontal = SimHomeHeroTokens.IslandHorizontalPadding,
                    vertical = SimHomeHeroTokens.IslandVerticalPadding
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentItem.showsAudioIndicator) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = chroma.dot,
                    modifier = Modifier.size(14.dp)
                )
                Box(modifier = Modifier.width(6.dp))
            }
            Canvas(modifier = Modifier.size(SimHomeHeroTokens.IslandDotCanvasSize)) {
                val outerAlpha = when {
                    currentItem.usesBreathing -> 0.22f * breathe
                    currentItem.usesPulse -> 0.32f * pulse
                    else -> 0.18f
                }
                val innerAlpha = when {
                    currentItem.usesBreathing -> breathe
                    currentItem.usesPulse -> pulse
                    else -> 1f
                }
                drawCircle(
                    color = chroma.dot.copy(alpha = outerAlpha),
                    radius = size.minDimension * 0.5f
                )
                drawCircle(
                    color = chroma.dot.copy(alpha = innerAlpha),
                    radius = size.minDimension * 0.30f
                )
            }
            Box(modifier = Modifier.width(SimHomeHeroTokens.IslandDotGap))
            Text(
                text = visibleText,
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

@Composable
private fun SimHomeHeroAmbientBatteryGlyph(
    percentage: Int,
    accentColor: Color
) {
    val boundedPercentage = percentage.coerceIn(0, 100)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(
                    width = SimHomeHeroTokens.AmbientBatteryWidth,
                    height = SimHomeHeroTokens.AmbientBatteryHeight
                )
                .border(
                    width = 1.5.dp,
                    color = accentColor,
                    shape = RoundedCornerShape(2.dp)
                )
                .drawBehind {
                    drawRoundRect(
                        color = accentColor,
                        size = Size(width = size.width * (boundedPercentage / 100f), height = size.height),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {}
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .size(
                    width = SimHomeHeroTokens.AmbientBatteryNubWidth,
                    height = SimHomeHeroTokens.AmbientBatteryNubHeight
                )
                .background(
                    color = accentColor,
                    shape = RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp)
                )
        )
    }
}

private data class SimHomeHeroIslandChroma(
    val dot: Color,
    val textGradient: List<Color>
) {
    companion object {
        fun from(
            item: DynamicIslandItem,
            palette: SimHomeHeroPalette,
            isDarkTheme: Boolean
        ): SimHomeHeroIslandChroma {
            return when (item.visualState) {
                DynamicIslandVisualState.SCHEDULER_CONFLICT -> SimHomeHeroIslandChroma(
                    dot = Color(0xFFFFD60A),
                    textGradient = listOf(Color(0xFFC93400), Color(0xFFFF9500))
                )
                DynamicIslandVisualState.SCHEDULER_IDLE -> SimHomeHeroIslandChroma(
                    dot = palette.islandIdleDot,
                    textGradient = listOf(palette.iconTint, palette.greetingSubtitle)
                )
                DynamicIslandVisualState.SESSION_TITLE_HIGHLIGHT -> SimHomeHeroIslandChroma(
                    dot = SimHomeHeroTokens.OutgoingBlue,
                    textGradient = listOf(Color(0xFF7BC0FF), SimHomeHeroTokens.OutgoingBlue)
                )
                DynamicIslandVisualState.CONNECTIVITY_CONNECTED -> SimHomeHeroIslandChroma(
                    dot = Color(0xFF34C759),
                    textGradient = listOf(Color(0xFFA4E38A), Color(0xFF34C759))
                )
                DynamicIslandVisualState.CONNECTIVITY_PARTIAL -> SimHomeHeroIslandChroma(
                    dot = Color(0xFFFF9F0A),
                    textGradient = listOf(Color(0xFFFFD380), Color(0xFFFF9F0A))
                )
                DynamicIslandVisualState.CONNECTIVITY_DISCONNECTED -> SimHomeHeroIslandChroma(
                    dot = Color.White.copy(alpha = 0.30f),
                    textGradient = listOf(Color(0xFFA0A0A5), Color(0xFF86868B))
                )
                DynamicIslandVisualState.CONNECTIVITY_RECONNECTING -> SimHomeHeroIslandChroma(
                    dot = Color(0xFF34C759),
                    textGradient = listOf(Color(0xFF86EFAC), Color(0xFF34C759))
                )
                DynamicIslandVisualState.CONNECTIVITY_NEEDS_SETUP -> SimHomeHeroIslandChroma(
                    dot = Color(0xFFFF9F0A),
                    textGradient = listOf(Color(0xFFFFD60A), Color(0xFFFF9F0A))
                )
                DynamicIslandVisualState.SCHEDULER_UPCOMING -> SimHomeHeroIslandChroma(
                    dot = if (isDarkTheme) Color(0xFFFF453A) else Color(0xFFD70015),
                    textGradient = if (isDarkTheme) {
                        listOf(Color(0xFFFF8A84), Color(0xFFFF453A))
                    } else {
                        listOf(Color(0xFFD70015), Color(0xFFFF3B30))
                    }
                )
                DynamicIslandVisualState.SYNC_IN_PROGRESS -> SimHomeHeroIslandChroma(
                    dot = Color(0xFF38BDF8),
                    textGradient = listOf(Color(0xFFA7D8F0), Color(0xFF38BDF8))
                )
                DynamicIslandVisualState.SYNC_COMPLETE -> SimHomeHeroIslandChroma(
                    dot = Color(0xFF34C759),
                    textGradient = listOf(Color(0xFFA4E38A), Color(0xFF34C759))
                )
                DynamicIslandVisualState.SYNC_UP_TO_DATE -> SimHomeHeroIslandChroma(
                    dot = Color.White.copy(alpha = 0.42f),
                    textGradient = listOf(Color.White, Color(0xFFA0A0A5))
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
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(SimHomeHeroTokens.HeaderIconTouchSize)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
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
        // 空首页问候语在常规手机高度下保持原型的居中节奏，不复用更激进的窄宽度压缩。
        val layoutMode = SimHomeHeroTokens.resolveGreetingLayoutMode(
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
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onVoiceDraftPermissionResult(granted)
    }
    val haptic = LocalHapticFeedback.current
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
                        elevation = if (isDarkTheme) 0.dp else 2.dp,
                        shape = RectangleShape,
                        clip = false,
                        ambientColor = palette.monolithShadow,
                        spotColor = palette.monolithShadow
                    )
                    .background(palette.monolithBackground)
                    .drawBehind {
                        if (!isDarkTheme) {
                            drawLine(
                                color = palette.monolithStroke,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
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
                            .heightIn(min = SimHomeHeroTokens.BottomMonolithHeight, max = 140.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(SimHomeHeroTokens.BottomIconTouchSize)
                                .testTag(SIM_ATTACH_BUTTON_TEST_TAG)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAttachClick()
                                },
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
                                singleLine = false,
                                maxLines = 4,
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
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onSend()
                                            }
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
