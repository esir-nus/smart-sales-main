import os
import re
import json

# Plain language explanations for stakeholders (English + Chinese)
MODULE_EXPLANATIONS = {
    # Layer 1
    "ConnectivityBridge": "Manages the Bluetooth connection to the physical badge. / 管理与实体蓝牙工牌的连接状态。",
    "NotificationService": "Shows system alerts and reminders on the phone. / 在手机上显示系统弹窗和任务提醒。",
    "OSS": "Uploads audio files to the cloud. / 负责将录音文件上传到云端存储。",
    "ASR": "Transcribes voice to text. / 将语音转换为文字 (备用)。",
    "TingwuPipeline": "阿里云听悟 - Transcribes audio and extracts meeting summaries. / 阿里云听悟接口，负责语音转文字和提取会议摘要。",
    "PipelineTelemetry": "Records internal system logs for debugging. / 记录系统内部运行日志，用于排查错误。",
    
    # Layer 2
    "EntityWriter": "Saves and updates people, companies, and locations in the database. / 负责在数据库中新建或更新联系人、公司等实体信息。",
    "EntityRegistry": "Reads people and companies from the database. / 负责从数据库中查询和读取实体信息。",
    "MemoryCenter": "Stores the raw chat history and voice transcripts. / 存储所有的聊天记录和语音转写历史。",
    "UserHabit": "Tracks user behavior patterns for AI personalization. / 记录用户的使用习惯，让AI更懂你。",
    "SessionHistory": "Manages the list of past conversations (sidebar). / 管理侧边栏的历史会话列表 (重命名、置顶)。",
    "SessionContext": "The temporary fast-memory for the current chat. / 当前聊天的临时高速缓存，AI思考时的数据源。",
    
    # Layer 3
    "ContextBuilder": "Packages all available background knowledge to feed to the AI. / 负责把历史记录和人物关系打包，送给AI大脑。",
    "InputParser": "Quickly figures out what the user wants to do (Intent). / 快速分析用户的意图，比如分辨用户是在闲聊还是下达命令。",
    "EntityDisambiguator": "Asks the user to clarify if two people have the same name. / 当遇到重名或指代不清时，负责向用户发起追问。",
    "LightningRouter": "The Traffic Cop - instantly routes simple questions away from deep analysis. / 交通警察：瞬间判断问题难易度，把简单问题直接放行，复杂的送去深度思考。",
    "EntityResolver": "Matches names spoken in audio to exact IDs in the database. / 将录音里提到的人名，精准匹配到数据库里的唯一ID。",
    "ModelRegistry": "Configuration for different AI models (e.g. fast vs smart). / 管理不同的AI大模型配置 (比如：思考快的模型 vs 思考深的模型)。",
    "Executor": "The actual bridge sending prompts to Dashscope/Qwen. / 真正负责向云端AI(比如通义千问)发请求并接收结果的桥梁。",
    "PluginRegistry": "Executes specific actions like exporting PDFs. / 负责执行具体的动作，比如生成PDF文件或发邮件。",
    "PrismOrchestrator": "The Boss (System II) - coordinates the entire deep-thinking process. / 系统大脑(系统二)：指挥整个深度思考流程，决定先做什么后做什么。",
    "UnifiedPipeline": "The assembly line that processes complex user requests step-by-step. / 处理流水线：将复杂请求按步骤(提取-组装-路由)流水化处理。",
    
    # Layer 4
    "Mascot (System I)": "The fast, friendly AI that handles greetings and quick chats. / 吉祥物(系统一)：负责打招呼、秒回聊天和显示气泡提示，反应极快。",
    "Scheduler": "Creates and manages To-Do tasks and reminders. / 负责创建和管理待办事项及日程提醒。",
    "ScheduleBoard": "Checks for time conflicts in the calendar. / 检查日程表中是否有时间冲突。",
    "BadgeAudioPipeline": "Controls the start/stop recording flow for the physical badge. / 控制实体工牌开始/停止录音的流程。",
    "AudioManagement": "UI state for syncing and transcribing audio files. / 处理录音文件同步、转写的界面状态。",
    "ConflictResolver": "Helps the user resolve calendar double-bookings. / 帮助用户解决时间冲突 (比如：改期或取消)。",
    "DevicePairing": "The UI flow for connecting a new Bluetooth badge. / 负责配对新蓝牙工牌的界面流程。",
    
    # Layer 5
    "ClientProfileHub": "Builds a complete psychological profile of a customer. / 综合所有历史信息，为客户建立完整的画像和性格分析。",
    "RLModule": "Learns from past mistakes to improve future Prompts. / 强化学习模块：从过去的错误中学习，优化未来给AI的提示词。"
}
import json

