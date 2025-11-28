#!/usr/bin/env python3
# 文件：tools/update_react_align_plan.py
# 模块：tools
# 说明：从 JSON 数据生成 react_align_plan.md，保持文档与计划同步
# 作者：创建于 2025-11-27

import json
import sys
from pathlib import Path

DATA_FILENAME = "react_align_plan_data.json"
OUTPUT_FILENAME = "react_align_plan.md"


def load_plan(data_path: Path) -> dict:
    with data_path.open(encoding="utf-8") as handle:
        return json.load(handle)


def render_markdown(plan: dict) -> str:
    title = plan["title"]
    lines = [title, "=" * len(title), ""]
    lines.append(f"Goal: {plan['goal']}")
    source_note = plan.get("source_note")
    if source_note:
        lines.append("")
        lines.append(f"Source: {source_note}")
    lines.append("")
    for section in plan.get("sections", []):
        lines.append(section["heading"])
        for bullet in section.get("bullets", []):
            lines.append(f"- {bullet}")
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent
    data_path = repo_root / "docs" / DATA_FILENAME
    output_path = repo_root / "docs" / OUTPUT_FILENAME

    if not data_path.exists():
        print(f"数据文件缺失：{data_path}", file=sys.stderr)
        return 1

    plan = load_plan(data_path)
    content = render_markdown(plan)
    output_path.write_text(content, encoding="utf-8")
    print(f"已生成 {output_path.relative_to(repo_root)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
