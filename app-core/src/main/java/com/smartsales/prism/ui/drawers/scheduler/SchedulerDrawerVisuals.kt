package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartsales.prism.ui.sim.SimConversationSurfaceTokens
import com.smartsales.prism.ui.sim.SimHomeHeroTokens
import com.smartsales.prism.ui.theme.AccentAmber
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.AccentDanger
import com.smartsales.prism.ui.theme.BackgroundSurface
import com.smartsales.prism.ui.theme.BackgroundSurfaceMuted
import com.smartsales.prism.ui.theme.BorderSubtle
import com.smartsales.prism.ui.theme.TextMuted
import com.smartsales.prism.ui.theme.TextPrimary
import com.smartsales.prism.ui.theme.TextSecondary

enum class SchedulerDrawerVisualMode {
    STANDARD,
    SIM
}

@Immutable
data class SchedulerDrawerVisuals(
    val containerColor: Color,
    val containerBorder: Color,
    val handleColor: Color,
    val handleTrackColor: Color,
    val timeLabelColor: Color,
    val taskTitleColor: Color,
    val taskDoneTitleColor: Color,
    val taskTimeColor: Color,
    val taskContextColor: Color,
    val taskMetaColor: Color,
    val taskSecondaryIconColor: Color,
    val timelineDotColor: Color,
    val timelineLineColor: Color,
    val calendarPrimaryText: Color,
    val calendarSecondaryText: Color,
    val calendarMutedText: Color,
    val calendarWeekdayText: Color,
    val calendarChevronColor: Color,
    val calendarPillColor: Color,
    val calendarPillBorder: Color,
    val calendarSelectedContainer: Color,
    val calendarSelectedText: Color,
    val calendarTodayContainer: Color,
    val calendarTodayText: Color,
    val calendarOutOfBoundsText: Color,
    val attentionNormal: Color,
    val attentionWarning: Color,
    val shelfBackground: Color,
    val shelfCardBackground: Color,
    val shelfTagBackground: Color,
    val shelfTagText: Color,
    val shelfHeaderText: Color,
    val shelfHeaderIcon: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val cardHighlight: Color,
    val cardConflictBackground: Color,
    val cardConflictBorder: Color,
    val conflictBannerBackground: Color,
    val conflictBannerText: Color,
    val multiSelectBarColor: Color,
    val debugPanelColor: Color,
    val debugPanelButtonColor: Color,
    val debugPanelButtonActiveColor: Color,
    val drawerContentHorizontalPadding: Dp,
    val timelineLabelWidth: Dp,
    val timelineAxisWidth: Dp,
    val timelineDotSize: Dp,
    val timelineLineWidth: Dp,
    val timelineTopInset: Dp,
    val timelineRailGap: Dp,
    val timelineCardBottomSpacing: Dp,
    val timelineBottomFadeHeight: Dp,
    val cardCornerRadius: Dp,
    val cardIndicatorWidth: Dp,
    val cardContentHorizontalPadding: Dp,
    val cardContentVerticalPadding: Dp,
    val cardHeaderTimeWidth: Dp,
    val cardHeaderGap: Dp
)

private val StandardSchedulerDrawerVisuals = SchedulerDrawerVisuals(
    containerColor = Color(0xFFF9F9F9),
    containerBorder = BorderSubtle,
    handleColor = BorderSubtle,
    handleTrackColor = BorderSubtle.copy(alpha = 0.5f),
    timeLabelColor = TextMuted,
    taskTitleColor = TextPrimary,
    taskDoneTitleColor = TextMuted,
    taskTimeColor = TextMuted,
    taskContextColor = TextMuted,
    taskMetaColor = TextSecondary,
    taskSecondaryIconColor = TextMuted,
    timelineDotColor = Color(0xFFD1D1D6),
    timelineLineColor = Color(0xFFE5E5EA),
    calendarPrimaryText = TextPrimary,
    calendarSecondaryText = TextSecondary,
    calendarMutedText = TextMuted,
    calendarWeekdayText = TextMuted,
    calendarChevronColor = TextSecondary,
    calendarPillColor = BackgroundSurfaceMuted,
    calendarPillBorder = BorderSubtle,
    calendarSelectedContainer = TextPrimary,
    calendarSelectedText = Color.White,
    calendarTodayContainer = TextMuted,
    calendarTodayText = Color.White,
    calendarOutOfBoundsText = TextMuted.copy(alpha = 0.4f),
    attentionNormal = AccentBlue,
    attentionWarning = AccentAmber,
    shelfBackground = BackgroundSurfaceMuted.copy(alpha = 0.5f),
    shelfCardBackground = BackgroundSurface.copy(alpha = 0.8f),
    shelfTagBackground = AccentBlue.copy(alpha = 0.16f),
    shelfTagText = AccentBlue,
    shelfHeaderText = TextPrimary,
    shelfHeaderIcon = TextMuted,
    cardBackground = Color(0xFF1E1E23).copy(alpha = 0.6f),
    cardBorder = Color.White.copy(alpha = 0.05f),
    cardHighlight = Color.White.copy(alpha = 0.08f),
    cardConflictBackground = Color(0xFFFFD60A).copy(alpha = 0.05f),
    cardConflictBorder = Color(0xFFFFD60A).copy(alpha = 0.5f),
    conflictBannerBackground = Color(0xFFFF9800).copy(alpha = 0.15f),
    conflictBannerText = Color(0xFFFFB74D),
    multiSelectBarColor = Color(0xFFAF52DE),
    debugPanelColor = Color(0xFF2ECC71).copy(alpha = 0.9f),
    debugPanelButtonColor = Color.White.copy(alpha = 0.2f),
    debugPanelButtonActiveColor = Color(0xFFE74C3C).copy(alpha = 0.8f),
    drawerContentHorizontalPadding = 16.dp,
    timelineLabelWidth = 48.dp,
    timelineAxisWidth = 24.dp,
    timelineDotSize = 8.dp,
    timelineLineWidth = 2.dp,
    timelineTopInset = 10.dp,
    timelineRailGap = 8.dp,
    timelineCardBottomSpacing = 16.dp,
    timelineBottomFadeHeight = 40.dp,
    cardCornerRadius = 16.dp,
    cardIndicatorWidth = 2.dp,
    cardContentHorizontalPadding = 12.dp,
    cardContentVerticalPadding = 16.dp,
    cardHeaderTimeWidth = 50.dp,
    cardHeaderGap = 8.dp
)

