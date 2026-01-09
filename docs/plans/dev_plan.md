# 开发计划（Layered + KISS + TDD）

本计划基于 `relevant_map.json` 中的成熟实践以及 wireframe 截图 (`concepts/Weixin Image_*.png`)。  
它拆分模块，强调 **T0 → T1 → T2 → T3 分层交付**，并在关键点使用 TDD。

开发中的所有阶段性结果，必须在 `docs/progress-log.md` 中记录。

---

## 一、输入资料（Inputs）

- `agent.md`：跟踪原则、文档、流程。开发前先阅读，任务结束后回传结论。
- `relevant_map.json`：旧项目的最佳实践，尤其是 AI、OSS、网络模块的接口设计。
- `concepts/Weixin Image_*`：UI 参考。需要保持结构（主界面、聊天历史、音频下拉、图像编辑器）。
- `reference-source/`：当需要 SDK 或 API 细节时阅读；总结写回该目录或本计划。

---

## 二、分层交付模型（T0 → T3）

我们使用 T0–T3 分层 + 混合 TDD 策略：

- **T0 – Running（最高优先级）**  
  目标：得到一个粗糙但贯通的端到端流程（Walking Skeleton）

  T0 目标：
  - BLE → Wi-Fi 凭据传输
  - 至少一次音频文件同步
  - 最小化 Qwen 调用返回文本
  - 最小化 Tingwu 文本/转写展示
  - 基础 Markdown → PDF/CSV 导出（无格式美化）

  **交付物**：可以跑通的整体骨架，手动测试可接受。

- **T1 – Stable（TDD 为主）**  
  目标：用测试和观测手段把关键路径“拧紧”。

  T1 重点（TDD 优先）：
  - BLE 状态机
  - 同步重试 / 退避策略
  - Tingwu 解析与 AI 错误处理
  - Markdown → PDF/CSV 流程
  - 离线 / 在线行为与状态恢复

  **交付物**：核心行为稳定可预期，具备单测 & 适当集成测试。

- **T2 – UI Polish**  
  目标：让 UI/UX 达到产品级体验。

  内容：
  - Loading / Empty / Error 状态
  - Chat 氛围、动画、交互细节
  - Tablet 布局与响应式设计

- **T3 – Extras**  
  目标：锦上添花的能力。

  内容：
  - 高级媒体处理（更多 filter / 编辑能力）
  - 额外 AI prompt / 模板
  - 实验性功能

> **原则：** T0 有绝对优先级，除非稳定性问题已经严重影响继续开发。  
> T1 是 TDD 主要投入阶段；T2/T3 在行为复杂时做选择性测试。

---

## 三、阶段概览（按模块/阶段展开）

下面的阶段安排结合了 T0–T3 概念：  
- 每个阶段写明当前主要服务哪个层级（T0 / T1），  
- 并指出在何处使用 TDD。

#### :data:ai-core roadmap

- **阶段 1：接口稳固 + 可配置真/假实现**  
  - 核心目标：把 `AiChatService`、`TingwuCoordinator`、`ExportManager` 等接口与 `AiCoreConfig`（`data/ai-core/.../AiCoreConfig.kt:7-9`）定型，明确哪些 App Flavor 注入真/假实现。  
  - 任务：梳理公共数据类（`AiChatRequest/Response`、`TingwuRequest`、`ExportResult`）；补充文档/注释约束；在 `AiCoreModule`（`data/ai-core/.../AiCoreModule.kt:18-47`）里提供 feature flag（`BuildConfig` 或 Hilt qualifier）让调试/真机可以切换。  
  - 测试：为纯数据/工具函数写单测（例如 Markdown 拼接、文件名生成），使用 `:core:test` 的 `runTest` 环境，确保模块可独立构建（`./gradlew :data:ai-core:test`）。
- **阶段 2：DashScope 聊天服务（真实 SDK）**  
  - 任务：补齐 `DashscopeAiChatService` 的异常映射、重试、日志与 metrics（`data/ai-core/.../DashscopeAiChatService.kt:13-140`）。实现 streaming 或 chunk 回调接口，至少支持非 streaming 输出；提供 `FakeAiChatService` 扩展字段模拟失败。  
  - 测试：编写 JVM 单测，注入假 `Generation` 客户端（可用 interface + fake）验证 prompt 构造、Markdown 合并策略；使用 Robolectric 或 instrumentation（当 SDK 需要 Android 环境）做冒烟调试。  
  - 模块化：以接口 + DTO 封装 DashScope，避免 UI/Feature 层直接依赖 SDK；将 API key/model 从 `BuildConfig` 读取，配合 `local.properties` 说明。
