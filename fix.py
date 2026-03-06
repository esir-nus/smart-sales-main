import re

file_path = '/home/cslh-frank/main_app/docs/cerb/dashboard.html'
with open(file_path, 'r', encoding='utf-8') as f:
    text = f.read()

# Pattern to find the section to replace
pattern = r'(<div id="estimate" class="tab-content">.*?)<script>'

replacement = """<div id="estimate" class="tab-content">
            <div class="container">
                
                <div class="card full-width">
                    <h2>当前状态快照 (代码清理后)</h2>
                    <div class="stat-grid" style="grid-template-columns: repeat(4, 1fr);">
                        <div class="stat-box"><div class="stat-value glow">200,058</div><div class="stat-label">总代码行数</div></div>
                        <div class="stat-box"><div class="stat-value">188,821</div><div class="stat-label">核心 Kotlin 代码</div></div>
                        <div class="stat-box"><div class="stat-value">11,150</div><div class="stat-label">测试 Kotlin 代码</div></div>
                        <div class="stat-box"><div class="stat-value">87</div><div class="stat-label">资源 XML 文件</div></div>
                    </div>
                </div>

                <div class="card">
                    <h2>生产级别投产预测目标</h2>
                    <div class="stat-grid">
                        <div class="stat-box" style="border-color: var(--warning);">
                            <div class="stat-value" style="color: var(--warning);">222,058</div>
                            <div class="stat-label">保守预测 (最小可行)</div>
                            <div style="font-size: 0.8rem; color: #94a3b8; margin-top: 5px;">约增加 22,000 行</div>
                        </div>
                        <div class="stat-box" style="border-color: var(--success);">
                            <div class="stat-value" style="color: var(--success);">238,058</div>
                            <div class="stat-label">理想预测 (完整指标)</div>
                            <div style="font-size: 0.8rem; color: #94a3b8; margin-top: 5px;">约增加 38,000 行</div>
                        </div>
                    </div>
                    <p style="color: var(--text-muted); font-size: 0.95rem; margin-top: 20px;">
                        未来的代码增长必须集中在<strong>架构重构</strong>（+1.5万行）、<strong>系统加固</strong>（+1.5万行）以及<strong>体验打磨</strong>（+8千行）。测试覆盖已初步达标，目前的重点是异常恢复、安全加密与边缘持久化机制。
                    </p>
                </div>

                <div class="card">
                    <h2>当前代码组成分布</h2>
                    <div class="chart-container"><canvas id="compositionChart"></canvas></div>
                </div>

                <div class="card full-width" style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 40px; background: rgba(0,0,0,0.2);">
                    <div>
                        <h2>产研关键差距分析 (T2-T3)</h2>
                        <div style="margin-top: 20px;">
                            <div class="increment-item">
                                <span class="increment-title" title="The old Fake spaghetti is gone. You have a solid 11k line armor of high-leverage tests. Maintain this.">🛡️ 测试覆盖率 (L1-L3)</span>
                                <span class="gap-badge gap-success">✅ 已攻克 (Paid)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title" title="If the LLM fails or BLE drops, the app currently crashes or hangs. We need retry loops and graceful fallbacks.">💥 错误处理与异常恢复</span>
                                <span class="gap-badge gap-warning">⚠️ 开发中 (~3k)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title" title="Running a 200k line app blind is suicide. We need Crashlytics and performance tracing (APM) immediately.">👁️ 性能监控与埋点 (APM)</span>
                                <span class="gap-badge gap-error">❌ 缺失 (~2k)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title" title="API keys in local.properties is a prototype move. We need Android Keystore integration and encrypted DBs.">🔒 安全性与加密</span>
                                <span class="gap-badge gap-warning">⚠️ 开发中 (~1.5k)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title" title="Offline mode and sync conflict resolution are non-negotiable for an enterprise CRM.">⚙️ 边缘持久化 (离线/冲突)</span>
                                <span class="gap-badge gap-warning">⚠️ 开发中 (~5k)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title" title="Users hate waiting. We need skeleton screens, fluid animations, and empty states to mask pipeline latency.">✨ UX 个性化与微交互</span>
                                <span class="gap-badge gap-warning">⚠️ 开发中 (~4k)</span>
                            </div>
                        </div>
                    </div>

                    <div>
                        <h2>The Road to Production (+38k)</h2>
                        <div style="margin-top: 20px;">
                            <div class="increment-item" style="border: none; padding-bottom: 5px;">
                                <span class="increment-title">1. 架构重构 (Refactoring)</span>
                                <span class="increment-lines">+15,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 39%; background: var(--warning);"></div></div>
                            
                            <div class="increment-item" style="border: none; padding-bottom: 5px; margin-top: 10px;">
                                <span class="increment-title">2. 系统加固 (Hardening)</span>
                                <span class="increment-lines">+15,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 39%; background: var(--error);"></div></div>

                            <div class="increment-item" style="border: none; padding-bottom: 5px; margin-top: 10px;">
                                <span class="increment-title">3. 体验打磨 (Polish)</span>
                                <span class="increment-lines">+8,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 22%; background: var(--glow);"></div></div>
                        </div>
                    </div>
                </div>

                <div class="card full-width">
                    <h2>历史代码量增长趋势</h2>
                    <div class="chart-container" style="height: 400px;"><canvas id="trendChart"></canvas></div>
                </div>
            </div>
        </div>

        <script>"""

new_text = re.sub(pattern, replacement, text, flags=re.DOTALL)
if new_text != text:
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_text)
    print("Structure fixed successfully")
else:
    print("Regex replacement failed")

