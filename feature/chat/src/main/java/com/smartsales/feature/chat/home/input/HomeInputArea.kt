// File: feature/chat/src/main/java/com/smartsales/feature/chat/home/input/HomeInputArea.kt
// Module: :feature:chat
// Summary: Home input area components extracted from HomeScreen
// Author: created on 2026-01-06

package com.smartsales.feature.chat.home.input

import com.smartsales.feature.chat.home.theme.AppColors // Added for GlassShadow
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
import androidx.compose.foundation.layout.size // Added import
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartsales.domain.config.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.QuickSkillUi
import com.smartsales.domain.export.ExportGateState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import com.smartsales.feature.chat.home.components.KnotSymbol // Imported
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.height
import com.smartsales.feature.chat.home.theme.AppSpacing
import com.smartsales.feature.chat.home.theme.AppRadius
import com.smartsales.feature.chat.home.theme.AppElevation
import com.smartsales.feature.chat.home.theme.AppDimensions

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
    val showSendButton = inputValue.isNotBlank() // Only show Send button when typing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(
                horizontal = AppSpacing.MD, 
                vertical = AppSpacing.LG
            ), // 24dp vertical per prototype
        verticalArrangement = Arrangement.spacedBy(AppSpacing.MD) // 16dp gap
    ) {
        // 1. Quick Skills Docked ABOVE Input
        if (showQuickSkills) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.SM)) {
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
        }

        // 2. Glass Pill Input Bar
        // Simulated Glass: Translucent Surface + Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.InputBarHeight) // 64.dp
                .shadow(
                    elevation = 12.dp, // Increased elevation for float
                    shape = RoundedCornerShape(AppRadius.Pill), 
                    spotColor = AppColors.GlassShadow.copy(alpha = 0.5f), // T2: Blue Glow
                    ambientColor = AppColors.GlassShadow.copy(alpha = 0.3f)
                )
                // Scan Animation Overlay
                .drawWithContent {
                    drawContent()
                    // Translate animation: -100% to 200% width
                    val progress = (System.currentTimeMillis() % 4000L) / 4000f // Simple cycle
                    val startX = -size.width
                    val endX = size.width * 2
                    val currentX = startX + (endX - startX) * progress

                    // Only draw scan line if within the active sweep phase (0-30% of cycle)
                    // The prototype has a wait period. Mapping to simple linear for now as "good enough" for MVP V4.
                    
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        start = Offset(currentX, 0f),
                        end = Offset(currentX + 100f.dp.toPx(), 0f) // approximate 60px width
                    )
                    drawRect(brush = brush, blendMode = BlendMode.Overlay)
                }
                .background(
                    // T3: Frosted Glass (0.50 alpha) - enhanced transparency for Aurora visibility
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.50f), 
                    shape = RoundedCornerShape(AppRadius.Pill)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(AppRadius.Pill)
                )
                .padding(horizontal = AppSpacing.SM), // 8dp internal padding
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS) // 4dp gap
            ) {
                // Left: Attachment Button
                Box {
                    IconButton(
                        onClick = { uploadMenuExpanded = true },
                        enabled = enabled && !busy,
                        modifier = Modifier.size(AppDimensions.InputBarIconSize) // 48dp
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add, 
                            contentDescription = "上传或附件",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                // Middle: Input Field (Transparent)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                   BasicTextField(
                        value = inputValue,
                        onValueChange = onInputChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { onInputFocusChanged(it.isFocused) }
                            .testTag(HomeScreenTestTags.INPUT_FIELD),
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (inputValue.isEmpty()) {
                                Text(
                                    text = if (isSmartAnalysisMode) "说明你想分析什么..." else "输入消息...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                   )
                }

                // Right: Knot Symbol OR Send Button
                if (showSendButton) {
                    IconButton(
                        onClick = onSendClicked,
                        enabled = canSend,
                        modifier = Modifier
                            .size(AppDimensions.InputBarIconSize)
                            .testTag(HomeScreenTestTags.SEND_BUTTON)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Knot Symbol (Breathing/Spinning)
                   Box(
                        modifier = Modifier.size(AppDimensions.InputBarIconSize),
                        contentAlignment = Alignment.Center
                    ) {
                       // Fix T5: Bind animation state
                       KnotSymbol(
                           isThinking = busy,
                           modifier = Modifier.size(AppDimensions.KnotSymbolSmall)
                       ) // 40dp
                    }
                }
            }
            
            // 14.4 Input Scan Shine (Zero State) inside the glass pill
             if (!busy && inputValue.isEmpty() && showQuickSkills) {
                 InputScanShine()
             }
        }
    }
}

@Composable
private fun InputScanShine() {
    val transition = rememberInfiniteTransition(label = "scan_shine")
    val translateAnim by transition.animateFloat(
        initialValue = -100f, // Start off-screen left
        targetValue = 1000f,  // End off-screen right
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                translate(left = translateAnim) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.3f), // Shine color
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 200f // Width of shine
                        ),
                        // Assuming the Box fills the parent (Input Area Surface)
                        // Verify size.width is sufficient
                    )
                }
            }
    )
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
        horizontalArrangement = Arrangement.spacedBy(12.dp), // T4: Increased Gap to 12dp
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(skills, key = { it.id }) { skill ->
            val isExportSkill = skill.id == QuickSkillId.EXPORT_PDF || skill.id == QuickSkillId.EXPORT_CSV
            val isSelected = !isExportSkill && skill.id == selectedSkillId
            val skillEnabled = enabled
            val skillTag = if (isExportSkill) "export_skill_${skill.id}" else "quick_skill_${skill.id}"
            
            // Interaction State for Lift Effect
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val liftOffset by animateDpAsState(if (isPressed) (-2).dp else 0.dp, label = "lift")

            AssistChip(
                onClick = {
                    if (isExportSkill) {
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
                interactionSource = interactionSource,
                modifier = Modifier
                    .testTag(skillTag)
                    .height(AppDimensions.QuickSkillChipHeight)
                    .offset(y = liftOffset), // T4: Chip Lift (No shadow - clean glass per V8)
                label = {
                    Text(
                        text = skill.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        // T1: Glass Style (0.65 alpha) - Match Web Prototype --glass-bg
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    },
                    labelColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        skill.id == QuickSkillId.SMART_ANALYSIS -> MaterialTheme.colorScheme.primary // Highlight analysis
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        // T1: Cut Glass Border - Match Web Prototype --glass-border (White @ 0.4)
                        Color.White.copy(alpha = 0.4f)
                    }
                ),
                shape = RoundedCornerShape(AppRadius.Pill)
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
        modifier = Modifier.padding(horizontal = AppSpacing.SM)
    )
}
