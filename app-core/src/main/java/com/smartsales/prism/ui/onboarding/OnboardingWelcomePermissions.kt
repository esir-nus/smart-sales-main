package com.smartsales.prism.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.smartsales.prism.ui.components.ShellLayoutMode
import com.smartsales.prism.ui.components.resolveShellLayoutMode

internal data class PermissionsPrimerMetrics(
    val titleSize: TextUnit,
    val bodySize: TextUnit,
    val bodyLineHeight: TextUnit,
    val topSpacing: Dp,
    val introSpacing: Dp,
    val sectionSpacing: Dp,
    val cardGap: Dp,
    val noteSpacing: Dp,
    val ctaBottomPadding: Dp,
    val scrollBottomClearance: Dp,
    val cardHorizontalPadding: Dp,
    val cardVerticalPadding: Dp,
    val cardCornerRadius: Dp,
    val iconTileSize: Dp,
    val iconSize: Dp,
    val cardTitleSize: TextUnit,
    val cardBodySize: TextUnit,
    val cardBodyLineHeight: TextUnit
)

internal fun permissionsPrimerMetrics(layoutMode: ShellLayoutMode): PermissionsPrimerMetrics =
    when (layoutMode) {
        ShellLayoutMode.TALL -> PermissionsPrimerMetrics(33.sp, 15.sp, 23.sp, 6.dp, 12.dp, 30.dp, 16.dp, 18.dp, 8.dp, 116.dp, 22.dp, 22.dp, 30.dp, 50.dp, 22.dp, 18.sp, 14.sp, 22.sp)
        ShellLayoutMode.COMPACT -> PermissionsPrimerMetrics(30.sp, 14.sp, 22.sp, 2.dp, 10.dp, 22.dp, 12.dp, 14.dp, 8.dp, 108.dp, 20.dp, 18.dp, 28.dp, 46.dp, 20.dp, 17.sp, 13.sp, 20.sp)
        ShellLayoutMode.TIGHT -> PermissionsPrimerMetrics(28.sp, 14.sp, 21.sp, 0.dp, 8.dp, 18.dp, 10.dp, 12.dp, 6.dp, 104.dp, 18.dp, 16.dp, 26.dp, 42.dp, 18.dp, 16.sp, 13.sp, 19.sp)
    }

@Composable
internal fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(OnboardingLogoTile)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("SS", color = OnboardingText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(28.dp))
        Text("SmartSales", color = OnboardingText, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("您的 AI 销售教练", color = OnboardingMuted, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        PrimaryPillButton(
            text = "开启旅程",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        )
    }
}

@Composable
internal fun PermissionsPrimerStep(onContinue: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutMode = resolveShellLayoutMode(availableWidth = maxWidth, availableHeight = maxHeight)
        val metrics = permissionsPrimerMetrics(layoutMode)
        val scrollState = rememberScrollState()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = metrics.scrollBottomClearance),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(Modifier.height(metrics.topSpacing))
                Text("需要一些权限", color = OnboardingText, fontSize = metrics.titleSize, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(metrics.introSpacing))
                Text(
                    text = "为了提供完整体验，我们需要以下基础访问权限。系统会在您即将使用相关功能时，再按需发起授权请求。",
                    color = OnboardingMuted,
                    fontSize = metrics.bodySize,
                    lineHeight = metrics.bodyLineHeight
                )
                Spacer(Modifier.height(metrics.sectionSpacing))
                PermissionPrimerCard(Icons.Default.Mic, "麦克风", "开始语音互动时请求，用于收音、转写和持续语音体验。", metrics)
                Spacer(Modifier.height(metrics.cardGap))
                PermissionPrimerCard(Icons.Default.Bluetooth, "蓝牙", "搜索并连接 SmartBadge 时请求，用于发现附近设备与建立连接。", metrics)
                Spacer(Modifier.height(metrics.cardGap))
                PermissionPrimerCard(Icons.Default.Alarm, "闹钟与提醒", "创建提醒与日程通知时使用，确保关键节点能按时触达。", metrics)
                Spacer(Modifier.height(metrics.noteSpacing))
                FrostedCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.White.copy(alpha = 0.05f),
                    borderColor = Color.White.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = "说明：这一页只做提前说明，不会立刻弹出系统授权。后续在您准备使用相关功能时，Android 才会按需询问。",
                        color = OnboardingMuted.copy(alpha = 0.96f),
                        fontSize = 13.sp,
                        lineHeight = 21.sp
                    )
                }
            }

            PrimaryPillButton(
                text = "继续",
                onClick = onContinue,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .testTag(ONBOARDING_PERMISSIONS_CONTINUE_TEST_TAG)
                    .navigationBarsPadding()
                    .padding(bottom = metrics.ctaBottomPadding)
            )
        }
    }
}

@Composable
internal fun PermissionPrimerCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    metrics: PermissionsPrimerMetrics
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(metrics.cardCornerRadius),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(metrics.cardCornerRadius))
                .padding(horizontal = metrics.cardHorizontalPadding, vertical = metrics.cardVerticalPadding)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(metrics.iconTileSize)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OnboardingText,
                        modifier = Modifier.size(metrics.iconSize)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = OnboardingText, fontSize = metrics.cardTitleSize, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(description, color = OnboardingMuted, fontSize = metrics.cardBodySize, lineHeight = metrics.cardBodyLineHeight)
                }
            }
        }
    }
}
