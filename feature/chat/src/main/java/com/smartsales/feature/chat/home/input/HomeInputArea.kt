// File: feature/chat/src/main/java/com/smartsales/feature/chat/home/input/HomeInputArea.kt
// Module: :feature:chat
// Summary: Home input area components extracted from HomeScreen
// Author: created on 2026-01-06

package com.smartsales.feature.chat.home.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.QuickSkillUi
import com.smartsales.feature.chat.home.export.ExportGateState

@Composable
internal fun HomeInputArea(
    quickSkills: List<QuickSkillUi>,
    selectedSkill: QuickSkillUi?,
    showQuickSkills: Boolean,
    enabled: Boolean,
    busy: Boolean,
    inputValue: String,
    isSmartAnalysisMode: Boolean,
    exportGateState: ExportGateState?,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    onExportPdfClicked: () -> Unit,
    onExportCsvClicked: () -> Unit,
    onPickAudio: () -> Unit,
    onPickImage: () -> Unit,
    onInputFocusChanged: (Boolean) -> Unit
) {
    var uploadMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val canSend = enabled && !busy && (
        inputValue.isNotBlank() || selectedSkill != null
        )
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            if (showQuickSkills) {
                QuickSkillRow(
                    skills = quickSkills,
                    selectedSkillId = selectedSkill?.id,
                    enabled = enabled && !busy,
                    onQuickSkillSelected = onQuickSkillSelected,
                    onExportPdfClicked = onExportPdfClicked,
                    onExportCsvClicked = onExportCsvClicked
                )
                ExportGateHint(exportGateState = exportGateState)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(
                        onClick = { uploadMenuExpanded = true },
                        enabled = enabled && !busy
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = "上传或附件")
                    }
                    DropdownMenu(
                        expanded = uploadMenuExpanded,
                        onDismissRequest = { uploadMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.AudioFile, contentDescription = null) },
                            text = { Text(text = "上传音频") },
                            onClick = {
                                uploadMenuExpanded = false
                                onPickAudio()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                            text = { Text(text = "上传图片") },
                            onClick = {
                                uploadMenuExpanded = false
                                onPickImage()
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onInputChanged,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onInputFocusChanged(it.isFocused) }
                        .testTag(HomeScreenTestTags.INPUT_FIELD),
                    placeholder = {
                        val hint = if (isSmartAnalysisMode) {
                            "说明你想分析什么（默认分析最近的一段长内容）"
                        } else {
                            "上传文件或输入消息..."
                        }
                        Text(text = hint)
                    },
                    shape = MaterialTheme.shapes.large,
                    enabled = enabled,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    ),
                    singleLine = false,
                    minLines = 2
                )
                TextButton(
                    onClick = onSendClicked,
                    enabled = canSend,
                    modifier = Modifier.testTag(HomeScreenTestTags.SEND_BUTTON)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = if (busy) "发送中" else "发送")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (busy) "发送中..." else "发送")
                }
            }
        }
    }
}

@Composable
private fun ExportActionRow(
    exporting: Boolean,
    onExportPdfClicked: () -> Unit,
    onExportCsvClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onExportPdfClicked,
            enabled = !exporting,
            modifier = Modifier.testTag(HomeScreenTestTags.EXPORT_PDF),
            label = { Text(text = "导出 PDF") }
        )
        AssistChip(
            onClick = onExportCsvClicked,
            enabled = !exporting,
            modifier = Modifier.testTag(HomeScreenTestTags.EXPORT_CSV),
            label = { Text(text = "导出 CSV") }
        )
    }
}

@Composable
private fun AttachmentRow(
    enabled: Boolean,
    onPickAudio: () -> Unit,
    onPickImage: () -> Unit
){
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onPickAudio,
            enabled = enabled,
            label = { Text(text = "上传音频") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = "上传音频"
                )
            }
        )
        AssistChip(
            onClick = onPickImage,
            enabled = enabled,
            label = { Text(text = "上传图片") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "上传图片"
                )
            }
        )
    }
}

@Composable
internal fun QuickSkillRow(
    skills: List<QuickSkillUi>,
    selectedSkillId: QuickSkillId?,
    enabled: Boolean,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    onExportPdfClicked: () -> Unit,
    onExportCsvClicked: () -> Unit
) {
    if (skills.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(skills, key = { it.id }) { skill ->
            val skillTag = "home_quick_skill_${skill.id}"
            val isExportSkill = skill.id == QuickSkillId.EXPORT_PDF || skill.id == QuickSkillId.EXPORT_CSV
            val isSelected = !isExportSkill && skill.id == selectedSkillId
            val skillEnabled = enabled
            val colors = AssistChipDefaults.assistChipColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                labelColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    // 智能分析未选中时保持中性色，避免误导
                    skill.id == QuickSkillId.SMART_ANALYSIS -> MaterialTheme.colorScheme.onSurface
                    skill.isRecommended -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            AssistChip(
                onClick = {
                    if (isExportSkill) {
                        // 导出类技能为立即动作：不改变输入框/选中态。
                        when (skill.id) {
                            QuickSkillId.EXPORT_PDF -> onExportPdfClicked()
                            QuickSkillId.EXPORT_CSV -> onExportCsvClicked()
                            else -> Unit
                        }
                    } else {
                        onQuickSkillSelected(skill.id)
                    }
                },
                enabled = skillEnabled,
                modifier = Modifier
                    .testTag(skillTag)
                    .padding(vertical = 2.dp),
                label = {
                    Text(
                        text = skill.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                // 快捷技能作为快捷填充，无需额外气泡
                colors = colors,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    }
                )
            )
        }
    }
}

@Composable
internal fun ExportGateHint(exportGateState: ExportGateState?) {
    if (exportGateState == null || exportGateState.ready || exportGateState.reason.isBlank()) return
    Text(
        text = exportGateState.reason,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}
