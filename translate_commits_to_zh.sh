#!/bin/bash
# 将 alignment 分支的英文提交信息翻译为中文

cd /home/cslh-frank/main_app

echo "警告：这将修改 git 历史，需要强制推送到 GitHub"
echo "按 Enter 继续，或 Ctrl+C 取消..."
read

# 获取 master 分支的最新提交
MASTER_COMMIT=$(git rev-parse master)

# 使用 git filter-branch 批量修改提交信息
git filter-branch -f --msg-filter '
    # 获取当前提交的主题
    subject=$(git log -1 --pretty=format:"%s")
    
    # 如果提交信息已经包含中文字符，保持原样
    if echo "$subject" | grep -qP "[\x{4e00}-\x{9fa5}]"; then
        cat
        exit 0
    fi
    
    # 根据提交主题进行翻译
    case "$subject" in
        "refactor: stabilize AI feature shell navigation")
            cat <<EOF
重构：稳定 AI 功能壳导航

- 在 AiFeatureTestActivity 中添加 PAGE_HOME 和 ROOT 测试标签包装器
  - 为导航验证提供一致的测试目标
- 统一 goHome 函数与 PageSelector 导航逻辑
  - 集中管理首页导航，防止不一致
- 恢复 NavigationSmokeTest 的 overlay 把手测试标签
  - 支持可靠的抽屉交互测试
- 保持 ChatHistory 会话选择流程
  - 保留现有可用的导航路径

这通过提供清晰的测试目标和统一的导航入口点来稳定导航系统，使测试更加可靠。
EOF
            ;;
        "feat: add peekable drawers and mode-only quick skills")
            cat <<EOF
功能：添加可窥视抽屉和仅模式快捷技能

- 为音频/设备 overlay 添加窥视边距和拖拽把手
- 保持测试标签可见，同时允许拖拽关闭/返回关闭
- 将快捷技能优化为模式选择器（智能分析/生成 PDF/CSV）
- 移除聊天气泡，高亮选中的芯片，将模式映射到聊天请求
- 修复 activity 拼写错误和缺失的 RoundedCornerShape 导入

通过改进的 UX 启用可窥视 overlay 抽屉，并将快捷技能转换为模式选择，提升工作流清晰度。
EOF
            ;;
        "refactor: improve home UI state management")
            cat <<EOF
重构：改进首页 UI 状态管理

- 计算 hasActiveChat 以清晰分离 UI 状态
- 仅在不存在聊天消息时显示会话列表/空状态
- 当会话有消息时，用聊天列表填充内容区域
- 保持每个会话自动滚动到最新消息
- 保持可见的助手气泡标签方法
- 保留转写 overlay 路由功能

通过分离空状态和活跃聊天状态来提升 UX 清晰度，同时保持现有的滚动和导航行为。
EOF
            ;;
        "feat: improve home UI and add message copy feature")
            cat <<EOF
功能：改进首页 UI 并添加消息复制功能

- 更新顶栏：显示 SmartSales 助手标题和设备状态芯片
- 将快捷技能本地化为中文，同时保持 React 顺序
- 增强聊天气泡，添加圆角和更好的对齐
- 为助手消息添加复制按钮，集成剪贴板功能

改进与 React 设计的 UI 一致性，并添加便捷的消息复制功能以提升用户体验。
EOF
            ;;
        "refactor: implement overlay shell with drag support")
            cat <<EOF
重构：实现支持拖拽的 overlay 壳

- 引入 OverlayState 以统一音频/首页/设备状态管理
- 添加 VerticalLayerShell，支持动画 overlay 过渡和拖拽手势
- 重构内容区域：首页/音频/设备使用壳层，其他路由不变
- 保持单一数据源：currentPage 映射到 OverlayState
- 保留所有页面和 overlay 的唯一 TestTags

启用流畅的拖拽交互和统一的 overlay 状态管理，同时保持测试兼容性和现有路由行为。
EOF
            ;;
        "test(app): stabilize home overlay shell and navigation tests")
            cat <<EOF
测试（应用）：稳定首页 overlay 壳和导航测试

- 修复 AiFeatureTestActivity 首页/overlay 壳，在测试下保持首页可见
- 规范化 HomeScreenTestTags.ROOT 和 AiFeatureTestTags.PAGE_*，使每个页面唯一
- 连接 overlay 把手和背景（音频/设备）以正确打开/关闭 overlay
- 将 ChatHistory/UserCenter/AudioFiles 路由与 UI 测试使用的 PAGE_* 标签对齐
- 将 ChatHistory 会话点击挂回首页，并选中会话
- 保持所有连接的 UI 测试通过（AudioTranscriptToChatTest 保持跳过）

通过保持首页可见性和所有路由的正确 overlay 导航语义来确保可靠的测试执行。
EOF
            ;;
        "test: stabilize Home overlay shell tags and navigation tests")
            cat <<EOF
测试：稳定首页 overlay 壳标签和导航测试

- 统一首页 ROOT/PAGE 测试标签，使用单实例语义
- 将 AudioFiles/DeviceManager/ChatHistory/UserCenter 路由连接到稳定的页面标签
- 修复 overlay 把手/背景语义，实现可靠的测试导航
- 更新 Compose UI 测试，覆盖首页默认、overlay 导航和路由

