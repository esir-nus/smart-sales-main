package com.smartsales.feature.chat.home.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for SmartSales "Living Intelligence" Design.
 * 
 * Values are derived from design-tokens.json (base = 8dp).
 * See docs/design/design-tokens.json for the source of truth.
 */
object AppSpacing {
    // Base unit (8dp)
    val Base = 8.dp

    // Scale
    val None = 0.dp
    val XS = 4.dp       // Base * 0.5
    val SM = 8.dp       // Base * 1
    val MD = 16.dp      // Base * 2
    val LG = 24.dp      // Base * 3
    val XL = 32.dp      // Base * 4
    val XXL = 48.dp     // Base * 6
    val XXXL = 64.dp    // Base * 8

    // Animation Durations
    const val AuroraCycleMs = 10000 // 10s (was 20s) - Faster, more alive
    const val ScanCycleMs = 4000   // 4s - Subtle sweep
    
    // Layout
    val ScreenHorizontalPadding = 16.dp
    val ContentVerticalPadding = 24.dp
}

/**
 * Border radius tokens.
 * See docs/design/design-tokens.json for the source of truth.
 */
object AppRadius {
    val None = 0.dp
    val SM = 4.dp
    val MD = 8.dp
    val LG = 12.dp
    val XL = 16.dp
    val XXL = 24.dp
    val Pill = 9999.dp  // Use with .clip(CircleShape) or RoundedCornerShape(Pill)
    
    // Asymmetric Bubbles
    val BubbleCornerLarge = 20.dp
    val BubbleCornerSmall = 4.dp
}

/**
 * Elevation (shadow) tokens.
 * See docs/design/design-tokens.json for the source of truth.
 */
object AppElevation {
    val None = 0.dp
    val SM = 2.dp
    val MD = 4.dp
    val LG = 8.dp
    val XL = 16.dp
}

/**
 * Component-specific dimension tokens.
 * See docs/design/design-tokens.json for the source of truth.
 */
object AppDimensions {
    // Input Bar
    // Input Bar
    val InputBarHeight = 56.dp // T1: Reduced from 64dp
    val InputBarIconSize = 44.dp // Slightly smaller button
    const val InputBarGlassAlpha = 0.85f

    // Knot Symbol
    val KnotSymbolSmall = 40.dp
    val KnotSymbolLarge = 120.dp
    val KnotSymbolStrokeWidth = 3.dp
    const val KnotSymbolBreathingScale = 1.15f

    // Quick Skill Chips
    val QuickSkillChipHeight = 36.dp

    // Header
    val DebugDotSize = 8.dp
}
