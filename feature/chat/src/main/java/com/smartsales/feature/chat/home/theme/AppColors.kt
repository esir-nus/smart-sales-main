package com.smartsales.feature.chat.home.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // ========================================================================
    // Light Mode Palette
    // ========================================================================
    val LightBackground = Color(0xFFF7F7F7)
    val LightSurfaceCard = Color(0xFFFFFFFF)
    val LightSurfaceMuted = Color(0xFFF2F2F7)
    val LightBorderDefault = Color(0xFFE5E5EA)
    
    val LightAccentPrimary = Color(0xFF007AFF)
    val LightTextPrimary = Color(0xFF000000)
    val LightTextSecondary = Color(0xFF3A3A3C)
    val LightTextMuted = Color(0xFF8E8E93)
    val LightDangerText = Color(0xFFEF4444)

    // ========================================================================
    // Dark Mode Palette
    // ========================================================================
    val DarkBackground = Color(0xFF0D0D12)
    val DarkSurfaceCard = Color(0xFF1C1C1E)
    val DarkSurfaceMuted = Color(0xFF2C2C2E)
    val DarkBorderDefault = Color(0xFF38383A)
    
    val DarkAccentPrimary = Color(0xFF0A84FF)
    val DarkTextPrimary = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFFEBEBF5)
    val DarkTextMuted = Color(0xFF98989D)
    val DarkDangerText = Color(0xFFFF453A)

    // ========================================================================
    // Wave Gradients (The Signature)
    // ========================================================================
    // Idle: Blue -> Purple
    val WaveIdle = Brush.horizontalGradient(
        listOf(Color(0xFF007AFF), Color(0xFFA259FF))
    )
    
    // Listening: Green -> Teal
    val WaveListening = Brush.horizontalGradient(
        listOf(Color(0xFF34C759), Color(0xFF00C7BE))
    )
    
    // Thinking: Purple -> Pink
    val WaveThinking = Brush.horizontalGradient(
        listOf(Color(0xFFAF52DE), Color(0xFFFF2D55))
    )
    
    // Error: Red -> Orange
    val WaveError = Brush.horizontalGradient(
        listOf(Color(0xFFD70015), Color(0xFFFF3B30))
    )

    // ========================================================================
    // Hero Gradients
    // ========================================================================
    // Chromatic Text: #00C6FF -> #0072FF (Diagonal 135 deg approx)
    val ChromaticText = Brush.linearGradient(
        colors = listOf(Color(0xFF00C6FF), Color(0xFF0055FF)), // Slightly deeper end blue
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )

    // Aurora Background Colors (Radial) - Boosted for Visibility (V16 Polish)
    val AuroraTopLeft = Color(0xFF007AFF).copy(alpha = 0.28f) // Brighter Tech Blue (Balanced)
    val AuroraCenterRight = Color(0xFF5E5CE6).copy(alpha = 0.24f) // Deep Indigo (Balanced)
    val AuroraBottomLeft = Color(0xFF00C7BE).copy(alpha = 0.20f) // Teal/Cyan pop (Balanced)

    // Debug HUD
    val DebugDotActive = Color(0xFF34C759)
    val DebugDotInactive = Color.Gray
    
    // Glass Effects
    // Glass Effects
    val GlassShadow = Color(0xFF007AFF) // Blue glow for input shadow
    
    // Crystal Schematic Tokens (Pro Max)
    val AuroraMint = Color(0xFF00E676)
    val CrystalCardBg = Color(0xFFFFFFFF).copy(alpha = 0.08f) // Boosted for visibility (was 0.05)
    val TechnicalBorder = Color(0xFFFFFFFF).copy(alpha = 0.4f) // Boosted contrast (was 0.2)
    val DeepCrystalBg = Color(0xFF0A0A0C).copy(alpha = 0.6f) // Darker fallback for blur simulation

    // Chat Bubbles (Aurora)
    val BubbleGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFF5856D6)),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val AvatarBlue = Brush.linearGradient(
        colors = listOf(Color(0xFF5856D6), Color(0xFF007AFF)), // Indigo (TL) -> Blue (BR)
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val BubbleShadow = Color(0xFF007AFF).copy(alpha = 0.25f) // User bubble shadow (Softer)
    // History Drawer
    val DrawerFooterLight = Color(0xFFE6E6F0) // Slight blue-ish grey tint
    val DrawerFooterDark = Color(0xFF141218) // Deep purple/black tint
    val SettingsIconTintLight = Color.Black.copy(alpha = 0.4f)
    val SettingsIconTintDark = Color.White.copy(alpha = 0.4f)
    val SettingsIconBgLight = Color.Black.copy(alpha = 0.05f)
    val SettingsIconBgDark = Color.White.copy(alpha = 0.1f)

    // History Session Rows (V8 Polish)
    val SessionRowActiveBg = Color(0xFF007AFF).copy(alpha = 0.1f)
    val SessionRowHoverBg = Color.White.copy(alpha = 0.05f)

    // Status Dots (V8 Polish)
    val StatusDotDisconnectedRing = Color(0xFFFF453A).copy(alpha = 0.2f)

    // Device Card (V9 Polish)
    val DeviceCardBgLight = Color.Black.copy(alpha = 0.02f) // V10: Reduced from 0.04f
    val DeviceCardBorderLight = Color.Black.copy(alpha = 0.08f) // V10: Reduced from 0.1f
}
