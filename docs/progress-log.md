<!-- 文件路径: docs/progress-log.md -->
<!-- 文件作用: 记录每次提交或变更的进度日志 -->
<!-- 文件目的: 强制团队记录TDD状态与风险 -->
<!-- 相关文件: docs/current-state.md, plans/dev_plan.md, agent.md, plans/tdd-plan.md -->

# Progress Log (Mandatory After Each Step)

Use the template below every time any module or plan changes.

---

## <YYYY-MM-DD> – <short title>

Layer: T0 | T1 | T2 | T3  
Modules: <e.g. :feature:connectivity, :data:ai-core>

Summary:
- Bullet list of work completed.

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- Known gaps, tech debt, missing tests.
- Ticket IDs if created.

---

## 2025-11-14 – Imported legacy dependencies

Layer: T0  
Modules: third_party, reference-source

Summary:
- Copied Alibaba/OSS SDK Maven repo from legacy delivery into `third_party/maven-repo` for offline builds.
- Imported dependency guidance docs (`GRADLE_DEPENDENCIES.md`, `DEPENDENCY_VERSIONING.md`, `README_DELIVERY.md`) into `reference-source/legacy` for quick lookup.

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- Need to wire Gradle settings to point at the local Maven repo and mirror doc guidance.
- Ensure copied docs stay read-only references.

---

## 2025-11-14 – Bootstrapped Gradle build + module skeletons

Layer: T0  
Modules: :app, :aiFeatureTestApp, :data:ai-core, :core:util, :core:test, :feature:chat, :feature:media, :feature:connectivity, third_party

Summary:
- Added Gradle wrapper, version catalog, settings, and module build files referencing the local Maven mirror.
- Seeded minimal Hilt/Compose code for the app shell and feature/test harness modules plus shared utility primitives.
- Mirrored Android Gradle Plugin 8.6.1 artifacts and configured resolution + JDK 17 toolchain to enable offline builds.

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- Every new dependency must be mirrored into `third_party/maven-repo` or builds will fail offline.
- Feature modules still contain placeholder logic; need real implementations + tests to progress beyond T0.

---

## 2025-11-14 – Connectivity module scaffolding + tests

Layer: T0  
Modules: :feature:connectivity, :core:util, :core:test

Summary:
- Implemented `ConnectionState` contracts, BLE session + Wi-Fi credential models, and a DI-bound `DefaultDeviceConnectionManager` with simulated provisioning/heartbeat flows.
- Added a fake `WifiProvisioner` and Hilt bindings so the module runs offline while mirroring the real architecture.
- Introduced coroutine-based unit tests covering pairing success, retry behavior, and hotspot credential requests via `kotlinx-coroutines-test`.

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- Replace simulated provisioning with real BLE/Wi-Fi plumbing; expand tests for timeout and permission failures.
- Ensure any new dependencies (BLE SDKs, storage) are mirrored into `third_party/maven-repo`.

---

## 2025-11-14 – Chat module T0 skeleton + controller tests

Layer: T0  
Modules: :feature:chat, :data:ai-core, :aiFeatureTestApp

Summary:
- Expanded `:data:ai-core` with richer `AiChatService` request/response models plus fake `TingwuCoordinator` and `ExportManager` bindings so chat logic can orchestrate AI, transcripts, and exports offline.
- Rebuilt `ChatController` with new domain models, session repository, transcript flow, and markdown/export actions; added Compose `ChatPanel` UI for the chat experience and upgraded the feature test app to host it.
- Added coroutine-driven unit tests verifying chat state transitions, export status, and transcript injection.

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- `./gradlew :feature:chat:test` currently fails to resolve `hilt-android-compiler:2.52` because the offline Maven mirror lacks this artifact; mirror the dependency or adjust repositories to unblock CI.
- Need persistent storage (Room) and real clipboard/export plumbing before moving toward T1.

---

## 2025-11-14 – Connectivity unit tests stabilized

Layer: T1  
Modules: :feature:connectivity, :core:test

