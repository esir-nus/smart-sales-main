package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.scheduler.ClarificationState
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.TextMuted

@Composable
fun TaskCardClarification(
    clarificationState: ClarificationState,
    onDisambiguationConfirm: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 58.dp, top = 16.dp, bottom = 8.dp)
            .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp))
            .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        when (clarificationState) {
            is ClarificationState.AmbiguousPerson -> {
                Text(
                    text = clarificationState.question,
                    fontSize = 13.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                clarificationState.candidates.forEach { candidate ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFF2A2A35), RoundedCornerShape(6.dp))
                            .clickable { onDisambiguationConfirm(candidate.entityId) }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(text = candidate.displayName, fontSize = 14.sp, color = Color.White)
                            candidate.description?.let {
                                Text(text = it, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
            is ClarificationState.MissingInformation -> {
                Text(
                    text = clarificationState.question,
                    fontSize = 13.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
