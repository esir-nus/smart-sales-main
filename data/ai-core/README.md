<!-- 文件路径: data/ai-core/README.md -->
<!-- 文件作用: 描述 AI 核心数据层与 SDK 封装 -->
<!-- 文件目的: 统一 DashScope、Tingwu、OSS 的实现方式 -->
<!-- 相关文件: docs/current-state.md, reference-source/legacy/DEPENDENCY_VERSIONING.md, plans/tdd-plan.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# AI Core 模块

## 模块职责
- 封装 DashScope/Qwen 聊天、Tingwu 转写、Aliyun OSS 上传等云端能力，向上暴露 `AiChatService`、`TingwuCoordinator`、`ExportManager`、`OssRepository`。
- 管理 Retrofit/OkHttp 客户端、认证拦截器、数据模型、Hilt Module（`AiCoreModule` 等）。
- 不直接处理 UI 或业务策略，只提供数据访问和 SDK 适配。
- 提供 `AiCoreConfig` / `DashscopeCredentialsProvider` / `DashscopeClient` 配置点，便于在不同 App 中切换 Fake/真实实现以及重试/超时/流式策略。

## 关键接口与依赖
- `AiChatService`：`DashscopeAiChatService` 通过 `com.alibaba.dashscope:dashscope-sdk-java:2.14.0` 直连 Qwen，`FakeAiChatService` 仅用于 JVM 单测与离线场景。
- `TingwuCoordinator`：`RealTingwuCoordinator` 使用 Retrofit + OkHttp + `TingwuAuthInterceptor` 调用后端，支持 Flow 轮询、超时、错误映射与 Markdown 生成；`FakeTingwuCoordinator` 保留给测试/离线模式。
- `ExportManager`：`RealExportManager` 基于 `MarkdownPdfEncoder`、`MarkdownCsvEncoder` 生成真实字节并写入本地缓存，同时返回 `ExportResult.localPath` 供分享；`FakeExportManager` 仍用于 JVM 测试。
- BuildConfig：在 `data/ai-core` 注入 `DASHSCOPE_API_KEY`、`DASHSCOPE_MODEL`、`TINGWU_API_KEY`、`TINGWU_BASE_URL` 等字段（均来源 `local.properties`）。
- 依赖：Kotlin Coroutines、DashScope SDK、Retrofit 2.9.0、OkHttp 4.12.0、Gson 2.11.0、Hilt 2.52。

## 配置说明（local.properties + AiCoreConfig）

1. 在仓库根目录创建/更新 `local.properties`，写入以下键值：

| 键名 | 对应 BuildConfig | 默认值 | 说明 |
| --- | --- | --- | --- |
| `DASHSCOPE_API_KEY` | `BuildConfig.DASHSCOPE_API_KEY` | 空字符串 | DashScope/Qwen 调用所需的 API Key，若为空会抛出 `AiCoreException(source=DASH_SCOPE, reason=MISSING_CREDENTIALS)`。 |
| `DASHSCOPE_MODEL` | `BuildConfig.DASHSCOPE_MODEL` | `qwen-turbo` | DashScope 模型名称，可自定义且将透传给 SDK。 |
| `TINGWU_API_KEY` | `BuildConfig.TINGWU_API_KEY` | 空字符串 | Tingwu Bearer Token，缺失时无法提交真实任务。 |
| `TINGWU_BASE_URL` | `BuildConfig.TINGWU_BASE_URL` | （必填）如 `https://tingwu.cn/` | 指向 Tingwu 代理服务地址，需以 `/` 结尾。 |

示例：

```
DASHSCOPE_API_KEY=sk-***
DASHSCOPE_MODEL=qwen-turbo
TINGWU_API_KEY=demo-api-key
ALIBABA_CLOUD_ACCESS_KEY_ID=ak-demo
ALIBABA_CLOUD_ACCESS_KEY_SECRET=sk-demo
TINGWU_BASE_URL=https://tingwu.cn/
```

2. 通过 Hilt Module 提供 `AiCoreConfig` 可切换真/假实现与调试行为。例如 `AiFeatureTestAiCoreOverrides` 中配置：

```kotlin
@Provides
@Singleton
fun provideAiCoreConfig(): AiCoreConfig = AiCoreConfig(
    preferFakeAiChat = false,
    dashscopeMaxRetries = 2,
    dashscopeRequestTimeoutMillis = 15_000,
    dashscopeEnableStreaming = true,
    preferFakeTingwu = true,
    preferFakeExport = true
)
```