Summary:
- Refactored `DefaultDeviceConnectionManager` to accept an external scope and guard its heartbeat loop with `isActive`, preventing runaway coroutines in JVM tests.
- Rewrote `DefaultDeviceConnectionManagerTest` to use `runTest`, scheduler-controlled dispatchers, and deterministic time advances instead of ad-hoc blocking logic.
- Verified the flow via `./gradlew :feature:connectivity:testDebugUnitTest`, which now completes without hanging.

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- Additional negative-path coverage (timeouts, permission failures) still pending for T1 graduation.
- Keep mirroring any new dependencies into `third_party/maven-repo` so test tasks stay offline-capable.

---

## <YYYY-MM-DD> – Updated dependency strategy to Aliyun mirrors

Layer: T0  
Modules: build-config, docs

Summary:
- Revised `plans/dev_plan.md` stage 1 to prioritize Aliyun Google/Public mirrors and only mirror artifacts into `third_party/maven-repo` when a dependency is missing or private.
- Updated `docs/current-state.md` to reflect the new strategy and removed the offline-only blocker description.
- Confirmed the recently added Compose icon + Hilt compiler dependencies are accessible via Aliyun, so no immediate offline mirroring work remains.

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- If Aliyun mirrors become unavailable, be ready to copy critical artifacts into `third_party/maven-repo` or fall back to `--offline` builds.

---

## 2025-11-15 – App shell foundation + connectivity wiring

Layer: T0  
Modules: :app, :feature:connectivity, build-config

Summary:
- Added `Theme.SmartSales`, Material Components, and Compose icon dependencies so manifests and UI references resolve against Material3 resources.
- Introduced `AppShellState`+`AppShellScreen` wiring with navigation drawer, connectivity banner, media panel, and image editor hint; hooked `AppShellRepository` up to `DeviceConnectionManager` to show live pairing/sync status and expose fake media/image data via flows.
- Updated `AppShellViewModel` and manifest to consume the new repository and theme, keeping `ChatHomeRoute` embedded as the main content slot.

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- Media/image panels still rely on stub data; need real coordinators plus Compose tests when modules mature.
- Time strings use simple relative formatting and may require localization/polish later.

---

## 2025-11-15 – Media coordinator wired into App shell

Layer: T0  
Modules: :feature:media, :app, :aiFeatureTestApp

Summary:
- Rebuilt `MediaSyncCoordinator` with connection-aware state, realistic clip models, and gating logic tied to `DeviceConnectionManager`.
- Connected `AppShellRepository`/UI and the feature test app to the new coordinator output so media panels show live status, disable sync when offline, and render detailed clip rows.
- Added coroutine-based unit tests for `FakeMediaSyncCoordinator`, updated Gradle dependencies, and verified via `./gradlew :feature:media:test`.

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- 媒体模块仍为 Fake，实现真实 OSS 上传/删除和与硬件协议对接仍待完成。
- App Shell 仅展示示例媒体内容，尚未落地图像编辑器与分享/复制流程。

---

## <YYYY-MM-DD> – Added relevant_map index and legacy references

Layer: T0  
Modules: docs, reference-source

Summary:
- Added a JSON pointer index to `reference-source/relevant_map.json` so future readers can jump straight to features, dependencies, and best-practice sections per governance rules.
- Curated new candidate good_examples/testing_guidelines/pitfalls entries that cite validated Tingwu/DashScope/BLE files from the legacy delivery for reuse during migrations.

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- Ensure teammates follow the “read index first” rule and keep those referenced legacy files synchronized with modern implementations.

---

## 2025-11-16 – Synced AI clipboard docs

Layer: T1  
Modules: docs, :data:ai-core, :feature:chat

Summary:
- 更新 `docs/current-state.md`，记录 FakeAiChatService 现已将助手 display 文本写入 Markdown，并在风险/下一步提醒真实 SDK 也需保持该格式。
- 调整 `data/ai-core` 与 `feature/chat` README，说明剪贴板/Markdown 合并策略与既有单测约束，防止未来接入 DashScope/Tingwu 时回归。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 接入真实 SDK 时必须验证 Markdown 仍包含助手 display 文本，否则剪贴板与导出提示会回归旧 Bug。

---

## 2025-11-16 – Wired DashScope SDK for chat

Layer: T1  
Modules: :data:ai-core, :feature:chat, docs

