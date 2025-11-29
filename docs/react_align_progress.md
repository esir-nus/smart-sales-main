React→Compose 对齐进度
=====================

最新进展（2025-11-29）
- Home 叠层壳：保持按钮触发 Audio/Device 抽屉，Home 默认可见，抽屉手柄 testTag 复用在顶栏按钮；后续再评估恢复拖拽。
- 历史入口对齐：Home HISTORY_TOGGLE 现直接导航到 ChatHistory，并确保 PAGE_CHAT_HISTORY / chat_history_page 根 tag 挂在可见节点。
- 聊天可见性：当 chatMessages 非空时隐藏 Session 列表，新增滚动定位到最后一条助手气泡；仅最新助手气泡带 home_assistant_message，保证转写场景可见。
- 转写跳转：转写 CTA 先写 pendingSessionId 再触发 onTranscriptionRequested，防止会话切换丢失；AudioTranscriptToChatTest 暂时忽略，待权限修复后重跑。
- 测试状态：Gradle wrapper lock 权限仍阻塞仪表测试，需先修复权限再重跑完整用例。
- React UI audit：继续保持 Home/Sidebar/AudioFiles/UserCenter 已对齐的交互与文案。
- 下一步：解除 Gradle wrapper 权限阻塞，重跑 AudioTranscriptToChatTest 与全量仪表；补充 Home/DeviceManager 视觉细节与拖拽动画对齐 React。

实机参考
- `real-interface/*.jpg` 包含转写详情、UserCenter、设备管理、Home 欢迎页、音频列表等布局，可用于对齐色板/间距/文案。
