import re

with open("docs/tools/generate_map.py", "r") as f:
    code = f.read()

# Make sure topo cards have with_deps=True
code = code.replace('prefix="topo-", with_deps=False', 'prefix="topo-", with_deps=True')

# Replace the script block
old_script_start = code.find("<script>")
old_script_end = code.find("</script>") + len("</script>")

new_script = """<script>
            let topoLines = [];
            let featLines = [];
            let topoLinesInitialized = false;
            let featLinesInitialized = false;

            function initLines(viewMode) {
                let initialized = viewMode === 'topology' ? topoLinesInitialized : featLinesInitialized;
                if (initialized) return;
                
                if (viewMode === 'topology') topoLinesInitialized = true;
                else featLinesInitialized = true;

                const cards = document.querySelectorAll(`#${viewMode} .module-card`);
                const lineArray = viewMode === 'topology' ? topoLines : featLines;

                // Adjust color and path for semantic difference
                const lineColor = viewMode === 'topology' ? 'rgba(156, 163, 175, 0.4)' : 'rgba(59, 130, 246, 0.4)';
                const linePath = viewMode === 'topology' ? 'fluid' : 'grid';
                const shadowColor = viewMode === 'topology' ? 'rgba(156, 163, 175, 0.2)' : 'rgba(192, 132, 252, 0.4)';

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
                                        color: lineColor,
                                        startSocket: 'bottom',
                                        endSocket: 'top',
                                        path: linePath,
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
                                            color: shadowColor
                                        },
                                        hide: false
                                    }
                                );
                                lineArray.push(line);
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
                if (window.AnimEvent) {
                    window.addEventListener('scroll', AnimEvent.add(function() {
                        topoLines.forEach(l => l.position());
                        featLines.forEach(l => l.position());
                    }), false);
                }
            }

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
                
                setTimeout(() => {
                    if (tabId === 'topology') {
                        initLines('topology');
                        topoLines.forEach(l => {
                            l.show('draw', {duration: 500, timing: 'ease-out'});
                            l.position();
                        });
                        featLines.forEach(l => l.hide('fade', {duration: 100}));
                    } else {
                        initLines('feature');
                        featLines.forEach(l => {
                            l.show('draw', {duration: 500, timing: 'ease-out'});
                            l.position();
                        });
                        topoLines.forEach(l => l.hide('fade', {duration: 100}));
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
        </script>"""

code = code[:old_script_start] + new_script + code[old_script_end:]

with open("docs/tools/generate_map.py", "w") as f:
    f.write(code)