Summary:
- 引入 `com.alibaba.dashscope:dashscope-sdk-java:2.14.0` 并在 `DashscopeAiChatService` 中构造真实 Qwen 请求（读取 `local.properties` API Key、组装 system/user message、沿用 Markdown 合并策略）。
- 更新 Hilt 绑定与 `BuildConfig` 字段，允许通过真机 app 调用真实 DashScope，同时保留 `FakeAiChatService` 供 JVM 测试。
- 同步 `docs/current-state.md`、`data/ai-core` 与 `feature/chat` README，说明密钥要求、测试方式与后续需要的错误分级。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 真机调试需在 `local.properties` 配置有效 `DASHSCOPE_API_KEY`，且网络可访问 DashScope；后续需要补充错误重试、限流与 instrumentation 测试。

---

## 2025-11-16 – 媒体服务器 API 接入测试壳

Layer: T1  
Modules: reference-source, :aiFeatureTestApp

Summary:
- 将 `reference-source/wifi.py` 调整为纯后端服务：新增 `GET /files` JSON 列表，保留 `/upload`、`/download/<name>`、`/delete/<name>`、`/apply/<name>`，去除内置 HTML 前端以便由 App 承接 UI。
- 新增 `MediaServerClient`（HttpURLConnection 实现）和 Compose 控件，在 `AiFeatureTestActivity` 中实时列出媒体文件，支持 GIF 预览、VideoView 播放、上传（SAF）、删除、应用与下载，完全映射服务器端行为。
- 更新 `docs/current-state.md` 记录设备媒体 HTTP API，`aiFeatureTestApp` 引入 Coil 依赖以展示图片/GIF。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 尚未覆盖文件系统权限/异常场景，下载写入外部目录的行为需要在真机验证。
- `MediaServerClient` 使用手写 HttpURLConnection，后续接入正式模块时可抽象为 Retrofit + 更完善的错误映射。

---

## <YYYY-MM-DD> – DashScope 流式管线与配置开关

Layer: T1  
Modules: :data:ai-core, docs

Summary:
- 重构 `DashscopeAiChatService`，抽象 `DashscopeClient` / `DashscopeCredentialsProvider`，加入可配置的重试、超时与日志封装，并通过 `AiCoreConfig` 管理 `preferFakeAiChat`、`dashscopeMaxRetries`、`dashscopeEnableStreaming` 等开关。
- 新增 `AiChatStreamEvent` Flow，Fake/真实实现均支持 chunk→Completed 事件；`DefaultDashscopeClient` 基于 `Generation.streamCall` 将 SDK 回调转换为 Flow，方便 UI 逐步渲染。
- 更新 `data/ai-core/README.md` 与 Hilt override 说明，记录如何在 `AiFeatureTestAiCoreOverrides` 等模块中提供真实配置以启用 DashScope streaming。
- 编写 `DashscopeAiChatServiceTest`、`FakeAiChatServiceTest` 流式单测，验证重试与 chunk 聚合行为。

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- 需要在 `:app` / `:aiFeatureTestApp` UI 层消费流式事件并记录真机 chunk 行为；真机调试必须通过 Hilt module 提供 `AiCoreConfig`，否则仍会回退到 Fake。
- DashScope streaming 依赖稳定网络，尚未记录指标/限流策略；后续需在 `docs/current-state.md` 与 README 中持续更新调试指南。

---

## 2025-11-16 – Tingwu HTTP 协调器与真实导出管线

Layer: T1  
Modules: :data:ai-core, docs, reference-source

Summary:
- 在 `gradle/libs.versions.toml` 与 `data/ai-core/build.gradle.kts` 中引入 Retrofit 2.9.1、OkHttp 4.12.0、Gson 2.11.0，并通过 BuildConfig 注入 `TINGWU_*` 密钥以及 `AiCoreConfig` 新增的 Tingwu/Export 开关。
- 实现 `TingwuApi`、`TingwuAuthInterceptor`、`RealTingwuCoordinator`（包含轮询/超时/Markdown 构造）以及 `RealExportManager` + `MarkdownCsvEncoder`，并通过 Hilt 选择 Fake/Real；同步扩充 `ExportResult.localPath` 与分享逻辑。
- 新增 `MarkdownCsvEncoderTest`、`RealExportManagerTest`、`RealTingwuCoordinatorTest` 验证编码与轮询行为；更新 `data/ai-core/README.md`、`docs/current-state.md`、`reference-source/legacy/DEPENDENCY_VERSIONING.md` 记录依赖与现状。

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- 实际 Tingwu HTTP 网关尚未在真机验证，`TingwuAuthInterceptor` 当前仅注入 Header，若后端要求签名或自定义参数需进一步补齐。
- Export 仅写入 App 缓存目录，后续仍需整合 ShareSheet/OSS 上传以及失败指标上报；`AiCoreConfig` 默认走真实实现，调试环境需显式传入 Fake 选项。

