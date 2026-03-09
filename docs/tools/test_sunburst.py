import os
import json

def generate_sunburst_data():
    valid_exts = {'.kt', '.java', '.xml'}
    ignore_dirs = {'build', '.git', '.gradle', 'docs', 'tmp', 'res'}
    skip_structural = {'src', 'main', 'java', 'com', 'smartsales', 'prism'}
    
    def crawl(path, depth=1, max_depth=4):
        try:
            entries = os.listdir(path)
        except Exception:
            return []
            
        children = []
        files_loc = 0
        
        for entry in entries:
            full_path = os.path.join(path, entry)
            if os.path.isdir(full_path):
                if entry in ignore_dirs:
                    continue
                
                # If it's a structural dir, we don't increase depth, and we just return its children directly 
                if entry in skip_structural or entry == os.path.basename(path).replace('-', '_'):
                    sub_children = crawl(full_path, depth, max_depth)
                    children.extend(sub_children)
                    continue
                
                if depth >= max_depth:
                    loc = sum(1 for root, _, files in os.walk(full_path) for f in files if os.path.splitext(f)[1] in valid_exts for _ in open(os.path.join(root, f), 'r', encoding='utf-8', errors='ignore'))
                    if loc > 0:
                        children.append({"name": entry, "value": loc})
                else:
                    sub_children = crawl(full_path, depth + 1, max_depth)
                    if sub_children:
                        children.append({"name": entry, "children": sub_children})
                        
            elif os.path.splitext(entry)[1] in valid_exts:
                try:
                    with open(full_path, 'r', encoding='utf-8', errors='ignore') as f:
                        files_loc += sum(1 for _ in f)
                except Exception:
                    pass
        
        if files_loc > 0:
            children.append({"name": "(Files)", "value": files_loc})
                
        optimized = []
        for c in children:
            if "children" in c and len(c["children"]) == 1 and c["children"][0]["name"] == "(Files)":
                optimized.append({"name": c["name"], "value": c["children"][0]["value"]})
            else:
                optimized.append(c)
                
        return optimized

    top_level = ['app', 'app-core', 'app-prism', 'core', 'data', 'domain']
    data = []
    for mod in top_level:
        if os.path.exists(mod):
            child_nodes = crawl(mod, depth=1, max_depth=4)
            if len(child_nodes) == 1 and child_nodes[0].get("name") == "(Files)":
                data.append({"name": mod, "value": child_nodes[0]["value"]})
            elif child_nodes:
                data.append({"name": mod, "children": child_nodes})
                
    return data

if __name__ == '__main__':
    data = generate_sunburst_data()
    print(json.dumps(data, indent=2))
