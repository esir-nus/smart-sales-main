<!-- 文件路径: docs/current-state.md -->
<!-- 文件作用: 汇总Smart Sales App当前状态 -->
<!-- 文件目的: 让任何人快速了解系统进展及风险 -->
<!-- 相关文件: agent.md, plans/dev_plan.md, docs/progress-log.md, relevant_map.json -->

# Current State Overview

此文件为 Smart Sales App 的权威快照，所有模块 README 需与本状态保持一致。

## 系统快照（System Snapshot）
- **整体层级**：`:core:util`、`:core:test` 维持 T0；`:feature:connectivity` 处于 T1（状态机 + 单测稳定）；聊天/AI/媒体仍是 T0 行走骨架，但部分行为（Markdown/剪贴板）已按 T1 质量收敛。
- **端到端路径**：
  - BLE → Wi-Fi → Sync：`DefaultDeviceConnectionManager` 现可接入真实 `AndroidBleWifiProvisioner + GattBleGateway`（release flavor），debug 仍默认 `SimulatedWifiProvisioner`；`GattBleGateway` 已改为在连接后自动发现并缓存设备实际暴露的 Service/Characteristic，状态机支持权限/超时/传输错误映射与两次自动重试，但真实硬件仍待验证。
  - Chat → AI → Markdown/Export：`ChatController` 以 Fake 服务贯通整条链路，Room `AiSessionDatabase` 已持久化历史；`FakeAiChatService` 现将助手 display 文本一并写入 Markdown，使剪贴板复制与导出提示保持一致，可在 `:app` 与 `:aiFeatureTestApp` 体验。
  - Tingwu：`RealTingwuCoordinator` 通过 Retrofit + OkHttp 访问 Tingwu HTTP API，默认 2s 轮询 + 90s 超时，可在 Hilt 中切换 Fake 以便离线调试。
  - 媒体：`FakeMediaSyncCoordinator` 已消费 `DeviceConnectionManager.state` 并生成可禁用同步的真实面板状态，但仍未连接真实 OSS/硬件。
- **APK 形态**：`:app` 通过 `AppShellRoute` 展示导航抽屉、连接横幅、媒体面板与图像提示，并嵌入聊天主视图；`:aiFeatureTestApp` 现除了聊天/媒体演示，还挂载一个“本地音频转写”卡片（调用 `TingwuTestViewModel`）以及新的 “WiFi/BLE 调试” 分区，直接复用 `:wifiBleTestApp` 的 ViewModel 管理扫描、WiFi 下发、网络诊断与 Web 控制台跳转；`:tingwuTestApp` 已转成纯功能库，不再单独打包。以上入口均可通过 `assembleDebug` 运行。
- **计划对齐**：整体仍位于阶段 2～3，刚完成 App shell 基础和连接状态对接，接下来进入媒体/AI 真实现与 T1 测试。

## 依赖与工具（Dependencies & Tooling）
- **JDK**：17.0.11+9（`gradle.properties` 中固定 `org.gradle.java.home=/home/cslh-frank/jdks/jdk-17.0.11+9`）。
- **Gradle/AGP/Kotlin**：Gradle 8.13、AGP 8.13.0、Kotlin 1.9.25、Compose Compiler 1.5.15。
- **UI 栈**：Compose BOM 2024.09.02、Material3 1.3.1、`com.google.android.material:material:1.12.0`、Compose material icons core/extended，用于 AppShell 图标与主题。
- **DI/DB**：Hilt 2.52、`androidx.hilt:hilt-navigation-compose:1.2.0`、Room 2.6.1。
- **仓库策略**：`settings.gradle.kts` 先指向 Aliyun Google/Public，再落到 `third_party/maven-repo` 兜底；缺失依赖时按 `reference-source/legacy/DEPENDENCY_VERSIONING.md` 镜像。
- **Gradle 属性**：`org.gradle.jvmargs=-Xmx4g`、`org.gradle.parallel=true`、`org.gradle.caching=true`，禁用自动下载 JDK；需手动维护 `.gradle/wrapper` 缓存以便离线。

