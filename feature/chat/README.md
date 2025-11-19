<!-- 文件路径: feature/chat/README.md -->
<!-- 文件作用: 描述聊天特性模块的职责与 API -->
<!-- 文件目的: 约束聊天功能的接口、数据流与测试方法 -->
<!-- 相关文件: docs/current-state.md, data/ai-core/README.md, plans/tdd-plan.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# Chat Feature 模块

## 模块职责
- 负责聊天状态机、技能切换、消息记录、Tingwu 转写注入、结构化 Markdown 以及导出请求。
- 提供 `ChatController`、`ChatState`、`AiSessionRepository` 等接口供 UI 与其它特性模块使用。
- 不直接处理 UI 布局（Compose 组件在 `ui/` 目录，可供多个 App 引用），也不负责底层网络/SDK。

## 关键接口与依赖
- `ChatController`：暴露 `state: StateFlow<ChatState>` 及 `send`, `updateDraft`, `toggleSkill`, `copyMarkdown`, `requestExport`, `startTranscriptJob`, `importTranscript`（外部模块可直接注入 Markdown 结果）等操作。
- `AiSessionRepository`：使用 Room (`AiSessionDatabase`) 持久化最近会话，提供 Flow 给抽屉使用。
- 依赖：`projects.data.aiCore`（AiChatService、ExportManager、TingwuCoordinator）、`projects.core.util`（Result、Dispatchers）、Room（未来接入持久化）。
- UI：`ChatPanel` 提供可复用 Compose 视图，供 `:app` 和 `:aiFeatureTestApp` 直接引用。

## 数据与控制流
1. UI 调用 `ChatController` Intent。
2. Controller 通过互斥锁按顺序推送 User 消息 → 调用 `AiChatService` → 收到结果更新消息、Markdown、导出状态 → 同步写入 `AiSessionRepository`。
3. Tingwu 流程：`startTranscriptJob` → 调用 `TingwuCoordinator.submit` → `observeJob()` Flow 注入转写消息。
4. 导出流程：调用 `ExportManager`，根据结果更新 `ChatExportState` 并提示 UI 复制/分享。

## 当前状态（T0→T1）
- 默认注入 `DashscopeAiChatService`（需配置 `DASHSCOPE_API_KEY`），也可在测试代码中显式传入 `FakeAiChatService` 以离线运行；无论真实/假实现都会把助手 display 文本写入 Markdown，确保 `copyMarkdown` 行为与 UI 提示一致。
- 剪贴板复制与导出分享通过 `AndroidChatShareHandler` 直接落地到系统 Clipboard + Share Sheet，并已由 `DefaultChatControllerTest.copyMarkdown_pushesToClipboardHandler` 验证。
- `./gradlew :feature:chat:test` 需要确保 Hilt/Room KAPT 依赖可从 Aliyun 获取（或镜像）。

## 风险与限制
- 没有真实 SDK，无法验证 streaming SSE、长文 Markdown、导出体积等问题。
- 缺乏错误重试、技能提示文案、多语言支持，易导致 UI 体验不一致。
- Session Repository 缺持久层，App 重启即丢失历史。

## 下一步动作
- 引入 Room + DAO，将 `AiSessionRepository` 替换为持久实现，并提供抽屉数据源。
- 接入真实 DashScope/Tingwu/Export，实现流式响应与分享；补齐错误处理与重试策略，同时验证真实 SDK 返回的 Markdown 继续包含助手 display 文本。
- 丰富单测：技能切换、导出失败、Tingwu 错误、剪贴板状态寿命，确保达成 T1 要求。

## 调试与验证
- 单测：`./gradlew :feature:chat:test`。
- 真机：在 `local.properties` 设置 `DASHSCOPE_API_KEY` 后安装 `:app` 或 `:aiFeatureTestApp`，验证真实 DashScope 调用、剪贴板、导出提示是否正常。
