package com.smartsales.feature.designlab.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Stub Design Tokens for Design Lab.
 * TODO: Extract to :core:design-system post-project.
 */
object LabColors {
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
    // Wave Gradients
    // ========================================================================
    val WaveIdle = Brush.horizontalGradient(
        listOf(Color(0xFF007AFF), Color(0xFFA259FF))
    )
    val WaveListening = Brush.horizontalGradient(
        listOf(Color(0xFF34C759), Color(0xFF00C7BE))
    )
    val WaveThinking = Brush.horizontalGradient(
        listOf(Color(0xFFAF52DE), Color(0xFFFF2D55))
    )
    val WaveError = Brush.horizontalGradient(
        listOf(Color(0xFFD70015), Color(0xFFFF3B30))
    )

    // ========================================================================
    // Aurora & Effects
    // ========================================================================
    val AuroraTopLeft = Color(0xFF007AFF).copy(alpha = 0.28f)
    val AuroraCenterRight = Color(0xFF5E5CE6).copy(alpha = 0.24f)
    val AuroraBottomLeft = Color(0xFF00C7BE).copy(alpha = 0.20f)
    val GlassShadow = Color(0xFF007AFF)

    // ========================================================================
    // Crystal Schematic Tokens
    // ========================================================================
    val AuroraMint = Color(0xFF00E676)
    val CrystalCardBg = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val TechnicalBorder = Color(0xFFFFFFFF).copy(alpha = 0.4f)
    val DeepCrystalBg = Color(0xFF0A0A0C).copy(alpha = 0.6f)

    // ========================================================================
    // Chat Bubbles
    // ========================================================================
    val BubbleGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFF5856D6)),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val AvatarBlue = Brush.linearGradient(
        colors = listOf(Color(0xFF5856D6), Color(0xFF007AFF)),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val BubbleShadow = Color(0xFF007AFF).copy(alpha = 0.25f)

    // ========================================================================
    // History Drawer
    // ========================================================================
    val DrawerFooterLight = Color(0xFFE6E6F0)
    val DrawerFooterDark = Color(0xFF141218)
    val SettingsIconTintLight = Color.Black.copy(alpha = 0.4f)
    val SettingsIconTintDark = Color.White.copy(alpha = 0.4f)
    val SettingsIconBgLight = Color.Black.copy(alpha = 0.05f)
    val SettingsIconBgDark = Color.White.copy(alpha = 0.1f)
    val SessionRowActiveBg = Color(0xFF007AFF).copy(alpha = 0.1f)
    val SessionRowHoverBg = Color.White.copy(alpha = 0.05f)

    // ========================================================================
    // Misc
    // ========================================================================
    val StatusDotDisconnectedRing = Color(0xFFFF453A).copy(alpha = 0.2f)
    val DeviceCardBgLight = Color.Black.copy(alpha = 0.02f)
    val DeviceCardBorderLight = Color.Black.copy(alpha = 0.08f)
    
    val DebugDotActive = Color(0xFF34C759)
    val DebugDotInactive = Color.Gray
}
