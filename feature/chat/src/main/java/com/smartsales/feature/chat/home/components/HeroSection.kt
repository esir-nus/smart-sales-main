// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/components/HeroSection.kt
// 模块：:feature:chat
// 说明：Home 页面空状态欢迎区域（Hero Section）
// 作者：从 HomeScreen.kt 重写于 2026-01-11

package com.smartsales.feature.chat.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.theme.AppColors

/**
 * Home 页面空状态欢迎区域。
 *
 * 显示：
 * - 个性化问候语（带 Chromatic 效果）
 * - 副标题介绍
 *
 * @param userName 用户名（用于个性化问候）
 */
@Composable
fun HeroSection(
    userName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeScreenTestTags.HERO)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Text Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Greetings with Chromatic Effect
                val greetingText = buildAnnotatedString {
                    append("你好, ") // Plain text
                    withStyle(SpanStyle(brush = AppColors.ChromaticText)) {
                        append("SmartSales 用户") // Chromatic text
                    }
                }
                Text(
                    text = greetingText,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "我是您的销售助手",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            
            // NOTE: QuickSkillRow is rendered in HomeInputArea as per Design Brief
        }
    }
}
