import re

with open("docs/tools/generate_map.py", "r", encoding="utf-8") as f:
    code = f.read()

# I need to find where the topological view is currently rendered and replace it.
# It starts around: '# 1. Topology View' and ends right before '# 2. Feature Dataflow View'

# We'll also inject the new CSS right before the HTML string interpolation starts in `generate_html`

start_marker = "<!-- 1. Topology View -->"
end_marker = "# 2. Feature Dataflow View"

# Let's locate the CSS block insertion point (the end of the existing css string)
css_end = code.find('    """\n\n    html_content = f"""')

new_css = """
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

code = code[:css_end] + new_css + code[css_end:]


# Re-evaluate start/end since code index shifted
start_idx = code.find("<!-- 1. Topology View -->")
# The actual view code starts right after:
#         <!-- 1. Topology View -->
#         <div id="topology" class="tab-content active">
#             <div class="layer-container" style="position: relative; padding-left: 40px;">
#     """
# 
#     for i, layer in enumerate(layers):

end_idx = code.find("# 2. Feature Dataflow View")

replacement_topology = """<!-- 1. Topology View -->
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

    # Reverse layers so L5 is at the top
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
        
        modules_html = "\\n".join(module_names)
        
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
    \"\"\"

    """

code = code[:start_idx] + replacement_topology + code[end_idx:]

with open("docs/tools/generate_map.py", "w", encoding="utf-8") as f:
    f.write(code)

