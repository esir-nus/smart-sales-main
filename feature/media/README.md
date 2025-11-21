<!-- 文件路径: feature/media/README.md -->
<!-- 文件作用: 描述媒体同步模块的职责与流程 -->
<!-- 文件目的: 指导音频下拉和图像编辑器占位实现 -->
<!-- 相关文件: docs/current-state.md, feature/connectivity/README.md, data/ai-core/README.md, plans/dev_plan.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# Media Feature 模块（DeviceManager + AudioFiles）

## 模块职责
- 负责媒体资源的抓取、展示与操作：DeviceManager（图像/视频等静态文件）与 AudioFiles（录音 + Tingwu 转写）。
- 向 UI 暴露 `MediaSyncCoordinator`（聊天首页假数据）与新的 `AudioFilesViewModel`、`AudioPlaybackController`。
- 不直接管理 BLE/Wi-Fi，但依赖 `DeviceConnectionManager` 或 WiFi/BLE Tester 提供的 baseUrl；与 AI 交互仅限转写/上传。

## 关键接口与依赖
- `MediaSyncCoordinator`：向 Home 横幅提供 Fake clip 数据。
- `DeviceManagerViewModel`：拉设备媒体列表 + 上传/删除/应用，依赖 `DeviceMediaGateway`（App 中由 `MediaServerClient` 实现）。
- `AudioFilesViewModel`：新加入，协调 MediaServer 文件 → 本地缓存 → OSS 上传 → Tingwu 提交 → AudioFilesUiState；播放通过 `AudioPlaybackController`（App 层实现为 `MediaPlayerAudioPlaybackController`）。
- 依赖：`projects.core.util`（Result/Dispatchers）、`projects.data.aiCore`（OssUploadClient、TingwuCoordinator）、`projects.feature.connectivity`（连接状态）、`MediaServerClient`（位于 :app）。

## 数据与控制流
### DeviceManager
1. `DeviceManagerViewModel.onRefreshFiles()` → `DeviceMediaGateway.fetchFiles(baseUrl)` → 更新 `DeviceManagerUiState.files/visibleFiles`。
2. 上传/删除/应用直接调用 Gateway → 成功后刷新列表。
3. BaseUrl 自动检测通过 `DeviceConnectionManager.queryNetworkStatus`（WiFi/BLE Tester 也可手动输入）。

### AudioFiles
1. `onSyncClicked` → 媒体服务器 `/files` → 合并到内部 `RecordingState`。
2. 若需转写，逐条执行：下载文件 → `OssUploadClient.uploadAudio` → `TingwuCoordinator.submit`。
3. 观察 `TingwuJobState` Flow，将状态映射成 Idle/Syncing/Transcribing/Transcribed/Error，并附带 `progress/transcriptSummary`。
4. 播放逻辑调用 `AudioPlaybackController.play` / `pause`，App 层具体实现负责 MediaPlayer 生命周期。

## 当前状态（T0→T1）
- Home 面板仍由 `FakeMediaSyncCoordinator` 驱动，用于展示示例 clip；未连接真实硬件。
- DeviceManager：已联通 MediaServerClient，支持 baseUrl 自动检测、上传、应用、删除，并有 UI + JVM 测试保障。
- AudioFiles：新建 ViewModel + Compose 屏幕，驱动 MediaServer → OSS → Tingwu → 播放全链路；状态/错误均在 UI 显示，并有 `AudioFilesViewModelTest` + Compose 测试。
- 仍缺：自动同步 baseUrl、真实硬件/OSS 的回归验证、媒体面板的图像编辑器。

## 风险与限制
- DeviceManager/AudioFiles baseUrl 仍需手动协作，没有自动和 WiFi/BLE 页面同步，易填错。
- AudioFiles 下载/上传/转写均为串行处理，大文件或网络不稳时可能导致长时间阻塞；尚无重试/限速。
- 播放控制器当前仅处理简单 play/pause，没有 AudioFocus 与生命周期管理。
- 真实设备/OSS/Tingwu 组合尚未纳入 CI，必须人工验证。

## 下一步动作
- 让 WiFi/BLE Tester 或 Connectivity 页面直接推送 baseUrl 给 DeviceManager/AudioFiles，减少手工输入。
- 为 AudioFiles 增加并行/队列与重试策略，避免单条失败阻塞整次同步。
- 把转写结果写回聊天或导出流程，形成统一的语料管线。
- 扩展 `:core:test` Fake（MediaServer、OSS、Tingwu）以便在 JVM 层模拟成功/失败/延迟。

## 调试与验证
- DeviceManager：`./gradlew :feature:media:testDebugUnitTest`（覆盖 ViewModel + Fake Gateway）；UI 测试位于 `app/src/androidTest/.../DeviceManagerScreenTest.kt`。
- AudioFiles：`./gradlew :feature:media:testDebugUnitTest` + `./gradlew :app:connectedDebugAndroidTest`（AudioFiles UI）；运行前需保证 Gradle wrapper 已缓存。
- 真机：在 `local.properties` 配置 `ALIBABA_CLOUD_*`、`TINGWU_*`，并准备可访问的 MediaServer baseUrl；关注 Logcat `SmartSalesMedia`。
