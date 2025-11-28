React→Compose 对齐进度
=====================

最新进展（2025-11-28）
- Shell/navigation aligned：AiFeatureTestActivity 消费 Home navigation requests，设备叠层离线走 DeviceSetup、在线走 DeviceManager，返回均落到 Home。
- Tests refreshed：覆盖无 Chip 导航路径；NavigationSmokeTest 断言 AudioFiles/UserCenter 文案并复位设备状态，避免 flake。
- Home chat：助手气泡支持「复制」，空态改为销售助手欢迎文案；会话长按抽屉可置顶/重命名/删除；首页叠层渐变背景 + 设备/音频卡片 DesignKit 色板，空态文案偏技能导向。
- DeviceManager：文案对齐 React，未连接/空态提示更新；上传按钮“上传新文件”；列表显示文件总数与“当前展示”徽标；预览卡片使用 DesignKit 渐变。
- DeviceSetup：扫描/配对/等待/完成提示调整，完成后 CTA “前往设备管理”，背景加入渐变。
- AudioFiles：标题“录音文件”，描述与空态贴合 React，列表含录音总数；转写抽屉 CTA “用 AI 分析本次通话”，状态标签文案同步。
- UserCenter：顶部说明改为账号/订阅/隐私导语，头像副标题对齐 React 菜单导语，新增“设备管理/订阅管理/隐私与安全/通用设置”快捷入口。
- AudioFiles → Chat 桥接：转写完成事件携带 jobId/markdown 推送到 Home，会话命名“通话分析”，Activity 直接创建会话；补齐 ViewModel/Route 事件与测试。
- DeviceManager baseUrl 自动同步：监听 DeviceHttpEndpointProvider，将 Wi-Fi/BLE 发现的 baseUrl 自动写入并触发刷新，减少手填。
- Lint 清理：HTTP DNS 注解、BLE/Wi-Fi 权限、音频元数据 API、bcprov 版本与版本库归档，`./gradlew lint` 无阻塞错误。
- Instrumentation 烟测：AudioFiles 转写 → “用 AI 分析本次通话” → Home 会话链路；JVM Compose UI 测试暂依赖仪表覆盖。
- React UI audit 完成：Home 叠层、Sidebar 历史分组/重命名/置顶、设备状态卡、DeviceSetup→DeviceManager 自动跳转、AudioFiles/Transcript CTA、UserCenter 菜单项等差异已记录。
- Next up：如有新差异，补充 Home/DeviceManager 视觉细节；监控导航仪表 flake；视情况重开 AudioFiles JVM 测试，保持仪表烟测。

实机参考
- `real-interface/*.jpg` 包含转写详情、UserCenter、设备管理、Home 欢迎页、音频列表等布局，可用于对齐色板/间距/文案。