## AI 与云集成（AI & Cloud Integration）
- **DashScope/Qwen**：`DashscopeAiChatService`（`:data:ai-core`）使用 `com.alibaba.dashscope:dashscope-sdk-java:2.14.0` 调用 Qwen，支援重试/超时与 `AiChatStreamEvent` Flow；真实/假实现切换、最大重试次数、`dashscopeEnableStreaming` 等通过 `AiCoreConfig`（由 Hilt Module 提供）控制，仍依赖 `local.properties` 的 `DASHSCOPE_API_KEY`、`DASHSCOPE_MODEL`；`FakeAiChatService` 供 JVM 单测与离线模式复用同一接口。
- **错误结构**：DashScope/Tingwu/Export 统一抛出 `AiCoreException(source, reason, suggestion)`；`ChatController` 调用 `userFacingMessage` 直接展示给用户，方便区分“缺少密钥”“网络失败”等。
- **Markdown 裁剪策略**：真实与 Fake AI 均将 `displayText` 前置拼接到 `structuredMarkdown`，保证剪贴板/导出副本包含助手原话；真实 SDK 必须持续遵守该约束。
  - **Tingwu**：`RealTingwuCoordinator` 使用 `TingwuApi` 轮询真实任务，`AiCoreConfig` 控制轮询间隔/超时/是否回退 Fake；若未在 `local.properties` 配置 Tingwu Key，将自动退回 Fake。
- **日志 Tag**：新增 `SmartSalesAi`（AI Core）、`SmartSalesChat`、`SmartSalesMedia`、`SmartSalesConn`、`SmartSalesTest` 等 Tag，可在 Logcat 通过 `tag:SmartSalesAi`、`tag:SmartSalesTest` 快速过滤 AI/Tingwu 与测试 App 日志。
- **Tingwu 测试入口**：`tingwuTestApp` 作为库输出 `TingwuTestViewModel` / `TingwuTestUiState` / `cacheAudioFromUri` / `uploadLocalAudioAndSubmit` 等工具，宿主可在自己的 Hilt Module 中引用；`aiFeatureTestApp` 现已接入该库，通过单个“选择音频文件”按钮完成缓存→OSS 上传→真实 Tingwu 转写，并在卡片展示任务 ID 与 Markdown 结果，同时通过 `ChatController.importTranscript` 将最新转写同步到聊天消息列表。
- **AiCoreConfig 提供方式**：默认不再由 `:tingwuTestApp` 自动安装 Hilt Module，任何 App 若需启用真实 DashScope/Tingwu/Export，需在本地 Hilt Module 中调用 `TingwuTestAiCoreOverrides.defaultConfig()` 或自定义配置，避免与现有 `AiFeatureTestAiCoreOverrides` 产生重复绑定。
- **OSS**：`FakeMediaSyncCoordinator` 依据连接状态模拟音频同步并生成 clip 列表；真实 `OssRepository`/`OssManager` 依赖阿里 SDK，当前未启用，上传/删除/推送指令仍需与硬件协议对齐。
  - **Export**：`RealExportManager` 通过 `MarkdownPdfEncoder` / `MarkdownCsvEncoder` 生成真实字节并写入缓存，`ExportResult.localPath` 可被分享/上传；`AiCoreConfig` 允许回退 Fake 以便 JVM 测试。
- **设备媒体 HTTP API**：`reference-source/wifi.py` 现提供纯后端服务（默认 `http://<device-ip>:8000`），主要端点包括：
  - `GET /files`：返回 `[{name,sizeBytes,mimeType,modifiedAtMillis,mediaUrl,downloadUrl}]` JSON 列表；
  - `POST /upload`：`multipart/form-data` 字段 `file`，仅接受图片/视频且单文件 ≤5MB；
  - `DELETE /delete/<name>`：删除服务器上的媒体文件；
  - `POST /apply/<name>`：记录选中的媒体并通过 UDP `wifi-gif:RELOAD` 通知硬件；
  - `GET /download/<name>` / `GET /<name>`：分别用于原始下载与预览流。
  `:aiFeatureTestApp` 的“硬件媒体库”面板已对接上述 API，可实时列出 GIF/MP4，直接预览/播放并触发上传、删除、应用与下载。

