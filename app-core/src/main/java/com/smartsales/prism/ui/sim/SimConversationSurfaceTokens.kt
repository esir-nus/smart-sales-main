package com.smartsales.prism.ui.sim

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import com.smartsales.prism.ui.theme.PrismThemeDefaults

internal object SimConversationSurfaceTokens {
    val Surface = Color(0xFF171B22).copy(alpha = 0.76f)
    val Border = Color.White.copy(alpha = 0.11f)
    val Divider = Color.White.copy(alpha = 0.06f)
    val Title = Color.White.copy(alpha = 0.96f)
    val Body = Color.White.copy(alpha = 0.92f)
    val BodyMuted = Color.White.copy(alpha = 0.72f)
    val Icon = Color.White.copy(alpha = 0.62f)
    val QuietFill = Color.White.copy(alpha = 0.04f)
}

internal data class SimConversationSurfacePalette(
    val surface: Color,
    val border: Color,
    val divider: Color,
    val title: Color,
    val body: Color,
    val bodyMuted: Color,
    val icon: Color,
    val quietFill: Color
)

@Composable
internal fun rememberSimConversationSurfacePalette(): SimConversationSurfacePalette {
    val prismColors = PrismThemeDefaults.colors
    return if (PrismThemeDefaults.isDarkTheme) {
        SimConversationSurfacePalette(
            surface = SimConversationSurfaceTokens.Surface,
            border = SimConversationSurfaceTokens.Border,
            divider = SimConversationSurfaceTokens.Divider,
            title = SimConversationSurfaceTokens.Title,
            body = SimConversationSurfaceTokens.Body,
            bodyMuted = SimConversationSurfaceTokens.BodyMuted,
            icon = SimConversationSurfaceTokens.Icon,
            quietFill = SimConversationSurfaceTokens.QuietFill
        )
    } else {
        SimConversationSurfacePalette(
            surface = Color.White.copy(alpha = 0.72f),
            border = Color.Black.copy(alpha = 0.08f),
            divider = Color.Black.copy(alpha = 0.05f),
            title = prismColors.textPrimary,
            body = prismColors.textPrimary,
            bodyMuted = prismColors.textSecondary,
            icon = prismColors.textSecondary,
            quietFill = Color.Black.copy(alpha = 0.04f)
        )
    }
}
