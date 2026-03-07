import re

with open("docs/tools/generate_map.py", "r", encoding="utf-8") as f:
    text = f.read()

# 1. Fix the layout of the Estimate View by replacing the broken divs
old_estimate = """        <!-- 3. Code Estimate View -->
<div id="estimate" class="tab-content {est_container_active}">
            <div class="container">
                <div class="card full-width">
                    <h2>当前状态快照 (代码清理后)</h2>
                    <div class="stat-grid" style="grid-template-columns: repeat(4, 1fr);">
                        <div class="stat-box"><div class="stat-value glow">{total:,}</div><div class="stat-label">总代码行数</div></div>
                        <div class="stat-box"><div class="stat-value">{core:,}</div><div class="stat-label">核心 Kotlin 代码</div></div>
                        <div class="stat-box"><div class="stat-value">{tests:,}</div><div class="stat-label">测试 Kotlin 代码</div></div>
                        <div class="stat-box"><div class="stat-value">{res:,}</div><div class="stat-label">资源 XML 文件</div></div>
                    </div>
                </div>

                <div class="card">
                    <h2>生产级别投产预测目标</h2>
                    <div class="stat-grid">
                        <div class="stat-box" style="border-color: var(--warning);">
                            <div class="stat-value" style="color: var(--warning);">{conservative:,}</div>
                            <div class="stat-label">保守预测 (最小可行)</div>
                            <div style="font-size: 0.8rem; color: #94a3b8; margin-top: 5px;">约增加 22,000 行</div>
                        </div>
                        <div class="stat-box" style="border-color: var(--success);">
                            <div class="stat-value" style="color: var(--success);">{ideal:,}</div>
                            <div class="stat-label">理想预测 (完整指标)</div>
                            <div style="font-size: 0.8rem; color: #94a3b8; margin-top: 5px;">约增加 38,000 行</div>
                        </div>
                    </div>
                    <p style="color: var(--text-muted); font-size: 0.95rem; margin-top: 20px;">
                        未来的代码增长必须集中在<strong>测试覆盖率</strong>（需增加 1.5万 - 2万行）以及<strong>错误处理和性能监控</strong>（需增加 5千 - 1.3万行），只有稳固这些模块才能将项目成功演进至重度生产阶段 (T2-T3)。
                    </p>
                </div>

                        </div>
                    </div>
                </div>
            </div>

            <!-- Additional Container for Full Width Metrics -->
            <div class="container" style="margin-top: 40px; display: block;">
                <!-- Comprehensive Metrics Row -->
                <div class="card full-width" style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 40px; background: rgba(0,0,0,0.2);">
                    <!-- Key Gaps -->
                    <div>
                        <h2>产研关键差距分析 (T2-T3)</h2>
                        <div style="margin-top: 20px;">
                            <div class="increment-item">
                                <span class="increment-title">测试覆盖率 (单元/集成 L1-L3)</span>
                                <span class="gap-badge gap-success">✅ 已达标</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">错误处理与异常恢复机制</span>
                                <span class="gap-badge gap-warning">⚠️ 需要增强 (高优)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">性能监控与埋点 (Crashlytics)</span>
                                <span class="gap-badge gap-error">❌ 缺失 (高优)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">安全性 (加密, Keystore, 证书)</span>
                                <span class="gap-badge gap-warning">⚠️ 需要提升 (高优)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">真实云端集成验证 (Aliyun/Tingwu)</span>
                                <span class="gap-badge gap-warning">⚠️ 需要验证 (高优)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">文档与注释完整性 (KDoc)</span>
                                <span class="gap-badge gap-warning">⚠️ 需要完善 (中优)</span>
                            </div>
                        </div>
                    </div>
                    <!-- Distribution -->
                    <div>
                        <h2>核心增量分布 (+38k)</h2>
                        <div style="margin-top: 20px;">
                            <div class="increment-item" style="border: none; padding-bottom: 5px;">
                                <span class="increment-title">1. 完整测试覆盖补充</span>
                                <span class="increment-lines">+15,000 ~ 20,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 45%; background: var(--success);"></div></div>

                            <div class="increment-item" style="border: none; padding-bottom: 5px; margin-top: 10px;">
                                <span class="increment-title">2. 缺失核心功能 (连接/同步/设置)</span>
                                <span class="increment-lines">+5,000 ~ 8,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 25%; background: var(--warning);"></div></div>

                            <div class="increment-item" style="border: none; padding-bottom: 5px; margin-top: 10px;">
                                <span class="increment-title">3. UX/UI 用户体验深度优化</span>
                                <span class="increment-lines">+3,000 ~ 5,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 15%; background: var(--glow);"></div></div>

                            <div class="increment-item" style="border: none; padding-bottom: 5px; margin-top: 10px;">
                                <span class="increment-title">4. 错误容错与数据持久化</span>
                                <span class="increment-lines">+4,000 ~ 6,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 20%; background: var(--error);"></div></div>
                        </div>
                    </div>
                </div>
            </div>

                <div class="card">
                    <h2>当前代码组成分布</h2>
                    <div class="chart-container"><canvas id="compositionChart"></canvas></div>
                </div>

                <div class="card full-width">
                    <h2>历史代码量增长趋势</h2>
                    <div class="chart-container" style="height: 400px;"><canvas id="trendChart"></canvas></div>
                </div>
            </div>
        </div>"""

