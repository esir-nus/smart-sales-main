# Repository Guidelines

## Role Contract Reference

This project uses a fixed multi-agent workflow:

- **Operator** = You (human developer, writes all real code)  
- **Orchestrator** = ChatGPT default mode (specs, flows, contracts, no code)  
- **Codex** = ChatGPT code-analysis mode (invoked with `Codex:`)

The full rules and invocation protocol are defined in **`role-contract.md`**.  
All agents must follow that document as the authoritative behavior contract.

- **Orchestrator / MetaHub 合约来源**：`docs/Orchestrator-MetadataHub-V5.md` 是编排与 MetaHub 的最新权威规范（CURRENT）；`docs/Orchestrator-MetadataHub-V4.md` 已归档（ARCHIVED），仅作历史参考。
- **UX 合约来源优先级**：
  1. `docs/ux-contract.md`（唯一现行 UX 真实来源，交互/布局/流程）
  2. `docs/Orchestrator-MetadataHub-V5.md`（推理与元数据，V5 为现行规范；V4/V3 仅历史参考）
  3. `docs/style-guide.md`（视觉规范）
  4. 现有 Android 实现与测试
  5. 归档的 React/UI 仅作历史参考；`assistant-ux-contract.md` 已归档，请勿作为现行规范。

---

## 文档版本与纪律（Versioning & Doc Discipline）

1. **现行规范**
   - CURRENT：`docs/Orchestrator-MetadataHub-V5.md`
   - ARCHIVED：`docs/Orchestrator-MetadataHub-V4.md`

2. **XFyun-first（文档口径）**
   - 讯飞 REST 的唯一权威来源：`docs/xfyun-asr-rest-api.md`
   - 禁止在其它文档复制其参数大表（只能“链接 + 结论摘要”）

3. **能力护栏（必须一致）**
   - 当前仅允许 `transfer`；`translate/predict/analysis` 默认禁用，禁止“先发请求试试看”（避免 `failType=11` 等失败）

4. **Tingwu 与 OSS 的位置**
   - Tingwu：legacy/deprecated，仅遗留维护（不做文档默认路径）
   - OSS：保留为未来扩展工具箱（异步/分发/大文件/多提供方），不强制参与当前 XFyun file-stream 上传链路

## 核心行为要求（对 Codex）

1. **优先写干净、简单、模块化的代码**
   - 小函数。小类。清晰的模块边界。
   - 避免过度抽象和过深继承。
   - 能用简单方案解决时，不追求花哨设计。

2. **使用简单、易懂的语言**
   - 注释和文档用简体中文。
   - 句子要短。少用长句和复杂结构。
   - 变量命名清晰。表达真实含义。
   - All重要代码块必须添加简短中文注释，方便其他开发者快速理解。

3. **不要偷懒，始终完整阅读相关文件**
   - 在修改一个文件前，应读完整个文件。
   - 在引用一个文档（如 `dev_plan.md`）前，应读完整个文档。
   - 不根据“猜测”或“片段”做决定。

4. **中国网络环境优先**
   - 修改构建脚本或依赖配置时：
     - 优先考虑使用阿里云镜像（Aliyun mirror）或其它国内镜像。
     - 避免依赖访问极慢或经常超时的海外源。
   - 在提示命令或配置时，可给出示例镜像配置，但不强制改动现有脚本。

5. **Kotlin 项目请合理使用 helper**
   - 按最佳工程实践调用辅助脚本/工具，遵守现有代码风格、测试与依赖规范。
   - 不以省事为由跳过质量检查；helper 仅用于加速，而非替代审查和验证。

