React→Compose 对齐进度
=====================

最新进展（2025-11-29）
- Home 叠层壳迭代：先保留左侧 Drawer（汉堡），暂时关闭上下拖拽，改为按钮触发 Audio/Device 抽屉，Home 默认可见，后续再分步恢复单/双抽屉拖拽。
- 测试调整：历史按钮 testTag 修复（HISTORY_TOGGLE），抽屉手柄 testTag 复用在顶栏按钮；需先修复 Gradle wrapper 权限后重跑仪表/单测。
- Connectivity/Chat JVM 测试稳定性：补齐 Main dispatcher set/reset、时间推进，修正 Fake Profile/Media 模型；仍有个别用例跳过/待重跑（受 wrapper 权限阻塞）。
- 既有 UI 保持：Home 助手复制、欢迎文案；DeviceManager 文案与徽标；DeviceSetup 渐变 CTA；AudioFiles 文案/CTA；UserCenter 菜单入口；转写 → Home 会话链路；baseUrl 自动同步。
- React UI audit：已对齐 Home overlay 行为、Sidebar 分组/置顶/重命名、设备状态卡、DeviceSetup→DeviceManager 跳转、AudioFiles Transcript CTA、UserCenter 菜单。
- 下一步：修复 Gradle wrapper 权限后重跑完整测试；补充 Home/DeviceManager 视觉细节与拖拽动画；按 React 规格继续收敛导航按钮交互。

实机参考
- `real-interface/*.jpg` 包含转写详情、UserCenter、设备管理、Home 欢迎页、音频列表等布局，可用于对齐色板/间距/文案。