- **阶段 3：Tingwu 协调器（真实 HTTP / SDK）**  
  - 任务：在 `TingwuCoordinator` 接口下添加真实实现（Retrofit/OkHttp 或官方 SDK），把 `FakeTingwuCoordinator` 仅用于测试。引入 job 状态缓存 + backoff，支持断线恢复，确保 Flow 不泄露 scope（参考 `TingwuCoordinator.kt:20-68` 现有 fake 逻辑）。  
  - 测试：对解析/状态机写单测（mock REST responses、超时、cancel）；用 instrumented fake server（`reference-source/wifi.py` 风格）跑集成测试。通过 Hilt test module 替换协程调度器，验证 UI 层 Flow 消费稳定。
- **阶段 4：导出管线（PDF/CSV/后端/OSS）**  
  - 任务：强化 `ExportManager`（`data/ai-core/.../ExportManager.kt:10-55`），拆分编码器（PDF → `MarkdownPdfEncoder`, CSV → 专用 formatter），再提供真实导出（本地文件 + Android ShareSheet）或上传到服务器。  
  - 测试：单测校验 Markdown→PDF/CSV 的字节输出（使用 `ByteArrayInputStream` + parser）；集成测试模拟导出成功/失败，验证错误传递；对大 Markdown 做性能测试。
- **阶段 5：横向关注点**  
  - 配置管理：整理 `local.properties`/`BuildConfig` 使用方法，在 `data/ai-core/README.md` 写清楚 key/endpoint 要求，对应 `docs/current-state.md` 的约束。  
  - 监控与日志：统一封装日志 Tag（避免 UI 模块直接访问 SDK log）；对失败场景返回结构化错误供上层展示。  
  - 依赖镜像：所有新 SDK（DashScope、Tingwu、OSS）按 `agent.md 4` 和 `reference-source/legacy/DEPENDENCY_VERSIONING.md` 记录镜像来源，保证 CI 离线可用。  
  - 文档/流程：完成上述阶段后触发 `invoke logging workflow`，向 `docs/progress-log.md` 补日志并视情况更新 `docs/current-state.md`，确保与 `plans/dev_plan.md:60-110` 的阶段 2 要求保持一致。

---

### 阶段 1：基础设施上线（偏 T0）

目标：项目能编译、能跑，基础工具就绪。

- 搭建 Gradle wrapper、settings、version catalog、Hilt/Compose/Room 等依赖。
- 配置阿里云 Maven 镜像（google/public）作为外部主仓库，`third_party/maven-repo` 只在镜像缺失或需私有 SDK 时补充，避免强制离线同步全部依赖。
- 输出初版 `README`，说明如何启动项目与复制 SDK。
- 所有模块建立时添加四行中文头部注释模板（见 `agent.md`）。
- **测试（可选，非阻塞 T0）：**
  - 根任务运行 `./gradlew help`
  - `ktlintCheck` / `detekt` 验证脚本基础可用。

记录：在 `docs/progress-log.md` 中标记为 **T0 / infra**。

---

### 阶段 2：核心模块初始化（T0 → T1）

目标：为后续 Feature 提供通用基础能力。

- `:core:util`
  - 定义 `Result` 类型、调度器、日志工具等。
  - TDD 建议：为纯函数 / 错误封装写单元测试（T1）。

- `:core:test`
  - 构建假数据（样例 markdown、音频元数据、BLE 状态）。
  - 提供跨模块公用的 Test Helpers / Fakes。
  - 测试可以轻量，重在可复用性。

- `:data:ai-core`
  - 迁移 / 实现 AI & Tingwu 服务接口（例如 Retrofit 客户端或 SDK 封装）。
  - 先用 mock 客户端（参照 `relevant_map.json`）。
  - **TDD 焦点（T1）：**
    - API 模型序列化测试。
    - AuthInterceptor header 注入测试。
    - Transcript 解析 / 正规化（对边界 case 做测试）。

记录：将已经完成 T0 骨架的部分，以及已经进入 T1 的测试点记录到 `progress-log.md`。

---

### 阶段 3：连接模块 `:feature:connectivity`（T0 → T1 核心）

目标：打通 BLE → Wi-Fi → Sync 的链路，是整个项目的“底座”。

- 需求：BLE 提供 Wi-Fi 凭据，Wi-Fi 同步状态给其他模块。
- 任务：
  1. 定义 `DeviceConnectionManager` 接口（`Flow` 状态 + 命令）。
  2. 实现 Fake 版本，模拟成功、失败、重试。
  3. 暴露 `ConnectionState` 数据结构供 UI / 媒体模块订阅。
- **TDD（T1 重点）：**
  - 状态机单元测试（`pairing -> connected -> syncing` 等）。
  - 失败 / 重试 / 超时逻辑测试。
- 文档：
  - 在本文件末尾或 `reference-source/ble-notes.md` 记录真实设备协议细节。

T0：先完成一条最小“真实设备可以连上并传一次凭据”的路径。  
T1：用 TDD 把状态机和错误处理补强。

