<!-- 文件路径: core/util/README.md -->
<!-- 文件作用: 说明通用工具模块的功能 -->
<!-- 文件目的: 指导团队复用统一的工具与约束 -->
<!-- 相关文件: docs/current-state.md, core/test/README.md, data/ai-core/README.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# Core Util 模块

## 模块职责
- 提供跨模块共享的轻量工具：`Result` 封装、`DispatcherProvider`、日志/文件校验等（目前仅含 Result/Dispatchers）。
- 不包含特定业务逻辑，也不依赖 Android UI；保持纯 Kotlin 以便利测试。
- 为 Fake/真实实现都提供统一依赖，避免重复造轮子。

## 关键接口与依赖
- `Result<T>`：标准化成功/失败返回，未来可拓展 `map/fold` 等辅助函数。
- `DispatcherProvider` + `DefaultDispatcherProvider`：集中管理 IO/Main/Default，方便在测试中注入 `FakeDispatcherProvider`。
- 依赖轻量：Kotlin stdlib + kotlinx-coroutines；后续可加入日志接口、文件工具等。

## 数据与控制流
- Feature/数据模块通过构造器或 Hilt 注入 `DispatcherProvider`，在发布协程时保持一致。
- `Result` 类型贯穿所有网络/SDK 调用，UI 根据 `Result.Error` 展示错误消息。

## 当前状态（T0）
- 仅实现了 Result 与 DispatcherProvider；尚无 Logger/FileHelper/IdGenerator 等常用工具。
- 没有单测，但代码简单；可在 `:core:test` 中添加 Fake 以支撑 TDD。

## 风险与限制
- 工具过少时，Feature 会重复实现各自的 helper，导致风格不统一。
- 若 DispatcherProvider 使用 Main Dispatcher，但在单测中未替换，可能导致 `IllegalStateException`。

## 下一步动作
- 扩展常用工具：日志 facade、时间/ID 生成、文件大小/类型校验、剪贴板适配器等。
- 添加单测或示例，说明如何在测试中注入 `FakeDispatcherProvider`。
- 每次新增工具需在 `docs/progress-log.md` 写明，避免成为“杂物间”。

## 调试与验证
- 运行 `./gradlew :core:util:test`（当前暂无测试，后续补齐）。
- Feature 模块使用时，请在 PR 中说明新增工具的目的并引用本 README。
