import re

with open("docs/tools/generate_map.py", "r", encoding="utf-8") as f:
    text = f.read()

# 1. Remove window.switchTab entirely from the template text to avoid bugs.
text = re.sub(r'window\.switchTab = function.*?};', '', text, flags=re.DOTALL)

# 2. Replace the window.addEventListener load block with a placeholder {init_script}
load_block = r"window\.addEventListener\('load', \(\) => \{.*?\n\s+\}\);"
text = re.sub(load_block, '{init_script}', text, flags=re.DOTALL)

# 3. Add the active class to the containers
text = text.replace('<div id="topology" class="tab-content active">', '<div id="topology" class="tab-content {top_container_active}">')
text = text.replace('<div id="feature" class="tab-content">', '<div id="feature" class="tab-content {feat_container_active}">')
text = text.replace('<div id="estimate" class="tab-content">', '<div id="estimate" class="tab-content {est_container_active}">')

# 4. Modify generate_html to inject the right pieces
old_generate = """
        # Setup tags
        html = html.replace('{top_active}', 'active' if page_type == 'topology' else '')
        html = html.replace('{feat_active}', 'active' if page_type == 'feature' else '')
        html = html.replace('{est_active}', 'active' if page_type == 'estimate' else '')
"""

new_generate = """
        # Setup tags
        html = html.replace('{top_active}', 'active' if page_type == 'topology' else '')
        html = html.replace('{feat_active}', 'active' if page_type == 'feature' else '')
        html = html.replace('{est_active}', 'active' if page_type == 'estimate' else '')
        
        html = html.replace('{top_container_active}', 'active' if page_type == 'topology' else '')
        html = html.replace('{feat_container_active}', 'active' if page_type == 'feature' else '')
        html = html.replace('{est_container_active}', 'active' if page_type == 'estimate' else '')
"""
text = text.replace(old_generate.strip(), new_generate.strip())


# 5. Fix the JS injection logic
old_js_inject = """
        # Strip other sections using regex
        if page_type == 'topology':
            html = re.sub(r'<!-- 2\. Feature Dataflow View -->.*<!-- 3\. Code Estimate View -->', '<!-- 2. Feature Dataflow View -->', html, flags=re.DOTALL)
            html = re.sub(r'<!-- 3\. Code Estimate View -->.*</div>\s*</div>\s*</div>', '<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            html = re.sub(r'<script>\s*let lines = \[\];.*?</script>', '<script>\\n // No tab switching needed \\n</script>', html, flags=re.DOTALL)
        elif page_type == 'feature':
            html = re.sub(r'<!-- 1\. Topology View -->.*<!-- 2\. Feature Dataflow View -->', '<!-- 1. Topology View -->\\n<!-- 2. Feature Dataflow View -->', html, flags=re.DOTALL)
            html = re.sub(r'<!-- 3\. Code Estimate View -->.*</div>\s*</div>\s*</div>', '<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            html = html.replace("window.addEventListener('load', () => {", "window.addEventListener('load', () => { initLines(); lines.forEach(l => { l.show('draw', {duration: 500, timing: 'ease-out'}); l.position(); });")
        elif page_type == 'estimate':
            html = re.sub(r'<!-- 1\. Topology View -->.*<!-- 3\. Code Estimate View -->', '<!-- 1. Topology View -->\\n<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            html = html.replace("window.addEventListener('load', () => {", "window.addEventListener('load', () => { initCharts(); ")
"""

new_js_inject = """
        # Strip other sections using regex
        if page_type == 'topology':
            html = re.sub(r'<!-- 2\. Feature Dataflow View -->.*<!-- 3\. Code Estimate View -->', '<!-- 2. Feature Dataflow View -->', html, flags=re.DOTALL)
            html = re.sub(r'<!-- 3\. Code Estimate View -->.*<\/div>\s*<\/div>\s*<\/div>', '<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            html = html.replace('{init_script}', '')
        elif page_type == 'feature':
            html = re.sub(r'<!-- 1\. Topology View -->.*<!-- 2\. Feature Dataflow View -->', '<!-- 1. Topology View -->\\n<!-- 2. Feature Dataflow View -->', html, flags=re.DOTALL)
            html = re.sub(r'<!-- 3\. Code Estimate View -->.*<\/div>\s*<\/div>\s*<\/div>', '<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            js = "window.addEventListener('load', () => { initLines(); setTimeout(() => { lines.forEach(l => { l.show('draw', {duration: 500, timing: 'ease-out'}); l.position(); }); }, 200); });"
            html = html.replace('{init_script}', js)
        elif page_type == 'estimate':
            html = re.sub(r'<!-- 1\. Topology View -->.*<!-- 3\. Code Estimate View -->', '<!-- 1. Topology View -->\\n<!-- 3. Code Estimate View -->', html, flags=re.DOTALL)
            js = "window.addEventListener('load', () => { initCharts(); });"
            html = html.replace('{init_script}', js)
"""
text = text.replace(old_js_inject.strip(), new_js_inject.strip())

with open("docs/tools/generate_map.py", "w", encoding="utf-8") as f:
    f.write(text)
print("Patch applied")