## 模块状态矩阵（Module Status Matrix）

| 模块 | 层级 (T0–T3) | 真实依赖 | Fake / Real 状态 | 主要风险 / 限制 | 下一步建议 |
| --- | --- | --- | --- | --- | --- |
| `:core:util` | T0 | Kotlin stdlib, coroutines | 纯工具，无外部依赖 | 仅含 Result/Dispatchers，尚缺日志/文件等实用工具 | 扩展统一日志、文件校验，保持纯函数 + 单测覆盖 |
| `:core:test` | T0 | coroutines-test | 仅提供测试工具 | Fake 资产较少，尚不覆盖 BLE/OSS 复杂场景 | 补充 BLE/OSS/AI fakes + builder，配合 TDD |
| `:data:ai-core` | T1 | Retrofit/OkHttp/Gson, DashScope SDK | DashScope 通过 `DashscopeAiChatService` + `DashscopeClient` 直连 SDK，支持重试/超时与 `AiChatStreamEvent` Flow；`RealTingwuCoordinator` + `TingwuApi` 已可调用真实 HTTP，`RealExportManager` 写入缓存并返回本地路径 | 需在 `local.properties` 配置 Tingwu/DashScope Key 且保证网络畅通；仍缺少指标与失败上报；OSS 实现未接通 | 在 `app`/`aiFeatureTestApp` 中提供 `AiCoreConfig` override 控制 Fake/Real；补齐 Tingwu 断线恢复、OSS 仓库以及监控 |
| `:feature:connectivity` | T1 | `:core:util`, `:core:test` | debug 使用 `SimulatedWifiProvisioner`，release 使用 `AndroidBleWifiProvisioner + GattBleGateway`，内建权限/超时/传输错误映射与双次自动重连 | 真机 GATT 仍是占位 UUID，尚未完成供应商 SDK 联调 | 用正式 UUID/SDK 替换占位值，并补充真机回归测试 |
| `:feature:chat` | T0→T1 | `:data:ai-core`, Room, `:core:util` | ChatController 默认注入真实 DashScope/Tingwu/Export 服务（可通过 `AiCoreConfig` 切换 Fake），Room 持久化会话；剪贴板复制与分享由 `AndroidChatShareHandler` 承担 | DashScope 异常尚未细分，Tingwu 结果缺少 UI 呈现，导出依旧停留在缓存文件分享 | 补充错误处理与历史列表 UI，暴露 Tingwu 进度提示，扩充导出失败用例测试 |
| `:feature:media` | T0 | `:core:util`, `:data:ai-core`, `:feature:connectivity` | `FakeMediaSyncCoordinator` 消费连接状态并提供 clip 列表 + 单测 | 仍为 Fake，缺少真实 OSS/硬件 API，UI 仅展示音频列表 | 接入 OSS 上传/删除/推送，补充图像编辑器与 Compose 测试 |
| `:app` | T0 | Compose, Hilt, feature modules | AppShell 订阅连接状态 + Fake 媒体/图像面板 | 壳层具备导航、横幅、面板，但媒体/图像仍是假数据，剪贴板/分享未接入 | 接通真实媒体/图像协调器，增加分享/剪贴板 Hook 与 Compose UI 测试 |
| `:aiFeatureTestApp` | T0 | Compose, Hilt, feature modules, `:tingwuTestApp`, `:wifiBleTestApp` | Playground 现集成 `TingwuTestViewModel`（本地音频转写卡片）与 `WifiBleTestViewModel`（BT311 配网+网络诊断面板），并通过 `ChatController.importTranscript` 将 Markdown 注入聊天；聊天/媒体仍默认 Fake | 缺少 Fake/Real 切换入口、诊断导出以及 UI/instrumentation 测试；媒体/聊天在无网络时体验不一致 | 增加模式切换、录音入口与基础 UI/UX 校验；持续扩展日志导出与自动化测试 |

