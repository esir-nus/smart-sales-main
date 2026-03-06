import re
with open("docs/tools/generate_map.py", "r") as f:
    code = f.read()

start = code.find("def generate_html(layers):")

new_generate_html = """def clean_markdown(text):
    text = text.replace("**", "")
    link_match = re.search(r"\[(.*?)\]\((.*?)\)", text)
    if link_match:
        return f'<a href="{link_match.group(2)}">{link_match.group(1)}</a>'
    return text

def get_status_class_and_label(status_str):
    if "✅" in status_str: return "status-shipped", "Shipped"
    if "🔲" in status_str: return "status-planned", "Planned"
    if "📐" in status_str: return "status-interface", "Interface"
    if "��" in status_str: return "status-wip", "WIP"
    if "⏸️" in status_str: return "status-blocked", "Blocked"
    return "status-planned", "Unknown"

def render_card(mod, prefix, with_deps):
    status_class, status_label = get_status_class_and_label(mod.get("Status", ""))
    raw_module = mod.get("Module", "Unknown")
    name_html = clean_markdown(raw_module)
    clean_name_key = re.sub(r"\[(.*?)\]\((.*?)\)", r"\\1", raw_module).replace("**", "").strip()
    explanation = MODULE_EXPLANATIONS.get(clean_name_key, "后端组件")
    dom_id = prefix + clean_name_key.lower().replace(" ", "-").replace("(", "").replace(")", "").strip()
    signature = mod.get("Key Interface", "").replace("`", "")
    
    raw_reads_from = mod.get("Reads From", "").replace("`", "")
    raw_reads_direct = mod.get("Reads From (directly)", "").replace("`", "") if "Reads From (directly)" in mod else ""
    
    deps_json_attr = "{}"
    if with_deps:
        all_deps = raw_reads_from + "," + raw_reads_direct
        deps_list = [d.strip().lower().replace(" ", "-").replace("(", "").replace(")", "") for d in all_deps.split(",") if d.strip() and d.strip().lower() != "eventbus" and d.strip().lower() != "none" and d.strip().lower() != "app"]
        deps_list = [prefix + d for d in deps_list]
        import json
        deps_json_attr = json.dumps(deps_list)

    reads_from = raw_reads_from
    if "Receives From (via Orchestrator)" in mod:
        reads_from = f"Direct / 直接读取: {raw_reads_direct} <br> Via Orch / 经编排器接收: {mod.get('Receives From (via Orchestrator)', '')}".replace("`", "")
    else:
        reads_from = clean_markdown(reads_from)
        
    owns = mod.get("Owns (Writes)", "")
    owns_html = clean_markdown(owns)
    os_layer = mod.get("OS Layer", "")

    if not owns: owns_html = "无"
    if not reads_from: reads_from = "无"

    return f'''
        <div class="module-card" id="{dom_id}" data-reads-from=\\'{deps_json_attr}\\'>
            <div class="anchor-top" id="anchor-top-{dom_id}"></div>
            <div class="anchor-bottom" id="anchor-bottom-{dom_id}"></div>
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
    '''

def generate_html(layers):
    css = \"\"\"
    :root {
        --bg: #0f172a;
        --surface: rgba(30, 41, 59, 0.8);
        --surface-hover: rgba(51, 65, 85, 0.9);
        --border: #334155;
        --text: #f8fafc;
        --text-muted: #94a3b8;
        --accent: #3b82f6;
        --glow: #c084fc;
        
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
        margin-bottom: 60px;
        text-align: center;
        position: relative;
        z-index: 10;
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
        border-radius: 12px;
        padding: 20px;
        background: var(--surface);
        backdrop-filter: blur(10px);
        border: 1px solid var(--border);
        transition: all 0.3s ease;
        position: relative;
        overflow: visible;
        z-index: 15; /* Ensure cards are above everything else in grid */
    }

    .module-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 30px rgba(0,0,0,0.4);
        z-index: 20;
        border-color: var(--accent);
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
        display: flex;
        gap: 15px;
        margin-bottom: 40px;
        justify-content: center;
        position: relative;
        z-index: 50;
    }
    .tab-btn {
        background: var(--surface);
        border: 1px solid var(--border);
        color: var(--text-muted);
        padding: 12px 24px;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
        font-size: 1.1rem;
        transition: all 0.2s;
        font-family: inherit;
    }
    .tab-btn:hover {
        background: var(--surface-hover);
        color: var(--text);
    }
    .tab-btn.active {
        background: var(--accent);
        color: #fff;
        border-color: var(--accent);
        box-shadow: 0 0 15px rgba(59, 130, 246, 0.4);
    }
    .tab-content {
        display: none;
    }
    .tab-content.active {
        display: block;
    }
    \"\"\"

    html_content = f\"\"\"
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
            <h1>Cerb Architecture Dashboard</h1>
            <p>Real-time module status and domain I/O contracts</p>
        </div>
        
        <div class="legend">
            <div class="legend-item"><span class="module-status status-shipped">Shipped</span> (Real impl) / <span class="translation-hint">已上线</span></div>
            <div class="legend-item"><span class="module-status status-interface">Interface</span> (Fake impl) / <span class="translation-hint">仅接口 (假实现)</span></div>
            <div class="legend-item"><span class="module-status status-planned">Planned</span> (Not coded) / <span class="translation-hint">计划中 (未开发)</span></div>
            <div class="legend-item"><span class="module-status status-wip">WIP</span> / <span class="translation-hint">开发中</span></div>
        </div>

        <div class="tabs">
            <button class="tab-btn active" onclick="switchTab(event, 'topology')">Topology View (Layers)</button>
            <button class="tab-btn" onclick="switchTab(event, 'feature')">Feature Dataflow (Tracks)</button>
        </div>

        <!-- 1. Topology View -->
        <div id="topology" class="tab-content active">
            <div class="layer-container" style="position: relative; padding-left: 40px;">
    \"\"\"

    for i, layer in enumerate(layers):
        if not layer["modules"]: continue
        html_content += f\"\"\"
        <div class="layer">
            <div class="layer-title">Layer {len(layers) - i}: {layer['name']}</div>
            <div class="modules-grid">
        \"\"\"
        for mod in layer["modules"]:
            html_content += render_card(mod, prefix="topo-", with_deps=False)
        html_content += \"\"\"
            </div>
        </div>
        \"\"\"

    html_content += \"\"\"
            </div>
        </div>
    \"\"\"

    # 2. Feature Dataflow View
    html_content += \"\"\"
        <div id="feature" class="tab-content">
            <div class="layer-container" style="flex-direction: column; position: relative; padding-left: 40px; gap: 40px;">
    \"\"\"
    
    # Group by track
    from collections import defaultdict
    tracks = defaultdict(list)
    for layer in layers:
        for mod in layer["modules"]:
            track = mod.get("Track", "Unknown Track").replace("`", "")
            if track == "Unknown Track":
                track = "Uncategorized"
            tracks[track].append(mod)

    for track_name, track_modules in tracks.items():
        html_content += f\"\"\"
        <div class="layer" style="margin-bottom: 20px;">
            <div class="layer-title" style="border-color: var(--glow); color: var(--glow); box-shadow: 0 0 10px rgba(192, 132, 252, 0.2);">{track_name}</div>
            <div class="modules-grid" style="grid-template-columns: repeat(3, 1fr);">
        \"\"\"
        for mod in track_modules:
            html_content += render_card(mod, prefix="feat-", with_deps=True)
        html_content += \"\"\"
            </div>
        </div>
        \"\"\"
        
    html_content += \"\"\"
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
                                        hide: false
                                    }
                                );
                                lines.push(line);
                            }
                        });
                    } catch (e) {
                        console.error('Failed to parse deps for', sourceId, e);
                    }
                });
                
                // Force all SVG lines to the absolute background so they don't block cards
                setTimeout(() => {
                    document.querySelectorAll('.leader-line').forEach(svg => {
                        svg.style.zIndex = '-1';
                    });
                }, 100);

                // Redraw on scroll or resize
                window.addEventListener('scroll', AnimEvent.add(function() {
                    lines.forEach(l => l.position());
                }), false);
            }

            // Also make sure to hide lines when clicking LeaderLine might overlay tab clicks
            window.switchTab = function(event, tabId) {
                // Update button active state
                document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
                event.currentTarget.classList.add('active');
                
                // Keep track of which display value to use
                document.querySelectorAll('.tab-content').forEach(content => {
                    content.style.display = 'none';
                    content.classList.remove('active');
                });
                
                // Show target tab
                const targetContent = document.getElementById(tabId);
                targetContent.style.display = 'block';
                targetContent.classList.add('active');
                
                if (tabId === 'feature') {
                    // Give DOM a frame to render before measuring points
                    setTimeout(() => {
                        initLines();
                        lines.forEach(l => {
                            l.show('draw', {duration: 500, timing: 'ease-out'});
                            l.position();
                        });
                    }, 50);
                } else {
                    lines.forEach(l => l.hide('fade', {duration: 100}));
                }
            };
        </script>
    </body>
    </html>
    \"\"\"
    
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        f.write(html_content)
    
    print(f"Successfully generated {OUTPUT_PATH}")

if __name__ == "__main__":
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    layers = parse_interface_map()
    generate_html(layers)
"""

final = code[:start] + new_generate_html
with open("docs/tools/generate_map.py", "w") as f:
    f.write(final)

