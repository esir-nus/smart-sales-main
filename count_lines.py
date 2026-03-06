import os

core_lines = 0
test_lines = 0
res_xml_lines = 0

for root, dirs, files in os.walk("."):
    if ".git" in root or "build" in root or "_deprecated_purge" in root:
        continue
    for f in files:
        path = os.path.join(root, f)
        if path.endswith(".kt"):
            try:
                with open(path, "r", encoding="utf-8") as file:
                    lines = sum(1 for _ in file)
                    if "/src/test/" in path or "/src/androidTest/" in path:
                        test_lines += lines
                    else:
                        core_lines += lines
            except Exception as e:
                pass
        elif path.endswith(".xml") and "/src/main/res/" in path:
            try:
                with open(path, "r", encoding="utf-8") as file:
                    res_xml_lines += sum(1 for _ in file)
            except:
                pass

print(f"Core Kotlin: {core_lines}")
print(f"Test Kotlin: {test_lines}")
print(f"Resource XML: {res_xml_lines}")
