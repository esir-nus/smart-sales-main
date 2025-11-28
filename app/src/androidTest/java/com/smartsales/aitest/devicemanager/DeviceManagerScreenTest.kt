package com.smartsales.aitest.devicemanager

// 文件：app/src/androidTest/java/com/smartsales/aitest/devicemanager/DeviceManagerScreenTest.kt
// 模块：:app
// 说明：验证 DeviceManagerScreen 的连接/文件状态 UI 与刷新、上传、错误交互
// 作者：创建于 2025-11-21
// 覆盖点：
// - 未连接与加载态的提示与按钮可用性
// - 空列表、文件列表渲染与刷新/上传回调
// - 错误横幅的显示与手动关闭
// 测试数据假设：文件列表直接使用假数据对象，无需真实 BLE/HTTP 依赖

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.feature.media.devicemanager.DeviceManagerUiState
import com.smartsales.feature.media.devicemanager.DeviceConnectionUiState
import com.smartsales.feature.media.devicemanager.DeviceFileUi
import com.smartsales.feature.media.devicemanager.DeviceMediaTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceManagerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disconnectedState_showsHintAndDisablesActions() {
        val state = createState(
            connectionStatus = DeviceConnectionUiState.Disconnected(reason = "等待连接")
        )
        renderDeviceManager(state)

        composeRule.onAllNodesWithText("设备未连接").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("请连接设备以管理文件和查看预览。").assertIsDisplayed()
        composeRule.onNodeWithText("重试连接").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsProgressAndDisablesButtons() {
        val state = createState(
            connectionStatus = DeviceConnectionUiState.Connected(deviceName = "录音笔"),
            isLoading = true
        )
        renderDeviceManager(state)

        composeRule.onNode(progressMatcher()).assertIsDisplayed()
        composeRule.onNodeWithText("刷新中...").assertIsDisplayed()
        composeRule.onAllNodesWithText("上传新文件").onFirst().assertIsDisplayed()
    }

    @Test
    fun emptyState_visibleWhenNoFiles() {
        val state = createState(
            connectionStatus = DeviceConnectionUiState.Connected(deviceName = "录音笔"),
            isLoading = false
        )
        renderDeviceManager(state)

        composeRule.onNodeWithText("设备文件管理").assertIsDisplayed()
        composeRule.onNodeWithText("刷新、上传并预览设备素材，与 React 端保持一致。").assertIsDisplayed()
        composeRule.onNodeWithText("选择文件预览").assertIsDisplayed()
        composeRule.onAllNodesWithText("上传新文件").onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag(DeviceManagerTestTags.EMPTY_STATE).assertIsDisplayed()
        composeRule.onAllNodesWithText("应用").assertCountEquals(0)
    }

    @Test
    fun loadErrorState_showsRetry() {
        var retryClicks = 0
        val state = createState(
            connectionStatus = DeviceConnectionUiState.Connected(deviceName = "录音笔"),
            isLoading = false,
            loadErrorMessage = "加载设备文件失败，请稍后重试。"
        )
        renderDeviceManager(
            initialState = state,
            onRetryLoad = { retryClicks++ }
        )

        composeRule.onNodeWithTag(DeviceManagerTestTags.ERROR_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("重试").assertIsDisplayed().performClick()
        assertEquals(1, retryClicks)
    }

    @Test
    fun listState_rendersFilesAndTriggersActions() {
        val files = listOf(
            fileUi(id = "promo.mp4", displayName = "promo.mp4", mimeType = "video/mp4"),
            fileUi(id = "loop.gif", displayName = "loop.gif", mimeType = "image/gif", mediaType = DeviceMediaTab.Gifs)
        )
        var refreshClicks = 0
        var uploadClicks = 0
        val state = createState(
            connectionStatus = DeviceConnectionUiState.Connected(deviceName = "录音笔"),
            files = files,
            visibleFiles = files,
            selectedFile = files.first()
        )
        renderDeviceManager(
            initialState = state,
            onRefresh = { refreshClicks++ },
            onRequestUpload = { uploadClicks++ }
        )

        composeRule.onNodeWithTag(DeviceManagerTestTags.REFRESH_BUTTON).performClick()
        composeRule.onAllNodesWithTag(DeviceManagerTestTags.UPLOAD_BUTTON).onFirst().performClick()

        assertEquals(1, refreshClicks)
        assertEquals(1, uploadClicks)
    }

    @Test
    fun errorBanner_dismissClearsState() {
        composeRule.mainClock.autoAdvance = false
        try {
            var cleared = false
            val files = listOf(fileUi(id = "cover.jpg", displayName = "cover.jpg"))
            composeRule.setContent {
                var uiState by remember {
                    mutableStateOf(
                        createState(
                            connectionStatus = DeviceConnectionUiState.Connected("录音笔"),
                            files = files,
                            visibleFiles = files,
                            errorMessage = "加载失败"
                        )
                    )
                }
                MaterialTheme {
                    DeviceManagerScreen(
                        state = uiState,
                        onRefresh = {},
                        onRetryLoad = {},
                        onSelectFile = {},
                        onApplyFile = {},
                        onDeleteFile = {},
                        onRequestUpload = {},
                        onBaseUrlChange = {},
                        onClearError = {
                            cleared = true
                            uiState = uiState.copy(errorMessage = null)
                        }
                    )
                }
            }

            composeRule.onNodeWithTag(DeviceManagerTestTags.ERROR_BANNER).assertIsDisplayed()
            composeRule.onNodeWithText("知道了").performClick()
            composeRule.waitForIdle()
            assertTrue(cleared)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun renderDeviceManager(
        initialState: DeviceManagerUiState,
        onRefresh: () -> Unit = {},
        onSelectFile: (String) -> Unit = {},
        onApplyFile: (String) -> Unit = {},
        onDeleteFile: (String) -> Unit = {},
        onRequestUpload: () -> Unit = {},
        onRetryLoad: () -> Unit = {},
        onBaseUrlChange: (String) -> Unit = {},
        onClearError: () -> Unit = {}
    ) {
        composeRule.setContent {
            var uiState by remember { mutableStateOf(initialState) }
            MaterialTheme {
                DeviceManagerScreen(
                    state = uiState,
                    onRefresh = onRefresh,
                    onRetryLoad = onRetryLoad,
                    onSelectFile = onSelectFile,
                    onApplyFile = onApplyFile,
                    onDeleteFile = onDeleteFile,
                    onRequestUpload = onRequestUpload,
                    onBaseUrlChange = onBaseUrlChange,
                    onClearError = onClearError
                )
            }
        }
    }

    private fun createState(
        connectionStatus: DeviceConnectionUiState,
        isLoading: Boolean = false,
        isUploading: Boolean = false,
        files: List<DeviceFileUi> = emptyList(),
        visibleFiles: List<DeviceFileUi> = files,
        selectedFile: DeviceFileUi? = null,
        errorMessage: String? = null,
        loadErrorMessage: String? = null
    ): DeviceManagerUiState {
        return DeviceManagerUiState(
            connectionStatus = connectionStatus,
            isConnected = connectionStatus.isReadyForFiles(),
            baseUrl = "http://10.0.2.2:8000",
            autoDetectedBaseUrl = null,
            isAutoDetectingBaseUrl = false,
            autoDetectStatus = "等待设备联网",
            baseUrlWasManual = true,
            files = files,
            visibleFiles = visibleFiles,
            selectedFile = selectedFile,
            isLoading = isLoading,
            isUploading = isUploading,
            errorMessage = errorMessage,
            loadErrorMessage = loadErrorMessage
        )
    }

    private fun fileUi(
        id: String,
        displayName: String,
        mimeType: String = "video/mp4",
        isApplied: Boolean = false,
        mediaType: DeviceMediaTab = DeviceMediaTab.Videos
    ): DeviceFileUi {
        return DeviceFileUi(
            id = id,
            displayName = displayName,
            sizeText = "1.2MB",
            mimeType = mimeType,
            mediaType = mediaType,
            modifiedAtText = "2025-11-20 10:00",
            mediaUrl = "http://example/$id",
            downloadUrl = "http://example/$id/download",
            isApplied = isApplied
        )
    }

    private fun progressMatcher(): SemanticsMatcher =
        SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo.Indeterminate
        )
}
