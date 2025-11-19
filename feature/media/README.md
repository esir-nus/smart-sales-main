<!-- 文件路径: feature/media/README.md -->
<!-- 文件作用: 描述媒体同步模块的职责与流程 -->
<!-- 文件目的: 指导音频下拉和图像编辑器占位实现 -->
<!-- 相关文件: docs/current-state.md, feature/connectivity/README.md, data/ai-core/README.md, plans/dev_plan.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# Media Feature 模块

## 模块职责
- 负责拉取/展示设备端音频、图像等资产，并提供上传、删除、应用至设备的命令。
- 向 UI 暴露 `MediaSyncCoordinator`（当前实现为 `FakeMediaSyncCoordinator`）及相关状态模型。
- 不直接管理 BLE/Wi-Fi，也不解析 AI 文本；核心职责是同步媒体和触发 OSS / 设备 API。

## 关键接口与依赖
- `MediaSyncCoordinator`：暴露 `state: Flow<MediaSyncState>` 与 `triggerSync()`（后续需扩展 `apply`, `delete`, `upload`）。
- 依赖：`projects.core.util`（Result、Dispatchers）、`projects.data.aiCore`（OssRepository、TingwuCoordinator）以及未来的 `projects.feature.connectivity` 状态。
- UI 入口：`ChatPanel` 下拉面板、独立图像编辑页面（尚未实现）。

## 数据与控制流
1. UI 下拉或点击按钮 → 调用 `triggerSync`。
2. Coordinator 根据连接状态决定是否调用设备 API/OSS，转换结果为 `MediaSyncState` 列表。
3. 用户可对列表项执行 apply/delete/upload → 通过 Repository 与设备/OSS 通信 → 更新状态。
4. 媒体更新后，chat/tingwu 可读取最新音频进行转写。

## 当前状态（T0，含最小单测）
- `FakeMediaSyncCoordinator` 已消费 `DeviceConnectionManager.state`，根据连接状态禁用同步并生成示例 clip 数据。
- 媒体实体、图像编辑器与真实 OSS/硬件 API 仍未实现；`:app` / `:aiFeatureTestApp` 只展示占位列表。
- 提供 `FakeMediaSyncCoordinatorTest` 覆盖同步成功、断开失败、并发请求等路径，为后续接入 OSS 提供回归保障。

## 风险与限制
- 缺失连接依赖导致状态无法反映真实网络情况。
- 大型音频/图片上传需要分片与进度；目前没有实现，易出现 OOM 或超时。
- Android 13+ 媒体权限（READ_MEDIA_AUDIO/IMAGES）未处理。

## 下一步动作
- 依据 wireframe 制作媒体实体（音频条目、图像卡片）及 Compose 面板，连接 `DeviceConnectionManager.state`。
- 接入 `OssRepository` 与真实设备 API，实现上传/删除/应用命令，并添加重试策略。
- 编写单测（借助 `:core:test` Fake）验证同步成功、失败、部分成功等场景，推动模块迈向 T0 → T1。

## 调试与验证
- 单测：`./gradlew :feature:media:test`（覆盖 Fake 协调器逻辑）。
- 当前仅能在 `:aiFeatureTestApp` / `:app` 中查看 Fake 列表；真实设备联调时需在 `docs/progress-log.md` 记录并通过 logging workflow 输出。