---

## 2025-11-17 – AiCore 配置与错误结构统一

Layer: T1  
Modules: :data:ai-core, :feature:chat, docs, reference-source

Summary:
- 新增 `AiCoreException`（含 source/reason/suggestion），DashScope/Tingwu/Export 统一包装缺少密钥、网络、超时等错误，并通过 `AiCoreLogger` 记录；`TingwuJobState.Failed`、`ChatController` 读取 `userFacingMessage` 直接提示用户。
- `DashscopeAiChatService`、`RealTingwuCoordinator`、`RealExportManager` 根据 `local.properties` 校验密钥并输出结构化提示，同时为 Tingwu 轮询超时/状态异常补充日志。
- `data/ai-core/README.md` 增加 local.properties → BuildConfig 映射表与 `AiCoreConfig` 示例；`docs/current-state.md`、`reference-source/legacy/DEPENDENCY_VERSIONING.md` 记录错误结构和 Retrofit/OkHttp/Gson 镜像要求。
- 新增/更新单元测试覆盖 DashScope 错误原因、Tingwu 缺失密钥分支与 ChatController 异常提示路径。

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- `./gradlew :data:ai-core:test :feature:chat:testDebugUnitTest` 因 `third_party/maven-repo` 中缺少 `com.squareup.retrofit2:retrofit:2.9.1` 与 `converter-gson:2.9.1` 无法完成，需要先镜像依赖。
- Tingwu/Export 真实实现仍缺少指标与断线恢复，需在后续迭代补充。

---

## 2025-11-17 – Mirrored Retrofit stack for Tingwu HTTP

Layer: T0  
Modules: third_party, docs

Summary:
- 复制 `com.squareup.retrofit2:retrofit:2.9.0`、`converter-gson:2.9.0`、`com.squareup.okhttp3:okhttp:4.12.0`、`logging-interceptor:4.12.0` 与 `com.google.code.gson:gson:2.11.0` 的 JAR/POM，从 Gradle 本地缓存同步到 `third_party/maven-repo`，保证离线构建可解析 Tingwu HTTP 依赖。
- 更新 `data/ai-core/README.md` 与 `reference-source/legacy/DEPENDENCY_VERSIONING.md`，将 Retrofit 版本说明纠正为当前使用的 2.9.0，并记录新镜像路径。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 若后续升级到 Retrofit 2.9.1/或其它版本，需要重新镜像对应构件并同步文档，避免再次出现版本漂移。

---

## 2025-11-17 – Tingwu OSS 测试入口 + 模块化日志 Tag

Layer: T1  
Modules: :core:util, :data:ai-core, :aiFeatureTestApp, docs

Summary:
- 新增 `LogTags` 常量集中管理 `SmartSalesAi/Chat/Media/Conn/Test` 等 Tag，并让 DashScope/Tingwu/Export 统一以 `SmartSalesAi/...` 输出日志，方便 Logcat 过滤 AI 事件。
- `aiFeatureTestApp` 增加“Tingwu 测试（OSS）”面板，可粘贴 OSS 音频 URL（预置官方测试链接）直接触发 `ChatController.startTranscriptJob`，按钮会以 `SmartSalesTest` Tag 记录操作。
- 更新 `docs/current-state.md` 记录新的日志过滤方式与 Tingwu 测试入口，方便现场调试。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 预置的 OSS 测试链接会过期，如需长期测试需运营侧维护可复用的公共音频 URL。
- Tingwu 面板仍依赖外网 OSS URL，真机需确保可访问对应 bucket。

---


## 2025-11-17 – 新增 Tingwu 专用测试 App

Layer: T1  
Modules: :tingwuTestApp, :data:ai-core, docs

