#!/bin/bash
# 逐个修改最近的提交信息为中文

# 提交信息映射函数
translate_msg() {
    local msg="$1"
    case "$msg" in
        "docs: add comprehensive onboarding guide for new team members")
            echo "文档：添加全面的新人入职指南"
            ;;
        "Test: Add UI tests for Home scroll button & DeviceManager states")
            echo "测试：添加 Home 滚动按钮和设备管理器状态的 UI 测试"
            ;;
        "feat(chat): add scroll-to-latest button to Home chat")
            echo "功能(聊天)：在 Home 聊天中添加滚动到最新消息按钮"
            ;;
        "test: add Compose UI tests and tags for quick-skill confirmation and active skill chip")
            echo "测试：添加快速技能确认和活跃技能芯片的 Compose UI 测试和标签"
            ;;
        "feat(chat): implement active skill chip in HomeInputArea; add clearSelectedSkill() and full UI wiring")
            echo "功能(聊天)：在 HomeInputArea 中实现活跃技能芯片；添加 clearSelectedSkill() 和完整 UI 连接"
            ;;
        "feat(chat): change quick skills to mode-setting with short assistant confirmation;")
            echo "功能(聊天)：将快速技能改为模式设置，带有简短助手确认"
            ;;
        "test: harden Home shell and quick skills on device")
            echo "测试：强化 Home shell 和设备上的快速技能"
            ;;
        "test: cover Home-first navigation shell in AiFeatureTestActivity")
            echo "测试：覆盖 AiFeatureTestActivity 中的 Home-first 导航 shell"
            ;;
        "AppShell: Home-first navigation + Hilt wiring for HomeScreen")
            echo "功能：AppShell Home-first 导航 + HomeScreen 的 Hilt 连接"
            ;;
        "Feature: DeviceManager ViewModel + device files UI & API docs")
            echo "功能：DeviceManager ViewModel + 设备文件 UI 和 API 文档"
            ;;
        "ui skeleton building, core funcstion complete,")
            echo "功能：UI 骨架构建，核心功能完成"
            ;;
        "Refactor: Clean up AiFeatureTestActivity and update progress log")
            echo "重构：清理 AiFeatureTestActivity 并更新进度日志"
            ;;
        "Update .gitignore to include gradle-wrapper.jar")
            echo "配置：更新 .gitignore 以包含 gradle-wrapper.jar"
            ;;
        "Initial commit: Smart Sales Android application")
            echo "初始提交：Smart Sales Android 应用程序"
            ;;
        *)
            echo "$msg"
            ;;
    esac
}

echo "脚本已创建。要执行修改，请运行："
echo "  ./translate_recent_commits.sh"
echo ""
echo "注意：修改已推送的历史需要 git push --force，请谨慎操作！"