new_estimate = """        <!-- 3. Code Estimate View -->
<div id="estimate" class="tab-content {est_container_active}">
            <div class="container">
                <div class="card full-width">
                    <h2>当前状态快照 (代码清理后)</h2>
                    <div class="stat-grid" style="grid-template-columns: repeat(4, 1fr);">
                        <div class="stat-box"><div class="stat-value glow">{total:,}</div><div class="stat-label">总代码行数</div></div>
                        <div class="stat-box"><div class="stat-value">{core:,}</div><div class="stat-label">核心 Kotlin 代码</div></div>
                        <div class="stat-box"><div class="stat-value">{tests:,}</div><div class="stat-label">测试 Kotlin 代码</div></div>
                        <div class="stat-box"><div class="stat-value">{res:,}</div><div class="stat-label">资源 XML 文件</div></div>
                    </div>
                </div>

                <div class="card full-width" style="margin-top: 40px;">
                    <h2>生产级别投产预测目标</h2>
                    <div class="stat-grid">
                        <div class="stat-box" style="border-color: var(--warning);">
                            <div class="stat-value" style="color: var(--warning);">{conservative:,}</div>
                            <div class="stat-label">保守预测 (最小可行)</div>
                            <div style="font-size: 0.8rem; color: #94a3b8; margin-top: 5px;">约增加 22,000 行</div>
                        </div>
                        <div class="stat-box" style="border-color: var(--success);">
                            <div class="stat-value" style="color: var(--success);">{ideal:,}</div>
                            <div class="stat-label">理想预测 (完整指标)</div>
                            <div style="font-size: 0.8rem; color: #94a3b8; margin-top: 5px;">约增加 38,000 行</div>
                        </div>
                    </div>
                    <p style="color: var(--text-muted); font-size: 0.95rem; margin-top: 20px;">
                        未来的代码增长必须集中在<strong>测试覆盖率</strong>（需增加 1.5万 - 2万行）以及<strong>错误处理和性能监控</strong>（需增加 5千 - 1.3万行），只有稳固这些模块才能将项目成功演进至重度生产阶段 (T2-T3)。
                    </p>
                </div>

                <!-- Comprehensive Metrics Row -->
                <div class="card full-width" style="margin-top: 40px; display: grid; grid-template-columns: repeat(2, 1fr); gap: 40px; background: rgba(0,0,0,0.2);">
                    <!-- Key Gaps -->
                    <div>
                        <h2>产研关键差距分析 (T2-T3)</h2>
                        <div style="margin-top: 20px;">
                            <div class="increment-item">
                                <span class="increment-title">测试覆盖率 (单元/集成 L1-L3)</span>
                                <span class="gap-badge gap-success">✅ 已达标</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">错误处理与异常恢复机制</span>
                                <span class="gap-badge gap-warning">⚠️ 需要增强 (高优)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">性能监控与埋点 (Crashlytics)</span>
                                <span class="gap-badge gap-error">❌ 缺失 (高优)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">安全性 (加密, Keystore, 证书)</span>
                                <span class="gap-badge gap-warning">⚠️ 需要提升 (高优)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">真实云端集成验证 (Aliyun/Tingwu)</span>
                                <span class="gap-badge gap-warning">⚠️ 需要验证 (高优)</span>
                            </div>
                            <div class="increment-item">
                                <span class="increment-title">文档与注释完整性 (KDoc)</span>
                                <span class="gap-badge gap-warning">⚠️ 需要完善 (中优)</span>
                            </div>
                        </div>
                    </div>
                    <!-- Distribution -->
                    <div>
                        <h2>核心增量分布 (+38k)</h2>
                        <div style="margin-top: 20px;">
                            <div class="increment-item" style="border: none; padding-bottom: 5px;">
                                <span class="increment-title">1. 完整测试覆盖补充</span>
                                <span class="increment-lines">+15,000 ~ 20,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 45%; background: var(--success);"></div></div>

                            <div class="increment-item" style="border: none; padding-bottom: 5px; margin-top: 10px;">
                                <span class="increment-title">2. 缺失核心功能 (连接/同步/设置)</span>
                                <span class="increment-lines">+5,000 ~ 8,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 25%; background: var(--warning);"></div></div>

                            <div class="increment-item" style="border: none; padding-bottom: 5px; margin-top: 10px;">
                                <span class="increment-title">3. UX/UI 用户体验深度优化</span>
                                <span class="increment-lines">+3,000 ~ 5,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 15%; background: var(--glow);"></div></div>

                            <div class="increment-item" style="border: none; padding-bottom: 5px; margin-top: 10px;">
                                <span class="increment-title">4. 错误容错与数据持久化</span>
                                <span class="increment-lines">+4,000 ~ 6,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 20%; background: var(--error);"></div></div>
                        </div>
                    </div>
                </div>

                <div class="card full-width" style="margin-top: 40px;">
                    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 40px;">
                        <div>
                            <h2>当前代码组成分布</h2>
                            <div class="chart-container"><canvas id="compositionChart"></canvas></div>
                        </div>
                        <div>
                            <h2>历史代码量增长趋势</h2>
                            <div class="chart-container" style="height: 400px;"><canvas id="trendChart"></canvas></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>"""

