import os
import sys
import re
import json

# 面向利益相关者的通俗解释 (中文)
MODULE_EXPLANATIONS = {
    # Layer 1
    "ConnectivityBridge": "管理与实体蓝牙工牌的连接状态。",
    "NotificationService": "在手机上显示系统弹窗和任务提醒。",
    "OSS": "负责将录音文件上传到云端存储。",
    "ASR": "将语音转换为文字 (备用)。",
    "TingwuPipeline": "阿里云听悟接口，负责语音转文字和提取会议摘要。",
    "PipelineTelemetry": "记录系统内部运行日志，用于排查错误。",
    
    # Layer 2
    "EntityWriter": "负责在数据库中新建或更新联系人、公司等实体信息。",
    "EntityRegistry": "负责从数据库中查询和读取实体信息。",
    "MemoryCenter": "存储所有的聊天记录和语音转写历史。",
    "UserHabit": "记录用户的使用习惯，让AI更懂你。",
    "SessionHistory": "管理侧边栏的历史会话列表 (重命名、置顶)。",
    "SessionContext": "当前聊天的临时高速缓存，AI思考时的数据源。",
    
    # Layer 3
    "ContextBuilder": "负责把历史记录和人物关系打包，送给AI大脑。",
    "InputParser": "快速分析用户的意图，例如分辨是在闲聊还是下达命令。",
    "EntityDisambiguator": "当遇到重名或指代不清时，负责向用户发起追问。",
    "LightningRouter": "交通警察：快速判断问题难易度，简单问题直接放行，复杂的转交深度思考。",
    "EntityResolver": "将录音里提到的人名，精准匹配到数据库里的唯一ID。",
    "ModelRegistry": "管理不同的AI大模型配置 (例如：小模型快速响应 vs 大模型深度思考)。",
    "Executor": "真正负责向云端AI (如通义千问) 发请求并接收结果的桥梁。",
    "PluginRegistry": "负责执行具体的动作，比如生成PDF文件或发短信。",
    "PrismOrchestrator": "系统大脑(系统二)：指挥整个深度思考流程，决定先做什么后做什么。",
    "UnifiedPipeline": "处理流水线：将复杂请求按步骤 (提取-组装-路由) 流水化处理。",
    
    # Layer 4
    "Mascot (System I)": "吉祥物(系统一)：负责打招呼、秒回聊天和显示气泡提示，反应极快。",
    "Scheduler": "负责创建和管理待办事项及日程提醒。",
    "ScheduleBoard": "检查日程表中是否有时间冲突。",
    "BadgeAudioPipeline": "控制实体工牌开始/停止录音的流程。",
    "AudioManagement": "处理录音文件同步、转写的界面状态。",
    "ConflictResolver": "帮助用户解决时间冲突 (比如：改期或取消)。",
    "DevicePairing": "负责配对新蓝牙工牌的界面流程。",
    "AgentIntelligenceUI": "处理复杂任务时，展示给用户的中间思考状态与等待骨架屏。",
    
    # Layer 5
    "ClientProfileHub": "综合所有历史信息，为客户建立完整的画像和性格分析。",
    "RLModule": "强化学习模块：从过去的交互中学习，优化未来给AI的提示词。"
}
import json

MAP_PATH = "docs/cerb/interface-map.md"

def get_sunburst_data():
    valid_exts = {'.kt', '.java', '.xml'}
    ignore_dirs = {'build', '.git', '.gradle', 'docs', 'tmp', 'res'}
    skip_structural = {'src', 'main', 'java', 'com', 'smartsales', 'prism'}
    
    def crawl(path, depth=1, max_depth=4):
        try:
            entries = os.listdir(path)
        except Exception:
            return []
            
        children = []
        files_loc = 0
        
        for entry in entries:
            full_path = os.path.join(path, entry)
            if os.path.isdir(full_path):
                if entry in ignore_dirs:
                    continue
                
                # If it's a structural dir, we don't increase depth, and we just return its children directly 
                if entry in skip_structural or entry == os.path.basename(path).replace('-', '_'):
                    sub_children = crawl(full_path, depth, max_depth)
                    children.extend(sub_children)
                    continue
                
                if depth >= max_depth:
                    loc = sum(1 for root, _, files in os.walk(full_path) for f in files if os.path.splitext(f)[1] in valid_exts for _ in open(os.path.join(root, f), 'r', encoding='utf-8', errors='ignore'))
                    if loc > 0:
                        children.append({"name": entry, "value": loc})
                else:
                    sub_children = crawl(full_path, depth + 1, max_depth)
                    if sub_children:
                        children.append({"name": entry, "children": sub_children})
                        
            elif os.path.splitext(entry)[1] in valid_exts:
                try:
                    with open(full_path, 'r', encoding='utf-8', errors='ignore') as f:
                        files_loc += sum(1 for _ in f)
                except Exception:
                    pass
        
        if files_loc > 0:
            children.append({"name": "(根目录文件)", "value": files_loc})
                
        optimized = []
        for c in children:
            if "children" in c and len(c["children"]) == 1 and c["children"][0]["name"] == "(根目录文件)":
                optimized.append({"name": c["name"], "value": c["children"][0]["value"]})
            else:
                optimized.append(c)
                
        return optimized

    top_level = ['app', 'app-core', 'app-prism', 'core', 'data', 'domain']
    data = []
    for mod in top_level:
        if os.path.exists(mod):
            child_nodes = crawl(mod, depth=1, max_depth=4)
            if len(child_nodes) == 1 and child_nodes[0].get("name") == "(根目录文件)":
                data.append({"name": mod, "value": child_nodes[0]["value"]})
            elif child_nodes:
                data.append({"name": mod, "children": child_nodes})
                
    return data
OUTPUT_PATH = "docs/cerb/dashboard.html" # Obsolete

def parse_markdown_table(lines):
    rows = []
    headers = []
    for line in lines:
        if not line.strip() or not line.startswith("|"):
            continue
        col_data = [c.strip() for c in line.split("|")[1:-1]]
        if "---" in col_data[0]:
            continue
        if not headers:
            headers = col_data
        else:
            rows.append(dict(zip(headers, col_data)))
    return rows


