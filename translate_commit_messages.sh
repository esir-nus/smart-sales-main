#!/bin/bash
# 批量将提交信息从英文翻译成中文的脚本

# 创建备份分支（如果还没有）
git branch backup-before-translate-commits 2>/dev/null || echo "备份分支已存在"

# 准备提交信息映射（哈希 -> 中文消息）
declare -A MSG_MAP=(
  ["1a6448c"]="文档：添加全面的新人入职指南"
  ["78528ab"]="测试：添加 Home 滚动按钮和设备管理器状态的 UI 测试"
  ["76d01e6"]="功能(聊天)：在 Home 聊天中添加滚动到最新消息按钮"
  ["72b760e"]="测试：添加快速技能确认和活跃技能芯片的 Compose UI 测试和标签"
  ["f23e109"]="功能(聊天)：在 HomeInputArea 中实现活跃技能芯片；添加 clearSelectedSkill() 和完整 UI 连接"
  ["0968b65"]="功能(聊天)：将快速技能改为模式设置，带有简短助手确认"
  ["32bfab9"]="测试：强化 Home shell 和设备上的快速技能"
  ["0392768"]="测试：覆盖 AiFeatureTestActivity 中的 Home-first 导航 shell"
  ["062c6d4"]="功能：AppShell Home-first 导航 + HomeScreen 的 Hilt 连接"
  ["94fa107"]="功能：DeviceManager ViewModel + 设备文件 UI 和 API 文档"
  ["1c7128f"]="功能：UI 骨架构建，核心功能完成"
  ["4871d71"]="重构：清理 AiFeatureTestActivity 并更新进度日志"
  ["2817cf7"]="配置：更新 .gitignore 以包含 gradle-wrapper.jar"
  ["ad88da3"]="初始提交：Smart Sales Android 应用程序"
)

# 使用 git filter-branch 或者逐个使用 git rebase
# 由于需要修改多个提交，我们使用 rebase -i 的自动化方法

echo "开始修改提交信息为中文..."
echo "注意：这将修改 git 历史，需要 force push"

# 从 HEAD~13 开始 rebase（14 个提交）
# 我们需要创建一个脚本来为每个提交修改消息

cat > /tmp/rebase_editor.sh << 'EDITOR_SCRIPT'
#!/bin/bash
# 自动修改提交信息的编辑器脚本

COMMIT_FILE="$1"
COMMIT_MSG=$(cat "$COMMIT_FILE")

# 将英文提交信息映射到中文
case "$COMMIT_MSG" in
  "docs: add comprehensive onboarding guide for new team members")
    echo "文档：添加全面的新人入职指南" > "$COMMIT_FILE"
    ;;
  "Test: Add UI tests for Home scroll button & DeviceManager states")
    echo "测试：添加 Home 滚动按钮和设备管理器状态的 UI 测试" > "$COMMIT_FILE"
    ;;
  "feat(chat): add scroll-to-latest button to Home chat")
    echo "功能(聊天)：在 Home 聊天中添加滚动到最新消息按钮" > "$COMMIT_FILE"
    ;;
  "test: add Compose UI tests and tags for quick-skill confirmation and active skill chip")
    echo "测试：添加快速技能确认和活跃技能芯片的 Compose UI 测试和标签" > "$COMMIT_FILE"
    ;;
  "feat(chat): implement active skill chip in HomeInputArea; add clearSelectedSkill() and full UI wiring")
    echo "功能(聊天)：在 HomeInputArea 中实现活跃技能芯片；添加 clearSelectedSkill() 和完整 UI 连接" > "$COMMIT_FILE"
    ;;
  "feat(chat): change quick skills to mode-setting with short assistant confirmation;")
    echo "功能(聊天)：将快速技能改为模式设置，带有简短助手确认" > "$COMMIT_FILE"
    ;;
  "test: harden Home shell and quick skills on device")
    echo "测试：强化 Home shell 和设备上的快速技能" > "$COMMIT_FILE"
    ;;
  "test: cover Home-first navigation shell in AiFeatureTestActivity")
    echo "测试：覆盖 AiFeatureTestActivity 中的 Home-first 导航 shell" > "$COMMIT_FILE"
    ;;
  "AppShell: Home-first navigation + Hilt wiring for HomeScreen")
    echo "功能：AppShell Home-first 导航 + HomeScreen 的 Hilt 连接" > "$COMMIT_FILE"
    ;;
  "Feature: DeviceManager ViewModel + device files UI & API docs")
    echo "功能：DeviceManager ViewModel + 设备文件 UI 和 API 文档" > "$COMMIT_FILE"
    ;;
  "ui skeleton building, core funcstion complete,")
    echo "功能：UI 骨架构建，核心功能完成" > "$COMMIT_FILE"
    ;;
  "Refactor: Clean up AiFeatureTestActivity and update progress log")
    echo "重构：清理 AiFeatureTestActivity 并更新进度日志" > "$COMMIT_FILE"
    ;;
  "Update .gitignore to include gradle-wrapper.jar")
    echo "配置：更新 .gitignore 以包含 gradle-wrapper.jar" > "$COMMIT_FILE"
    ;;
  "Initial commit: Smart Sales Android application")
    echo "初始提交：Smart Sales Android 应用程序" > "$COMMIT_FILE"
    ;;
  *)
    # 如果没有匹配，保持原样
    echo "$COMMIT_MSG" > "$COMMIT_FILE"
    ;;
esac
EDITOR_SCRIPT

chmod +x /tmp/rebase_editor.sh

echo "脚本已准备就绪。"
echo "要执行修改，请运行："
echo "  export GIT_SEQUENCE_EDITOR='sed -i \"s/^pick/reword/g\"'"
echo "  git rebase -i HEAD~14"
echo ""
echo "然后为每个提交运行："
echo "  export GIT_EDITOR=/tmp/rebase_editor.sh"
echo "  (rebase 会自动调用)"

