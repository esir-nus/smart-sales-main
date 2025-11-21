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
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.feature.media.devicemanager.DeviceConnectionUiState
import com.smartsales.feature.media.devicemanager.DeviceFileUi
import com.smartsales.feature.media.devicemanager.DeviceManagerUiState
import com.smartsales.feature.media.devicemanager.DeviceMediaTab
import com.smartsales.aitest.devicemanager.DeviceManagerScreen
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

        composeRule.onNodeWithText("设备未连接").assertIsDisplayed()
        composeRule.onNodeWithText("连接 SmartSales 录音设备后即可浏览和刷新文件。").assertIsDisplayed()
        composeRule.onNodeWithTag("device_manager_refresh_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("device_manager_upload_button").assertIsNotEnabled()
    }

    @Test
    fun loadingState_showsProgressAndDisablesButtons() {
        val state = createState(
            connectionStatus = DeviceConnectionUiState.Connected(deviceName = "录音笔"),
            isLoading = true
        )
        renderDeviceManager(state)

        composeRule.onNode(progressMatcher()).assertIsDisplayed()
        composeRule.onNodeWithTag("device_manager_refresh_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("device_manager_upload_button").assertIsNotEnabled()
        composeRule.onNodeWithText("刷新中...").assertIsDisplayed()
        composeRule.onNodeWithText("上传文件").assertIsDisplayed()
    }

    @Test
    fun emptyState_visibleWhenNoFiles() {
        val state = createState(
            connectionStatus = DeviceConnectionUiState.Connected(deviceName = "录音笔"),
            isLoading = false
        )
        renderDeviceManager(state)

        composeRule.onNodeWithTag("device_manager_empty_state").assertIsDisplayed()
        composeRule.onAllNodesWithText("应用").assertCountEquals(0)
    }

    @Test
    fun listState_rendersFilesAndTriggersActions() {
        val files = listOf(
            fileUi(id = "cover.jpg", displayName = "cover.jpg"),
            fileUi(id = "promo.mp4", displayName = "promo.mp4", mimeType = "video/mp4")
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

        composeRule.onNodeWithText("cover.jpg").assertIsDisplayed()
        composeRule.onNodeWithText("promo.mp4").assertIsDisplayed()
        composeRule.onNodeWithTag("device_manager_refresh_button").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("device_manager_upload_button").assertIsEnabled().performClick()

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
                        onSelectTab = {},
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

            composeRule.onNodeWithTag("device_manager_error_banner").assertIsDisplayed()
            composeRule.onNodeWithText("知道了").performClick()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithTag("device_manager_error_banner").assertCountEquals(0)
            assertTrue(cleared)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun renderDeviceManager(
        initialState: DeviceManagerUiState,
        onRefresh: () -> Unit = {},
        onSelectTab: (DeviceMediaTab) -> Unit = {},
        onSelectFile: (String) -> Unit = {},
        onApplyFile: (String) -> Unit = {},
        onDeleteFile: (String) -> Unit = {},
        onRequestUpload: () -> Unit = {},
        onBaseUrlChange: (String) -> Unit = {},
        onClearError: () -> Unit = {}
    ) {
        composeRule.setContent {
            var uiState by remember { mutableStateOf(initialState) }
            MaterialTheme {
                DeviceManagerScreen(
                    state = uiState,
                    onRefresh = onRefresh,
                    onSelectTab = onSelectTab,
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
        errorMessage: String? = null
    ): DeviceManagerUiState {
        return DeviceManagerUiState(
            connectionStatus = connectionStatus,
            baseUrl = "http://10.0.2.2:8000",
            autoDetectedBaseUrl = null,
            isAutoDetectingBaseUrl = false,
            autoDetectStatus = "等待设备联网",
            baseUrlWasManual = true,
            files = files,
            visibleFiles = visibleFiles,
            activeTab = DeviceMediaTab.Images,
            selectedFile = selectedFile,
            isLoading = isLoading,
            isUploading = isUploading,
            errorMessage = errorMessage
        )
    }

    private fun fileUi(
        id: String,
        displayName: String,
        mimeType: String = "image/png",
        isApplied: Boolean = false
    ): DeviceFileUi {
        return DeviceFileUi(
            id = id,
            displayName = displayName,
            sizeText = "1.2MB",
            mimeType = mimeType,
            mediaType = if (mimeType.startsWith("video")) DeviceMediaTab.Videos else DeviceMediaTab.Images,
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
