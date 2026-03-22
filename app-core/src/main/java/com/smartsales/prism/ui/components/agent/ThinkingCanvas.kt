package com.smartsales.prism.ui.components.agent

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data Contract for ThinkingCanvas.
 * Completely decoupled from ViewModel and network layer.
 */
data class ThinkingCanvasState(
    val isVisible: Boolean,
    val streamingContent: String,
    val isCollapsed: Boolean
)

/**
 * A fluid container visualizing raw LLM Chain-of-Thought processing.
 * Enforces the Zero-Chrome Policy (no scrollbars, expands fully or collapses).
 */
@Composable
fun ThinkingCanvas(
    state: ThinkingCanvasState,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (state.isCollapsed) {
            // Collapsed Pill (Agent Context)
            Surface(
                modifier = modifier
                    .padding(vertical = 4.dp)
                    .clickable { onToggleExpanded() },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A2E).copy(alpha = 0.8f) // Glassmorphism-esque
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x154FC3F7)
                    ) {
                        Text(
                            text = "CTX",
                            color = Color(0xFF88CCFF),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Agent Context",
                        color = Color(0xFFAAFFAA),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Expanded Canvas (Full Monologue)
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1A1A2E).copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x154FC3F7)
                            ) {
                                Text(
                                    text = "CTX",
                                    color = Color(0xFF88CCFF),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Agent Internal Monologue",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = state.streamingContent,
                        color = Color(0xFFAAFFAA),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ThinkingCanvasExpandedPreview() {
    ThinkingCanvas(
        state = ThinkingCanvasState(
            isVisible = true,
            streamingContent = "> [Tool: search_memory(\"Huawei visit\")]\n> Found entry: CTO Li, security concerns\n> [Tool: check_calendar(\"Next Wed\")]",
            isCollapsed = false
        ),
        onToggleExpanded = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ThinkingCanvasCollapsedPreview() {
    ThinkingCanvas(
        state = ThinkingCanvasState(
            isVisible = true,
            streamingContent = "",
            isCollapsed = true
        ),
        onToggleExpanded = {}
    )
}
