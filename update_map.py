import re
import os

docs_dir = "/home/cslh-frank/main_app/docs/cerb"
map_path = os.path.join(docs_dir, "interface-map.md")

with open(map_path, "r") as f:
    lines = f.readlines()

# Get all module directories
all_modules = [m for m in os.listdir(docs_dir) if os.path.isdir(os.path.join(docs_dir, m))]
mapped_modules = set()

def find_module_dir(name):
    target = name.lower().replace(" ", "-")
    for m in all_modules:
        if m.startswith(target) or target.startswith(m) or target in m or m in target:
            return m
    # specific overrides
    overrides = {
        "oss": "oss-service",
        "asr": "asr-service",
        "sessionhistory": "session-history",
        "architect": "analyst-architect",
        "orchestrator": "analyst-orchestrator",
        "executor": "model-routing", # Executor is in model-routing? Let's check. Actually no, Executor is often in core. Model-routing has Executor.
        "mascot (system i)": "mascot-service",
        "scheduleboard": "scheduler",
        "badgeaudiopipeline": "badge-audio-pipeline",
        "notificationservice": "notifications"
    }
    if name.lower() in overrides:
        return overrides[name.lower()]
    return None

def get_link(mod_dir):
    if os.path.exists(os.path.join(docs_dir, mod_dir, "spec.md")):
        return f"./{mod_dir}/spec.md"
    elif os.path.exists(os.path.join(docs_dir, mod_dir, "interface.md")):
        return f"./{mod_dir}/interface.md"
    return f"./{mod_dir}"

new_lines = []
for line in lines:
    if line.startswith("| **") and not line.startswith("| **Module**"):
        # Extract module name
        match = re.search(r'\| \*\*(.*?)\*\*', line)
        if match:
            mod_name = match.group(1).replace("[", "").replace("]", "")
            # check if it's already linked
            if "](" in mod_name:
                mod_name = mod_name.split("](")[0]
            
            mod_dir = find_module_dir(mod_name)
            if mod_dir:
                mapped_modules.add(mod_dir)
                link = get_link(mod_dir)
                new_line = line.replace(f"**{match.group(1)}**", f"**[{mod_name}]({link})**")
                new_lines.append(new_line)
                continue
    new_lines.append(line)

unmapped = set(all_modules) - mapped_modules

with open(map_path, "w") as f:
    f.writelines(new_lines)
    if unmapped:
        f.write("\n## Unmapped Modules (Need Placement)\n\n")
        f.write("| Module | Owns (Writes) | Reads From | Key Interface | OS Layer | Status |\n")
        f.write("|--------|--------------|------------|---------------|----------|--------|\n")
        for um in sorted(list(unmapped)):
            link = get_link(um)
            f.write(f"| **[{um}]({link})** | ? | ? | ? | ? | ? |\n")

print(f"Mapped modules: {len(mapped_modules)}")
print(f"Unmapped modules: {len(unmapped)}")
print(", ".join(unmapped))

