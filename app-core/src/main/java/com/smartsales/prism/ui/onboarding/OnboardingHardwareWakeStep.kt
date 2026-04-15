package com.smartsales.prism.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HardwareWakeStep(
    onContinue: () -> Unit,
    onSkipToQuickStart: (() -> Unit)? = null,
    skipButtonText: String = "跳过，直接体验日程"
) {
    val transition = rememberInfiniteTransition(label = "hardwareWake")
    val glow by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "hardwareGlow"
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("唤醒您的 SmartBadge", color = OnboardingText, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text("长按中间按钮 3 秒，直到中央蓝灯开始呼吸闪烁。", color = OnboardingMuted, fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(28.dp))
        FrostedCard(modifier = Modifier.fillMaxWidth(), containerColor = OnboardingCardSoft, borderColor = Color.White.copy(alpha = 0.12f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(228.dp).clip(RoundedCornerShape(26.dp)).background(Color(0x100D1118)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(156.dp)
                            .clip(RoundedCornerShape(38.dp))
                            .background(Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.025f))))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(38.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(10.dp).alpha(glow).clip(CircleShape).background(OnboardingBlue))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(14.dp))
                Text("3 秒中心长按", color = OnboardingMuted.copy(alpha = 0.88f), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(28.dp))
        PrimaryPillButton("蓝灯已经在闪了", onContinue, modifier = Modifier.fillMaxWidth())
        onSkipToQuickStart?.let {
            Spacer(Modifier.height(10.dp))
            QuietGhostButton(
                text = skipButtonText,
                onClick = it,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
