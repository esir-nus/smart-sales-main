package com.smartsales.feature.media.audio

// 文件：feature/media/src/test/java/com/smartsales/feature/media/audio/AudioFilesScreenTest.kt
// 模块：:feature:media
// 说明：验证 AudioFilesScreen 的转写占位 UI 与交互
// 作者：创建于 2025-11-25

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartsales.feature.media.audio.AudioFilesTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.Ignore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("UI 覆盖由 instrumentation 提供，Robolectric 版本不再执行")
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "port-xxhdpi",
    manifest = Config.NONE,
    instrumentedPackages = ["androidx.loader.content"]
)
class AudioFilesScreenTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

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
                    onAskAiClicked = {},
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
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier
                )
            }
        }

        composeRule.onNodeWithTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}p1").assertIsDisplayed()
        composeRule.onNodeWithTag(AudioFilesTestTags.RECORDING_LIST)
            .performScrollToNode(hasText("转写中…"))
        composeRule.onNodeWithTag(AudioFilesTestTags.RECORDING_LIST)
            .performScrollToNode(hasText("等待转写完成"))
        composeRule.onNodeWithTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}d1").assertIsDisplayed()
        composeRule.onNodeWithTag(AudioFilesTestTags.RECORDING_LIST)
            .performScrollToNode(hasText("转写完成"))
        composeRule.onNodeWithTag("${AudioFilesTestTags.TRANSCRIPT_BUTTON_PREFIX}d1").assertIsDisplayed()
    }

    @Test
    fun transcriptViewer_showsFullContent_andDismisses() {
        var asked = false
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "d2",
                                title = "d2",
                                fileName = "d2.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.DONE,
                                transcriptPreview = "preview",
                                fullTranscriptMarkdown = "# 标题\n- 行1\n正文"
                            )
                        ),
                        transcriptPreviewRecording = AudioRecordingUi(
                            id = "d2",
                            title = "d2",
                            fileName = "d2.wav",
                            createdAtText = "today",
                            transcriptionStatus = TranscriptionStatus.DONE,
                            transcriptPreview = "preview",
                            fullTranscriptMarkdown = "# 标题\n- 行1\n正文"
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onAskAiClicked = { asked = true },
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.TRANSCRIPT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(AudioFilesTestTags.TRANSCRIPT_CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText("标题").assertIsDisplayed()
        composeRule.onNodeWithText("行1").assertIsDisplayed()
        composeRule.onNodeWithText("正文").assertIsDisplayed()
        composeRule.onNodeWithText("用 AI 分析本次通话").performClick()
        assert(asked)
    }

    @Test
    fun transcriptViewer_showsSummary_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "d3",
                                title = "d3",
                                fileName = "d3.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.DONE,
                                smartSummary = TingwuSmartSummaryUi(
                                    summary = "概览",
                                    keyPoints = listOf("要点A"),
                                    actionItems = listOf("行动1")
                                )
                            )
                        ),
                        transcriptPreviewRecording = AudioRecordingUi(
                            id = "d3",
                            title = "d3",
                            fileName = "d3.wav",
                            createdAtText = "today",
                            transcriptionStatus = TranscriptionStatus.DONE,
                            smartSummary = TingwuSmartSummaryUi(
                                summary = "概览",
                                keyPoints = listOf("要点A"),
                                actionItems = listOf("行动1")
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
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.TRANSCRIPT_SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithText("智能总结").assertIsDisplayed()
        composeRule.onNodeWithText("概览").assertIsDisplayed()
        composeRule.onNodeWithText("要点A").assertIsDisplayed()
        composeRule.onNodeWithText("行动1").assertIsDisplayed()
    }

    @Test
    fun transcriptViewer_hidesSummary_whenAbsent() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "d4",
                                title = "d4",
                                fileName = "d4.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.DONE,
                                transcriptPreview = "preview",
                                fullTranscriptMarkdown = "内容"
                            )
                        ),
                        transcriptPreviewRecording = AudioRecordingUi(
                            id = "d4",
                            title = "d4",
                            fileName = "d4.wav",
                            createdAtText = "today",
                            transcriptionStatus = TranscriptionStatus.DONE,
                            transcriptPreview = "preview",
                            fullTranscriptMarkdown = "内容"
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier
                )
            }
        }

        composeRule.onAllNodesWithTag(AudioFilesTestTags.TRANSCRIPT_SUMMARY).assertCountEquals(0)
        composeRule.onAllNodesWithText("智能总结").assertCountEquals(0)
    }
}
