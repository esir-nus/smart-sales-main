<!-- 文件路径: docs/current-state.md -->
<!-- 文件作用: 汇总 Smart Sales App 当前状态 -->
<!-- 文件目的: 统一团队对层级、依赖、风险与下一步的理解 -->
<!-- 相关文件: agent.md, plans/dev_plan.md, plans/tdd-plan.md, docs/progress-log.md -->

# Current State Overview

本文件为唯一可信快照。更新任一模块 README 或重大流程后，必须同步这里并触发 logging workflow。

## 系统快照
- **整体层级**：`:core:util`、`:core:test` 维持 T0；`:feature:connectivity` 与 `:feature:chat` 的核心路径已进入 T1；`:feature:media` 目前介于 T0→T1（DeviceManager、AudioFiles 已能驱动媒体服务器 + Tingwu 管线，但仍依赖 Fake 数据）；`:data:ai-core` 处在 T1-（真实 DashScope/Tingwu/OSS 已连通，尚缺全面联动测试）；`:app` 为 T0+ 的导航壳（主要体验可跑通，但缺乏自动化构建验证）。
- **端到端路径**：
  - **连接链路**：`DefaultDeviceConnectionManager` + `AndroidBleWifiProvisioner` + `GattBleGateway` 支持扫描、配网、网络诊断，WiFi/BLE Tester 页面可触发所有指令。真实 BLE 仍待更多真机验证。
  - **聊天链路**：`HomeScreenRoute`→`HomeScreenViewModel`→`ChatController`。默认以 Fake AI 运行，可通过 `AiCoreConfig` 切换 DashScope/Qwen；Room `AiSessionRepository` 已缓存历史；新 UI 测试覆盖滚动至底部按钮。
  - **媒体链路**：DeviceManager 页面复用 `MediaServerClient` 管理上传/删除/应用；AudioFiles 页面新接入 `AudioFilesViewModel`，可拉取媒体服务器列表、下载到本地、上传至 OSS 并调用 Tingwu，状态分为 Idle/Syncing/Transcribing/Transcribed/Error。
  - **Tingwu/OSS**：`RealTingwuCoordinator` 负责提交与轮询；`RealOssUploadClient` 负责上传并返回 900s 有效的 presigned URL；AudioFiles 流程已将两者串联并展示转写摘要。
  - **App 壳**：`AiFeatureTestActivity` 集成 Home、WiFi/BLE、DeviceManager、DeviceSetup、音频库、用户中心占位等标签；`:app` 仍是主要调试入口。
- **测试覆盖**：Connectivity/DeviceManager 具备 JVM 测试；新建 `AudioFilesViewModelTest` + Compose UI 测试（AudioFiles、DeviceManager、Home scroll-to-bottom）。受环境限制，Gradle wrapper 需提前下载至 `.gradle/wrapper` 才能运行 Instrumentation。

## 依赖与工具
- **JDK/Gradle/AGP**：JDK 17.0.11+9（写死在 `gradle.properties`），Gradle 8.13，AGP 8.13.0，Kotlin 1.9.25，Compose Compiler 1.5.15。
- **Compose/Material**：Compose BOM 2024.09.02，Material3 1.3.1，Compose Icons core/extended。Instrumentation 依赖 `androidx.compose.ui:ui-test-junit4` 与 `ui-test-manifest`（:feature:chat 亦已引入）。
- **DI/DB**：Hilt 2.52（含 `hilt-navigation-compose`），Room 2.6.1（Chat 历史用）。
- **网络/SDK**：DashScope SDK 2.14.0、Retrofit/OkHttp 4.12.0、Aliyun OSS SDK。`settings.gradle.kts` 优先 Aliyun 镜像，缺失时退回 `third_party/maven-repo`。
- **构建约束**：Gradle Wrapper 下载需手动适配内网/代理；`org.gradle.jvmargs=-Xmx4g`、`parallel/caching` 已启用。若必须离线，先同步 `.gradle/wrapper/dists`。

