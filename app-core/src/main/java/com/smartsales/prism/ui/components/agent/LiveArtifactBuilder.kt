package com.smartsales.prism.ui.components.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Data Contract for LiveArtifactBuilder.
 * Completely decoupled from ViewModel and network layer.
 */
data class LiveArtifactBuilderState(
    val isVisible: Boolean,
    val title: String,
    val markdownContent: String,
    val conflictNode: ConflictData?
)

data class ConflictData(
    val title: String,
    val description: String,
    val resolvable: Boolean,
    val primaryAction: String,
    val secondaryAction: String
)

/**
 * A specialized payload container showing a document or plan being built.
 * Handles inline conflict resolution CTAs when blocked.
 */
@Composable
fun LiveArtifactBuilder(
    state: LiveArtifactBuilderState,
    onResolveConflict: (ConflictData, Boolean) -> Unit, // true for primary, false for secondary
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    val isBlocked = state.conflictNode != null
    val borderColor = if (isBlocked) Color(0xFFFFAA00) else Color(0xFF4FC3F7)
    val bgColor = Color(0xFF2A2A40) // surface.response

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📄", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body
            Text(
                text = state.markdownContent,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
            
            // Blinking cursor if not blocked
            if (!isBlocked) {
                var cursorVisible by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(500)
                        cursorVisible = !cursorVisible
                    }
                }
                if (cursorVisible) {
                    Text(
                        text = "▌",
                        color = Color(0xFF4FC3F7),
                        fontSize = 14.sp
                    )
                }
            }

            // Conflict node injection
            if (isBlocked && state.conflictNode != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ConflictNodeCard(
                    conflict = state.conflictNode,
                    onPrimaryAction = { onResolveConflict(state.conflictNode, true) },
                    onSecondaryAction = { onResolveConflict(state.conflictNode, false) }
                )
            }
        }
    }
}

@Composable
private fun ConflictNodeCard(
    conflict: ConflictData,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit
) {
    Surface(
        color = Color(0xFF332200),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFFAA00))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚠️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = conflict.title,
                    color = Color(0xFFFFAA00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = conflict.description,
                color = Color(0xFFDDDDDD),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAA00)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = conflict.primaryAction, color = Color.Black, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onSecondaryAction,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFAA00)),
                    border = BorderStroke(1.dp, Color(0xFFFFAA00)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = conflict.secondaryAction, fontSize = 12.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LiveArtifactBuilderBuildingPreview() {
    LiveArtifactBuilder(
        state = LiveArtifactBuilderState(
            isVisible = true,
            title = "Huawei Next Steps Plan",
            markdownContent = "1. Demo Data Security architecture\n2. Bring Systems Architect Chen",
            conflictNode = null
        ),
        onResolveConflict = { _, _ -> }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LiveArtifactBuilderBlockedPreview() {
    LiveArtifactBuilder(
        state = LiveArtifactBuilderState(
            isVisible = true,
            title = "Huawei Next Steps Plan",
            markdownContent = "1. Demo Data Security architecture\n2. Bring Systems Architect Chen",
            conflictNode = ConflictData(
                title = "SCHEDULING CONFLICT",
                description = "Architect Chen is on PTO next Wed.",
                resolvable = true,
                primaryAction = "Reschedule to Thu",
                secondaryAction = "Drop Chen"
            )
        ),
        onResolveConflict = { _, _ -> }
    )
}