## 已知风险与约束（Known Risks & Constraints）
- **镜像依赖**：当 Aliyun 镜像缺失（例：`hilt-android-compiler:2.52`）时，`./gradlew :feature:chat:test` 会失败；需及时将缺失构件镜像到 `third_party/maven-repo` 或临时启用外网源。
- **DashScope API Key / Config**：`DashscopeAiChatService` 需读取 `local.properties` 中的密钥且要求设备可访问 DashScope 域名；缺失或网络受限将抛出 `AiCoreException(source=DASH_SCOPE)` 并提示“配置 DASHSCOPE_API_KEY / 检查网络”。若未在 Hilt Module 提供 `AiCoreConfig`（如 `dashscopeEnableStreaming=true`、`preferFakeAiChat=false`），则默认停留在 Fake，实现 streaming 前需显式 override。
- **Markdown 一致性**：真实 DashScope 接入后若返回结构化 Markdown 缺少 display 文本，将破坏剪贴板提示与测试；需在 SDK 层维持当前拼接策略。
- **真实硬件缺席**：BLE/Wi-Fi、OSS 仍为模拟实现，任何端到端实测前都必须替换为真实 SDK 并补充 T1 级别测试；Tingwu 已可连接 HTTP，但尚未经过大量真机验证。
- **剪贴板/分享**：Android 权限/UX 规则尚未落实；当前仅在状态中记录“已复制”提示。
- **多模块同步**：没有自动化流程同步 snapshot 与 README，需依赖人工；已在本次任务中规划 `workflows/update-workflow.md` 解决。

## 最佳实践与硬约束（Best Practices & Conventions）
- 遵守 `agent.md`：所有注释/文档使用简体中文、句子短；Kotlin/Java 文件必须包含统一文件头。
- 计划顺序：严格按照 `plans/dev_plan.md` 执行（`core` → `feature:connectivity` → `data:ai-core` → `feature:chat` → `feature:media` → `aiFeatureTestApp` → `app`）。
- TDD 规则：T0 先打通骨架，T1 开始补齐关键路径单测（BLE 状态机、Tingwu 解析、导出流程），参考 `plans/tdd-plan.md`。
- 依赖新增：先检查 Aliyun 镜像；仅在缺失时复制到 `third_party/maven-repo`；更新 `reference-source/legacy/DEPENDENCY_VERSIONING.md` 记录偏差。
- 变更流程：任何实质工作需 `invoke logging workflow`，并在 `docs/progress-log.md` 追加日志；重大阶段性进展同步更新本文件。

## 下一批关键任务（Next Critical Steps）
- **Connectivity**：替换 `SimulatedWifiProvisioner`，串联真实 BLE/Wi-Fi SDK，扩展错误/权限分支并同步到 `reference-source`。
- **AI Core + Chat**：对 DashScope/Tingwu 真实接入补齐指标、容错与 UI 显示（例如 Tingwu 轮询提示、导出错误），并继续串联 OSS；基于 Room 历史实现抽屉列表 UI，并确保 Hilt/Room KAPT 依赖已镜像；真实响应需继续保留“display + Markdown”合并逻辑。
- **Media**：在现有连接感知的 `MediaSyncCoordinator` 基础上接入真实 OSS API，补齐上传/删除/推送命令与图像编辑器 UI。
- **App Shell**：让媒体面板消费真实数据、添加分享/复制入口，并补充 Compose UI/semantics 测试。
- **Process**：保持 `docs/current-state.md` 与各 README 对齐，每次文档更新后执行 logging workflow 并记录计划变更。