Summary:
- 新增 `:tingwuTestApp` 模块（含 Hilt Application、网络安全配置、Compose 壳），严格沿用官方 SDK (`V2.7.0-039-20251010_Android_OpenSSL/example/.../token/HttpRequest.java`) 的 ROA 签名实践，便于复现 SignatureDoesNotMatch。
- 实现 `TingwuTestViewModel` + UI 表单，允许输入 FileUrl/SourceLanguage/TaskKey、查看 job 状态、Markdown 预览与脱敏密钥，所有请求都强制走真实 `TingwuCoordinator` 并输出日志。
- 更新 `docs/current-state.md` 描述新的 APK 角色与 Tingwu 测试入口，提醒团队使用专用 App 诊断签名/参数问题。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- App 目前只接受手动输入的 OSS/HTTPS URL，后续需要集成文件选择 + OSS 上传；同时建议补充 instrumentation 测试覆盖空字段/异常流。
## <YYYY-MM-DD> – Tingwu 测试默认样例与防重提交

Layer: T1  
Modules: :tingwuTestApp

Summary:
- 将 Tingwu Test App 默认 `FileUrl` 替换为工程提供的长期可用 OSS 音频链接，减少每次手动粘贴。
- 在 `TingwuTestViewModel` 中加入防重提交：若同一 fileUrl+language 已有进行中的 job，则提示并忽略；额外保留 3 秒时间窗口防抖，避免连续点击造成多次提交。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 如需更长时间窗口或区分 TaskKey，可把指纹扩展到 TaskKey/Parameters；当前防抖仅靠 fileUrl+language。
- 默认链接含临时凭证，过期后需更新常量或提供配置入口。

---

## 2025-11-17 – Tingwu Test App 两步式管线

Layer: T1  
Modules: :tingwuTestApp, :data:ai-core, docs

Summary:
- 重构 `TingwuTestViewModel`，移除手动填写 URL 的入口，强制“本地音频→OSS 上传→Tingwu 提交”两步流程，并在提交/上传成功时刷新状态徽章。
- 新增 `PipelineCheckpointUi`/诊断面板，实时展示 Tingwu/OSS 凭据是否加载、上传状态与最新 JobId，日志默认输出凭据加载结果，方便排查密钥缺失。
- 调整 Compose UI：卡片化“步骤 1”与“步骤 2”，加入可复制的 OSS URL、提交按钮限流，README 更新为新的操作流程。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 仍缺少 instrumentation/UI 测试验证 SAF 选文件等流程，真机需持续自测。
- OSS 上传仍依赖本地临时凭证，需确保 `local.properties` 配置完整且不过期。

---

## 2025-11-17 – Tingwu 诊断 Logger 升级

Layer: T1  
Modules: :tingwuTestApp, :data:ai-core, docs

Summary:
- 在 `TingwuTestViewModel` 中新增结构化 `PipelineLogEntry`，针对凭据加载、OSS 上传、Tingwu 请求、Job 轮询、URL 有效期、UI 事件等关键节点输出 SUCCESS/WARNING/ERROR 级别，默认自动侦测 URL 过期/HTTP 通道等风险。
- `submitTask`/`uploadLocalAudio` 等流程全部打点，复用统一的 `recordDiagnostic`，并在 UI 中以“详细日志”卡片展示（含时间、阶段、等级），便于一眼看出是否卡在凭据、上传或 Tingwu 请求。
- README 同步描述新的日志矩阵，让调试者知道如何借助诊断卡定位凭据缺失或 URL 失效。
- `RealTingwuCoordinator` 现在记录最近一次 `TINGWU_BASE_URL`，遇到 HTTP 404 会在错误提示中附带当前 BaseUrl，提醒检查地域/网关配置而不仅仅是 OSS URL。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 诊断列表仍依赖手动操作触发；后续可考虑接入持久化或导出功能，便于 QA 附件收集。
- 尚未在 `:aiFeatureTestApp` / `:app` 共享该 Logger，如需跨模块复用需抽象到公共组件。

---

## 2025-11-18 – Tingwu 转写结果可视化

Layer: T1  
Modules: :tingwuTestApp

