<!-- 文件路径: core/test/README.md -->
<!-- 文件作用: 介绍测试辅助模块的内容 -->
<!-- 文件目的: 规范复用的假数据与测试基建 -->
<!-- 相关文件: docs/current-state.md, core/util/README.md, plans/tdd-plan.md -->

> 注意：在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# Core Test 模块

## 模块职责
- 提供跨模块共享的 Fake、测试调度器、样例数据与断言工具，支撑 `plans/tdd-plan.md` 中的 TDD 目标。
- 不会被打包进 APK，仅通过 `testImplementation(project(":core:test"))` 供单测使用。

## 现有能力
- `FakeDispatcherProvider`：为 `runTest` 注入统一调度器。
- （待补充）Fake Wi-Fi/BLE、Fake OSS、Fake AI 响应、样例 Markdown/音频等。
- 约定：所有 Fake 需有可配置延迟/错误参数，确保状态机可测试。
- 仓库级统一入口：优先使用 `scripts/run-tests.sh` 触发当前自动化测试任务。

## 当前状态（T0）
- 目前只有 `FakeDispatcherProvider`，无法覆盖连接、AI、媒体等复杂流程。
- 已有基础断言测试，机械验证 `FakeDispatcherProvider` 的统一调度与可控推进。
- 更完整的共享 Fake 已迁移到 `:core:test-fakes-domain` 与 `:core:test-fakes-platform`。

## 风险与限制
- Fake 太少导致 Feature 测试不得不创建自定义 stub，进而分散维护成本。
- 若 Fake 行为与真实 SDK 偏差过大，可能掩盖 Bug；需在 README 说明任何假设。

## 下一步动作
- 按模块陆续加入 Fake：`WifiProvisioner`、`AiChatService`、`TingwuCoordinator`、`ExportManager`、`OssRepository`、媒体/音频样例。
- 提供 DSL/Builder 方便配置延迟、失败率；搭配 `flowTestObserver` 等辅助断言。
- 在 Fake 更新后同步修改各模块 README，确保测试步骤清晰。

## 调试与验证
- 运行 `scripts/run-tests.sh infra` 检查测试基础设施相关任务。
- 运行 `scripts/run-tests.sh l2` 执行高价值 L2 模拟测试切片。
- 运行 `scripts/run-tests.sh pipeline` / `scheduler` 执行命名验证切片。
- 运行 `scripts/run-tests.sh all` 执行当前仓库级默认单测入口。
- `all` 是策展后的默认切片，不是所有模式的并集。
- `./gradlew :core:test:test` 仍可单独执行，但当前仅用于确认模块装配是否正常。
- 在 Feature 单测中引用 Fake 时，请更新 `docs/progress-log.md`，说明新增测试覆盖范围。
