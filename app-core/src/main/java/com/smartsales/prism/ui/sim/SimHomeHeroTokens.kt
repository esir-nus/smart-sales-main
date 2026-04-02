package com.smartsales.prism.ui.sim

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.smartsales.prism.ui.components.ShellLayoutMode

internal data class SimHomeHeroLayoutMetrics(
    val centerCanvasVerticalPadding: Dp,
    val greetingHorizontalPadding: Dp,
    val greetingVerticalPadding: Dp,
    val greetingUpwardOffsetRatio: Float,
    val greetingMaxUpwardOffset: Dp,
    val greetingTitleSize: TextUnit,
    val greetingSubtitleSize: TextUnit,
    val greetingSubtitleTopPadding: Dp,
    val greetingTopBiasPadding: Dp,
    val bottomTopPadding: Dp,
    val bottomBottomPadding: Dp
)

internal object SimHomeHeroTokens {
    val AppBackground = Color(0xFF0D0D12)
    val MonolithBackground = Color(0xFF020205)
    val TextSecondary = Color(0xFF86868B)
    val TextMuted = Color(0xFFAEAEB2)
    val OutgoingBlue = Color(0xFF0A84FF)
    val IslandIdleDot = Color.White.copy(alpha = 0.42f)

    val HeaderHeight = 64.dp
    val HeaderHorizontalPadding = 16.dp
    val HeaderIconTouchSize = 38.dp
    val HeaderIconSize = 21.dp
    val AmbientIconHorizontalOffset = 72.dp
    val AmbientLinkIconSize = 16.dp
    val AmbientBatteryWidth = 18.dp
    val AmbientBatteryHeight = 10.dp
    val AmbientBatteryNubWidth = 2.dp
    val AmbientBatteryNubHeight = 5.dp
    val TopSeamInsideSofteningHeight = 8.dp
    val TopSeamOutsideFeatherHeight = 14.dp
    const val TopSeamInnerHighlightAlpha = 0.04f
    const val TopSeamOutsideHazeAlpha = 0.18f

    val IslandMaxWidth = 240.dp
    val IslandVerticalPadding = 6.dp
    val IslandHorizontalPadding = 14.dp
    val IslandDotCanvasSize = 10.dp
    val IslandDotGap = 8.dp
    val IslandTextSize = 13.sp
    val IslandLetterSpacing = 0.5.sp

    val GreetingHorizontalPadding = 16.dp
    val GreetingVerticalPadding = 10.dp
    val CenterCanvasHorizontalPadding = 16.dp
    val CenterCanvasVerticalPadding = 10.dp
    const val GreetingUpwardOffsetRatio = 0.14f
    val GreetingMaxUpwardOffset = 72.dp
    val GreetingTitleSize = 25.sp
    val GreetingSubtitleSize = 15.sp
    val GreetingSubtitleTopPadding = 8.dp

    val BottomMonolithHeight = 56.dp
    val BottomHorizontalPadding = 16.dp
    val BottomTopPadding = 10.dp
    val BottomBottomPadding = 16.dp
    val BottomIconTouchSize = 36.dp
    val BottomIconSize = 20.dp
    val BottomSendIconSize = 18.dp
    val BottomContentGap = 12.dp
    val BottomInputTextSize = 16.sp
    val BottomInputLineHeight = 24.sp
    val BottomProgressSize = 18.dp
    val BottomSeamInsideSofteningHeight = 10.dp
    val BottomSeamOutsideFeatherHeight = 24.dp
    const val BottomSeamInnerHighlightAlpha = 0.06f
    const val BottomSeamOutsideHazeAlpha = 0.28f

    const val AuroraBlueCenterX = 0.18f
    const val AuroraBlueCenterY = 0.18f
    const val AuroraBlueRadius = 0.62f
    const val AuroraIndigoCenterX = 0.70f
    const val AuroraIndigoCenterY = 0.58f
    const val AuroraIndigoRadius = 0.72f
    const val AuroraCyanCenterX = 0.28f
    const val AuroraCyanCenterY = 0.86f
    const val AuroraCyanRadius = 0.58f
    val AuroraBlueCore = Color(0x470A84FF)
    val AuroraBlueMid = Color(0x140A84FF)
    val AuroraIndigoCore = Color(0x405E5CE6)
    val AuroraIndigoMid = Color(0x185E5CE6)
    val AuroraCyanCore = Color(0x3364D2FF)
    val AuroraCyanMid = Color(0x1264D2FF)

    fun layoutMetrics(layoutMode: ShellLayoutMode): SimHomeHeroLayoutMetrics = when (layoutMode) {
        ShellLayoutMode.TALL -> SimHomeHeroLayoutMetrics(
            centerCanvasVerticalPadding = CenterCanvasVerticalPadding,
            greetingHorizontalPadding = GreetingHorizontalPadding,
            greetingVerticalPadding = GreetingVerticalPadding,
            greetingUpwardOffsetRatio = GreetingUpwardOffsetRatio,
            greetingMaxUpwardOffset = GreetingMaxUpwardOffset,
            greetingTitleSize = GreetingTitleSize,
            greetingSubtitleSize = GreetingSubtitleSize,
            greetingSubtitleTopPadding = GreetingSubtitleTopPadding,
            greetingTopBiasPadding = 0.dp,
            bottomTopPadding = BottomTopPadding,
            bottomBottomPadding = BottomBottomPadding
        )
        ShellLayoutMode.COMPACT -> SimHomeHeroLayoutMetrics(
            centerCanvasVerticalPadding = 8.dp,
            greetingHorizontalPadding = 14.dp,
            greetingVerticalPadding = 8.dp,
            greetingUpwardOffsetRatio = 0.08f,
            greetingMaxUpwardOffset = 36.dp,
            greetingTitleSize = 22.sp,
            greetingSubtitleSize = 14.sp,
            greetingSubtitleTopPadding = 6.dp,
            greetingTopBiasPadding = 0.dp,
            bottomTopPadding = 8.dp,
            bottomBottomPadding = 12.dp
        )
        ShellLayoutMode.TIGHT -> SimHomeHeroLayoutMetrics(
            centerCanvasVerticalPadding = 6.dp,
            greetingHorizontalPadding = 12.dp,
            greetingVerticalPadding = 6.dp,
            greetingUpwardOffsetRatio = 0f,
            greetingMaxUpwardOffset = 0.dp,
            greetingTitleSize = 20.sp,
            greetingSubtitleSize = 13.sp,
            greetingSubtitleTopPadding = 5.dp,
            greetingTopBiasPadding = 52.dp,
            bottomTopPadding = 8.dp,
            bottomBottomPadding = 12.dp
        )
    }
}