6. **限制感知策略（必须遵守）**
   - 如果失败像是 **网络 / 沙盒 / 凭证限制**（例如：依赖下载失败、DNS/TLS 错误、401/403、429 限流、主机被阻断），只尝试 **1–2 次**（最多 1 次普通尝试 + 1 次最小必要的复查/重试）。
   - **不要尝试绕过**（不要反复重试、换源、加代理、改依赖版本、降级/升级等）——先停下来。
   - 输出“证据包”，并向 Operator 申请 **最小人工介入**：包含 `命令`、关键 `报错行`、疑似受限 `host/endpoint`、需要 Operator 做的最小动作（如：确认网络/凭证/白名单/镜像）。
   - 证据中不要泄露密钥/Token/个人信息。

   --- 
## Code Style & Language Rules

- 所有 **源码文件** 必须：
  1. 顶部包含统一中文文件头，例如：

     ```kotlin
     // 文件：<相对路径>
     // 模块：<:moduleName>
     // 说明：<简要中文说明>
     // 作者：创建于 <YYYY-MM-DD> # use real time, do not invent or guess
     ```

## Project Structure & Module Organization
The workspace is defined in `settings.gradle.kts` and intentionally lean. `app/` hosts the Compose shell and remains the single entry point, `tingwuTestApp/` provides Tingwu upload helpers, `data/ai-core/` wraps DashScope/Tingwu/OSS clients, and `core/util` plus `core/test` hold reusable primitives and JVM fixtures. Feature code stays inside `feature/chat`, `feature/media`, and `feature/connectivity`. Assets live in `app/src/main/res/`, docs and plans go under `docs/` and `plans/`, and cached artifacts belong in `third_party/maven-repo/`.

## Build, Test, and Development Commands
Run Gradle from the repo root:
- `./gradlew :app:assembleDebug` – builds the Compose shell and all dependent modules.
- `./gradlew :app:installDebug && adb shell am start com.smartsales.aitest/.AiFeatureTestActivity` – deploys and launches on a device or emulator.
- `./gradlew testDebugUnitTest` – runs JVM tests across modules; narrow the scope with `:feature:connectivity:testDebugUnitTest` while iterating.
- `./gradlew lint` – executes Android/Kotlin static analysis before review.

## Coding Style & Naming Conventions
Write Kotlin with four-space indentation, trailing commas in multi-line parameters, and `val`-first data classes. Keep Compose functions file-private unless reused and match filenames to their primary type. Packages stay under `com.smartsales.<layer>`, resource IDs use lower snake case (`media_server_card_title`), and Gradle modules should preserve the existing `:feature:*` / `:core:*` naming pattern.

## Testing Guidelines
Place unit tests in `<module>/src/test/` with JUnit 4 and `kotlinx-coroutines-test`, reusing helpers from `core/test` for fake dispatchers or result assertions. Instrumented or Compose UI tests belong in `<module>/src/androidTest/`; run them with `./gradlew :app:connectedDebugAndroidTest` on an emulator that has BLE, storage, and microphone permissions granted. Name suites after the behavior under test (`DeviceConnectionManagerTest`, `TingwuRequestMapperTest`).

## Commit & Pull Request Guidelines
Recent history (`Refactor: Clean up AiFeatureTestActivity`, `Update .gitignore…`) uses a `Scope: Imperative summary` style—keep the first verb capitalized, keep bodies wrapped near 72 columns, and add `Test:` trailers for executed commands. PRs must outline affected modules, reproduction steps, test/lint commands, and screenshots or logs for UI, BLE, or Tingwu changes. Link issues or TODO IDs and highlight dependency or credential updates explicitly.

## Security & Configuration Tips
Never commit `local.properties`; it stores DashScope, Tingwu, and OSS keys consumed by `data/ai-core`. `TINGWU_BASE_URL` automatically appends `/openapi/tingwu/v2/` unless you pass a proxy URL—note any overrides in PR descriptions. The repo pins Java 17 in `gradle.properties`; verify `JAVA_HOME` before running Gradle. Prefer the cached `third_party/maven-repo/` when network access is limited and scrub logs so chat transcripts or BLE payloads do not leak into commits.
