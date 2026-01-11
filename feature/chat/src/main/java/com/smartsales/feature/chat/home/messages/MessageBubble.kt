// File: feature/chat/src/main/java/com/smartsales/feature/chat/home/messages/MessageBubble.kt
// Module: :feature:chat
// Summary: Message bubble UI component extracted from HomeScreen
// Author: created on 2026-01-06

package com.smartsales.feature.chat.home.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.MarkdownMessageText
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.smartsales.feature.chat.home.theme.AppColors
import com.smartsales.feature.chat.home.theme.AppRadius

@Composable
internal fun MessageBubble(
    message: ChatMessageUi,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
    reasoningText: String? = null,
    onCopyAssistant: (String) -> Unit = {},
    showRawAssistantOutput: Boolean = false
) {
    // Audit Finding: Accessibility semantics for Error states
    val errorSemantics = if (message.hasError) {
        Modifier.semantics {
            liveRegion = LiveRegionMode.Assertive
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .then(errorSemantics)
            .then(if (alignEnd) Modifier.testTag(HomeScreenTestTags.USER_MESSAGE) else modifier),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // Aurora Styling (V9)
        val backgroundColor = if (alignEnd) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        }

        val border = if (alignEnd) {
            null
        } else {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
        }

        val shape = if (alignEnd) {
            RoundedCornerShape(
                topStart = AppRadius.BubbleCornerLarge,
                topEnd = AppRadius.BubbleCornerSmall,
                bottomEnd = AppRadius.BubbleCornerLarge,
                bottomStart = AppRadius.BubbleCornerLarge
            )
        } else {
            RoundedCornerShape(
                topStart = AppRadius.BubbleCornerSmall,
                topEnd = AppRadius.BubbleCornerLarge,
                bottomEnd = AppRadius.BubbleCornerLarge,
                bottomStart = AppRadius.BubbleCornerLarge
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .then(
                    if (alignEnd) {
                         // User Bubble Gradient + Shadow
                         Modifier
                             .shadow(
                                 elevation = 4.dp,
                                 shape = shape,
                                 spotColor = AppColors.GlassShadow,
                                 ambientColor = AppColors.GlassShadow
                             )
                             .background(
                                 brush = AppColors.BubbleGradient,
                                 shape = shape
                             )
                    } else {
                        // Assistant Bubble Shadow
                        Modifier.shadow(
                             elevation = 1.dp,
                             shape = shape,
                             spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f)
                        )
                    }
                ),
            shape = shape,
            color = backgroundColor,
            border = border,
            shadowElevation = 0.dp // Handled manually for control
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!alignEnd) {
                    var hasCopied by remember { mutableStateOf(false) }
                    LaunchedEffect(hasCopied) {
                        if (hasCopied) {
                            delay(2_000)
                            hasCopied = false
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onCopyAssistant(message.content)
                                hasCopied = true
                            },
                            modifier = Modifier.testTag("${HomeScreenTestTags.ASSISTANT_COPY_PREFIX}${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasCopied) "已复制" else "复制",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // Typewriter Logic for Assistant (Streaming)
                var displayedText by remember(message.id) { mutableStateOf(if (message.isStreaming) "" else message.content) }
                
                LaunchedEffect(message.content, message.isStreaming) {
                    if (message.isStreaming) {
                        val target = message.content
                        if (displayedText.length < target.length) {
                             // Smooth reveal
                             for (i in displayedText.length until target.length) {
                                 displayedText += target[i]
                                 delay(10) // 10ms per char for smooth effect
                             }
                        } else {
                             // Correct any mismatch
                             displayedText = target
                        }
                    } else {
                        // Instant update if not streaming
                        displayedText = message.content
                    }
                }
                
                val finalContent = if (!alignEnd) {
                    if (message.isStreaming) "$displayedText ▋" else displayedText
                } else {
                    message.content
                }

                val renderMarkdown = !alignEnd && !message.isSmartAnalysis && !message.hasError && !showRawAssistantOutput
                if (renderMarkdown) {
                    MarkdownMessageText(
                        text = finalContent
                    )
                } else {
                    Text(
                        text = finalContent,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (alignEnd) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!alignEnd && !message.hasError && !reasoningText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = reasoningText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (message.hasError) {
                    // Error message is already in content, styled differently
                }
            }
        }
    }
}
