import re
import os

with open("docs/tools/generate_map.py", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update OUTPUT_PATH definition
content = content.replace('OUTPUT_PATH = "docs/cerb/dashboard.html"', 'OUTPUT_PATH = "docs/cerb/dashboard.html" # Obsolete')

# 2. Add parse_estimate function
parse_fn = """
def parse_estimate():
    metrics = {
        "total": 0, "core": 0, "tests": 0, "res": 0,
        "conservative": 0, "ideal": 0
    }
    try:
        with open("PRODUCTION_CODE_ESTIMATE.md", "r", encoding="utf-8") as f:
            text = f.read()
            m_total = re.search(r"Current XP\*\*:\s*`([\d,]+)`", text)
            if m_total: metrics["total"] = int(m_total.group(1).replace(",", ""))
            
            m_breakdown = re.search(r"\*\(Core:\s*`([\d,]+)`\s*\|\s*Tests:\s*`([\d,]+)`\s*\|\s*Resources:\s*`([\d,]+)`\)\*", text)
            if m_breakdown:
                metrics["core"] = int(m_breakdown.group(1).replace(",", ""))
                metrics["tests"] = int(m_breakdown.group(2).replace(",", ""))
                metrics["res"] = int(m_breakdown.group(3).replace(",", ""))
                
            m_target = re.search(r"Target XP\*\*:\s*`~([\d,]+)`", text)
            if m_target:
                # We know the conservative is +22k from total, ideal is +38k (which matches 238k)
                ideal = int(m_target.group(1).replace(",", ""))
                metrics["ideal"] = ideal
                metrics["conservative"] = metrics["total"] + 22000
    except Exception as e:
        print("Error parsing estimate:", e)
    return metrics
"""

if "def parse_estimate" not in content:
    content = content.replace('def parse_interface_map():', parse_fn + '\ndef parse_interface_map():')

# 3. Modify generate_html to take 'page_type' and output the separate files
# Find the signature of generate_html
content = content.replace('def generate_html(layers):', 'def generate_html_single(layers, page_type, metrics):')

# Find the tabs HTML in the template and replace it with static links
tabs_html_old = """
        <div class="tabs">
            <button class="tab-btn active" onclick="switchTab(event, 'topology')">Topology Sequence (Layers)</button>
            <button class="tab-btn" onclick="switchTab(event, 'feature')">Feature Dataflow (Tracks)</button>
            <button class="tab-btn" onclick="switchTab(event, 'estimate')">代码量估算 (Code Estimates)</button>
        </div>
"""

tabs_html_new = """
        <div class="tabs">
            <a href="dashboard-topology.html" class="tab-btn {top_active}" style="text-decoration: none; display: inline-block;">拓扑层级</a>
            <a href="dashboard-dataflow.html" class="tab-btn {feat_active}" style="text-decoration: none; display: inline-block;">跨模块数据流</a>
            <a href="dashboard-estimate.html" class="tab-btn {est_active}" style="text-decoration: none; display: inline-block;">代码量估算</a>
        </div>
"""
content = content.replace(tabs_html_old.strip(), tabs_html_new.strip())

# Replace the dynamic hardcoded values in the charts and boxes
content = re.sub(r'<div class="stat-value glow">[\d,]+</div>', '<div class="stat-value glow">{total:,}</div>', content)
content = re.sub(r'<div class="stat-value">188,821</div>', '<div class="stat-value">{core:,}</div>', content)
content = re.sub(r'<div class="stat-value">11,150</div>', '<div class="stat-value">{tests:,}</div>', content)
content = re.sub(r'<div class="stat-value">87</div>', '<div class="stat-value">{res:,}</div>', content)

content = re.sub(r'<div class="stat-value" style="color: var\(--warning\);">222,058</div>', '<div class="stat-value" style="color: var(--warning);">{conservative:,}</div>', content)
content = re.sub(r'<div class="stat-value" style="color: var\(--success\);">238,058</div>', '<div class="stat-value" style="color: var(--success);">{ideal:,}</div>', content)

# update pie chart JS data
content = re.sub(r'data: \[188821, 11150, 87\],', 'data: [{core}, {tests}, {res}],', content)
# update bar chart JS data
content = re.sub(r'data: \[22232, 118579, 188821, 193821, 201821\],', 'data: [22232, 118579, {core}, {conservative_core}, {ideal_core}],', content)
content = re.sub(r'data: \[72658, 146976, 200058, 222058, 238058\],', 'data: [72658, 146976, {total}, {conservative}, {ideal}],', content)


# 4. Modify the end of the script
main_block_old = """
if __name__ == "__main__":
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    layers = parse_interface_map()
    generate_html(layers)
"""

main_block_new = """
def generate_html(layers):
    metrics = parse_estimate()
    
    # We will generate the 3 files
    for page_type in ["topology", "feature", "estimate"]:
        html = generate_html_single(layers, page_type, metrics)
        
        # We need to filter the sections based on page_type
        # To do this safely, we replace the {total} tags first
        
        # Setup tags
        html = html.replace('{top_active}', 'active' if page_type == 'topology' else '')
        html = html.replace('{feat_active}', 'active' if page_type == 'feature' else '')
        html = html.replace('{est_active}', 'active' if page_type == 'estimate' else '')
        
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
        
        # Calculate derived core
        conservative_core = metrics['core'] + 15000
        ideal_core = metrics['core'] + 30000
        html = html.replace('{conservative_core}', str(conservative_core))
        html = html.replace('{ideal_core}', str(ideal_core))
        
        # Strip other sections using regex
        if page_type == 'topology':
            html = re.sub(r'<!-- 2. Feature Dataflow View -->.*<!-- 3. Code Estimate View -->', '<!-- 2. Feature Dataflow View -->', html, flags=re.DOTALL)
            html = re.sub(r'<!-- 3. Code Estimate View -->.*</div>\s*</div>\s*</div>', '<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            # Remove JS charting and line drawing
            html = re.sub(r'<script>\s*let lines = \[\];.*?</script>', '<script>\\n // No tab switching needed \\n</script>', html, flags=re.DOTALL)
        elif page_type == 'feature':
            html = re.sub(r'<!-- 1. Topology View -->.*<!-- 2. Feature Dataflow View -->', '<!-- 1. Topology View -->\\n<!-- 2. Feature Dataflow View -->', html, flags=re.DOTALL)
            html = re.sub(r'<!-- 3. Code Estimate View -->.*</div>\s*</div>\s*</div>', '<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            # update JS
            html = html.replace("window.addEventListener('load', () => {", "window.addEventListener('load', () => { initLines(); lines.forEach(l => { l.show('draw', {duration: 500, timing: 'ease-out'}); l.position(); });")
        elif page_type == 'estimate':
            html = re.sub(r'<!-- 1. Topology View -->.*<!-- 3. Code Estimate View -->', '<!-- 1. Topology View -->\\n<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            html = html.replace("window.addEventListener('load', () => {", "window.addEventListener('load', () => { initCharts(); ")

        out_name = "dashboard-topology.html" if page_type == "topology" else ("dashboard-dataflow.html" if page_type == "feature" else "dashboard-estimate.html")
        out_path = os.path.join("docs", "cerb", out_name)
        with open(out_path, "w", encoding="utf-8") as f:
            f.write(html)
        print(f"Generated {out_path}")

if __name__ == "__main__":
    layers = parse_interface_map()
    generate_html(layers)
"""

if "def generate_html_single" in content:
    # Already patched maybe?
    pass
else:
    content = content.replace("    with open(OUTPUT_PATH, \"w\", encoding=\"utf-8\") as f:\n        f.write(html_content)\n    \n    print(f\"Successfully generated {OUTPUT_PATH}\")", "    return html_content")
    if main_block_old.strip() in content:
        content = content.replace(main_block_old.strip(), main_block_new.strip())

with open("docs/tools/generate_map.py", "w", encoding="utf-8") as f:
    f.write(content)

print("Patching complete!")