## AI 与云集成
- **DashScope/Qwen**：`DashscopeAiChatService` 通过 `DashscopeClient` 调用 SDK，支持 streaming Flow（`AiChatStreamEvent`），失败统一抛出 `AiCoreException(source=DASH_SCOPE)`。UI 通过 `AiCoreConfig` 控制 Fake/真实、重试次数、超时与是否启用 streaming。
- **Tingwu**：`RealTingwuCoordinator` 使用 Retrofit + ROA 签名拦截器，轮询周期默认 2s，超时 90s；`AiCoreConfig.preferFakeTingwu` 可切 Fake。AudioFiles 集成 Tingwu job 观察，状态映射为 Syncing/Transcribing/Transcribed/Error，并同步 Markdown 摘要。
- **OSS 上传**：`RealOssUploadClient` 校验 `local.properties` 中的 AccessKey/Bucket/Endpoint，上传后返回 presigned URL，AudioFiles 用其提交 Tingwu。若凭据缺失会抛 `AiCoreException(source=OSS, reason=MISSING_CREDENTIALS)`。
- **日志/错误**：所有云端模块统一返回 `AiCoreException`，UI 读取 `userFacingMessage`；Logcat Tag 约定 `SmartSalesAi`、`SmartSalesMedia` 等，方便过滤。

## 模块状态矩阵
| 模块 | 层级 | 亮点 | 限制 |
| --- | --- | --- | --- |
| `:core:util` | T0 | 提供 Result、DispatcherProvider。 | 缺少日志/时间/文件工具。 |
| `:core:test` | T0 | `FakeDispatcherProvider` 供单测使用。 | 仍无其它 Fake，难以覆盖复杂流。 |
| `:feature:connectivity` | T1 | 状态机 + WiFi/BLE 流程带单测，AndroidBleScanner/BleGateway 可配置。 | 真实硬件与权限流未全面验证。 |
| `:feature:chat` | T1- | ChatController + Room 历史 + Home scroll-to-bottom UI 测试；剪贴板/Markdown 逻辑统一。 | 仍主要靠 Fake AI，DashScope streaming 未在 UI 落地。 |
| `:feature:media` | T0→T1 | DeviceManager + AudioFiles 屏幕接入 MediaServer、OSS、Tingwu；AudioFiles 提供播放控制器接口。 | baseUrl 需手动输入，媒体同步仍缺自动化测试与真机验证。 |
| `:data:ai-core` | T1- | DashScope/Tingwu/OSS 真实实现均已封装，可通过 AiCoreConfig 切换 Fake。 | 缺少流量治理与指标；文档尚未涵盖 AudioFiles 场景。 |
| `:app` | T0+ | AiFeatureTestActivity 集成多个页面并新增 AudioFilesRoute。 | Gradle wrapper 下载受限；导航仍缺 Compose 测试。 |

## 已知风险
- Gradle Wrapper 无法在受限网络中自动下载，需要提前缓存或配置代理，否则任何 `./gradlew` 命令都会失败。
- AudioFiles/DeviceManager 的 MediaServer base URL 仍依赖人工输入；未与 Connectivity 的自动探测打通，易出错。
- DashScope/Tingwu/OSS 生产凭据保存在 `local.properties`，若误提交或缺失会阻断真实链路。
- 真实 BLE/Wi-Fi 设备尚未接入常规回归流程，现有实现可能与设备实际协议不一致。
- `:feature:media` 播放控制使用 `MediaPlayer`，暂未处理生命周期/AudioFocus，背景播放会被系统回收。

## 最佳实践
- 修改任一模块时先阅读对应 README 与本文件，确保 Layer 与测试策略一致。
- 需要日志或诊断时使用既有 Tag（`SmartSalesAi` 等），避免随意新增。
- 所有涉及 DashScope/Tingwu/OSS 的 PR 必须说明所需的 `local.properties` 键值并提醒如何切换 Fake。
- 对连接/媒体/AI 关键路径遵循 `plans/tdd-plan.md`：尽可能先写或补单测；若因外部依赖无法测试，需在 `docs/progress-log.md` 记录。
- 构建/测试前确认本地 `.gradle/wrapper` 可用或已通过镜像缓存好 Gradle 8.13。

## 下一步
1. **媒体链路**：让 AudioFiles/DeviceManager 自动消费 `DeviceConnectionManager` 的网络查询结果，移除手动 baseUrl；为 OSS/Tingwu 流程补 UI 提示与自动化测试。
2. **真实 AI 整合**：在 Home 页启用 DashScope streaming，确保剪贴板/导出逻辑在真实响应下仍成立，并补充对应单测。
3. **BLE 实机验证**：与硬件团队对接 BT311 或商定设备，完善扫描权限、错误提示与日志；在 `docs/progress-log.md` 记录每次协议调试。
4. **构建/测试稳定性**：缓存 Gradle Wrapper，恢复 `./gradlew :app:assembleDebug`、`:feature:media:testDebugUnitTest`、`:app:connectedDebugAndroidTest` 等命令，以便 CI 运行。
5. **文档同步**：各模块 README 必须与本快照保持同步，并在 logging workflow 中记录本次文档更新。
