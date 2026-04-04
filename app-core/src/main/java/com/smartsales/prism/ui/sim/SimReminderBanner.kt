package com.smartsales.prism.ui.sim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ReminderBannerBackground = Color(0xD9141416)
private val ReminderBannerBorder = Color(0x1AFFFFFF)
private val ReminderBannerTextPrimary = Color.White
private val ReminderBannerTextSecondary = Color(0xFF86868B)
private val ReminderBannerBlue = Color(0xFF0A84FF)
private val ReminderBannerOrange = Color(0xFFFF9F0A)

@Composable
internal fun SimReminderBannerHost(
    bannerState: SimReminderBannerState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = bannerState != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(260, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(220)),
        modifier = modifier
    ) {
        val state = bannerState ?: return@AnimatedVisibility
        val accent = if (state.accentKind == SimReminderBannerAccent.WARNING) {
            ReminderBannerOrange
        } else {
            ReminderBannerBlue
        }
        val icon = if (state.accentKind == SimReminderBannerAccent.WARNING) {
            Icons.Default.WarningAmber
        } else {
            Icons.Default.Notifications
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ReminderBannerBackground, RoundedCornerShape(20.dp))
                .border(0.5.dp, ReminderBannerBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = state.headline,
                    color = ReminderBannerTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.description,
                    color = ReminderBannerTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
