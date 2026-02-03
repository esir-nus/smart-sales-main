package com.smartsales.aitest.ui.home

// 文件：app/src/main/java/com/smartsales/aitest/ui/home/HistoryDrawer.kt
// 模块：:app
// 说明：历史记录侧边栏 — "Sleek Glass" 风格
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.PrismCard
import com.smartsales.aitest.ui.components.PrismSurface
import com.smartsales.aitest.ui.theme.BackgroundSurface
import com.smartsales.aitest.ui.theme.TextMuted
import com.smartsales.aitest.ui.theme.TextPrimary

data class HistoryItem(
    val id: String,
    val title: String,
    val date: String,
    val summary: String
)

/**
 * HistoryDrawer — 历史会话侧边栏
 *
 * 使用 PrismSurface 构建玻璃质感背景，展示历史会话列表
 */
@Composable
fun HistoryDrawer(
    onItemClick: (HistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleHistory = listOf(
        HistoryItem("1", "Strategy Review", "Today", "Discussed Q4 targets..."),
        HistoryItem("2", "Client Prep via Tingwu", "Yesterday", "Analyzed audio recording..."),
        HistoryItem("3", "Competitor Analysis", "Mon", "Reviewing new market entrant...")
    )

    PrismSurface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp),
        backgroundColor = BackgroundSurface.copy(alpha = 0.95f), // Slightly more opaque for drawer
        elevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            LazyColumn(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                items(sampleHistory) { item ->
                    HistoryCard(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    onClick: () -> Unit
) {
    PrismCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.date,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