Summary:
- `TingwuJobUi` 现在保存完整的 `transcriptMarkdown`，ViewModel 在任务完成时直接写入原文而不仅仅是首行摘要。
- `TingwuJobCard` 新增“转写内容”区域，可滚动查看完整 Markdown，并支持一键复制，调试时无需再跳转其它 App。
- README 更新 UI 说明，让测试同学知道界面已经可以直接预览转写文本。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 当前仍仅展示 Markdown 原文，后续可考虑分段渲染/高亮说话人。
- 若任务特别长，UI 滚动区域可能不足，需视体验再做分页或全文导出。

## 2025-11-18 – Tingwu 网络与鉴权强化

Layer: T1  
Modules: :data:ai-core, :tingwuTestApp

Summary:
- 扩展 `AiCoreConfig` + `TingwuNetworkModule`，加入可配置的 connect/read/write 超时、首轮轮询延迟、HTTPDNS/事件监听、连接池参数与全局轮询上限，满足官方离线转写 QPS/超时指南。
- `RealTingwuCoordinator` 引入结构化审计日志、模型白名单 + 默认 `qwq`、首次轮询延迟≥2s、5s 间隔以及细分 DNS/连接/SSL 失败，强化全链路监控与容错。
- `TingwuAuthInterceptor` 回退 HMAC-SHA1 canonical 规则、清理额外头部，配合同步后的系统时间，在真机上验证 CreateTask/轮询均返回 200。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- HTTPDNS 仍为可选空实现，后续需接入真实解析器；需继续补充 Tingwu 相关单测/集成测试并在设备层确保时间同步以避免签名误差。

---

## 2025-11-18 – 整理 Tingwu 接口与权限策略

Layer: T1  
Modules: docs

Summary:
- 新建 `tingwu-doc.md`，记录 `GetTaskInfo` 请求语法、路径参数与返回字段，方便根据官方字段解析轮询结果与下载链接。
- 补充 RAM 权限策略通用结构，列出所有可用的 `tingwu:*` Action，强调 Resource 仅支持 `"*"`，便于排查鉴权配置。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 后续若官方字段或 Action 更新，需要同步维护该文档并在 `docs/current-state.md` 提醒以免信息过期。

---

## 2025-11-18 – 修复 Tingwu 构建冲突

Layer: T1  
Modules: :data:ai-core, :feature:chat

Summary:
- 为 `:data:ai-core` 与 `:feature:chat` 新增 `core.properties` packaging exclude，避免 Aliyun SDK 产生的重复资源导致 `MergeJavaRes` 失败。
- 更新 `RealTingwuCoordinatorTest`，补齐 `OssSignedUrlProvider` 假实现、`ossObjectKey` 参数与最新 `TingwuApi` 签名，解除 KAPT/JVM 测试编译错误。
- 补充 `META-INF/DEPENDENCIES` 排除项，防止 Apache HttpComponents JAR 在 AndroidTest 构建阶段重复打包。
- 进一步排除 `META-INF/LICENSE.md`，以压制 JAXB/Jakarta 相关依赖在 instrumentation 任务中的资源冲突。
- 最后排除 `META-INF/NOTICE.md`，彻底解决 JAXB/Jakarta 栈在 AndroidTest merge 阶段抛出的重复 NOTICE 报错。

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- 需执行 `./gradlew :data:ai-core:test :feature:chat:testDebugUnitTest` 确认 JVM 测试恢复通过，并确保 packaging 排除项不会掩盖潜在资源缺失。
- `FakeSignedUrlProvider` 仅返回固定 URL，后续需补充覆盖真实 `RealOssSignedUrlProvider` 的集成测试。

---

## 2025-11-18 – AiFeatureTestApp 新增 WiFi & BLE Tester 页面

Layer: T1  
Modules: :aiFeatureTestApp, :feature:connectivity

Summary:
- 在测试 App 中搭建完整的 “WiFi & BLE Tester” 页面，复刻当前状态/扫描控制、BT311 扫描状态、Wi-Fi 名称配置、网络状态查询、Web 控制台、BLE 数据窗口等分区。
- 扩展 `ConnectivityControlViewModel`，新增停止扫描/断开设备、热点读取、网络状态查询、控制台 IP/端口输入与日志记录，同时继续调用 `DeviceConnectionManager` 执行配网与热点请求。
- 主界面增加页面切换器，可在 “AI & 媒体” 和 “WiFi & BLE Tester” 两个页面之间跳转，让调试包兼顾 AI 功能与 BLE 工具模块。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 当前扫描/网络状态仍使用示例数据，需接入真实 BLE Gateway 与硬件回执后替换为实际响应。
- `./gradlew :app:compileDebugKotlin` 仍因 Gradle wrapper 权限被拒绝未执行，修复后需同时编译 `:aiFeatureTestApp` 以验证此页面。

