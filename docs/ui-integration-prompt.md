当前垂直 Overlay（AiFeatureTestActivity → VerticalOverlayShell）调整顺序与策略

1) 侧边抽屉（左→右，Hamburger）
- 保留：参考 React Layout.js 的 Drawer 行为，点击汉堡弹出侧边栏（设备状态、历史列表、用户链接）。
- 目标：先保证侧边抽屉稳定，其他拖拽暂缓。
- UI/UX：半透明遮罩、抽屉阴影，点击遮罩关闭。

2) 垂直抽屉（上→下 / 下→上）
- 先精简为“单抽屉 + 按钮开关”：
  - 顶部 Audio 抽屉：暂时用按钮触发开合，不依赖拖拽。Home 底部保留另一个按钮导航/打开 Device 页。
  - 底部 Device 抽屉：可以先关闭拖拽，后续按顺序恢复。
- 待 Home/UI 稳定后，再逐步恢复完整拖拽（仅在单抽屉模式验证通过后再加第二个抽屉）。

3) 底部栏/按钮（优先替代方案）
- 在 Home 中提供显式按钮切换到 Audio / Device 管理，不阻塞 Home 渲染与测试。
- 未来可将按钮替换为拖拽手柄（handle），但暂不启用。

测试与集成要点
- 先跑通 Home 渲染与导航测试：保留 testTag（OVERLAY_*、PAGE_*），但用按钮触发开合，确保 Compose 树即时可见。
- 待单抽屉模式稳定后，再开启第二个抽屉的拖拽；最后恢复完整双抽屉拖拽。

参考
- React UI/Components + Pages（Home.jsx、Layout.js）中的 Drawer/Vertical overlay 交互，先实现侧边抽屉，再逐步恢复上下拖拽。

目标
- 保证 Home 默认可见、测试不过时，按顺序解锁 1) 侧边抽屉 → 2) 单垂直抽屉（按钮开合） → 3) 完整双垂直拖拽。
