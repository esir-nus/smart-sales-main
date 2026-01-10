package com.smartsales.feature.chat.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.theme.AppColors
import com.smartsales.feature.chat.home.theme.AppTypography

/**
 * 2x2 Grid of Quick Actions.
 * Replaces the bullet point list in the Home Empty State.
 */
@Composable
fun ActionGrid(
    onNewTask: () -> Unit,
    onSummarize: () -> Unit,
    onIdeas: () -> Unit,
    onSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                icon = Icons.Outlined.Add,
                label = "New Task",
                onClick = onNewTask,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                icon = Icons.AutoMirrored.Outlined.ShortText,
                label = "Summarize",
                onClick = onSummarize,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                icon = Icons.Outlined.Lightbulb,
                label = "Ideas",
                onClick = onIdeas,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                icon = Icons.Outlined.CalendarToday,
                label = "Schedule",
                onClick = onSchedule,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine colors based on isSystemInDarkTheme() or use a Theme CompositionLocal.
    // Ideally we use MaterialTheme.colorScheme which maps to AppColors.
    // For specific control matching prototype, let's use AppColors.LightSurfaceCard for now, 
    // or rely on MaterialTheme.colorScheme.surface (which should be configured in Theme.kt).
    // Assuming Theme.kt is set up, but strict control:
    
    // We will use MaterialTheme colors here assuming they are wired, 
    // but specific icon color from AppColors.LightAccentPrimary needs dynamic switch?
    // Let's use hard tokens for now as per "Cosmetic Transplant".
    
    // TODO: Dynamic Dark Mode switch requires `isSystemInDarkTheme` check or LocalAppColors composition local.
    // For now, defaulting to Light tokens as proof of concept, or use MaterialTheme proxy.
    val cardBg = AppColors.LightSurfaceCard 
    val borderColor = AppColors.LightBorderDefault
    val iconColor = AppColors.LightAccentPrimary
    val textColor = AppColors.LightTextPrimary
    
    Surface(
        modifier = modifier
            .height(88.dp)
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = AppTypography.ActionLabel,
                color = textColor
            )
        }
    }
}
