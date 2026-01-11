// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/components/ScrollToLatestButton.kt
// 模块：:feature:chat
// 说明：滚动到底部按钮（FAB 样式）
// 作者：从 HomeScreen.kt 提取于 2026-01-11

package com.smartsales.feature.chat.home.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.HomeScreenTestTags

/**
 * 滚动到底部按钮。
 *
 * FAB 样式的浮动按钮，当消息列表未滚动到底部时显示。
 *
 * @param onClick 点击回调
 */
@Composable
fun ScrollToLatestButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag(HomeScreenTestTags.SCROLL_TO_LATEST),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "回到底部",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
