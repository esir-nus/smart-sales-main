import re

p = "docs/tools/generate_map.py"
with open(p, "r") as f:
    text = f.read()

# I will just write a python script to rewrite it instead of using multi_replace_file_content, 
# because rewriting the whole generate_html function is easier.