def parse_estimate():
    metrics = {
        "total": 0, "core": 0, "tests": 0, "res": 0,
        "conservative": 0, "ideal": 0,
        "pie_labels": "['核心 Kotlin 代码', '测试 Kotlin 代码', '资源 XML 文件']",
        "pie_data": "[0, 0, 0]"
    }
    try:
        with open("PRODUCTION_CODE_ESTIMATE.md", "r", encoding="utf-8") as f:
            text = f.read()
            m_total = re.search(r"Current XP\*\*:\s*`([\d,]+)`", text)
            if m_total: metrics["total"] = int(m_total.group(1).replace(",", ""))
            
            # Match both the old 'Core' and new 'Business Logic & Core Layers'
            m_breakdown = re.search(r"\*\((?:Core|Business Logic & Core Layers):\s*`([\d,]+)`\s*\|\s*Tests:\s*`([\d,]+)`\s*\|\s*Resources:\s*`([\d,]+)`\)\*", text)
            if m_breakdown:
                metrics["core"] = int(m_breakdown.group(1).replace(",", ""))
                metrics["tests"] = int(m_breakdown.group(2).replace(",", ""))
                metrics["res"] = int(m_breakdown.group(3).replace(",", ""))
                
            m_target = re.search(r"Target XP\*\*:\s*`~([\d,]+)`", text)
            if m_target:
                ideal = int(m_target.group(1).replace(",", ""))
                metrics["ideal"] = ideal
                metrics["conservative"] = metrics["total"] + 22000
            
            # Support dynamic pie charts if defined via mermaid pie
            mermaid_pie_match = re.search(r"pie title.*?$.*?(?:^    .*?$.*?)+", text, re.MULTILINE | re.DOTALL)
            if mermaid_pie_match:
                pie_block = mermaid_pie_match.group(0)
                labels = []
                data = []
                # Use regex to safely extract "Label" : Value even if Label contains colons
                for match in re.finditer(r'"(.*?)"\s*:\s*(\d+)', pie_block):
                    label = match.group(1).strip()
                    val = match.group(2).strip()
                    labels.append(f"'{label}'")
                    data.append(val)
                    
                if labels and data:
                    metrics["pie_labels"] = "[" + ", ".join(labels) + "]"
                    metrics["pie_data"] = "[" + ", ".join(data) + "]"

            metrics["trajectory_rows"] = []
            metrics["senior_take"] = ""
            
            # Find the table rows after "## 📉 Codebase Trajectory"
            traj_section = re.search(r"## 📉 Codebase Trajectory(.*?)(?=\n---|##)", text, re.MULTILINE | re.DOTALL)
            if traj_section:
                section_text = traj_section.group(1)
                for line in section_text.split('\n'):
                    if line.startswith('|') and '---' not in line and 'Module Layer' not in line and '代码模块分层' not in line:
                        cols = [c.strip().replace('`', '') for c in line.split('|')[1:-1]]
                        if len(cols) == 5:
                            metrics["trajectory_rows"].append({
                                "module": cols[0], "in": cols[1], "out": cols[2], "net": cols[3], "traj": cols[4].replace("**", "")
                            })
                
                senior_match = re.search(r"> \*\*资深架构师评估\*\*: \"(.*?)\"", section_text, re.DOTALL)
                if senior_match:
                    metrics["senior_take"] = senior_match.group(1).strip()

            metrics["doc_stats"] = []
            metrics["senior_doc_take"] = ""
            doc_section = re.search(r"## 📚 Documentation & Knowledge Base(.*?)(?=\n---|##)", text, re.MULTILINE | re.DOTALL)
            if doc_section:
                section_text = doc_section.group(1)
                for line in section_text.split('\n'):
                    if line.startswith('|') and '---' not in line and 'Knowledge Domain' not in line and '知识领域' not in line:
                        cols = [c.strip().replace('`', '') for c in line.split('|')[1:-1]]
                        if len(cols) == 4:
                            metrics["doc_stats"].append({
                                "domain": cols[0].replace("**", ""), "count": cols[1], "metric": cols[2], "status": cols[3].replace("**", "")
                            })
                            
                senior_doc_match = re.search(r"> \*\*资深架构师评估\*\*: \"(.*?)\"", section_text, re.DOTALL)
                if senior_doc_match:
                    metrics["senior_doc_take"] = senior_doc_match.group(1).strip()

    except Exception as e:
        print("Error parsing estimate:", e)
    return metrics

def parse_reports():
    import re
    reports = []
    reports_dir = "docs/reports"
    if not os.path.exists(reports_dir):
        return reports
        
    for f in os.listdir(reports_dir):
        if not f.endswith(".md") or f == "index.md":
            continue
            
        repo_path = os.path.join(reports_dir, f)
        try:
            with open(repo_path, "r", encoding="utf-8") as file:
                text = file.read()
                
                title_match = re.search(r"^#\s+(.*)", text, re.MULTILINE)
                date_match = re.search(r"\*\*Date\*\*:\s*(.*)", text)
                protocol_match = re.search(r"\*\*Protocol on Trial\*\*:\s*(.*)", text)
                target_match = re.search(r"\*\*Target Implementation\*\*:\s*(.*)", text)
                
                summary_match = re.search(r"##\s+(?:2\.\s+Executive Summary|1\.\s+Contextual Anchor.*)\n+(.*?)(?=\n\n|\n##|$)", text, re.DOTALL | re.IGNORECASE)
                summary_text = summary_match.group(1).replace("\n", " ").strip() if summary_match else ""
                
                reports.append({
                    "filename": f,
                    "title": title_match.group(1).strip() if title_match else f,
                    "date": date_match.group(1).strip() if date_match else "Unknown",
                    "protocol": protocol_match.group(1).strip() if protocol_match else "Unknown",
                    "target": target_match.group(1).strip() if target_match else "Unknown",
                    "summary": summary_text
                })
        except Exception as e:
            print(f"Error parsing report {f}: {e}")
            
    reports.sort(key=lambda x: x.get("date", ""), reverse=True)
    return reports