---

### 阶段 4：聊天功能 `:feature:chat`（T0 → T1）

目标：实现主界面聊天体验 + 结构化输出基础能力。

- 需求来自主界面与结构化输出截图。
- 步骤：
  1. 设计 `ChatController`（意图 -> 状态 Flow） + `ChatState`（messages, skills, exports）。
  2. 集成 `AiChatService`, `TingwuCoordinator`, `ExportManager` 接口（来自 `:data:ai-core`）。
  3. 会话存储（例如 `RoomAiSessionRepository`）+ sticky history 列表（遵守 `<date><person><theme>` 风格）。
  4. Compose UI 骨架：顶部 welcome、技能按钮、输入栏、结构化 markdown viewer。
  5. 添加 markdown copy + PDF/CSV 触发器（调用 `ExportManager` stub）。
- **测试（T1 推荐用 TDD）：**
  - `ChatControllerTest`：验证技能切换、流式响应、失败重试。
  - 在 UI 稳定后：Compose UI screenshot/semantics tests（button states, sticky headers）。
- 文档：
  - 在 `docs/chat/README.md`（待建）写交互说明，注明 T0/T1 已完成范围。

T0：可以发一条消息，收到 AI 文本，看到简单历史。  
T1：围绕 ChatState 和错误处理补充测试。

---

### 阶段 5：媒体功能 `:feature:media`（T0 → T1）

目标：实现音频下拉视图 + 媒体推送基础流程。

- 需求来自音频下拉和图像编辑器 wireframe。
- 步骤：
  1. `MediaSyncCoordinator` 负责下拉刷新、调用 `OssRepository`、通知 gadget。
  2. `AudioViewerState` 按 wireframe 顺序展示，并支持后续的播放/推送。
  3. `ImageEditorDelegate`：上传/删除/应用按钮逻辑（UI 占位，未来接真实 API）。
  4. Compose UI：下拉出现半透明面板，列表项含时间 + 客户名。
- **测试（T1 建议 TDD 或半 TDD）：**
  - Fake gadget API 测试同步过程（成功、失败、部分成功）。
  - UI tests 覆盖 pull-to-reveal 与列表渲染顺序。
- 文档：
  - 补充 `docs/media/README.md`，说明当前支持的操作和限制。

T0：能看到列表、触发一次同步和一次推送。  
T1：同步流程的错误分支和重试策略可被测试覆盖。

---

### 阶段 6：集成与打磨（主要 T2/T3，少量 T1）

目标：把各 Feature 接入 `:app`，并做整体体验调优。

- 在 `:app` 中通过 Hilt 连接各 Feature。
- 实现导航：主界面 + drawer + image editor。
- 响应式设计：手机 / 平板断点布局，hover/tooltip 支持（使用 Compose `TooltipArea`）。
- 错误处理：统一 snackbar/toast + retry；记录 telemetry。
- PDF/CSV 导出完成后使用 Android share sheet。
- **测试：**
  - 端到端 instrumentation tests（mock backends）。
  - 对关键导航 / 导出流做 UI 测试。
- 文档：
  - 更新 `README` 安装步骤。
  - `CHANGELOG` 写首个版本记录。
  - `TEST_PLAN` 列出通过的用例。

这一阶段主要属于 **T2/T3**，但若发现某些核心逻辑仍不稳定，应回补到 **T1/TDD**。

---

## 四、Dependencies & Order（依赖与顺序）

整体优先级和依赖关系如下：

- Connectivity first（连接优先，一切依赖设备链路）
- AI second（chat + transcripts）
- Media after sync is stable（在同步稳定后再做媒体体验）
- App shell last（最后接壳 & 导航）

与模块顺序对应：

1. `:core:util` / `:core:test`
2. `:feature:connectivity`
3. `:data:ai-core`
4. `:feature:chat`
5. `:feature:media`
6. `:aiFeatureTestApp`
7. `:app`

---

## 五、交付与沟通（Delivery & Communication）

- 每个阶段结束后，**必须**在 `docs/progress-log.md` 中追加一条日志（日期 + 完成内容 + 阻塞 + 对应层级 T0/T1/T2/T3）。
- 当需要变更计划时：
  1. 在 PR 描述中引用本文件的原条目；
  2. 修改本文件对应段落；
  3. 在 `progress-log.md` 中记录“计划变更”。
- 任何新工具 / 脚本：
  - 在 `agent.md` 的 “Tooling & Workflow” 部分加说明；
  - 并在本文件相关阶段引用（例如“运行脚本 X 以完成步骤 Y”）。
- 若新增模块 / 跨模块依赖：
  - 更新本文件的 “Dependencies & Order”；
  - 视情况更新 `plans/tdd-plan.md` 中的 Critical TDD Zones。
