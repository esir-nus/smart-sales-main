#!/bin/bash
# 修复 Cursor/VS Code 文件监听问题
# 参考: https://code.visualstudio.com/docs/setup/linux#_visual-studio-code-is-unable-to-watch-for-file-changes-in-this-large-workspace-error-enospc

echo "当前 inotify 限制:"
cat /proc/sys/fs/inotify/max_user_watches

echo ""
echo "检查 /etc/sysctl.conf 是否已配置..."

if grep -q "fs.inotify.max_user_watches" /etc/sysctl.conf; then
    echo "✓ 配置已存在，当前值："
    grep "fs.inotify.max_user_watches" /etc/sysctl.conf
    echo ""
    echo "要应用新值，请运行: sudo sysctl -p"
else
    echo "✗ 配置不存在"
    echo ""
    echo "要增加限制到 524288（VS Code 推荐值），请运行："
    echo "  sudo bash -c 'echo \"fs.inotify.max_user_watches=524288\" >> /etc/sysctl.conf'"
    echo "  sudo sysctl -p"
    echo ""
    echo "或者临时增加（重启后失效）："
    echo "  sudo sysctl fs.inotify.max_user_watches=524288"
fi