核心字段含义：
- `preferFakeAiChat` / `preferFakeTingwu` / `preferFakeExport`：控制是否强制注入 Fake。
- `dashscopeMaxRetries`、`dashscopeRequestTimeoutMillis`、`dashscopeEnableStreaming`：控制 Qwen 重试/超时/流式。
- `tingwuPollIntervalMillis`、`tingwuPollTimeoutMillis`、`enableTingwuHttpLogging`：控制 Tingwu 轮询节奏与网络日志。

## 数据与控制流
1. Feature 模块构造请求（`AiChatRequest` / `TingwuRequest` / Markdown 字符串）。
2. Service/Coordinator 执行 suspend/Flow，返回 `Result.Success` 或 `Result.Error`。
3. `ChatController`、`MediaSyncCoordinator` 等消费结果并更新状态。
4. 真实 SDK 接入后需在此模块处理鉴权、重试、超时、序列化。

## 当前状态（T1-）
- DashScope 聊天：`DashscopeAiChatService` 通过新的 `DashscopeClient` + `DashscopeCredentialsProvider` 调用真实 SDK，内建重试/超时/错误映射，并自动把显示文本拼接到 Markdown；若 `AiCoreConfig.dashscopeEnableStreaming=true` 则通过 `streamMessage` 暴露实时 chunk Flow；`FakeAiChatService` 亦实现 `streamMessage` 供 UI 预演。
- Tingwu：`RealTingwuCoordinator` 使用 `TingwuApi` + `TingwuNetworkModule` 轮询真实任务，默认 2s 间隔 + 90s 超时，成功后调用 `getTaskResult` 构造 Markdown；Hilt 可通过 `AiCoreConfig.preferFakeTingwu=true` 回退到 Fake。
- Export：`RealExportManager` 通过 `ExportFileStore` 写入缓存目录并返回本地路径；`AiCoreConfig.preferFakeExport=true` 可强制使用 Fake。
- Hilt 默认绑定真实实现，如需强制 Fake，请在 App 级 Hilt Module 提供自定义 `AiCoreConfig`。

## 风险与限制
- DashScope 请求依赖 `local.properties` 内的 API Key 与可访问阿里云域名的网络；缺失或网络受限会抛出 `AiCoreException(source=DASH_SCOPE)` 并附带“配置 DASHSCOPE_API_KEY”提示。
- Tingwu REST 接口只需 `TINGWU_API_KEY` 与可用的 `TINGWU_BASE_URL`（例如 `https://tingwu.cn/` 或内部代理）；若缺失将自动回退到 Fake 协调器。
- 尚未实现 DashScope/Tingwu 的指标采集或限流上报，长时间调用可能触发官方限流；需要在后续阶段统一日志。
- ExportManager 目前仅将文件写入 App 缓存目录，尚未接入 OSS 上传。
- 模块内所有失败场景都会抛出 `AiCoreException`，包含 `source`（DashScope/Tingwu/Export）、`reason`（网络、凭证、超时等）与 `suggestion`，UI 层可直接展示其 `userFacingMessage`。

## 下一步动作
- 扩展 DashScope 调用：基于现有 `DashscopeClient.stream` 接口补齐 SSE streaming Telemetry，并补充指标上报，保障大批量生成不会阻塞 UI。
- Tingwu：把官方 SDK/签名逻辑补充到 `TingwuAuthInterceptor`，并支持任务恢复（APP 重新打开后继续轮询）；同时补充断线重试与 metrics。
- Export：将 `ExportResult.localPath` 接入分享/上传链路，并记录导出结果（成功/失败）供业务观测。
- 使用 `:core:test` Fake/fixture 补充单测：验证 DashScope 请求构造、Tingwu 状态机与 Markdown/PDF/CSV 输出。

## 调试与验证
- 单测：`./gradlew :data:ai-core:test` 覆盖 DashScope/Tingwu/Export 纯逻辑（含 CSV/PDF 编码）。
- 真实 DashScope/Tingwu：在 `local.properties` 配置 `DASHSCOPE_API_KEY`、`TINGWU_API_KEY` 以及 OSS 凭证，并在 App 级 Module 提供 `AiCoreConfig(preferFakeAiChat=false, preferFakeTingwu=false, preferFakeExport=false)`，随后运行 `./gradlew :app:installDebug` 或 `:aiFeatureTestApp:installDebug` 真机验证；调试 Tingwu 时需保证设备可访问 `TINGWU_BASE_URL`。