---

## <YYYY-MM-DD> – BT311 Wi-Fi/BLE 指令与网络查询

Layer: T1  
Modules: :feature:connectivity, :aiFeatureTestApp

Summary:
- 新增 `DeviceNetworkStatus` 模型与 `queryNetworkStatus` 管线，扩展 `WifiProvisioner`、`DeviceConnectionManager`、`BleGateway`，并在 `GattBleGateway` 中实现 `wifi#connect` 指令和 `wifi#address` 查询字符串解析。
- 更新 `Simulated/AndroidBleWifiProvisioner` 与默认管理器，实现热点/网络查询、凭据发送错误映射，并补充 `DefaultDeviceConnectionManagerTest`/`AndroidBleWifiProvisionerTest` 覆盖新路径；验证：`./gradlew :feature:connectivity:test`.
- 重写 `ConnectivityControlViewModel` 扫描/连接逻辑，仅追踪 BT311、自动重扫、写入 `wifi#connect` 预览，并让 WiFi 配网/网络查询按钮通过真实 BLE 管线；同步 `WifiBleTesterPage` 与 `ConnectivityControlPanel` UI。

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- 需在真机 BT311 上验证 `wifi#connect`/`wifi#address` 协议及网络查询回执，当前扫描/信号仍为模拟逻辑。
- `queryNetworkStatus` 依赖已有配网会话，未连接设备时会提示缺少会话；后续可考虑在连接前提供更友好的提示或自动触发配网。

---

## 2025-11-18 – WiFi & BLE Tester 嵌入媒体库

Layer: T1  
Modules: :aiFeatureTestApp

Summary:
- 在 WiFi & BLE Tester Route 中合并硬件媒体库面板，复用 `MediaServerPanel`，并把 BLE 返回的 `wifi#address#ip#name` 自动写入“服务地址”字段，直接驱动上传/删除/应用/下载操作。
- 新增 `PhoneWifiMonitor`（基于 WifiManager/NetworkCallback）与 Hilt 绑定，ViewModel 订阅手机当前 Wi-Fi 名称并与设备上报的 SSID 比对，UI 按匹配/不匹配显示提示色。
- 重构 `ConnectivityControlViewModel` 状态：引入 `mediaServerBaseUrl`、网络匹配提示、服务地址更新 API，移除手动 IP/端口输入，网络查询成功时自动刷新 Web 控制台/媒体面板地址。
- WiFi 页面 UI 更新：扫描状态独立于网络匹配提示，Web 控制台区域改为单一服务地址输入框，页面底部直接渲染媒体库，保持与 AI & 媒体页一致的操作体验。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 读取手机 Wi-Fi SSID 依赖定位权限与系统设置，若用户拒绝将无法自动匹配；需在后续版本提示授权或提供降级方案。
- 媒体面板仍沿用 HTTP 上传逻辑，真实设备需验证自动填充地址后的上传/下载可用性，并考虑 HTTPS/鉴权需求。

---

## 2025-11-18 – DashScope 回复剪贴板 + 转写格式化

Layer: T1  
Modules: :feature:chat, :aiFeatureTestApp

Summary:
- 扩展 `ChatShareHandler`，让 DashScope 回复在 `handleAiSuccess` 返回后自动写入剪贴板，并通过 UI `clipboardMessage` 提示“助手回复已复制”；Android 端共用统一的 `copyText` 辅助，保留手动 `copyMarkdown` 能力。
- 引入 `PhoneWifiMonitor` 与 Wifi & BLE 页面之前已整合的媒体逻辑保持一致；（already done earlier? skip mention? but new entry should mention autop copy & transcription) Wait no: need mention transcript reformat, not wifi.
- 新增转写格式化工具，将无结构的 Tingwu Markdown 解析为 “### 转写内容 + 列表” 的 Markdown，`importTranscript` 与 `observeTranscript` 均在写入消息前调用，避免“意大利面”文本。
- 更新聊天单测：验证助手回复确实被复制、`clipboardMessage` 文案正确，且 `importTranscript` 会输出格式化 Markdown；跑通 `./gradlew :feature:chat:test`。

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- 自动复制依赖系统剪贴板权限，若被禁用会回退为错误提示；后续可考虑在 UI 中提供更明确的授权指引。
- 转写格式化目前基于句号/感叹号等分句，若未来服务端返回严格 Markdown，需避免重复包裹，可在协议中约定字段。

