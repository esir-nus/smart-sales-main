package com.smartsales.feature.media.audio

// 文件：feature/media/src/test/java/com/smartsales/feature/media/audio/AudioFilesScreenTest.kt
// 模块：:feature:media
// 说明：验证 AudioFilesScreen 的转写占位 UI 与交互
// 作者：创建于 2025-11-25

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AudioFilesScreenTest {

    @get:Rule
    val composeRule: ComposeTestRule = createComposeRule()

    @Test
    fun transcribeButton_visibleAndTriggersCallback_whenNone() {
        var transcribeId: String? = null
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "a1",
                                title = "a1",
                                fileName = "a1.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.NONE
                            )
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onDeleteClicked = {},
                    onTranscribeClicked = { transcribeId = it },
                    onTranscriptClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier
                )
            }
        }

        composeRule.onNodeWithTag("${AudioFilesTestTags.TRANSCRIBE_BUTTON_PREFIX}a1").assertIsDisplayed()
        composeRule.onNodeWithText("转写").performClick()
        assertEquals("a1", transcribeId)
    }

    @Test
    fun statusChip_showsForInProgressOrDone() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "p1",
                                title = "p1",
                                fileName = "p1.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.IN_PROGRESS
                            ),
                            AudioRecordingUi(
                                id = "d1",
                                title = "d1",
                                fileName = "d1.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.DONE,
                                transcriptPreview = "示例转写"
                            )
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier
                )
            }
        }

        composeRule.onNodeWithTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}p1").assertIsDisplayed()
        composeRule.onNodeWithText("转写中…").assertIsDisplayed()
        composeRule.onNodeWithTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}d1").assertIsDisplayed()
        composeRule.onNodeWithText("已完成").assertIsDisplayed()
        composeRule.onNodeWithTag("${AudioFilesTestTags.TRANSCRIPT_BUTTON_PREFIX}d1").assertIsDisplayed()
    }
}
