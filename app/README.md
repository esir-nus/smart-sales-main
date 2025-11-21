<!-- 文件路径: app/README.md -->
<!-- 文件作用: 说明 App 壳 / AiFeatureTestActivity 的职责 -->
<!-- 文件目的: 指导如何在单个 APK 中验证聊天、连接、媒体、Tingwu 等流程 -->
<!-- 相关文件: docs/current-state.md, data/ai-core/README.md, feature/chat/README.md, feature/media/README.md, feature/connectivity/README.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# App 壳（AiFeatureTestActivity）

## 模块职责
- 提供一个统一的 Compose 调试壳，集成 Home、WiFi & BLE Tester、DeviceManager、AudioFiles、DeviceSetup、用户中心占位等 Route。
- 用于在单一 APK 内验证聊天（DashScope/Fake）、媒体服务器（上传/删除/应用）、OSS/Tingwu 转写、本地音频播放以及连接状态机。
- 不承载生产 UI，后续正式 App 可根据需要复用这些 Route 或做重新包装。

## 关键接口与依赖
- `AiFeatureTestApplication` / `AiFeatureTestActivity`：应用入口 + 多 Tab 壳（FilterChip + Box 切换），通过 Hilt 注入所需 ViewModel/Controller。
- Route 列表：
  - `HomeScreenRoute`：聊天 + 设备/音频横幅、滚动到底部按钮。
  - `WifiBleTesterRoute`：BLE 扫描、配网、热点、媒体服务器诊断。
  - `DeviceManagerRoute`：列出/上传/应用/删除媒体服务器文件。
  - `AudioFilesRoute`：新接入的音频库（下载 → OSS 上传 → Tingwu 转写 → 播放），复用 `AudioFilesViewModel` 与 `AudioPlaybackController`。
  - `DeviceSetupRoute`：配网流程。
  - `UserCenterPlaceholder`：待后续填充。
- 依赖：`projects.feature.chat`, `projects.feature.media`, `projects.feature.connectivity`, `projects.data.aiCore`, `projects.tingwuTestApp`, `projects.core.util`。

## 数据与控制流
1. Activity 读取当前选中的 Chip → 渲染对应 Route。
2. 各 Route 独立注入 ViewModel，但共享 MediaServerClient baseUrl：WiFi/BLE 页面写入、DeviceManager/AudioFiles 读取。
3. AudioFiles 同步流程：媒体服务器 `/files` → 下载到 `device-media` 目录 → `OssUploadClient.uploadAudio` → `TingwuCoordinator.submit` → `AudioFilesViewModel` 监听 job 状态并更新 UI。
4. Tingwu 调试卡片仍位于 WiFi/BLE 页面，供单独提交本地音频文件。

## 当前状态
- **Layer**：整体属于 T0+（骨架贯通 + 局部 T1 行为）。聊天仍默认 Fake AI，支持在 Activity 级 `AiFeatureTestAiCoreOverrides` 切换 DashScope/Tingwu/Export。
- **AudioFiles**：已实装列表/空态/错误/SYNC 按钮与播放控制，状态分 Idle/Syncing/Transcribing/Transcribed/Error。
- **DeviceManager**：具备上传/删除/应用逻辑与 Compose UI 测试。
- **WiFi/BLE Tester**：包含扫描/热点/媒体服务器地址栏/诊断日志与 MediaServerPanel。
- **测试**：新增 Home scroll-to-bottom、DeviceManager、AudioFiles 的 Compose UI 测试；Shell 本身尚未做端到端 Instrumentation。

## 风险与限制
- Gradle Wrapper 不能在受限网络自动下载，运行 `./gradlew` 前需预先将 `gradle-8.13-bin.zip` 放入 `.gradle/wrapper`。
- AudioFiles/DeviceManager baseUrl 仍依赖人工输入或 WiFi/BLE 页面复制，尚未自动同步。
- 真实 DashScope/Tingwu/OSS 调试需要在 `local.properties` 提供 `DASHSCOPE_*`、`TINGWU_*`、`ALIBABA_CLOUD_*`，缺失时会回退 Fake。
- Playback 通过 `MediaPlayerAudioPlaybackController` 实现，暂未处理 AudioFocus/生命周期，背景切换可能中断。

## 下一步动作
- 让 WiFi/BLE Tester 自动推送 `queryNetworkStatus` 的 baseUrl 给 DeviceManager/AudioFiles，减少手动步骤。
- 将 Tingwu/AudioFiles 生成的 Markdown 回写 `ChatController`，保持单一历史视图。
- 为壳层添加导航相关 Compose 测试；在 CI 中缓存 Gradle Wrapper 以恢复 `./gradlew :app:assembleDebug`。
- 梳理 Fake/Real 切换 UI（例如设置页或调试按钮），便于 QA 不改代码即可切换。

## 调试与验证
- 构建：`./gradlew :app:assembleDebug`。（若 wrapper 下载受限，可设置 `GRADLE_USER_HOME` 指向已有缓存）
- 启动：`adb shell am start com.smartsales.aitest/.AiFeatureTestActivity`，通过滤芯按钮切换页面。
- Tingwu/OSS：确保 `local.properties` 配齐密钥；真机需可访问 Aliyun；关注 Logcat Tag `SmartSalesAi`、`SmartSalesMedia`、`SmartSalesTest`。
- 建议测试命令：`./gradlew :feature:media:testDebugUnitTest`、`:app:connectedDebugAndroidTest`（Gradle wrapper 就绪后执行）。
