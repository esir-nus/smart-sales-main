<!-- 文件路径: core/test/README.md -->
<!-- 文件作用: 介绍测试辅助模块的内容 -->
<!-- 文件目的: 规范复用的假数据与测试基建 -->
<!-- 相关文件: docs/current-state.md, core/util/README.md, plans/tdd-plan.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# Core Test 模块

## 模块职责
- 提供跨模块共享的 Fake、测试调度器、样例数据与断言工具，支撑 `plans/tdd-plan.md` 中的 TDD 目标。
- 不会被打包进 APK，仅通过 `testImplementation(project(":core:test"))` 供单测使用。

## 现有能力
- `FakeDispatcherProvider`：为 `runTest` 注入统一调度器。
- （待补充）Fake Wi-Fi/BLE、Fake OSS、Fake AI 响应、样例 Markdown/音频等。
- 约定：所有 Fake 需有可配置延迟/错误参数，确保状态机可测试。

## 当前状态（T0）
- 目前只有 `FakeDispatcherProvider`，无法覆盖连接、AI、媒体等复杂流程。
- 没有测试，只提供基础工具。

## 风险与限制
- Fake 太少导致 Feature 测试不得不创建自定义 stub，进而分散维护成本。
- 若 Fake 行为与真实 SDK 偏差过大，可能掩盖 Bug；需在 README 说明任何假设。

## 下一步动作
- 按模块陆续加入 Fake：`WifiProvisioner`、`AiChatService`、`TingwuCoordinator`、`ExportManager`、`OssRepository`、媒体/音频样例。
- 提供 DSL/Builder 方便配置延迟、失败率；搭配 `flowTestObserver` 等辅助断言。
- 在 Fake 更新后同步修改各模块 README，确保测试步骤清晰。

## 调试与验证
- 运行 `./gradlew :core:test:test`（补充样例后启用）。
- 在 Feature 单测中引用 Fake 时，请更新 `docs/progress-log.md`，说明新增测试覆盖范围。
