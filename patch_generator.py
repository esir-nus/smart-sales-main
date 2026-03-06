import re

with open('docs/cerb/dashboard.html', 'r', encoding='utf-8') as f:
    dashboard_html = f.read()

# Extract the <div id="estimate">...</div>
estimate_match = re.search(r'(<div id="estimate" class="tab-content">.*?</div>\n        </div>)', dashboard_html, re.DOTALL)
if not estimate_match:
    print("Could not find estimate tab")
    exit(1)
estimate_html = estimate_match.group(1)

# Extract localized header
header_match = re.search(r'(<div class="header">.*?</div>)', dashboard_html, re.DOTALL)
header_html = header_match.group(1) if header_match else ""

# Extract localized tabs
tabs_match = re.search(r'(<div class="tabs">.*?</div>)', dashboard_html, re.DOTALL)
tabs_html = tabs_match.group(1) if tabs_match else ""

# Extract the customized scripts (initCharts, switchTab logic for estimate)
script_match = re.search(r'(<script>.*?</script>)', dashboard_html, re.DOTALL)
script_html = script_match.group(1) if script_match else ""

# Extract customized CSS (chart styles, gaps)
css_match = re.search(r'<style>(.*?)</style>', dashboard_html, re.DOTALL)
css_html = css_match.group(1) if css_match else ""

# Now read generate_map.py and apply these parts
with open('docs/tools/generate_map.py', 'r', encoding='utf-8') as f:
    gen_content = f.read()

# Replace css
gen_content = re.sub(r'css = """(.*?)"""\n\n    def clean_markdown', f'css = """{css_html}"""\n\n    def clean_markdown', gen_content, flags=re.DOTALL)

# Replace the HTML outer template
# The template starts with html_content = f""" ... """
# and ends with """
# It's split into multiple parts
# Let's just do a manual replacement for the tabs and header

html_template_replace = f"""
        {header_html}
        
        <div class="legend">
            <div class="legend-item"><span class="module-status status-shipped">Shipped</span> (Real impl) / <span class="translation-hint">已上线</span></div>
            <div class="legend-item"><span class="module-status status-interface">Interface</span> (Fake impl) / <span class="translation-hint">仅接口 (假实现)</span></div>
            <div class="legend-item"><span class="module-status status-planned">Planned</span> (Not coded) / <span class="translation-hint">计划中 (未开发)</span></div>
            <div class="legend-item"><span class="module-status status-wip">WIP</span> / <span class="translation-hint">开发中</span></div>
        </div>

        {tabs_html}

        <!-- 1. Topology View -->
        <div id="topology" class="tab-content active">
            <div class="topology-sequence">
"""

gen_content = re.sub(r'<div class="header">.*?<div class="topology-sequence">', html_template_replace.replace('\\', '\\\\'), gen_content, flags=re.DOTALL)

# Replace the closing logic
html_close_replace = f"""
        </div>
        
        <!-- 3. Code Estimate View -->
{estimate_html}

        {script_html}
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    </body>
    </html>
"""

gen_content = re.sub(r'        </div>\n        \n        <script>.*?</script>\n    </body>\n    </html>', html_close_replace.replace('\\', '\\\\'), gen_content, flags=re.DOTALL)

with open('docs/tools/generate_map.py', 'w', encoding='utf-8') as f:
    f.write(gen_content)

print("Patched generate_map.py")
