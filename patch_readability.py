import re

with open("docs/tools/generate_map.py", "r", encoding="utf-8") as f:
    text = f.read()

# 1. We need to tell LeaderLine to dodge elements, but LeaderLine's native dodging is complex.
# A simpler solution is to drastically increase the horizontal/vertical gap in the CSS grid,
# and adjust the socket locations so lines don't cross over the center of cards.

# Find the CSS block for modules-grid
grid_css_start = text.find('.modules-grid {')
grid_css_end = text.find('}', grid_css_start) + 1

new_grid_css = """    .modules-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 80px 60px; /* Massively increased gap to give SVG lines room to breathe */
        position: relative;
        z-index: 10;
    }"""

text = text[:grid_css_start] + new_grid_css + text[grid_css_end:]

# 2. Update the LeaderLine Javascript configuration
# Right now, everything uses startSocket: 'bottom', endSocket: 'top'
# For grid layouts, 'auto' or dynamically calculating based on DOM order prevents spaghetti.
# We also want to increase the drop shadow contrast and line opacity so they stand out more.

js_config_start = text.find('const line = new LeaderLine(')
js_config_end = text.find(');', js_config_start) + 2

new_js_config = """                                const line = new LeaderLine(
                                    targetEl,
                                    card,
                                    {
                                        color: 'rgba(59, 130, 246, 0.75)', /* Much brighter line */
                                        startSocket: 'auto', /* Let LeaderLine choose the shortest, non-overlapping path */
                                        endSocket: 'auto',
                                        path: 'grid', /* Grid path ensures right angles */
                                        startPlug: 'disc', /* Clearer origin */
                                        endPlug: 'arrow3',
                                        size: 3, /* Thicker line */
                                        dash: {
                                            animation: true,
                                            len: 8,
                                            gap: 8
                                        },
                                        dropShadow: {
                                            dx: 0,
                                            dy: 0,
                                            blur: 6,
                                            color: 'rgba(255, 255, 255, 0.15)' /* White glow for dark mode contrast */
                                        },
                                        hide: true
                                    }
                                );"""

text = text[:js_config_start] + new_js_config + text[js_config_end:]

# 3. Z-Index fix: The issue in the screenshot is lines going UNDER cards, making them invisible.
# We want lines to go OVER the gaps, but realistically LeaderLine uses an absolute overlay.
# The lines are currently forced to z-index: -1. Let's remove that so they render on top,
# but since they use 'grid' and 'auto', they'll route AROUND the cards (mostly).

z_index_start = text.find('setTimeout(() => {\n                    document.querySelectorAll(\'.leader-line').forEach')
z_index_end = text.find('}, 100);', z_index_start) + 8

new_z_index_logic = """                setTimeout(() => {
                    document.querySelectorAll('.leader-line').forEach(svg => {
                        svg.style.zIndex = '5'; /* Place lines between the background and hover states */
                        svg.style.pointerEvents = 'none'; /* Don't block clicks on underlying elements */
                    });
                }, 100);"""

text = text[:z_index_start] + new_z_index_logic + text[z_index_end:]


with open("docs/tools/generate_map.py", "w", encoding="utf-8") as f:
    f.write(text)

