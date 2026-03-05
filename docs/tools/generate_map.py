import os
import re
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
        
        elif current_layer:
            if line.startswith("|"):
                in_table = True
                table_lines.append(line)
            elif in_table and not line.strip():
                in_table = False
                current_layer["modules"] = parse_markdown_table(table_lines)
                table_lines = []

    # Handle the last layer if table ended at EOF
    if current_layer and table_lines and not current_layer["modules"]:
         current_layer["modules"] = parse_markdown_table(table_lines)

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
    }

    .layer-title::before {
        content: '';
        display: block;
        width: 12px;
        height: 12px;
        background: var(--accent);
        border-radius: 50%;
        box-shadow: 0 0 10px var(--accent);
    }

    .modules-grid {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        gap: 30px;
        position: relative;
    }

    .module-card {
        width: 320px; /* Fixed width to enforce symmetric alignment */
        flex-shrink: 0;
        border-radius: 8px;
        padding: 16px;
        background: var(--bg);
        border: 1px solid var(--border);
        transition: all 0.3s ease;
        position: relative;
        overflow: hidden;
    }

    /* Virtual connection ports for the bloodlines */
    .module-card::before, .module-card::after {
        content: '';
        position: absolute;
        left: 50%;
        transform: translateX(-50%);
        width: 24px;
        height: 4px;
        background: var(--surface-hover);
        z-index: 10;
        border-radius: 4px;
        transition: background 0.3s ease;
    }
    
    .module-card::before { top: 0; }
    .module-card::after { bottom: 0; }
    
    .module-card:hover::before, .module-card:hover::after {
        background: var(--accent);
        box-shadow: 0 0 8px var(--accent);
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
    
    /* Data Flow SVG Styles */
    #data-flow-layer {
        position: absolute;
        top: 0;
        left: 0;
        pointer-events: none;
        z-index: 1; /* Below cards but above background */
    }
    
    .module-card {
        z-index: 2; /* Ensure cards are clickable above the SVG */
        position: relative;
    }

    .data-line {
        fill: none;
        stroke: var(--border);
        stroke-width: 2px;
        opacity: 0.3;
        transition: stroke 0.3s;
    }

    .data-pulse {
        fill: none;
        stroke: #ef4444; /* Crimson Bloodline */
        stroke-width: 3px;
        stroke-linecap: round;
        opacity: 0;
        filter: drop-shadow(0 0 8px #ef4444) drop-shadow(0 0 12px #dc2626);
        animation: flowPulse 2.5s infinite ease-in-out;
    }

    /* Gamified Pulse Animation */
    @keyframes flowPulse {
        0% { stroke-dasharray: 0, 1000; stroke-dashoffset: 0; opacity: 0; }
        10% { opacity: 0.8; stroke-width: 4px; }
        50% { stroke-dasharray: 100, 1000; stroke-dashoffset: -100; opacity: 1; }
        90% { opacity: 0.8; }
        100% { stroke-dasharray: 60, 1000; stroke-dashoffset: -400; opacity: 0; }
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
            <div class="legend-item"><span class="module-status status-shipped">Shipped</span> (Real impl)</div>
            <div class="legend-item"><span class="module-status status-interface">Interface</span> (Fake impl)</div>
            <div class="legend-item"><span class="module-status status-planned">Planned</span> (Not coded)</div>
            <div class="legend-item"><span class="module-status status-wip">WIP</span></div>
        </div>

        <div class="layer-container">
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
            
            signature = mod.get("Key Interface", "").replace("`", "")
            reads_from = mod.get("Reads From", "").replace("`", "")
            if "Receives From (via Orchestrator)" in mod:
                # Handle Layer 4 specific columns
                reads_from = f"Direct: {mod.get('Reads From (directly)', '')} <br> Via Orch: {mod.get('Receives From (via Orchestrator)', '')}".replace("`", "")
                
            owns = mod.get("Owns (Writes)", "")
            os_layer = mod.get("OS Layer", "")

            html_content += f"""
                <div class="module-card">
                    <div class="module-header">
                        <div class="module-name">{name_html}</div>
                        <div class="module-status {status_class}">{status_label}</div>
                    </div>
                    
                    <div class="module-detail">
                        <div><span class="label">Owns:</span> {clean_markdown(owns)}</div>
                        <div><span class="label">Reads From:</span> {clean_markdown(reads_from)}</div>
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
        
        <!-- SVG Data Flow Layer -->
        <svg id="data-flow-layer"></svg>

        <script>
            // Data Flow Visualization Logic
            const svgLayer = document.getElementById('data-flow-layer');
            const cards = Array.from(document.querySelectorAll('.module-card'));
            
            // Map module names to their DOM elements
            const cardMap = new Map();
            cards.forEach(card => {
                const name = card.querySelector('.module-name').textContent.trim();
                cardMap.set(name, card);
            });

            function getPorts(element) {
                const rect = element.getBoundingClientRect();
                return {
                    top: { x: rect.left + rect.width / 2 + window.scrollX, y: rect.top + window.scrollY },
                    bottom: { x: rect.left + rect.width / 2 + window.scrollX, y: rect.bottom + window.scrollY }
                };
            }

            function drawDataFlows() {
                // Clear existing lines
                svgLayer.innerHTML = '';
                
                // SVG needs to cover the entire document
                svgLayer.style.width = document.documentElement.scrollWidth + 'px';
                svgLayer.style.height = document.documentElement.scrollHeight + 'px';

                cards.forEach(card => {
                    const sourceName = card.querySelector('.module-name').textContent.trim();
                    const readsFromEl = Array.from(card.querySelectorAll('.label')).find(el => el.textContent === 'Reads From:');
                    
                    if (!readsFromEl) return;
                    
                    const readsFromText = readsFromEl.nextSibling ? readsFromEl.nextSibling.textContent : readsFromEl.parentElement.textContent.replace('Reads From:', '');
                    
                    // Extract module names from the "Reads From" text
                    // This is a naive extraction; it splits by commas and common words
                    const targets = readsFromText.split(/[,&]+| Direct: | Via Orch: /)
                        .map(s => s.trim())
                        .filter(s => s && s !== '—' && s !== 'None');

                    targets.forEach(targetRaw => {
                        // Try to find the closest matching module name in our map
                        let targetName = null;
                        for (const key of cardMap.keys()) {
                            if (targetRaw.includes(key) || key.includes(targetRaw.split(' ')[0])) {
                                targetName = key;
                                break;
                            }
                        }

                        if (targetName && cardMap.has(targetName)) {
                            const targetCard = cardMap.get(targetName);
                            // Draw line FROM target TO source (data flows UP/OUT from the dependency)
                            drawLine(targetCard, card);
                        }
                    });
                });
            }

            function drawLine(fromEl, toEl) {
                const fromPorts = getPorts(fromEl);
                const toPorts = getPorts(toEl);

                // Data flows UP from dependency (fromEl.top) to consumer (toEl.bottom)
                // However, "Reads From" is defined on the consumer.
                // fromEl = Target (Dependency), toEl = Source (Consumer defining "Reads From")
                const start = fromPorts.top;
                const end = toPorts.bottom;

                // Curved path (cubic bezier) for organic fluid flow
                const distanceX = Math.abs(end.x - start.x);
                const distanceY = Math.abs(end.y - start.y);
                const cpOffsetY = Math.max(distanceY * 0.5, 60); 
                
                // Add some organic sway to the x coordinates of control points based on distance
                const tension = 0.2;
                const cpOffsetX = (end.x - start.x) * tension;
                
                const pathString = `M ${start.x} ${start.y} 
                                    C ${start.x + cpOffsetX} ${start.y - cpOffsetY},
                                      ${end.x - cpOffsetX} ${end.y + cpOffsetY},
                                      ${end.x} ${end.y}`;

                // Create the visible tube
                const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                path.setAttribute('d', pathString);
                path.setAttribute('class', 'data-line');
                svgLayer.appendChild(path);

                // Create the animated bloodline tracking the tube
                const pulse = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                pulse.setAttribute('d', pathString);
                pulse.setAttribute('class', 'data-pulse');
                
                // Randomize animation delay to create a heartbeat-like continuous flow
                const delay = Math.random() * 2.5;
                pulse.style.animationDelay = `${delay}s`;
                
                svgLayer.appendChild(pulse);
            }

            // Initial draw
            setTimeout(drawDataFlows, 100);

            // Redraw on window resize
            window.addEventListener('resize', () => {
                requestAnimationFrame(drawDataFlows);
            });
        </script>
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
