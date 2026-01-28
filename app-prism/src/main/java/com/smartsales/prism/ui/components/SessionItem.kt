package com.smartsales.prism.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text

/**
 * 会话项 — 最小化单行格式
 * 格式: [ClientName]_[Summary (6 chars)]
 * 双色: 粗体白色标题 + 灰色摘要
 * 
 * 交互:
 * - 点击: 加载会话
 * - 长按: 上下文菜单 (置顶/重命名/删除)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionItem(
    clientName: String,
    summary: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 10.dp)
    ) {
        // 客户名/标题 - 粗体白色
        Text(
            text = clientName,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp
        )
        // 分隔符
        Text(
            text = "_",
            color = Color(0xFF888888),
            fontSize = 14.sp
        )
        // 摘要 - 灰色 (最多6字符)
        Text(
            text = summary.take(6),
            color = Color(0xFF888888),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