private val SimSchedulerDrawerVisuals = SchedulerDrawerVisuals(
    containerColor = Color(0xFF121216),
    containerBorder = Color.White.copy(alpha = 0.08f),
    handleColor = Color.White.copy(alpha = 0.20f),
    handleTrackColor = Color.White.copy(alpha = 0.08f),
    timeLabelColor = SimHomeHeroTokens.TextMuted,
    taskTitleColor = SimConversationSurfaceTokens.Title,
    taskDoneTitleColor = SimHomeHeroTokens.TextSecondary,
    taskTimeColor = SimHomeHeroTokens.TextMuted,
    taskContextColor = SimHomeHeroTokens.TextSecondary,
    taskMetaColor = SimHomeHeroTokens.TextSecondary,
    taskSecondaryIconColor = SimHomeHeroTokens.TextMuted,
    timelineDotColor = Color.White.copy(alpha = 0.30f),
    timelineLineColor = Color.White.copy(alpha = 0.10f),
    calendarPrimaryText = SimConversationSurfaceTokens.Title,
    calendarSecondaryText = SimHomeHeroTokens.TextSecondary,
    calendarMutedText = SimHomeHeroTokens.TextMuted,
    calendarWeekdayText = SimHomeHeroTokens.TextMuted,
    calendarChevronColor = SimHomeHeroTokens.TextSecondary,
    calendarPillColor = Color.White.copy(alpha = 0.07f),
    calendarPillBorder = Color.White.copy(alpha = 0.10f),
    calendarSelectedContainer = Color.White,
    calendarSelectedText = SimHomeHeroTokens.MonolithBackground,
    calendarTodayContainer = Color.White.copy(alpha = 0.12f),
    calendarTodayText = SimConversationSurfaceTokens.Title,
    calendarOutOfBoundsText = Color.White.copy(alpha = 0.22f),
    attentionNormal = SimHomeHeroTokens.TextSecondary,
    attentionWarning = AccentDanger,
    shelfBackground = Color.White.copy(alpha = 0.04f),
    shelfCardBackground = Color.White.copy(alpha = 0.05f),
    shelfTagBackground = SimHomeHeroTokens.OutgoingBlue.copy(alpha = 0.16f),
    shelfTagText = SimHomeHeroTokens.OutgoingBlue,
    shelfHeaderText = SimHomeHeroTokens.TextMuted,
    shelfHeaderIcon = SimHomeHeroTokens.TextMuted,
    cardBackground = SimConversationSurfaceTokens.Surface,
    cardBorder = SimConversationSurfaceTokens.Border,
    cardHighlight = Color.White.copy(alpha = 0.10f),
    cardConflictBackground = Color(0xFFFFD60A).copy(alpha = 0.05f),
    cardConflictBorder = Color(0xFFFFD60A).copy(alpha = 0.48f),
    conflictBannerBackground = AccentAmber.copy(alpha = 0.10f),
    conflictBannerText = Color(0xFFFFD479),
    multiSelectBarColor = Color(0xFF6F46D9),
    debugPanelColor = Color.White.copy(alpha = 0.05f),
    debugPanelButtonColor = Color.White.copy(alpha = 0.08f),
    debugPanelButtonActiveColor = AccentDanger.copy(alpha = 0.82f),
    drawerContentHorizontalPadding = 20.dp,
    timelineLabelWidth = 48.dp,
    timelineAxisWidth = 20.dp,
    timelineDotSize = 8.dp,
    timelineLineWidth = 1.dp,
    timelineTopInset = 16.dp,
    timelineRailGap = 16.dp,
    timelineCardBottomSpacing = 16.dp,
    timelineBottomFadeHeight = 48.dp,
    cardCornerRadius = 14.dp,
    cardIndicatorWidth = 4.dp,
    cardContentHorizontalPadding = 16.dp,
    cardContentVerticalPadding = 14.dp,
    cardHeaderTimeWidth = 44.dp,
    cardHeaderGap = 10.dp
)

internal val LocalSchedulerDrawerVisuals = staticCompositionLocalOf { StandardSchedulerDrawerVisuals }
internal val LocalSchedulerDrawerVisualMode = staticCompositionLocalOf { SchedulerDrawerVisualMode.STANDARD }

internal fun schedulerDrawerVisualsFor(mode: SchedulerDrawerVisualMode): SchedulerDrawerVisuals =
    when (mode) {
        SchedulerDrawerVisualMode.STANDARD -> StandardSchedulerDrawerVisuals
        SchedulerDrawerVisualMode.SIM -> SimSchedulerDrawerVisuals
    }

internal val currentSchedulerDrawerVisuals: SchedulerDrawerVisuals
    @Composable
    @ReadOnlyComposable
    get() = LocalSchedulerDrawerVisuals.current

internal val currentSchedulerDrawerVisualMode: SchedulerDrawerVisualMode
    @Composable
    @ReadOnlyComposable
    get() = LocalSchedulerDrawerVisualMode.current