MAP_PATH = "docs/cerb/interface-map.md"
OUTPUT_PATH = "docs/cerb/dashboard.html"

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
        
        elif line.startswith("## Data Flow") or line.startswith("## Ownership") or line.startswith("## Anti-Patterns"):
            # Stop parsing specific layers when we hit the footer sections
            if current_layer and table_lines:
                 current_layer["modules"] = parse_markdown_table(table_lines)
                 table_lines = []
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

def generate_html(layers):
    css = """
    :root {
        --bg: #0f172a;
        --surface: #1e293b;
        --surface-hover: #334155;
        --border: #334155;
        --text: #f8fafc;
        --text-muted: #94a3b8;
        --accent: #3b82f6;
        
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
        font-family: 'Inter', -apple-system, sans-serif;
        background-color: var(--bg);
        color: var(--text);
        line-height: 1.6;
        padding: 40px;
    }

    .header {
        margin-bottom: 40px;
        text-align: center;
    }
    
    .header h1 {
        font-size: 2.5rem;
        background: linear-gradient(135deg, #60a5fa, #c084fc);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        margin-bottom: 10px;
    }
    
    .header p { color: var(--text-muted); }

    .layer-container {
        display: flex;
        flex-direction: column-reverse; /* Bottom to top reading like a stack */
        gap: 40px;
        max-width: 1200px;
        margin: 0 auto;
    }

    .layer {
        background: var(--surface);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 24px;
        box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1);
    }

    .layer-title {
        font-size: 1.25rem;
        font-weight: 600;
        margin-bottom: 20px;
        color: var(--text);
        display: flex;
        align-items: center;
        gap: 10px;
        position: relative;
    }

    /* The glowing dataflow pipeline effect */
    .layer-container::before {
        content: '';
        position: absolute;
        width: 4px;
        background: linear-gradient(to bottom, var(--accent), #c084fc, var(--accent));
        top: 250px;
        bottom: 100px;
        left: 20px;
        border-radius: 4px;
        box-shadow: 0 0 15px var(--accent), 0 0 30px #c084fc;
        opacity: 0.6;
        animation: pulseflow 3s infinite ease-in-out;
    }

    @keyframes pulseflow {
        0% { opacity: 0.4; box-shadow: 0 0 10px var(--accent); }
        50% { opacity: 0.8; box-shadow: 0 0 20px var(--accent), 0 0 40px #c084fc; }
        100% { opacity: 0.4; box-shadow: 0 0 10px var(--accent); }
    }

    /* Nodes on the pipeline */
    .layer::before {
        content: '';
        position: absolute;
        left: 14px;
        margin-top: 30px;
        width: 16px;
        height: 16px;
        background: var(--bg);
        border: 4px solid var(--accent);
        border-radius: 50%;
        box-shadow: 0 0 10px var(--accent);
        z-index: 2;
    }

    .layer-title::before {
        display: none; /* Hide old dots */
    }

    .modules-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 20px;
    }

    .module-card {
        border-radius: 8px;
        padding: 16px;
        background: var(--bg);
        border: 1px solid var(--border);
        transition: all 0.3s ease;
        position: relative;
        overflow: hidden;
    }

    .module-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 20px rgba(0,0,0,0.2);
        z-index: 10;
        border-color: var(--text-muted);
    }

    .module-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;
    }

    .module-name {
        font-weight: 600;
        font-size: 1.1rem;
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
        padding: 4px 8px;
        border-radius: 4px;
        font-weight: 500;
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
        gap: 4px;
        padding-bottom: 8px;
        border-bottom: 1px dotted rgba(255,255,255,0.1);
    }
    
    .module-explanation {
        font-size: 0.8rem;
        color: #e2e8f0;
        margin-bottom: 12px;
        line-height: 1.4;
        background: rgba(0,0,0,0.2);
        padding: 8px;
        border-radius: 4px;
        border-left: 2px solid var(--accent);
    }

    .module-detail span.label {
        font-weight: 600;
        color: var(--text);
        opacity: 0.8;
    }

    .card-signature {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px dashed var(--border);
        font-family: 'Fira Code', monospace;
        font-size: 0.75rem;
        color: #a78bfa;
        word-break: break-all;
    }
    
    .os-layer {
        position: absolute;
        bottom: 0;
        right: 0;
        font-size: 0.65rem;
        background: var(--surface-hover);
        padding: 4px 8px;
        border-top-left-radius: 8px;
        color: var(--text-muted);
    }
    
    .legend {
        display: flex;
        justify-content: center;
        gap: 20px;
        margin-bottom: 30px;
        flex-wrap: wrap;
    }
    
    .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 0.9rem;
    }
    
    .translation-hint {
        font-size: 0.75rem;
        color: var(--text-muted);
        margin-left: 4px;
        font-weight: normal;
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
        if "✅" in status_str: return "status-shipped", "Shipped"
        if "🔲" in status_str: return "status-planned", "Planned"
        if "📐" in status_str: return "status-interface", "Interface"
        if "🚧" in status_str: return "status-wip", "WIP"
        if "⏸️" in status_str: return "status-blocked", "Blocked"
        return "status-planned", "Unknown"

    html_content = f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Cerb Interface Map - 2D Dashboard</title>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Fira+Code&display=swap" rel="stylesheet">
        <style>{css}</style>
    </head>
    <body>
        <div class="header">
            <h1>Cerb Architecture Dashboard</h1>
            <p>Real-time module status and domain I/O contracts</p>
        </div>
        
        <div class="legend">
            <div class="legend-item"><span class="module-status status-shipped">Shipped</span> (Real impl) / <span class="translation-hint">已上线</span></div>
            <div class="legend-item"><span class="module-status status-interface">Interface</span> (Fake impl) / <span class="translation-hint">仅接口 (假实现)</span></div>
            <div class="legend-item"><span class="module-status status-planned">Planned</span> (Not coded) / <span class="translation-hint">计划中 (未开发)</span></div>
            <div class="legend-item"><span class="module-status status-wip">WIP</span> / <span class="translation-hint">开发中</span></div>
        </div>

        <div class="layer-container" style="position: relative; padding-left: 40px;">
    """

    # We enumerate backwards or forwards? The CSS flex-direction: column-reverse handles the bottom-to-top rendering.
    for i, layer in enumerate(layers):
        if not layer["modules"]: continue
        
        html_content += f"""
        <div class="layer">
            <div class="layer-title">Layer {len(layers) - i}: {layer['name']}</div>
            <div class="modules-grid">
        """

        for mod in layer["modules"]:
            status_class, status_label = get_status_class_and_label(mod.get("Status", ""))
            raw_module = mod.get("Module", "Unknown")
            name_html = clean_markdown(raw_module)
            
            # Lookup explanation
            # Clean the name to match dict keys (strip markdown links)
            clean_name_key = re.sub(r"\[(.*?)\]\(.*?\)", r"\1", raw_module).replace("**", "").strip()
            explanation = MODULE_EXPLANATIONS.get(clean_name_key, "Backend Component / 后端组件")
            
            signature = mod.get("Key Interface", "").replace("`", "")
            
            # Extract and format the 'Reads From' handling with translations
            reads_from = mod.get("Reads From", "").replace("`", "")
            if "Receives From (via Orchestrator)" in mod:
                # Handle Layer 4 specific columns
                reads_from = f"Direct / 直接读取: {mod.get('Reads From (directly)', '')} <br> Via Orch / 经编排器接收: {mod.get('Receives From (via Orchestrator)', '')}".replace("`", "")
            else:
                reads_from = clean_markdown(reads_from)
                
            owns = mod.get("Owns (Writes)", "")
            owns_html = clean_markdown(owns)
            os_layer = mod.get("OS Layer", "")

            # If these columns are missing, fallback gracefully
            if not owns: owns_html = "None / 无"
            if not reads_from: reads_from = "None / 无"

            html_content += f"""
                <div class="module-card">
                    <div class="module-header">
                        <div class="module-name">{name_html}</div>
                        <div class="module-status {status_class}">{status_label}</div>
                    </div>
                    
                    <div class="module-explanation">
                        {explanation}
                    </div>
                    
                    <div class="module-detail">
                        <div><span class="label">Owns / 拥有 (写入):</span> {owns_html}</div>
                        <div><span class="label">Reads From / 读取自:</span> {reads_from}</div>
                    </div>
                    
                    <div class="card-signature">{signature}</div>
                    <div class="os-layer">{os_layer}</div>
                </div>
            """

        html_content += """
            </div>
        </div>
        """

    html_content += """
        </div>
    </body>
    </html>
    """
    
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        f.write(html_content)
    
    print(f"Successfully generated {OUTPUT_PATH}")

if __name__ == "__main__":
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    layers = parse_interface_map()
    generate_html(layers)