def parse_interface_map():
    with open(MAP_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    layers = []
    current_layer = None
    table_lines = []
    in_table = False

    for line in content.split("\n"):
        if line.startswith("## Layer"):
            if current_layer and table_lines:
                current_layer["modules"] = parse_markdown_table(table_lines)
                table_lines = []
            
            # Use regex to strip the number and clean up the layer name
            match = re.search(r"## Layer \d+:\s*(.+)", line)
            layer_name = match.group(1) if match else line.replace("##", "").strip()
            
            current_layer = {
                "name": layer_name,
                "modules": []
            }
            layers.append(current_layer)
        
        elif line.startswith("## Data Flow") or line.startswith("## Ownership") or line.startswith("## Anti-Patterns") or line.startswith("## Delivery"):
            # Stop parsing specific layers when we hit the footer sections
            if current_layer and table_lines:
                current_layer["modules"] = parse_markdown_table(table_lines)
                table_lines = []
            
            # Reset current_layer unconditionally on footer sections
            current_layer = None
        
        elif current_layer:
            if line.startswith("|"):
                in_table = True
                table_lines.append(line)
            elif in_table and not line.strip():
                in_table = False
                current_layer["modules"] = parse_markdown_table(table_lines)
                table_lines = []

    # Handle the very last layer if EOF arrived without a newline
    if current_layer and table_lines:
        if not current_layer["modules"]:
            current_layer["modules"] = parse_markdown_table(table_lines)
        elif len(table_lines) > 2:
            current_layer["modules"].extend(parse_markdown_table(table_lines))

    # Reverse the layers so Layer 1 is at the top of the list (since it's at the end of the file)
    # The flex-direction: column-reverse handles the visual stacking.
    return layers

def generate_html_single(layers, page_type, metrics, reports=None):
    if reports is None: reports = []
    css = """
    :root {
        --bg: #0f172a;
        --surface: rgba(30, 41, 59, 0.8);
        --surface-hover: rgba(51, 65, 85, 0.9);
        --border: #334155;
        --text: #f8fafc;
        --text-muted: #94a3b8;
        --glow: #c084fc;
        --glow: #c084fc;
        --success: #22c55e;
        --warning: #f59e0b;
        --error: #ef4444;
        
        --status-shipped: rgba(34, 197, 94, 0.2);
        --status-shipped-border: #22c55e;
        --status-planned: rgba(148, 163, 184, 0.1);
        --status-planned-border: #64748b;
        --status-interface: rgba(234, 179, 8, 0.2);
        --status-interface-border: #eab308;
        --status-wip: rgba(249, 115, 22, 0.2);
        --status-wip-border: #f97316;
        --status-blocked: rgba(239, 68, 68, 0.2);
        --status-blocked-border: #ef4444;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }
    
    body {
        font-family: 'Inter', system-ui, -apple-system, sans-serif;
        background: radial-gradient(circle at top, #1e293b 0%, #020617 100%);
        color: var(--text);
        line-height: 1.6;
        padding: 40px;
    }
    body::before {
        content: '';
        position: fixed;
        top: -200px;
        left: -200px;
        width: 600px;
        height: 600px;
        background: radial-gradient(circle, rgba(167, 139, 250, 0.15) 0%, transparent 60%);
        pointer-events: none;
        z-index: 1;
    }

    .header {
        margin-bottom: 60px;
        text-align: center;
        position: relative;
        z-index: 10;
    }
    
    .header h1 {
        font-size: 2.5rem;
        background: linear-gradient(135deg, #60a5fa, #c084fc);
        -webkit-background-clip: text;
        background-clip: text;
        -webkit-text-fill-color: transparent;
        margin-bottom: 10px;
    }
    
    .header p { color: var(--text-muted); }

    .layer-container {
        display: flex;
        flex-direction: column-reverse; /* Bottom to top reading like a stack */
        gap: 60px;
        max-width: 1200px;
        margin: 0 auto;
        position: relative;
    }

    .layer {
        position: relative;
        background: transparent;
        border: none;
        padding: 0;
        border-radius: 0;
        box-shadow: none;
    }

    .layer-title {
        font-size: 1.25rem;
        font-weight: 600;
        margin-bottom: 30px;
        color: var(--accent);
        display: inline-block;
        background: var(--bg);
        padding: 5px 15px;
        border: 1px solid var(--accent);
        border-radius: 20px;
        box-shadow: 0 0 10px rgba(59, 130, 246, 0.2);
        position: relative;
        z-index: 10;
    }

    .modules-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 40px 30px;
        position: relative;
        z-index: 10; /* Ensure grid is above the lines */
    }

    .module-card {
        border-radius: 16px;
        padding: 20px;
        background: rgba(30, 41, 59, 0.5);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        border: 1px solid rgba(255, 255, 255, 0.08);
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.4), inset 0 1px 1px rgba(255, 255, 255, 0.05);
        transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
        position: relative;
        overflow: visible;
        z-index: 15; /* Ensure cards are above everything else in grid */
    }

    .module-card:hover {
        transform: translateY(-4px);
        border-color: rgba(255, 255, 255, 0.2);
        box-shadow: 0 14px 20px -5px rgba(0,0,0,0.5), inset 0 1px 1px rgba(255, 255, 255, 0.1);
        z-index: 20;
    }

    /* Connection anchoring points */
    .anchor-top, .anchor-bottom {
        position: absolute;
        width: 8px;
        height: 8px;
        background: var(--accent);
        border-radius: 50%;
        left: 50%;
        transform: translateX(-50%);
        opacity: 0; /* Hidden by default, useful for debugging */
    }
    
    .anchor-top { top: -4px; }
    .anchor-bottom { bottom: -4px; background: var(--glow); }

    .module-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 15px;
        position: relative;
    }

    .module-name {
        font-weight: 700;
        font-size: 1.15rem;
    }
    
    .module-name a {
        color: var(--text);
        text-decoration: none;
    }
    
    .module-name a:hover {
        color: var(--accent);
        text-decoration: underline;
    }

    .module-status {
        font-size: 0.85rem;
        padding: 4px 10px;
        border-radius: 12px;
        font-weight: 600;
        border: 1px solid;
    }

    /* Status Styles */
    .status-shipped { background: var(--status-shipped); border-color: var(--status-shipped-border); color: var(--status-shipped-border); }
    .status-planned { background: var(--status-planned); border-color: var(--status-planned-border); color: var(--status-planned-border); border-style: dashed; }
    .status-interface { background: var(--status-interface); border-color: var(--status-interface-border); color: var(--status-interface-border); }
    .status-wip { background: var(--status-wip); border-color: var(--status-wip-border); color: var(--status-wip-border); }
    .status-blocked { background: var(--status-blocked); border-color: var(--status-blocked-border); color: var(--status-blocked-border); }

    .module-detail {
        font-size: 0.85rem;
        color: var(--text-muted);
        margin-bottom: 8px;
        display: flex;
        flex-direction: column;
        gap: 6px;
        padding-bottom: 8px;
        border-bottom: 1px dotted rgba(255,255,255,0.1);
    }
    
    .module-explanation {
        font-size: 0.85rem;
        color: #e2e8f0;
        margin-bottom: 15px;
        line-height: 1.5;
        background: rgba(0,0,0,0.3);
        padding: 10px 12px;
        border-radius: 6px;
        border-left: 3px solid var(--accent);
    }

    .module-detail span.label {
        font-weight: 600;
        color: var(--text);
        opacity: 0.9;
    }

    .card-signature {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px dashed var(--border);
        font-family: 'Fira Code', monospace;
        font-size: 0.8rem;
        color: #a78bfa;
        word-break: break-all;
    }
    
    .os-layer {
        position: absolute;
        bottom: 0;
        right: 0;
        font-size: 0.7rem;
        background: rgba(0,0,0,0.4);
        padding: 6px 10px;
        border-top-left-radius: 12px;
        border-bottom-right-radius: 12px;
        color: var(--text-muted);
    }
    
    .legend {
        display: flex;
        justify-content: center;
        gap: 20px;
        margin-bottom: 40px;
        flex-wrap: wrap;
        background: var(--surface);
        padding: 15px;
        border-radius: 12px;
        border: 1px solid var(--border);
    }
    
    .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 0.95rem;
        font-weight: 500;
    }
    
    .translation-hint {
        font-size: 0.75rem;
        color: var(--text-muted);
        margin-left: 4px;
        font-weight: normal;
    }

    .tabs {
        display: inline-flex;
        justify-content: center;
        gap: 5px;
        margin-bottom: 40px;
        position: relative;
        z-index: 20;
        background: rgba(0,0,0,0.2);
        padding: 6px;
        border-radius: 12px;
        left: 50%;
        transform: translateX(-50%);
    }
    
    .tab-btn {
        background: transparent;
        border: none;
        color: #94a3b8;
        padding: 10px 24px;
        border-radius: 8px;
        font-size: 1.05rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .tab-btn:hover {
        background: rgba(255,255,255,0.05);
        color: var(--text);
    }
    
    .tab-btn.active {
        background: #3b82f6;
        color: white;
        box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.5);
        border-color: transparent;
    }
    
    .tab-content {
        display: none;
        animation: fadeIn 0.3s ease-in-out;
    }
    
    .tab-content.active {
        display: block;
    }
    
    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }
    
    /* Code Estimate CSS */
    .container { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: repeat(2, 1fr); gap: 40px; }
    .card { 
        background: rgba(30, 41, 59, 0.5); 
        backdrop-filter: blur(12px); 
        border: 1px solid rgba(255, 255, 255, 0.08); 
        border-radius: 16px; 
        padding: 30px; 
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.4), inset 0 1px 1px rgba(255, 255, 255, 0.05); 
        transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease; 
        position: relative;
        z-index: 2;
    }
    .card:hover { 
        transform: translateY(-4px); 
        border-color: rgba(255, 255, 255, 0.2); 
        box-shadow: 0 14px 20px -5px rgba(0,0,0,0.5), inset 0 1px 1px rgba(255, 255, 255, 0.1); 
    }
    .card h2 { font-size: 1.5rem; color: var(--accent); margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 10px; }
    .stat-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 30px; }
    .stat-box { background: rgba(0,0,0,0.4); padding: 20px; border-radius: 12px; text-align: center; border: 1px solid var(--border); }
    .stat-value { font-size: 2.2rem; font-weight: 700; color: var(--text); font-family: 'Fira Code', monospace; margin-bottom: 5px; letter-spacing: -1px; text-shadow: 0 0 15px rgba(192, 132, 252, 0.3); }
    .stat-value.glow { color: var(--glow); text-shadow: 0 0 10px rgba(192, 132, 252, 0.5); }
    .stat-label { font-size: 0.9rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; }
    .full-width { grid-column: 1 / -1; }
    .chart-container { position: relative; height: 300px; width: 100%; margin-top: 20px; }
    
    .gap-badge { display: inline-block; padding: 4px 10px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; margin-left: 10px; }
    .gap-success { background: rgba(34, 197, 94, 0.15); color: var(--success); border: 1px solid var(--success); }
    .gap-warning { background: rgba(245, 158, 11, 0.15); color: var(--warning); border: 1px solid var(--warning); }
    .gap-error { background: rgba(239, 68, 68, 0.15); color: var(--error); border: 1px solid var(--error); }
    
    .reports-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
        gap: 30px;
        margin-top: 20px;
    }
    .report-card {
        background: rgba(30, 41, 59, 0.5);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 16px;
        padding: 24px;
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.4), inset 0 1px 1px rgba(255, 255, 255, 0.05);
        transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
        display: flex;
        flex-direction: column;
        z-index: 2;
    }
    .report-card:hover {
        transform: translateY(-4px);
        border-color: rgba(255, 255, 255, 0.2);
        box-shadow: 0 14px 20px -5px rgba(0,0,0,0.5), inset 0 1px 1px rgba(255, 255, 255, 0.1);
    }
    .report-date { font-family: 'Fira Code', monospace; color: var(--accent); font-size: 0.85rem; margin-bottom: 8px; }
    .report-title { font-size: 1.25rem; font-weight: 600; color: var(--text); margin-bottom: 12px; }
    .report-body { flex: 1; color: var(--text-muted); font-size: 0.9rem; margin-bottom: 15px; display: -webkit-box; -webkit-line-clamp: 4; line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
    .report-meta { font-size: 0.8rem; color: #64748b; background: rgba(0,0,0,0.3); padding: 10px; border-radius: 8px; margin-bottom: 15px; line-height: 1.4; }
    .report-link { margin-top: auto; display: inline-block; padding: 8px 16px; background: rgba(59, 130, 246, 0.15); color: #60a5fa; border-radius: 6px; text-decoration: none; font-size: 0.9rem; font-weight: 500; text-align: center; border: 1px solid rgba(59, 130, 246, 0.3); transition: all 0.2s; }
    .report-link:hover { background: rgba(59, 130, 246, 0.3); color: #fff; border-color: #60a5fa; }

    .increment-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--border); }
    .increment-item:last-child { border-bottom: none; }
    .increment-title { font-weight: 500; color: var(--text); }
    .increment-lines { font-family: 'Fira Code', monospace; color: var(--accent); }
    .increment-progress { height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; margin-top: 8px; overflow: hidden; }
    .increment-bar { height: 100%; border-radius: 3px; }

    .card table { width: 100%; text-align: left; border-collapse: collapse; margin-bottom: 20px; font-size: 0.95rem; }
    .card th { text-transform: uppercase; font-size: 11px; font-weight: 600; color: #64748b; letter-spacing: 0.1em; padding: 16px 12px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
    .card td { padding: 12px 14px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
    .card tr:hover td { background: rgba(255, 255, 255, 0.02); }
    
    .diagnosis { border-left: 4px solid; border-image: linear-gradient(to bottom, #f59e0b, #d97706) 1; background: rgba(245, 158, 11, 0.03); padding: 20px; color: #e2e8f0; }
    .diagnosis-doc { border-left: 4px solid; border-image: linear-gradient(to bottom, #c084fc, #9333ea) 1; background: rgba(192, 132, 252, 0.03); padding: 20px; color: #e2e8f0; }

    /* Topology specific CSS */
    .topology-sequence {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 20px;
        padding: 40px 0;
    }
    .layer-block {
        background: rgba(30, 41, 59, 0.5);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 16px;
        padding: 24px 32px;
        width: 100%;
        max-width: 800px;
        text-align: center;
        position: relative;
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.4), inset 0 1px 1px rgba(255, 255, 255, 0.05);
        transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
        z-index: 2;
    }
    .layer-block:hover {
        transform: translateY(-4px);
        border-color: rgba(255, 255, 255, 0.2);
        box-shadow: 0 14px 20px -5px rgba(0,0,0,0.5), inset 0 1px 1px rgba(255, 255, 255, 0.1);
    }
    .layer-block h2 {
        color: var(--accent);
        font-size: 1.5rem;
        margin-bottom: 12px;
        text-shadow: 0 0 10px rgba(167, 139, 250, 0.3);
    }
    .layer-desc {
        color: var(--text-muted);
        font-size: 0.95rem;
        margin-bottom: 20px;
        line-height: 1.5;
    }
    .layer-modules-list {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        justify-content: center;
    }
    .mini-module {
        background: rgba(30, 41, 59, 0.8);
        border: 1px solid rgba(255, 255, 255, 0.1);
        padding: 8px 18px;
        border-radius: 20px;
        font-size: 0.9rem;
        font-weight: 500;
        color: #e2e8f0;
        font-family: 'Fira Code', monospace;
        box-shadow: 0 4px 6px -1px rgba(0,0,0,0.3);
        transition: background 0.2s;
    }
    .mini-module:hover {
        background: rgba(255, 255, 255, 0.1);
    }
    .seq-arrow {
        color: var(--accent);
        font-size: 2rem;
        line-height: 1;
        opacity: 0.6;
        animation: pulse 2s infinite;
    }
    @keyframes pulse {
        0% { opacity: 0.3; transform: translateY(0); }
        50% { opacity: 0.8; transform: translateY(-5px); }
        100% { opacity: 0.3; transform: translateY(0); }
    }

    .two-col-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 40px; }
    .stat-grid-4 { grid-template-columns: repeat(4, 1fr); }

    @media (max-width: 992px) {
        .modules-grid { grid-template-columns: repeat(2, 1fr); }
        .container { grid-template-columns: 1fr; }
        .two-col-grid { grid-template-columns: 1fr; }
        .stat-grid-4 { grid-template-columns: repeat(2, 1fr); }
    }
    
    @media (max-width: 768px) {
        body { padding: 10px; }
        .header h1 { font-size: 2rem; }
        .modules-grid { grid-template-columns: 1fr; }
        .stat-grid { grid-template-columns: 1fr; }
        .stat-grid-4 { grid-template-columns: 1fr; }
        .tabs { flex-direction: column; }
        .layer-container { gap: 30px; padding-left: 10px; }
        .chart-container { height: 300px !important; }
        #compositionList { max-height: 400px; }
    }
    """

    def clean_markdown(text):
        # Remove bold
        text = text.replace("**", "")
        # Extract link text if it's a markdown link [Text](url)
        link_match = re.search(r"\[(.*?)\]\((.*?)\)", text)
        if link_match:
            return f'<a href="{link_match.group(2)}">{link_match.group(1)}</a>'
        return text

    def get_status_class_and_label(status_str):
        if "✅" in status_str: return "status-shipped", "已上线"
        if "🔲" in status_str: return "status-planned", "计划中"
        if "📐" in status_str: return "status-interface", "接口定义"
        if "🚧" in status_str: return "status-wip", "开发中"
        if "⏸️" in status_str: return "status-blocked", "已阻塞"
        return "status-planned", "未知"

    html_content = f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Cerb Interface Map - 2D Dashboard</title>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Fira+Code&display=swap" rel="stylesheet">
        <!-- SVG drawing library for architecture maps -->
        <script src="https://cdn.jsdelivr.net/npm/leader-line-new@1.1.9/leader-line.min.js"></script>
        <style>{css}</style>
    </head>
    <body>
        
        <div class="header">
            <h1>Cerb 服务端架构大盘 <br><span style="font-size: 1.5rem; color: #a78bfa;">(分布式架构与项目规模面板)</span></h1>
            <p>实时模块状态、领域层读写数据流契约与代码库物理演进大盘</p>
        </div>
        
        <div class="legend">
            <div class="legend-item"><span class="module-status status-shipped">已上线</span> <span class="translation-hint">(真实且完整的业务实现)</span></div>
            <div class="legend-item"><span class="module-status status-interface">接口定义</span> <span class="translation-hint">(仅接口契约声明或内存模拟器)</span></div>
            <div class="legend-item"><span class="module-status status-planned">计划中</span> <span class="translation-hint">(系统设计中，暂未投入研发)</span></div>
            <div class="legend-item"><span class="module-status status-wip">开发中</span> <span class="translation-hint">(特性代码施工中)</span></div>
            <div class="legend-item"><span class="module-status status-blocked">已阻塞</span> <span class="translation-hint">(被其他前置任务挂起)</span></div>
        </div>

        <div class="tabs">
            <a href="dashboard-topology.html" class="tab-btn {{top_active}}" style="text-decoration: none; display: inline-block;">拓扑层级</a>
            <a href="dashboard-dataflow.html" class="tab-btn {{feat_active}}" style="text-decoration: none; display: inline-block;">跨模块数据流</a>
            <a href="dashboard-estimate.html" class="tab-btn {{est_active}}" style="text-decoration: none; display: inline-block;">代码量估算</a>
            <a href="dashboard-reports.html" class="tab-btn {{rep_active}}" style="text-decoration: none; display: inline-block;">报告中心</a>
        </div>

        <!-- 1. Topology View -->
        <div id="topology" class="tab-content {{top_container_active}}">
            <div class="topology-sequence">

    """
    
    layer_descriptions = {
        "Layer 5": "全端聚合与学习层 (跨场景聚合汇总下层多源数据，承载深度学习模型训练数据流)",
        "Layer 4": "特性层与交互界面 (面向用户的顶层业务入口，接收底座引擎流水线处理结果)",
        "Layer 3": "系统核心中枢与分发编排引擎 (负责执行 AI 运算推理或跨服务业务流，属于全局大脑，严格限制直接读取本地设备)",
        "Layer 2": "核心领域防腐层及存储仓 (独占式接管业务领域底层数据交互、数据库操作，彻底打断非正常跨组件越权与耦合)",
        "Layer 1": "外部设施网络及端能力基建 (仅提供基础能力或无状态连接外部 API 的叶子节点服务，杜绝内联任何系统业务相关逻辑)"
    }

    # Sequence diagram: Render layers top to bottom (Layer 5 -> Layer 1)
    for i, layer in enumerate(reversed(layers)):
        if not layer["modules"]: continue
        
        layer_num = len(layers) - i
        layer_name = f"Layer {layer_num}: {layer['name']}"
        desc = layer_descriptions.get(f"Layer {layer_num}", "")
        
        module_names = []
        for mod in layer["modules"]:
            raw_module = mod.get("Module", "Unknown")
            name_text = clean_markdown(raw_module).replace('<a href', '<a style="color: inherit; text-decoration: none;" href')
            module_names.append(f'<div class="mini-module">{name_text}</div>')
        
        modules_html = "\n                ".join(module_names)
        
        if i > 0:
            html_content += """
            <div class="seq-arrow">↑</div>
            """
            
        html_content += f"""
        <div class="layer-block">
            <h2>{layer_name}</h2>
            <div class="layer-desc">{desc}</div>
            <div class="layer-modules-list">
                {modules_html}
            </div>
        </div>
        """

    html_content += """
            </div>
        </div>

        <!-- 2. Feature Dataflow View -->
        <div id="feature" class="tab-content {feat_container_active}">
            <div class="layer-container" style="flex-direction: column; position: relative; padding-left: 40px; gap: 40px;">
    """

    # Group by track
    from collections import defaultdict
    tracks = {}
    
    # We want a deterministic order for tracks, ideally matching PRD
    track_order = [
        "Hardware & Audio",
        "Entity Resolution",
        "System II & Routing",
        "Intelligent Scheduler",
        "System I & Ambient",
        "Memory & OS"
    ]
    
    for layer in layers:
        for mod in layer["modules"]:
            track = mod.get("Track", "Unknown Track").replace("`", "")
            if track == "Unknown Track" or not track:
                track = "Uncategorized"
                
            if track not in tracks:
                tracks[track] = []
            tracks[track].append(mod)

    # Sort the dictionary based on track_order, appending unknown tracks at the end
    sorted_tracks = {k: tracks[k] for k in track_order if k in tracks}
    for k in tracks.keys():
        if k not in sorted_tracks:
            sorted_tracks[k] = tracks[k]

    # Helper function to render a card
    def render_card(mod, prefix=""):
        status_class, status_label = get_status_class_and_label(mod.get("Status", ""))
        raw_module = mod.get("Module", "Unknown")
        name_html = clean_markdown(raw_module)
        clean_name_key = re.sub(r"\[(.*?)\]\(.*?\)", r"\1", raw_module).replace("**", "").strip()
        explanation = MODULE_EXPLANATIONS.get(clean_name_key, "后端组件")
        dom_id = clean_name_key.lower().replace(" ", "-").replace("(", "").replace(")", "").strip()
        signature = mod.get("Key Interface", "").replace("`", "")
        
        raw_reads_from = mod.get("Reads From", "").replace("`", "")
        raw_reads_direct = mod.get("Reads From (directly)", "").replace("`", "") if "Reads From (directly)" in mod else ""
        
        all_deps = raw_reads_from + "," + raw_reads_direct
        deps_list = [prefix + d.strip().lower().replace(" ", "-").replace("(", "").replace(")", "") for d in all_deps.split(",") if d.strip() and d.strip().lower() != "eventbus" and d.strip().lower() != "none" and d.strip().lower() != "app"]
        deps_json_attr = json.dumps(deps_list)

        reads_from = raw_reads_from
        if "Receives From (via Orchestrator)" in mod:
            reads_from = f"直接依赖读取: {raw_reads_direct} <br> 经由核心层编排解耦: {mod.get('Receives From (via Orchestrator)', '')}".replace("`", "")
        else:
            reads_from = clean_markdown(reads_from)
            
        owns = mod.get("Owns (Writes)", "")
        owns_html = clean_markdown(owns)
        os_layer = mod.get("OS Layer", "")

        if not owns: owns_html = "无"
        if not reads_from: reads_from = "无"

        return f"""
            <div class="module-card" id="{prefix}{dom_id}" data-reads-from='{deps_json_attr}'>
                <div class="anchor-top" id="anchor-top-{prefix}{dom_id}"></div>
                <div class="anchor-bottom" id="anchor-bottom-{prefix}{dom_id}"></div>
                <div class="module-header">
                    <div class="module-name">{name_html}</div>
                    <div class="module-status {status_class}">{status_label}</div>
                </div>
                
                <div class="module-explanation">
                    {explanation}
                </div>
                
                <div class="module-detail">
                    <div><span class="label">拥有 (写入控制权):</span> {owns_html}</div>
                    <div><span class="label">读取 (依赖源头):</span> {reads_from}</div>
                </div>
                
                <div class="card-signature">{signature}</div>
                <div class="os-layer">{os_layer}</div>
            </div>
        """

    track_translation_map = {
        "Hardware & Audio": "终端硬件与核心音频流通道",
        "Entity Resolution": "人物公司实体解析与关联引擎",
        "System II & Routing": "系统二大脑深度推理与神经路由",
        "Intelligent Scheduler": "多端智能时间排期与日程引擎",
        "System I & Ambient": "系统一直觉响应与环境级轻交互基石",
        "Memory & OS": "全局长时记忆中枢与应用级系统底座",
        "Uncategorized": "未分类系统组件"
    }
    
    for track_name, track_modules in sorted_tracks.items():
        display_track_name = track_translation_map.get(track_name, track_name)
        html_content += f"""
        <div class="layer" style="margin-bottom: 20px;">
            <div class="layer-title" style="border-color: var(--glow); color: var(--glow); box-shadow: 0 0 10px rgba(192, 132, 252, 0.2);">{display_track_name}</div>
            <div class="modules-grid">
        """

        for mod in track_modules:
            html_content += render_card(mod, prefix="feat_")

        html_content += """
            </div>
        </div>
        """

    html_content += """

        </div>
        
        <!-- 3. Code Estimate View -->
<div id="estimate" class="tab-content {est_container_active}">
            <div class="container">
                <div class="card full-width">
                    <h2>当前状态快照 (代码清理后)</h2>
                    <div class="stat-grid stat-grid-4">
                        <div class="stat-box"><div class="stat-value glow">{total:,}</div><div class="stat-label">总代码行数</div></div>
                        <div class="stat-box"><div class="stat-value">{core:,}</div><div class="stat-label">核心 Kotlin 代码</div></div>
                        <div class="stat-box"><div class="stat-value">{tests:,}</div><div class="stat-label">测试 Kotlin 代码</div></div>
                        <div class="stat-box"><div class="stat-value">{res:,}</div><div class="stat-label">资源 XML 文件</div></div>
                    </div>
                </div>

                <div class="card full-width" style="margin-top: 40px;">
                    <h2>生产级别投产预测目标</h2>
                    <div class="two-col-grid" style="align-items: center; margin-top: 20px;">
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
                        <p style="color: var(--text-muted); font-size: 0.95rem; margin-top: 20px; line-height: 1.6;">
                            未来的代码增长必须集中在<strong>测试覆盖率</strong>（需增加 1.5万 - 2万行）以及<strong>错误处理和性能监控</strong>（需增加 5千 - 1.3万行），只有稳固这些模块才能将项目成功演进至重度生产阶段 (T2-T3)。
                        </p>
                    </div>
                </div>

                <div class="card full-width" style="margin-top: 40px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 10px; margin-bottom: 20px; flex-wrap: wrap; gap: 15px;">
                        <h2 style="border-bottom: none; margin-bottom: 0; padding-bottom: 0;">当前代码物理文件组成分布 (实时扫描)</h2>
                        <button onclick="if(window.compositionChartInstance && typeof window.originalSunburstDataString !== 'undefined') { window.compositionChartInstance.setOption({series: {data: JSON.parse(window.originalSunburstDataString)}}); }" style="background: rgba(59, 130, 246, 0.15); border: 1px solid var(--accent); color: var(--accent); padding: 6px 16px; border-radius: 6px; cursor: pointer; transition: all 0.2s; font-size: 0.9rem; font-weight: 500; display: flex; align-items: center; gap: 6px;" onmouseover="this.style.background='rgba(59, 130, 246, 0.3)'" onmouseout="this.style.background='rgba(59, 130, 246, 0.15)'">
                            <span style="font-size: 1.1rem;">↺</span> 重置视图 (Reset Zoom)
                        </button>
                    </div>
                    <div class="two-col-grid" style="align-items: start; margin-top: 20px;">
                        <div class="chart-container" style="height: 500px; padding: 10px;"><div id="compositionChart" style="width: 100%; height: 100%;"></div></div>
                        <div id="compositionList" style="max-height: 500px; overflow-y: auto; padding-right: 15px;">
                            <!-- JS will populate this list -->
                        </div>
                    </div>
                </div>

                <!-- Comprehensive Metrics Row -->
                <div class="card full-width two-col-grid" style="margin-top: 40px; background: rgba(0,0,0,0.2);">
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

                    <!-- Incremental Needs -->
                    <div>
                        <h2>核心增量分布 (+38k)</h2>
                        <div style="margin-top: 20px;">
                            <div class="increment-item" style="border: none; padding-bottom: 5px;">
                                <span class="increment-title">1. 完整测试覆盖补充</span>
                                <span class="increment-lines">+15,000 ~ 20,000 行</span>
                            </div>
                            <div class="increment-progress"><div class="increment-bar" style="width: 50%; background: var(--success);"></div></div>
                            
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
                    <h2>代码演进轨迹与增长速率 (最近 30 天)</h2>
                    <div style="overflow-x: auto; margin-top: 20px;">
                        <table>
                            <thead>
                                <tr>
                                    <th>模块分层</th>
                                    <th>新增代码 (Insertions)</th>
                                    <th>删除代码 (Deletions)</th>
                                    <th>净增长趋势 (Net Change)</th>
                                    <th>演进结论 (Trajectory)</th>
                                </tr>
                            </thead>
                            <tbody>
                                {trajectory_rows_html}
                            </tbody>
                        </table>
                    </div>
                    {senior_take_html}
                </div>

                <div class="card full-width" style="margin-top: 40px;">
                    <h2>📚 知识库与架构沉淀 (文档级资产)</h2>
                    <div style="overflow-x: auto; margin-top: 20px;">
                        <table>
                            <thead>
                                <tr>
                                    <th>知识领域 (Knowledge Domain)</th>
                                    <th>文稿数量 (File Count)</th>
                                    <th>度量指标 (Metric)</th>
                                    <th>健康度 (Status)</th>
                                </tr>
                            </thead>
                            <tbody>
                                {doc_stats_html}
                            </tbody>
                        </table>
                    </div>
                    {senior_doc_take_html}
                </div>

                <div class="card full-width" style="margin-top: 40px;">
                    <h2>历史代码量增长趋势</h2>
                    <div class="chart-container" style="height: 400px;"><canvas id="trendChart"></canvas></div>
                </div>
            </div>
        </div>

        <!-- 4. Report Center View -->
        <div id="reports" class="tab-content {rep_container_active}">
            <div class="container" style="display: block;">
                <h2 style="color: var(--glow); margin-bottom: 10px;">系统评估与试验报告</h2>
                <p style="color: var(--text-muted); margin-bottom: 30px;">架构复盘、协议试验结果和高级研判的报告墓地</p>
                <div class="reports-grid">
                    {reports_html}
                </div>
            </div>
        </div>

        <script>
            let lines = [];
            let linesInitialized = false;

            function initLines() {
                if (linesInitialized) return;
                linesInitialized = true;

                const cards = document.querySelectorAll('#feature .module-card');
                
                cards.forEach(card => {
                    const sourceId = card.id;
                    const depsStr = card.getAttribute('data-reads-from');
                    if (!depsStr || depsStr === '{}') return;
                    
                    try {
                        const deps = JSON.parse(depsStr);
                        deps.forEach(targetId => {
                            const targetEl = document.getElementById(targetId);
                            
                            if (targetEl && targetEl !== card) {
                                const line = new LeaderLine(
                                    targetEl,
                                    card,
                                    {
                                        color: 'rgba(59, 130, 246, 0.35)',
                                        startSocket: 'bottom',
                                        endSocket: 'top',
                                        path: 'grid',
                                        startSocketGravity: [0, 50],
                                        endSocketGravity: [0, -50],
                                        startPlug: 'square',
                                        endPlug: 'arrow3',
                                        size: 2,
                                        dash: {
                                            animation: true,
                                            len: 6,
                                            gap: 6
                                        },
                                        dropShadow: {
                                            dx: 0,
                                            dy: 0,
                                            blur: 4,
                                            color: 'rgba(192, 132, 252, 0.4)'
                                        },
                                        hide: true
                                    }
                                );
                                lines.push(line);
                            }
                        });
                    } catch (e) {
                        console.error('Failed to parse deps for', sourceId, e);
                    }
                });
                
                setTimeout(() => {
                    document.querySelectorAll('.leader-line').forEach(svg => {
                        svg.style.zIndex = '-1';
                    });
                }, 100);

                if (window.AnimEvent) {
                    window.addEventListener('scroll', AnimEvent.add(function() {
                        lines.forEach(l => l.position());
                    }), false);
                }
            }

            
            
            let chartsInitialized = false;
            let sunburstData = {sunburst_data};
            window.originalSunburstDataString = JSON.stringify(sunburstData);
            function initCharts() {
                if (chartsInitialized) return;
                chartsInitialized = true;
                
                const ctxPie = document.getElementById('compositionChart');
                if (ctxPie && typeof echarts !== 'undefined') {
                    let myChart = echarts.init(ctxPie);
                    window.compositionChartInstance = myChart;
                    window.compositionChartOption = {
                        series: {
                            type: 'sunburst',
                            data: sunburstData,
                            radius: [0, '95%'],
                            sort: null,
                            emphasis: { focus: 'ancestor' },
                            itemStyle: { borderRadius: 4, borderWidth: 1, borderColor: '#1e293b' },
                            label: { show: false }
                        },
                        tooltip: {
                            formatter: function (info) {
                                var value = info.value;
                                var treePathInfo = info.treePathInfo;
                                var treePath = [];
                                for (var i = 1; i < treePathInfo.length; i++) {
                                    treePath.push(treePathInfo[i].name);
                                }
                                return '<div style="font-family: Inter;"><strong>' + treePath.join('/') + '</strong><br>物理代码行数 (LOC): ' + value.toLocaleString() + '</div>';
                            }
                        }
                    };
                    myChart.setOption(window.compositionChartOption);
                    
                    const listContainer = document.getElementById('compositionList');
                    if (listContainer) {
                        function calculateValue(node) {
                            if (node.value !== undefined) return node.value;
                            if (node.children) {
                                let sum = 0;
                                node.children.forEach(c => sum += calculateValue(c));
                                node.value = sum;
                                return sum;
                            }
                            return 0;
                        }
                        sunburstData.forEach(calculateValue);
                        
                        let html = '<table>';
                        html += '<thead><tr><th>物理模块 (Directory)</th><th style="text-align: right;">代码行数 (LOC)</th></tr></thead><tbody>';
                        
                        let sortedData = [...sunburstData].sort((a, b) => b.value - a.value);
                        
                        sortedData.forEach(item => {
                            html += '<tr>';
                            html += '<td style="font-weight: 500; display: flex; align-items: center; gap: 10px;">';
                            html += '<div style="width: 10px; height: 10px; border-radius: 50%; background: var(--glow);"></div>';
                            html += item.name + '</td>';
                            html += '<td style="text-align: right; color: var(--glow); font-family: monospace; font-weight: bold;">' + item.value.toLocaleString() + '</td>';
                            html += '</tr>';
                            
                            if (item.children) {
                                let sortedChildren = [...item.children].sort((a, b) => b.value - a.value);
                                sortedChildren.forEach(child => {
                                    html += '<tr>';
                                    html += '<td style="padding-left: 30px; color: var(--text-muted); font-size: 0.85rem; font-family: monospace;">↳ ' + child.name + '</td>';
                                    html += '<td style="text-align: right; color: var(--text-muted); font-family: monospace; font-size: 0.85rem;">' + child.value.toLocaleString() + '</td>';
                                    html += '</tr>';
                                    
                                    if (child.children) {
                                        let nestedChildren = [...child.children].sort((a, b) => b.value - a.value);
                                        nestedChildren.forEach(nested => {
                                            html += '<tr>';
                                            html += '<td style="padding-left: 50px; color: rgba(255,255,255,0.3); font-size: 0.8rem; font-family: monospace;">- ' + nested.name + '</td>';
                                            html += '<td style="text-align: right; color: rgba(255,255,255,0.3); font-family: monospace; font-size: 0.8rem;">' + nested.value.toLocaleString() + '</td>';
                                            html += '</tr>';
                                        });
                                    }
                                });
                            }
                        });
                        html += '</tbody></table>';
                        listContainer.innerHTML = html;
                    }
                }

                const ctxTrend = document.getElementById('trendChart').getContext('2d');
                new Chart(ctxTrend, {
                    type: 'bar',
                    data: {
                        labels: ['之前 (旧架构)', '清理前 (2026年3月)', '当前状态快照', '生产：保守目标', '生产：理想目标'],
                        datasets: [
                            { label: '核心代码量', data: [22232, 118579, {core}, {conservative_core}, {ideal_core}], backgroundColor: 'rgba(59, 130, 246, 0.6)', borderColor: 'rgba(59, 130, 246, 1)', borderWidth: 1 },
                            { label: '项目总行数', data: [72658, 146976, {total}, {conservative}, {ideal}], backgroundColor: 'rgba(192, 132, 252, 0.6)', borderColor: 'rgba(192, 132, 252, 1)', borderWidth: 1 }
                        ]
                    },
                    options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true, grid: { color: 'rgba(255, 255, 255, 0.1)' }, ticks: { color: '#94a3b8' }, title: { display: true, text: '代码行数', color: '#f8fafc' } }, x: { grid: { display: false }, ticks: { color: '#94a3b8' } } }, plugins: { legend: { labels: { color: '#f8fafc', font: { family: 'Inter' } } } } }
                });
            }

            {init_script}
        </script>
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js"></script>
    </body>
    </html>

    """
    
    rep_cards = ""
    for rep in reports:
        protocol_html = clean_markdown(rep.get('protocol', 'Unknown'))
        target_html = clean_markdown(rep.get('target', 'Unknown'))
        rep_cards += f'''
        <div class="report-card">
            <div class="report-date">{rep.get('date', '')}</div>
            <div class="report-title">{rep.get('title', '')}</div>
            <div class="report-body">{rep.get('summary', '')}</div>
            <div class="report-meta">
                <div><strong>Protocol:</strong> {protocol_html}</div>
                <div style="margin-top: 4px;"><strong>Target:</strong> {target_html}</div>
            </div>
            <a href="../reports/{rep.get('filename', '')}" target="_blank" class="report-link">查看全文 ↗</a>
        </div>
        '''
    html_content = html_content.replace("{reports_html}", rep_cards)
    
    return html_content

def generate_html(layers):
    metrics = parse_estimate()
    reports = parse_reports()
    
    # We will generate the 4 files
    for page_type in ["topology", "feature", "estimate", "reports"]:
        html = generate_html_single(layers, page_type, metrics, reports)
        
        # Setup tags
        html = html.replace('{top_active}', 'active' if page_type == 'topology' else '')
        html = html.replace('{feat_active}', 'active' if page_type == 'feature' else '')
        html = html.replace('{est_active}', 'active' if page_type == 'estimate' else '')
        html = html.replace('{rep_active}', 'active' if page_type == 'reports' else '')
        
        html = html.replace('{top_container_active}', 'active' if page_type == 'topology' else '')
        html = html.replace('{feat_container_active}', 'active' if page_type == 'feature' else '')
        html = html.replace('{est_container_active}', 'active' if page_type == 'estimate' else '')
        html = html.replace('{rep_container_active}', 'active' if page_type == 'reports' else '')
        
        html = html.replace('{total:,}', f"{metrics['total']:,}")
        html = html.replace('{core:,}', f"{metrics['core']:,}")
        html = html.replace('{tests:,}', f"{metrics['tests']:,}")
        html = html.replace('{res:,}', f"{metrics['res']:,}")
        html = html.replace('{conservative:,}', f"{metrics['conservative']:,}")
        html = html.replace('{ideal:,}', f"{metrics['ideal']:,}")
        
        html = html.replace('{total}', str(metrics['total']))
        html = html.replace('{core}', str(metrics['core']))
        html = html.replace('{tests}', str(metrics['tests']))
        html = html.replace('{res}', str(metrics['res']))
        html = html.replace('{conservative}', str(metrics['conservative']))
        html = html.replace('{ideal}', str(metrics['ideal']))
        
        html = html.replace('{pie_labels}', str(metrics.get('pie_labels', '[]')))
        html = html.replace('{pie_data}', str(metrics.get('pie_data', '[]')))
        
        traj_html = ""
        for row in metrics.get("trajectory_rows", []):
            traj_html += f'''
            <tr>
                <td style="font-weight: 500;">{row["module"]}</td>
                <td style="color: var(--success); font-family: 'Fira Code', monospace;">{row["in"]}</td>
                <td style="color: var(--error); font-family: 'Fira Code', monospace;">{row["out"]}</td>
                <td style="color: var(--glow); font-family: 'Fira Code', monospace; font-weight: bold;">{row["net"]}</td>
                <td style="font-weight: 600;">{row["traj"]}</td>
            </tr>
            '''
        html = html.replace('{trajectory_rows_html}', traj_html)
        
        take = metrics.get("senior_take", "")
        take_html = f'<div class="diagnosis" style="margin-top: 20px;"><strong style="color: var(--warning); font-size: 1.1rem; display: block; margin-bottom: 8px;">资深架构师评估:</strong> <span style="font-size: 0.95rem; line-height: 1.6; display: inline-block;">"{take}"</span></div>' if take else ""
        html = html.replace('{senior_take_html}', take_html)
        
        doc_html = ""
        for row in metrics.get("doc_stats", []):
            doc_html += f'''
            <tr>
                <td style="font-weight: 500;">{row["domain"]}</td>
                <td style="color: var(--glow); font-family: 'Fira Code', monospace; font-weight: bold;">{row["count"]}</td>
                <td style="color: var(--text-muted); font-family: 'Fira Code', monospace;">{row["metric"]}</td>
                <td style="font-weight: 600;">{row["status"]}</td>
            </tr>
            '''
        html = html.replace('{doc_stats_html}', doc_html)
        
        take_doc = metrics.get("senior_doc_take", "")
        take_doc_html = f'<div class="diagnosis-doc" style="margin-top: 20px;"><strong style="color: var(--glow); font-size: 1.1rem; display: block; margin-bottom: 8px;">资深架构师评估:</strong> <span style="font-size: 0.95rem; line-height: 1.6; display: inline-block;">"{take_doc}"</span></div>' if take_doc else ""
        html = html.replace('{senior_doc_take_html}', take_doc_html)
        
        # Calculate derived core
        conservative_core = metrics['core'] + 15000
        ideal_core = metrics['core'] + 30000
        html = html.replace('{conservative_core}', str(conservative_core))
        html = html.replace('{ideal_core}', str(ideal_core))
        html = html.replace('{sunburst_data}', json.dumps(get_sunburst_data()))
        
        # Safely slice the HTML based on page type to prevent regex failures
        top_start = html.find('<!-- 1. Topology View -->')
        feat_start = html.find('<!-- 2. Feature Dataflow View -->')
        est_start = html.find('<!-- 3. Code Estimate View -->')
        rep_start = html.find('<!-- 4. Report Center View -->')
        script_start = html.find('        <script>')
        
        header_html = html[:top_start]
        top_html = html[top_start:feat_start]
        feat_html = html[feat_start:est_start]
        est_html = html[est_start:rep_start]
        rep_html = html[rep_start:script_start]
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
            html = html.replace('{init_script}', js)
        elif page_type == 'reports':
            html = header_html + rep_html + footer_html
            html = html.replace('{init_script}', '')

        if page_type == "topology":
            out_name = "dashboard-topology.html"
        elif page_type == "feature":
            out_name = "dashboard-dataflow.html"
        elif page_type == "estimate":
            out_name = "dashboard-estimate.html"
        else:
            out_name = "dashboard-reports.html" 
        out_path = os.path.join("docs", "cerb", out_name)
        with open(out_path, "w", encoding="utf-8") as f:
            f.write(html)
        print(f"Generated {out_path}")

if __name__ == "__main__":
    layers = parse_interface_map()
    generate_html(layers)