确保导航冒烟测试能够可靠地驱动壳并验证所有路由的 overlay 行为。
EOF
            ;;
        "refactor: simplify shell with button-first strategy")
            cat <<EOF
重构：使用按钮优先策略简化壳

- 为 VerticalOverlayShell 添加 enableDrag 参数（默认 false）
  - 保留双抽屉/背景/把手逻辑，但仅在启用时拖拽生效
- 在 AiFeatureTestActivity 顶栏添加显式按钮用于音频/设备抽屉
  - 使用现有的把手 testTag 以便测试
  - 设置 enableDrag=false，使首页始终可见，抽屉通过按钮切换
- 通过在首页顶栏添加 HISTORY_TOGGLE 修复历史按钮 testTag

通过移除拖拽依赖使测试更加可靠，并为抽屉导航提供更清晰的 UI 控件。
EOF
            ;;
        "feat: introduce shared design tokens")
            cat <<EOF
功能：引入共享设计令牌

- 在 core/util 中添加核心 DesignTokens，捕获静音背景、卡片形状/边框/高度、CTA 渐变和 overlay 把手尺寸以供重用
- 将这些令牌连接到壳 overlay 轨道、DeviceManager 背景和 AudioFiles 背景/卡片/CTA 渐变，实现跨屏幕的一致样式
- 更新 DeviceManagerScreenTest 期望以匹配新的 hero 文案，同时保持空状态标签稳定
EOF
            ;;
        "style: refine overlay rail to match reference")
            cat <<EOF
样式：优化 overlay 轨道以匹配参考

- 缩小轨道宽度，略微降低不透明度，修剪内边距，并在 overlay 卡片内添加更粗的把手条和宽松间距
- 移除色调/阴影高度，保持轨道平坦并与 React 参考设计对齐
- 保留所有测试标签和行为不变，同时改进视觉一致性
EOF
            ;;
        "style: refine AudioFiles card visuals")
            cat <<EOF
样式：优化 AudioFiles 卡片视觉效果

- 更新 AudioFiles 列表项，使用白色高架卡片，带微妙边框、更大间距和状态着色状态图标（主要/第三/错误），背景柔和，空状态卡片匹配相同的圆角/边框样式
- 保持 DeviceSetup 和 ChatHistory 的最新样式调整，同时让 UserCenter 和 DeviceManager 与当前 React 参考对齐
EOF
            ;;
        "feat: polish Home hero and top bar")
            cat <<EOF
功能：优化首页 hero 和顶栏

- 居中首页顶栏，两侧为历史/个人资料图标，添加轻量内边距，并将背景切换为柔和的表面色调，更好地匹配 React 手机视觉壳
- 刷新首页 hero 空状态，使用更大的蓝色徽标、粗体问候、主要颜色副标题和更轻的间距，同时保持快捷技能开始行
- 更新 React 到 Compose 对齐计划，记录设计令牌优先、每屏幕迁移步骤，以便未来的 UI 工作遵循相同方法
EOF
            ;;
        "feat: align Compose UI with React spec")
            cat <<EOF
功能：对齐 Compose UI 与 React 规范

- 刷新首页、AudioFiles、DeviceManager 和 UserCenter 屏幕以匹配真实的 React/生产布局：hero/徽标/问候/药丸、同步/未同步/转写中/错误音频状态（带转写 CTA）、深色预览卡片 + 上传瓦片网格，以及浅色背景上的图标引导卡片网格
- 更新 React 对齐文档以引用真实截图，并将进度跟踪移至 docs/react_align_progress.md 以获得更清晰的状态
- 放宽设备 HTTP 端点测试，并为 JVM 保护 Android Log/端点调用，使连接性和音频路由在单元环境中保持可测试
EOF
            ;;
        "feat: bridge Tingwu transcripts to chat")
            cat <<EOF
功能：桥接 Tingwu 转写到聊天

- 在 AudioFilesViewModel 中添加 AudioFilesEvent.TranscriptReady 共享流，在成功的 Tingwu 作业完成时发出，包含 jobId、预览和完整 markdown
- 在壳中共享 HomeScreenViewModel/AudioFilesViewModel，收集转写事件，并将其输入 HomeScreenViewModel.onTranscriptionRequested，使手动和自动"用 AI 分析本次通话"使用真实的 jobId
- 扩展 AudioFilesViewModelTest 以验证转写就绪事件发出，并保持聊天历史集成覆盖
EOF
            ;;
        "feat: auto-sync DeviceManager base url")
            cat <<EOF
功能：自动同步 DeviceManager 基础 URL

- 在 DeviceManagerViewModel 中观察 DeviceHttpEndpointProvider.deviceBaseUrl，自动更新 baseUrl（除非手动覆盖），并在连接时触发文件加载，带有清晰的"自动同步设备地址"状态提示
- 保持 overlay 轨道视觉效果优化，更窄的宽度、更强的渐变、更高的表单高度/色调和内边距内容，以保持与 React 壳对齐
EOF
            ;;
        *)
            # 对于其他提交（可能已经是中文或不需要翻译），保持原样
            cat
            ;;
    esac
' ${MASTER_COMMIT}..HEAD

echo ""
echo "提交信息翻译完成！"
echo ""
echo "现在需要强制推送到 GitHub："
echo "  git push origin alignment --force"
echo ""
echo "注意：强制推送会覆盖远程历史，请确保没有其他人在使用这个分支！"

