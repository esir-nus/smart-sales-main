package com.smartsales.feature.chat.prism.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 冲突解决器 — 展示冲突选项供用户选择
 * @see prism-ui-ux-contract.md §3.4 Conflict Resolution
 */
@Composable
fun ConflictResolver(
    title: String,
    options: List<ConflictOption>,
    onConfirm: (ConflictOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf<ConflictOption?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2020)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚠️", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color(0xFFFFAA00),
                    fontSize = 16.sp
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFF5D3030),
                thickness = 1.dp
            )

            // 选项列表
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == option,
                        onClick = { selectedOption = option },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFFFAA00),
                            unselectedColor = Color(0xFF888888)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option.label,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 确认按钮
            Button(
                onClick = { selectedOption?.let { onConfirm(it) } },
                enabled = selectedOption != null,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFAA00),
                    disabledContainerColor = Color(0xFF555555)
                )
            ) {
                Text(
                    text = "确认",
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * 冲突选项
 */
data class ConflictOption(
    val id: String,
    val label: String,
    val resolution: String  // 解决方案描述
)

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ConflictResolverPreview() {
    ConflictResolver(
        title = "时间冲突：李总电话 vs 午餐会议",
        options = listOf(
            ConflictOption("1", "李总电话 优先 (午餐会议取消)", "cancel_lunch"),
            ConflictOption("2", "午餐会议 优先 (李总电话延后)", "defer_call"),
            ConflictOption("3", "我来重新安排...", "manual")
        ),
        onConfirm = {}
    )
}
