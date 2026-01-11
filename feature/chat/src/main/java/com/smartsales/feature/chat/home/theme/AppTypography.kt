package com.smartsales.feature.chat.home.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Semantic Typography Roles for SmartSales "Living Intelligence" Design.
 * 
 * See docs/guides/style-guide.md for usage rules.
 */
object AppTypography {
    // We use SansSerif (System default) as a proxy for Inter until font resources are added.
    private val DefaultFont = FontFamily.SansSerif

    // Top bar title: "AI Assistant", "Chat History"
    val AppTitle = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 18.sp, 
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    )

    // "LOGO" replacement / Big Brand Text
    val HeroBrand = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 32.sp, 
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp // T2: Tight tracking
    )

    // "Hello, {userName}"
    val HeroGreeting = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 24.sp, 
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp // T2: Tight tracking
    )

    // "I am your sales assistant"
    val HeroSubtitle = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 17.sp, // T2: Refined size (was 20.sp)
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp
    )

    // Section headers: "Device Manager", etc.
    val SectionTitle = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 16.sp, 
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    )

    // Normal descriptive text
    val Body = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 14.sp, 
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    )

    // Timestamps, subtle metadata
    val Caption = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 12.sp, 
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    )

    // Action Grid Labels (Medium weight)
    val ActionLabel = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    )

    // Button text
    val Button = TextStyle(
        fontFamily = DefaultFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    )
}
