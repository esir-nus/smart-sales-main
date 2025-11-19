<!-- 文件路径: aiFeatureTestApp/README.md -->
<!-- 文件作用: 说明 AI 测试 App 的用途 -->
<!-- 文件目的: 指导如何独立验证 AI 和媒体流程 -->
<!-- 相关文件: docs/current-state.md, data/ai-core/README.md, feature/chat/README.md, feature/media/README.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# AI Feature Test App

## 模块职责
- 提供轻量 Playground，用于人工验证 DashScope/Qwen 聊天、Tingwu 转写、OSS 上传、媒体同步等流程。
- 允许在不启动主壳 `:app` 的情况下调试 SDK、Fake 服务和 UI 状态。
- 集成 `:tingwuTestApp` 输出的 `TingwuTestViewModel`，通过单按钮“本地音频转写”卡片完成缓存→OSS→Tingwu 的真实链路，并在同一卡片展示 Markdown 结果。
- 不承载正式 UI，也不负责生产配置，主要面向研发/QA。

## 关键接口与依赖
- `AiFeatureTestApplication` / `AiFeatureTestActivity`：注入 `ChatController`、`MediaSyncCoordinator`、`TingwuTestViewModel`（来自 `:tingwuTestApp`）并渲染聊天 + 媒体 + Tingwu 卡片。
- 依赖：`projects.feature.chat`、`projects.feature.media`、`projects.feature.connectivity`、`projects.data.aiCore`、`projects.core.util`、`projects.tingwuTestApp`。
- UI 由 `ChatPanel`、媒体演示卡片和“本地音频转写”卡片组成；后者使用 `cacheAudioFromUri`、`uploadLocalAudioAndSubmit` 处理本地音频与诊断状态。

## 数据与控制流
1. Activity setContent → 聊天面板 + 媒体面板 + Tingwu 卡片；
2. 聊天/媒体部分仍直接调用注入的 Controller/Coordinator。
3. Tingwu 卡片通过 SAF 选择本地音频 → `cacheAudioFromUri` 缓存 → 调用 `TingwuTestViewModel.uploadLocalAudioAndSubmit` 上传到 OSS 并触发真实 Tingwu HTTP；
4. `TingwuTestUiState` 会更新上传状态、最近任务 ID、诊断日志及 Markdown 结果，Compose 卡片读取这些字段展示；
5. 当前版本仅在卡片内部展示转写结果，尚未写回 `ChatController` 消息流。

## 当前状态（T0）
- 聊天/媒体仍以内置 Fake 服务演示；Tingwu 卡片已接入真实 `TingwuCoordinator`，可在一台设备上完成本地音频→OSS→Tingwu 并查看 Markdown。
- 卡片展示任务 ID、最近上传音频、诊断日志与 Markdown；结果尚未注入 `ChatPanel`（聊天区域看不到新的“转写”消息）。
- 构建命令：`./gradlew :aiFeatureTestApp:assembleDebug`，安装后即可本地测试；若需真实 Tingwu，需要在 `local.properties` 配置 `TINGWU_*` 与 OSS 凭据，并在 App 级模块提供 `AiCoreConfig(preferFakeTingwu=false)`。

## 风险与限制
- 聊天/媒体部分仍主要依赖 Fake 服务，尚未提供 DashScope/Tingwu/Fake 切换控制。
- Tingwu 结果仅在卡片中展示，未同步到 `ChatController` 的消息流，容易造成体验割裂。
- 诊断信息集中在卡片中，缺乏导出/分享能力；整体 UI 没有 instrumentation/Compose 测试覆盖。

## 下一步动作
- 增加 Fake/Real 切换、录音入口与诊断导出，方便快速复现问题。
- 补充 Compose instrumentation，保障 Playground 在升级时仍可启动；后续可将媒体/聊天逻辑替换为真实 SDK，并考虑将 Tingwu 诊断导出到文件。

## 调试与验证
- 构建/安装：`./gradlew :aiFeatureTestApp:assembleDebug`。
- 运行：`adb shell am start com.smartsales.aitest/.AiFeatureTestActivity`。
- Tingwu 调试：确保 `local.properties` 配齐 OSS/Tingwu Key，真机连得上 OSS/Tingwu 域；观察 `SmartSalesTest/Tingwu` Tag 诊断日志。
