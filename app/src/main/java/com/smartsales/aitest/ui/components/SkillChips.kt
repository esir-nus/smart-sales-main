package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/SkillChips.kt
// 模块：:app
// 说明：技能建议 Chip 列表
// 作者：创建于 2025-12-02

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.screens.home.model.SkillSuggestion

@Composable
fun SkillChips(
    skills: List<SkillSuggestion>,
    onSkillClick: (SkillSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEach { skill ->
            SuggestionChip(
                onClick = { onSkillClick(skill) },
                label = { Text(text = skill.label) },
                colors = SuggestionChipDefaults.suggestionChipColors()
            )
        }
    }
}