if old_estimate in text:
    text = text.replace(old_estimate, new_estimate)
else:
    print("Could not find the old estimate HTML chunk to replace!")

# 2. Fix the Python string slicing part to properly separate chapters
old_python = """        # Strip other sections using regex
        if page_type == 'topology':
            html = re.sub(r'<!-- 2\. Feature Dataflow View -->.*<!-- 3\. Code Estimate View -->', '<!-- 2. Feature Dataflow View -->', html, flags=re.DOTALL)
            html = re.sub(r'<!-- 3\. Code Estimate View -->.*</div>\s*</div>\s*</div>', '<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            html = html.replace('{init_script}', '')
        elif page_type == 'feature':
            html = re.sub(r'<!-- 1\. Topology View -->.*<!-- 2\. Feature Dataflow View -->', '<!-- 1. Topology View -->\\n<!-- 2. Feature Dataflow View -->', html, flags=re.DOTALL)
            html = re.sub(r'<!-- 3\. Code Estimate View -->.*</div>\s*</div>\s*</div>', '<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            js = "window.addEventListener('load', () => { initLines(); setTimeout(() => { lines.forEach(l => { l.show('draw', {duration: 500, timing: 'ease-out'}); l.position(); }); }, 200); });"
            html = html.replace('{init_script}', js)
        elif page_type == 'estimate':
            html = re.sub(r'<!-- 1\. Topology View -->.*<!-- 3\. Code Estimate View -->', '<!-- 1. Topology View -->\\n<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            js = "window.addEventListener('load', () => { initCharts(); });"
            html = html.replace('{init_script}', js)"""

new_python = """        # Safely slice the HTML based on page type to prevent regex failures
        top_start = html.find('<!-- 1. Topology View -->')
        feat_start = html.find('<!-- 2. Feature Dataflow View -->')
        est_start = html.find('<!-- 3. Code Estimate View -->')
        script_start = html.find('        <script>')
        
        header_html = html[:top_start]
        top_html = html[top_start:feat_start]
        feat_html = html[feat_start:est_start]
        est_html = html[est_start:script_start]
        footer_html = html[script_start:]

        if page_type == 'topology':
            html = header_html + top_html + footer_html
            html = html.replace('{init_script}', '')
        elif page_type == 'feature':
            html = header_html + feat_html + footer_html
            js = "window.addEventListener('load', () => { initLines(); setTimeout(() => { lines.forEach(l => { l.show('draw', {duration: 500, timing: 'ease-out'}); l.position(); }); }, 200); });"
            html = html.replace('{init_script}', js)
        elif page_type == 'estimate':
            html = header_html + est_html + footer_html
            js = "window.addEventListener('load', () => { initCharts(); });"
            html = html.replace('{init_script}', js)"""

if old_python in text:
    text = text.replace(old_python, new_python)
else:
    print("Could not find old python regex chunk to replace!")

with open("docs/tools/generate_map.py", "w", encoding="utf-8") as f:
    f.write(text)

print("Applied rigorous string slicing and HTML layout fixes!")
