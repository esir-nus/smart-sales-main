import re
import os

with open("docs/tools/generate_map.py", "r", encoding="utf-8") as f:
    text = f.read()

# 1. Provide the CSS block up to .tab-content
css_addition = """
    .tabs {
        display: flex;
        justify-content: center;
        gap: 15px;
        margin-bottom: 40px;
        position: relative;
        z-index: 20;
    }
    
    .tab-btn {
        background: var(--surface);
        border: 1px solid var(--border);
        color: var(--text-muted);
        padding: 12px 24px;
        border-radius: 8px;
        font-size: 1.05rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .tab-btn:hover {
        background: var(--surface-hover);
        color: var(--text);
    }
    
    .tab-btn.active {
        background: rgba(59, 130, 246, 0.15);
        border-color: var(--accent);
        color: var(--accent);
        box-shadow: 0 0 15px rgba(59, 130, 246, 0.2);
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
    
    /* Topology specific CSS */
    .topology-sequence {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 20px;
        padding: 40px 0;
    }
    .layer-block {
        background: var(--surface);
        border: 2px solid var(--border);
        border-radius: 16px;
        padding: 24px 32px;
        width: 100%;
        max-width: 800px;
        text-align: center;
        position: relative;
        box-shadow: 0 4px 20px rgba(0,0,0,0.2);
        transition: all 0.3s ease;
    }
    .layer-block:hover {
        border-color: var(--accent);
        box-shadow: 0 8px 30px rgba(59, 130, 246, 0.2);
        transform: translateY(-2px);
    }
    .layer-block h2 {
        color: var(--accent);
        font-size: 1.5rem;
        margin-bottom: 12px;
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
        gap: 10px;
        justify-content: center;
    }
    .mini-module {
        background: rgba(0,0,0,0.4);
        border: 1px solid var(--border);
        padding: 6px 14px;
        border-radius: 20px;
        font-size: 0.85rem;
        color: var(--text);
        font-family: 'Fira Code', monospace;
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
"""

css_insert_idx = text.find('    """\n\n    def clean_markdown')
text = text[:css_insert_idx] + css_addition + text[css_insert_idx:]

# 2. Replace the HTML body setup to inject the tabs navigation container
html_start = text.find('<div class="layer-container"')
# Replace from there to the first for loop
html_end = text.find('    # We enumerate backwards or forwards?')

replacement_nav = """
        <div class="tabs">
            <button class="tab-btn active" onclick="switchTab(event, 'topology')">Topology Sequence (Layers)</button>
            <button class="tab-btn" onclick="switchTab(event, 'feature')">Feature Dataflow (Tracks)</button>
        </div>

        <!-- 1. Topology View -->
        <div id="topology" class="tab-content active">
            <div class="topology-sequence">
    \"\"\"
    
    layer_descriptions = {
        "Layer 5": "Cross-cutting services that aggregate data from multiple Layer 2 sources.",
        "Layer 4": "User-facing features. Each receives processed results from Orchestrator (Layer 3) and reads from Data Services (Layer 2).",
        "Layer 3": "Orchestrates LLM-powered processing. Reads from Layer 2 data services.",
        "Layer 2": "Store and query domain data. Other modules use their interfaces but never each other's storage.",
        "Layer 1": "Leaf services with no upstream dependencies. They don't call other modules."
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
        
        modules_html = "\\n                ".join(module_names)
        
        if i > 0:
            html_content += \"\"\"
            <div class="seq-arrow">↑</div>
            \"\"\"
            
        html_content += f\"\"\"
        <div class="layer-block">
            <h2>{layer_name}</h2>
            <div class="layer-desc">{desc}</div>
            <div class="layer-modules-list">
                {modules_html}
            </div>
        </div>
        \"\"\"

    html_content += \"\"\"
            </div>
        </div>

        <!-- 2. Feature Dataflow View -->
        <div id="feature" class="tab-content">
            <div class="layer-container" style="flex-direction: column; position: relative; padding-left: 40px; gap: 40px;">
    \"\"\"

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
        clean_name_key = re.sub(r"\\[(.*?)\\]\\(.*?\\)", r"\\1", raw_module).replace("**", "").strip()
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
            reads_from = f"Direct / 直接读取: {raw_reads_direct} <br> Via Orch / 经编排器接收: {mod.get('Receives From (via Orchestrator)', '')}".replace("`", "")
        else:
            reads_from = clean_markdown(reads_from)
            
        owns = mod.get("Owns (Writes)", "")
        owns_html = clean_markdown(owns)
        os_layer = mod.get("OS Layer", "")

        if not owns: owns_html = "无"
        if not reads_from: reads_from = "无"

        return f\"\"\"
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
                    <div><span class="label">Owns / 拥有 (写入):</span> {owns_html}</div>
                    <div><span class="label">Reads From / 读取自:</span> {reads_from}</div>
                </div>
                
                <div class="card-signature">{signature}</div>
                <div class="os-layer">{os_layer}</div>
            </div>
        \"\"\"

    for track_name, track_modules in sorted_tracks.items():
        html_content += f\"\"\"
        <div class="layer" style="margin-bottom: 20px;">
            <div class="layer-title" style="border-color: var(--glow); color: var(--glow); box-shadow: 0 0 10px rgba(192, 132, 252, 0.2);">{track_name}</div>
            <div class="modules-grid" style="grid-template-columns: repeat(3, 1fr);">
        \"\"\"

        for mod in track_modules:
            html_content += render_card(mod, prefix="feat_")

        html_content += \"\"\"
            </div>
        </div>
        \"\"\"

    html_content += \"\"\"
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

            window.switchTab = function(event, tabId) {
                document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
                event.currentTarget.classList.add('active');
                
                document.querySelectorAll('.tab-content').forEach(content => {
                    content.style.display = 'none';
                    content.classList.remove('active');
                });
                
                const targetContent = document.getElementById(tabId);
                targetContent.style.display = 'block';
                targetContent.classList.add('active');
                
                setTimeout(() => {
                    if (tabId === 'feature') {
                        initLines();
                        lines.forEach(l => {
                            l.show('draw', {duration: 500, timing: 'ease-out'});
                            l.position();
                        });
                    } else {
                        lines.forEach(l => l.hide('fade', {duration: 100}));
                    }
                }, 50);
            };

            window.addEventListener('load', () => {
                setTimeout(() => {
                    const activeBtn = document.querySelector('.tab-btn.active');
                    if (activeBtn) {
                        window.switchTab({currentTarget: activeBtn}, 'topology');
                    }
                }, 100);
            });
        </script>
    </body>
    </html>
    \"\"\"
"""

# The chunk we need to slice out from text starts at `<div class="layer-container"`
end_html_script = text.find('    with open(OUTPUT_PATH')
text = text[:html_start] + replacement_nav + text[end_html_script:]

with open("docs/tools/generate_map.py", "w", encoding="utf-8") as f:
    f.write(text)