---

## 2025-11-18 – WiFi & BLE 页面去除手动 MAC 输入

Layer: T1  
Modules: :aiFeatureTestApp

Summary:
- 参考 `SmartSalesAssistant_Final_Delivery` 版本，撤销 `ConnectivityControlViewModel` 的手动 MAC/名称字段与 `applyManualDevice`，重新启用设备 `phoneWifiName` 回传以维持网络匹配逻辑。
- `WifiBleTesterPage` UI 去掉“手动指定设备”分区，Route 端不再传入相应回调，避免用户误以为需要手动输入地址。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 仍需尽快把扫描逻辑切到真实 `GattBleGateway`，以免缺少手动输入时无法锁定测试设备。
- 若 BT311 仍未广播，页面会一直显示“未发现设备”，需要在文档补充模拟器使用指南。

---

## 2025-11-18 – WiFi & BLE Tester 接入真实 BLE 扫描

Layer: T1  
Modules: :aiFeatureTestApp, :feature:connectivity

Summary:
- 新增 `BleProfileConfig` 与 `BleScanner` 抽象，由 Hilt 提供 `AndroidBleScanner` 实现，集中处理 `BT311`/`6E400001` 过滤与权限错误；`ConnectivityControlViewModel` 仅消费扫描结果，不再直接操作 `BluetoothAdapter`，并在选择设备时通过 `DeviceConnectionManager.selectPeripheral` 建立即时会话，支持“查询设备网络”独立运行。
- `GattBleGateway` 对照厂家示例 `SpBLE.java` 打开 `0x2902` 通知描述符、在写入 `6E400002/6E400003` 特征后优先等待通知（超时后回退到主动读取），保证 `wifi#connect` 与 `wifi#address#ip#name` 等指令能够按照厂商建议的 UART 通道收发。
- `AndroidManifest` 声明 `BLUETOOTH`/`BLUETOOTH_ADMIN`/`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` 权限与 BLE feature，`AiFeatureTestActivity` 在启动时请求必要权限，避免 `SecurityException`。
- `WifiBleTesterPage` 保持 BT311 专属提示，只显示目标设备，便于聚焦真实配网流程。

TDD Status:
- [ ] Tests written first
- [ ] Tests added after implementation
- [x] Manual testing only

Risks / TODO:
- 需在真机验证扫描结果是否能稳定识别 BT311；若设备广播名与 `BT311` 不一致，需要放宽匹配规则。
- Android 12+ 的权限弹窗目前一次性请求，后续可在 UI 中增加显式入口（比如“重新授权”按钮）以便用户更改设置。

---

## 2025-11-19 – Tingwu API aligned with official doc

Layer: T1  
Modules: :data:ai-core, docs

Summary:
- 将 `RealTingwuCoordinator` / `TingwuApi` 从代理版 `/api/v1` 切换至官方 `PUT /openapi/tingwu/v2/tasks`，请求体遵循 `AppKey + Input + Parameters`，轮询与结果解析全面使用 `RequestId/Code/Message/Data.*` 结构。
- 新增 ROA 签名 `TingwuAuthInterceptor`，自动注入 `x-acs-*`，生成 HMAC-SHA1 签名并发送 `Authorization: acs <AccessKeyId>:<signature>`，移除旧版 Bearer Token。
- 更新 `APIs.txt`、`tingwu-doc.md` 及 `RealTingwuCoordinatorTest`，覆盖新的请求/响应模型并验证官方信封结构。

TDD Status:
- [ ] Tests written first
- [x] Tests added after implementation
- [ ] Manual testing only

Risks / TODO:
- 需在真机确认官方 `GetTranscription` 是否总是返回内联 JSON；若仅提供 `Result.Transcription` 链接，需补充签名 URL 下载处理。
- 实时任务的 `operation=stop` 仍未串联，后续接入实时模式时需扩展参数与测试。

---
