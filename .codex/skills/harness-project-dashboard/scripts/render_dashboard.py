#!/usr/bin/env python3
from __future__ import annotations

import argparse
import datetime as dt
import html
import os
import re
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
PROJECTS_DIR = ROOT / "docs" / "projects"
DEFAULT_OUTPUT = PROJECTS_DIR / "dashboard" / "index.html"
DEFAULT_SCORE_OUTPUT_NAME = "bake-scores.html"
TEMPLATE_PATH = Path(__file__).resolve().parents[1] / "assets" / "dashboard_template.html"

HEADING_RE = re.compile(r"^##\s+(.+?)\s*$", re.MULTILINE)
PROJECT_TITLE_RE = re.compile(r"^#\s+Project:\s+(.+?)\s*$", re.MULTILINE)
SPRINT_TITLE_RE = re.compile(r"^#\s+(.+?)\s*$", re.MULTILINE)
SCORE_LABELS = (
    "Pre-BAKE codebase score",
    "Work score",
    "Baked-codebase score",
)
DELTA_ROWS = (
    "Contract delta",
    "Behavior delta",
    "Simplification delta",
    "Drift corrected",
    "Assumption killed",
    "Duplication/dead code",
    "Blast radius",
    "Tests added/changed",
    "Runtime evidence",
    "Residual risk/debt",
    "Net judgment",
)


@dataclass
class Sprint:
    number: str
    slug: str
    status_raw: str
    summary: str
    contract_rel: str
    contract_path: Path
    contract_title: str

    @property
    def status_key(self) -> str:
        return normalize_status(self.status_raw)


@dataclass
class Project:
    title: str
    slug: str
    objective: str
    status_raw: str
    tracker_path: Path
    sprints: list[Sprint]

    @property
    def status_key(self) -> str:
        return normalize_status(self.status_raw)


@dataclass
class ScoreValue:
    value: float | None
    note: str


@dataclass
class ScoreRecord:
    project_title: str
    project_slug: str
    sprint_number: str
    sprint_slug: str
    sprint_title: str
    sprint_summary: str
    contract_path: Path
    scores: dict[str, ScoreValue]
    delta_rows: dict[str, str]

    @property
    def pre_score(self) -> float | None:
        return self.scores["Pre-BAKE codebase score"].value

    @property
    def work_score(self) -> float | None:
        return self.scores["Work score"].value

    @property
    def baked_score(self) -> float | None:
        return self.scores["Baked-codebase score"].value

    @property
    def improvement(self) -> float | None:
        if self.pre_score is None or self.baked_score is None:
            return None
        return self.baked_score - self.pre_score


def normalize_status(value: str) -> str:
    lowered = " ".join(value.strip().lower().split())
    if not lowered:
        return "fallback"
    if lowered.startswith("open"):
        return "open"
    if lowered.startswith("closed"):
        return "closed"
    for known in ("planned", "authored", "in-progress", "done", "stopped", "blocked"):
        if lowered.startswith(known):
            return known
    return "fallback"


def escape(value: str) -> str:
    return html.escape(value, quote=True)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def extract_section(text: str, title: str) -> str:
    pattern = re.compile(rf"^##\s+{re.escape(title)}\s*$", re.MULTILINE)
    match = pattern.search(text)
    if not match:
        return ""
    start = match.end()
    next_match = HEADING_RE.search(text, start)
    end = next_match.start() if next_match else len(text)
    return text[start:end].strip()


def first_paragraph(section: str) -> str:
    block = section.split("\n\n", 1)[0].strip()
    lines = [line.strip() for line in block.splitlines() if line.strip()]
    return " ".join(lines)


def normalize_prose(value: str) -> str:
    lines = []
    for line in value.splitlines():
        stripped = line.strip()
        if stripped:
            lines.append(stripped)
    return " ".join(lines)


def parse_project(tracker_path: Path) -> Project:
    text = read_text(tracker_path)
    title_match = PROJECT_TITLE_RE.search(text)
    title = title_match.group(1).strip() if title_match else tracker_path.parent.name
    objective = first_paragraph(extract_section(text, "Objective")) or "No objective recorded."
    status_raw = first_paragraph(extract_section(text, "Status")) or "unknown"
    sprint_table = extract_section(text, "Sprint Index")
    sprints = parse_sprint_table(sprint_table, tracker_path.parent)
    return Project(
        title=title,
        slug=tracker_path.parent.name,
        objective=objective,
        status_raw=status_raw,
        tracker_path=tracker_path,
        sprints=sprints,
    )


def parse_sprint_table(section: str, project_dir: Path) -> list[Sprint]:
    rows: list[Sprint] = []
    if not section:
        return rows

    lines = [line.strip() for line in section.splitlines() if line.strip()]
    for line in lines:
        if not line.startswith("|"):
            continue
        if set(line.replace("|", "").replace("-", "").replace(" ", "")) == set():
            continue
        cells = [cell.strip() for cell in line.strip("|").split("|")]
        if len(cells) < 5 or cells[0] == "#":
            continue
        contract_rel, contract_path = parse_markdown_link(cells[4], project_dir)
        contract_title = extract_contract_title(contract_path)
        rows.append(
            Sprint(
                number=cells[0],
                slug=cells[1],
                status_raw=cells[2],
                summary=cells[3] or "No summary recorded.",
                contract_rel=contract_rel,
                contract_path=contract_path,
                contract_title=contract_title,
            )
        )
    return rows


def parse_markdown_link(cell: str, project_dir: Path) -> tuple[str, Path]:
    match = re.search(r"\[[^\]]+\]\(([^)]+)\)", cell)
    if not match:
        rel = cell.strip()
        return rel, (project_dir / rel).resolve()
    rel = match.group(1).strip()
    return rel, (project_dir / rel).resolve()


def extract_contract_title(path: Path) -> str:
    if not path.exists():
        return "Contract file missing"
    text = read_text(path)
    match = SPRINT_TITLE_RE.search(text)
    return match.group(1).strip() if match else path.stem


def extract_score_value(text: str, label: str) -> ScoreValue:
    pattern = re.compile(
        rf"^-\s+\*\*{re.escape(label)}\*\*:\s*"
        rf"(?P<score>\d+(?:\.\d+)?)/5\.\s*(?P<note>.*?)(?=^\-\s+\*\*|\Z)",
        re.MULTILINE | re.DOTALL,
    )
    match = pattern.search(text)
    if not match:
        return ScoreValue(value=None, note="")
    return ScoreValue(
        value=float(match.group("score")),
        note=normalize_prose(match.group("note")),
    )


def extract_delta_rows(text: str) -> dict[str, str]:
    rows: dict[str, str] = {}
    marker = re.search(r"^-\s+\*\*Code delta transparency\*\*:\s*$", text, re.MULTILINE)
    if not marker:
        return rows
    table_lines = []
    for line in text[marker.end() :].splitlines():
        stripped = line.strip()
        if not stripped:
            if table_lines:
                break
            continue
        if not stripped.startswith("|"):
            if table_lines:
                break
            continue
        table_lines.append(stripped)
    for line in table_lines:
        cells = [cell.strip() for cell in line.strip("|").split("|")]
        if len(cells) < 2:
            continue
        if cells[0] == "Area" or set(cells[0].replace("-", "").replace(" ", "")) == set():
            continue
        if set(cells[0].replace("-", "").replace(" ", "")) == set():
            continue
        rows[cells[0]] = cells[1]
    return rows


def parse_score_record(project: Project, sprint: Sprint) -> ScoreRecord | None:
    if not sprint.contract_path.exists():
        return None
    text = read_text(sprint.contract_path)
    scores = {label: extract_score_value(text, label) for label in SCORE_LABELS}
    if all(score.value is None for score in scores.values()):
        return None
    return ScoreRecord(
        project_title=project.title,
        project_slug=project.slug,
        sprint_number=sprint.number,
        sprint_slug=sprint.slug,
        sprint_title=sprint.contract_title,
        sprint_summary=sprint.summary,
        contract_path=sprint.contract_path,
        scores=scores,
        delta_rows=extract_delta_rows(text),
    )


def collect_score_records(projects: list[Project]) -> list[ScoreRecord]:
    records: list[ScoreRecord] = []
    for project in projects:
        for sprint in project.sprints:
            record = parse_score_record(project, sprint)
            if record:
                records.append(record)
    return records


def load_projects() -> list[Project]:
    projects: list[Project] = []
    for tracker_path in sorted(PROJECTS_DIR.glob("*/tracker.md")):
        if tracker_path.parent.name == "dashboard":
            continue
        projects.append(parse_project(tracker_path))
    return projects


def relative_link(path: Path, output_path: Path) -> str:
    return escape(os.path.relpath(path, start=output_path.parent))


def build_status_counts(projects: list[Project]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for project in projects:
        counts[project.status_key] = counts.get(project.status_key, 0) + 1
        for sprint in project.sprints:
            counts[sprint.status_key] = counts.get(sprint.status_key, 0) + 1
    return counts


def render_status_chip(label: str, count: int, css_key: str) -> str:
    return (
        f'<li class="status-chip status-{escape(css_key)}">'
        f'<span class="status-dot"></span>'
        f"<span>{escape(label)}: {count}</span>"
        f"</li>"
    )


def render_project_card(project: Project, output_path: Path) -> str:
    tracker_href = relative_link(project.tracker_path, output_path)
    meta_pills = [
        f'<span class="meta-pill">Project status: {escape(project.status_raw)}</span>',
        f'<span class="meta-pill">{len(project.sprints)} sprint{"s" if len(project.sprints) != 1 else ""}</span>',
        f'<span class="meta-pill">Folder: {escape(project.slug)}</span>',
    ]
    if project.sprints:
        sprint_cards = "\n".join(render_sprint_card(project, sprint, output_path) for sprint in project.sprints)
    else:
        sprint_cards = '<div class="empty">No sprint index rows found in this tracker.</div>'
    return f"""
<article class="project-card">
  <div class="project-head">
    <div>
      <h2 class="project-title"><a href="{tracker_href}">{escape(project.title)}</a></h2>
      <p class="project-objective">{escape(project.objective)}</p>
      <div class="project-meta">
        {''.join(meta_pills)}
      </div>
    </div>
    <span class="status-chip status-{escape(project.status_key)}">
      <span class="status-dot"></span>
      <span>{escape(project.status_raw)}</span>
    </span>
  </div>
  <div class="sprint-grid">
    {sprint_cards}
  </div>
</article>
""".strip()


def render_sprint_card(project: Project, sprint: Sprint, output_path: Path) -> str:
    contract_exists = sprint.contract_path.exists()
    href = relative_link(sprint.contract_path, output_path) if contract_exists else "#"
    footer = f"{project.slug} / sprint {sprint.number}"
    return f"""
<article class="sprint-card">
  <div class="sprint-top">
    <div>
      <div class="sprint-label">{escape(sprint.slug)}</div>
      <h3 class="sprint-title"><a href="{href}">{escape(sprint.contract_title)}</a></h3>
    </div>
    <span class="status-chip status-{escape(sprint.status_key)}">
      <span class="status-dot"></span>
      <span>{escape(sprint.status_raw)}</span>
    </span>
  </div>
  <p class="sprint-summary">{escape(sprint.summary)}</p>
  <div class="sprint-footer">
    <span>{escape(footer)}</span>
    <span>{escape(sprint.contract_rel if sprint.contract_rel else "No contract link")}</span>
  </div>
</article>
""".strip()


def render_dashboard(projects: list[Project], output_path: Path) -> str:
    template = read_text(TEMPLATE_PATH)
    status_counts = build_status_counts(projects)
    sprint_count = sum(len(project.sprints) for project in projects)
    open_count = sum(1 for project in projects if project.status_key == "open")
    closed_count = sum(1 for project in projects if project.status_key == "closed")
    active_sprint_count = sum(
        1
        for project in projects
        for sprint in project.sprints
        if sprint.status_key in {"authored", "in-progress", "blocked", "stopped"}
    )
    done_sprint_count = status_counts.get("done", 0)
    rendered_statuses = [
        ("Open projects", open_count, "open"),
        ("Closed projects", closed_count, "closed"),
        ("Planned", status_counts.get("planned", 0), "planned"),
        ("Authored", status_counts.get("authored", 0), "authored"),
        ("In progress", status_counts.get("in-progress", 0), "in-progress"),
        ("Done", status_counts.get("done", 0), "done"),
        ("Stopped", status_counts.get("stopped", 0), "stopped"),
        ("Blocked", status_counts.get("blocked", 0), "blocked"),
    ]
    if status_counts.get("fallback", 0):
        rendered_statuses.append(("Other", status_counts["fallback"], "fallback"))
    status_chips = "\n".join(render_status_chip(label, count, css_key) for label, count, css_key in rendered_statuses)
    project_cards = "\n\n".join(render_project_card(project, output_path) for project in projects)
    generated_at = dt.datetime.now(dt.timezone.utc).astimezone().strftime("Generated %Y-%m-%d %H:%M %Z")
    replacements = {
        "{{GENERATED_AT}}": escape(generated_at),
        "{{SCORE_NAV_HREF}}": relative_link(output_path.parent / DEFAULT_SCORE_OUTPUT_NAME, output_path),
        "{{PROJECT_COUNT}}": str(len(projects)),
        "{{OPEN_COUNT}}": str(open_count),
        "{{CLOSED_COUNT}}": str(closed_count),
        "{{SPRINT_COUNT}}": str(sprint_count),
        "{{ACTIVE_SPRINT_COUNT}}": str(active_sprint_count),
        "{{DONE_SPRINT_COUNT}}": str(done_sprint_count),
        "{{BLOCKED_SPRINT_COUNT}}": str(status_counts.get("blocked", 0)),
        "{{AUTHORED_SPRINT_COUNT}}": str(status_counts.get("authored", 0)),
        "{{STATUS_CHIPS}}": status_chips,
        "{{PROJECT_CARDS}}": project_cards or '<div class="empty">No project trackers found under docs/projects.</div>',
    }
    for token, value in replacements.items():
        template = template.replace(token, value)
    return template


def format_score(value: float | None) -> str:
    if value is None:
        return "Not scored"
    if value.is_integer():
        return f"{int(value)}/5"
    return f"{value:.1f}/5"


def average(values: list[float]) -> float | None:
    if not values:
        return None
    return sum(values) / len(values)


def format_average(value: float | None) -> str:
    if value is None:
        return "—"
    return f"{value:.1f}"


def render_score_metric(label: str, value: str, meta: str) -> str:
    return f"""
<article class="metric">
  <div class="metric-label">{escape(label)}</div>
  <div class="metric-value">{escape(value)}</div>
  <div class="metric-meta">{escape(meta)}</div>
</article>
""".strip()


def svg_point(x: float, y: float, label: str, color: str) -> str:
    return f'<circle cx="{x:.2f}" cy="{y:.2f}" r="4.5" fill="{color}"><title>{escape(label)}</title></circle>'


def svg_path(points: list[tuple[float, float]], color: str) -> str:
    if not points:
        return ""
    if len(points) == 1:
        x, y = points[0]
        return f'<circle cx="{x:.2f}" cy="{y:.2f}" r="4.5" fill="{color}"></circle>'
    commands = [f"M {points[0][0]:.2f} {points[0][1]:.2f}"]
    commands.extend(f"L {x:.2f} {y:.2f}" for x, y in points[1:])
    return f'<path d="{" ".join(commands)}" fill="none" stroke="{color}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"></path>'


def render_trend_svg(records: list[ScoreRecord]) -> str:
    if not records:
        return '<div class="empty">No score-bearing BAKE sprint contracts found yet.</div>'
    width = 920
    height = 280
    left = 48
    right = 24
    top = 24
    bottom = 44
    plot_w = width - left - right
    plot_h = height - top - bottom
    denominator = max(1, len(records) - 1)

    def x_at(index: int) -> float:
        return left + (plot_w * index / denominator)

    def y_at(score: float) -> float:
        return top + ((5 - score) / 4) * plot_h

    series = [
        ("Pre", "Pre-BAKE codebase score", "#8b5e34"),
        ("Work", "Work score", "#2563eb"),
        ("Baked", "Baked-codebase score", "#15803d"),
    ]
    grid = []
    for score in range(1, 6):
        y = y_at(score)
        grid.append(f'<line x1="{left}" y1="{y:.2f}" x2="{width - right}" y2="{y:.2f}" class="grid-line"></line>')
        grid.append(f'<text x="14" y="{y + 4:.2f}" class="axis-label">{score}</text>')
    rendered_series = []
    rendered_points = []
    for short_label, score_label, color in series:
        points = []
        for index, record in enumerate(records):
            value = record.scores[score_label].value
            if value is None:
                continue
            x = x_at(index)
            y = y_at(value)
            points.append((x, y))
            rendered_points.append(svg_point(x, y, f"{record.sprint_slug}: {short_label} {value:g}/5", color))
        rendered_series.append(svg_path(points, color))
    x_labels = []
    for index, record in enumerate(records):
        x = x_at(index)
        label = record.sprint_number
        x_labels.append(f'<text x="{x:.2f}" y="{height - 14}" class="axis-label" text-anchor="middle">{escape(label)}</text>')
    legend = """
<g class="legend" transform="translate(610, 18)">
  <circle cx="0" cy="0" r="5" fill="#8b5e34"></circle><text x="12" y="4">Pre</text>
  <circle cx="70" cy="0" r="5" fill="#2563eb"></circle><text x="82" y="4">Work</text>
  <circle cx="150" cy="0" r="5" fill="#15803d"></circle><text x="162" y="4">Baked</text>
</g>
""".strip()
    return f"""
<svg class="trend-svg" viewBox="0 0 {width} {height}" role="img" aria-label="BAKE score trend chart">
  {''.join(grid)}
  {''.join(x_labels)}
  {legend}
  {''.join(rendered_series)}
  {''.join(rendered_points)}
</svg>
""".strip()


def render_improvement_bars(records: list[ScoreRecord]) -> str:
    scored = [record for record in records if record.improvement is not None]
    if not scored:
        return '<div class="empty">No pre-to-baked improvement values available yet.</div>'
    max_abs = max(1.0, max(abs(record.improvement or 0) for record in scored))
    bars = []
    for record in scored:
        improvement = record.improvement or 0
        width = abs(improvement) / max_abs * 100
        direction_class = "positive" if improvement >= 0 else "negative"
        bars.append(
            f"""
<div class="bar-row">
  <div class="bar-label">{escape(record.sprint_number)} · {escape(record.sprint_slug)}</div>
  <div class="bar-track"><span class="bar-fill {direction_class}" style="width: {width:.1f}%"></span></div>
  <div class="bar-value">{improvement:+.1f}</div>
</div>
""".strip()
        )
    return "\n".join(bars)


def render_delta_details(record: ScoreRecord) -> str:
    if not record.delta_rows:
        return '<div class="empty compact-empty">Code delta summary unavailable.</div>'
    items = []
    for row in DELTA_ROWS:
        value = record.delta_rows.get(row)
        if not value:
            continue
        items.append(
            f"""
<div class="delta-row">
  <dt>{escape(row)}</dt>
  <dd>{escape(value)}</dd>
</div>
""".strip()
        )
    return f"""
<details class="delta-details">
  <summary>Code-delta summary</summary>
  <dl>{''.join(items)}</dl>
</details>
""".strip()


def render_score_card(record: ScoreRecord, output_path: Path) -> str:
    href = relative_link(record.contract_path, output_path)
    improvement = record.improvement
    improvement_text = "n/a" if improvement is None else f"{improvement:+.1f}"
    net_judgment = record.delta_rows.get("Net judgment", "No net judgment recorded.")
    assumption = record.delta_rows.get("Assumption killed", "No assumption row recorded.")
    residual = record.delta_rows.get("Residual risk/debt", "No residual risk row recorded.")
    return f"""
<article class="score-card">
  <div class="score-card-head">
    <div>
      <div class="score-kicker">{escape(record.project_slug)} / sprint {escape(record.sprint_number)}</div>
      <h2><a href="{href}">{escape(record.sprint_title)}</a></h2>
      <p>{escape(record.sprint_summary)}</p>
    </div>
    <div class="score-triplet" aria-label="Score triplet">
      <span><b>{escape(format_score(record.pre_score))}</b><small>pre</small></span>
      <span><b>{escape(format_score(record.work_score))}</b><small>work</small></span>
      <span><b>{escape(format_score(record.baked_score))}</b><small>baked</small></span>
    </div>
  </div>
  <div class="insight-grid">
    <div><strong>Improvement</strong><span>{escape(improvement_text)}</span></div>
    <div><strong>Net judgment</strong><span>{escape(net_judgment)}</span></div>
    <div><strong>Assumption killed</strong><span>{escape(assumption)}</span></div>
    <div><strong>Residual risk</strong><span>{escape(residual)}</span></div>
  </div>
  {render_delta_details(record)}
</article>
""".strip()


def render_score_dashboard(projects: list[Project], output_path: Path) -> str:
    records = collect_score_records(projects)
    generated_at = dt.datetime.now(dt.timezone.utc).astimezone().strftime("Generated %Y-%m-%d %H:%M %Z")
    pre_values = [record.pre_score for record in records if record.pre_score is not None]
    work_values = [record.work_score for record in records if record.work_score is not None]
    baked_values = [record.baked_score for record in records if record.baked_score is not None]
    improvements = [record.improvement for record in records if record.improvement is not None]
    metric_cards = "\n".join(
        [
            render_score_metric("Scored sprints", str(len(records)), "Contracts with any BAKE score"),
            render_score_metric("Avg pre", format_average(average(pre_values)), "Incoming codebase quality"),
            render_score_metric("Avg work", format_average(average(work_values)), "Sprint execution quality"),
            render_score_metric("Avg baked", format_average(average(baked_values)), "Resulting codebase quality"),
            render_score_metric("Avg improvement", format_average(average(improvements)), "Baked minus pre"),
        ]
    )
    trend_note = (
        "Trend history begins after more scored BAKE coding sprints close."
        if len(records) == 1
        else "Scores are ordered by project tracker and sprint order."
    )
    score_cards = "\n".join(render_score_card(record, output_path) for record in records)
    if not score_cards:
        score_cards = '<div class="empty">No scored BAKE sprint closeouts found. Add the three score labels to a BAKE coding sprint closeout to populate this page.</div>'
    index_href = relative_link(output_path.parent / "index.html", output_path)
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>BAKE Score Trends</title>
  <style>
    :root {{
      --bg: #f6f4ee;
      --ink: #202426;
      --muted: #5e686f;
      --card: #fffdf8;
      --card-soft: #f8f2e8;
      --line: rgba(32, 36, 38, 0.12);
      --green: #15803d;
      --blue: #2563eb;
      --amber: #8b5e34;
      --red: #b91c1c;
      --shadow: 0 16px 44px rgba(54, 48, 36, 0.10);
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      color: var(--ink);
      background: linear-gradient(180deg, #faf7ef 0%, #ece8df 100%);
      min-height: 100vh;
    }}
    a {{ color: inherit; }}
    .shell {{ width: min(1320px, calc(100vw - 32px)); margin: 0 auto; padding: 24px 0 64px; }}
    .nav {{ display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 18px; }}
    .nav a {{ text-decoration: none; border: 1px solid var(--line); border-radius: 999px; padding: 8px 12px; background: rgba(255,255,255,0.72); color: var(--muted); }}
    .nav a.active {{ color: var(--ink); background: var(--card); box-shadow: var(--shadow); }}
    .hero, .panel, .score-card {{ background: var(--card); border: 1px solid var(--line); border-radius: 18px; box-shadow: var(--shadow); }}
    .hero {{ padding: clamp(18px, 3vw, 30px); margin-bottom: 18px; }}
    .eyebrow {{ text-transform: uppercase; letter-spacing: .12em; font-size: .76rem; color: var(--muted); margin-bottom: 10px; }}
    h1 {{ margin: 0 0 8px; font-size: clamp(2rem, 4vw, 3.4rem); line-height: 1.02; letter-spacing: 0; }}
    .hero p, .generated-at {{ color: var(--muted); max-width: 78ch; line-height: 1.6; }}
    .generated-at {{ font-size: .92rem; margin-top: 12px; }}
    .metrics {{ display: grid; grid-template-columns: repeat(5, minmax(0,1fr)); gap: 12px; margin-bottom: 18px; }}
    .metric {{ background: var(--card); border: 1px solid var(--line); border-radius: 16px; padding: 16px; min-width: 0; }}
    .metric-label {{ font-size: .76rem; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); margin-bottom: 8px; }}
    .metric-value {{ font-size: clamp(1.7rem, 3vw, 2.4rem); line-height: 1; }}
    .metric-meta {{ color: var(--muted); margin-top: 8px; font-size: .9rem; line-height: 1.35; }}
    .panel {{ padding: 18px; margin-bottom: 18px; overflow: hidden; }}
    .panel h2 {{ margin: 0 0 6px; font-size: 1.2rem; }}
    .panel-note {{ margin: 0 0 12px; color: var(--muted); }}
    .trend-svg {{ width: 100%; height: auto; display: block; background: var(--card-soft); border: 1px solid var(--line); border-radius: 14px; }}
    .grid-line {{ stroke: rgba(32,36,38,.14); stroke-width: 1; }}
    .axis-label, .legend text {{ fill: var(--muted); font-size: 12px; }}
    .bars {{ display: grid; gap: 10px; }}
    .bar-row {{ display: grid; grid-template-columns: minmax(150px, 280px) minmax(120px, 1fr) 56px; gap: 10px; align-items: center; }}
    .bar-label, .bar-value {{ color: var(--muted); font-size: .9rem; overflow-wrap: anywhere; }}
    .bar-track {{ height: 14px; background: var(--card-soft); border: 1px solid var(--line); border-radius: 999px; overflow: hidden; }}
    .bar-fill {{ display: block; height: 100%; border-radius: inherit; }}
    .bar-fill.positive {{ background: var(--green); }}
    .bar-fill.negative {{ background: var(--red); }}
    .score-list {{ display: grid; gap: 14px; }}
    .score-card {{ padding: 18px; }}
    .score-card-head {{ display: grid; grid-template-columns: minmax(0,1fr) auto; gap: 18px; align-items: start; }}
    .score-kicker {{ color: var(--muted); text-transform: uppercase; letter-spacing: .08em; font-size: .76rem; margin-bottom: 7px; }}
    .score-card h2 {{ margin: 0 0 8px; line-height: 1.25; font-size: 1.18rem; overflow-wrap: anywhere; }}
    .score-card h2 a {{ text-decoration: none; border-bottom: 1px solid transparent; }}
    .score-card h2 a:hover {{ border-bottom-color: currentColor; }}
    .score-card p {{ margin: 0; color: var(--muted); line-height: 1.55; overflow-wrap: anywhere; }}
    .score-triplet {{ display: grid; grid-template-columns: repeat(3, 82px); gap: 8px; }}
    .score-triplet span {{ border: 1px solid var(--line); border-radius: 14px; padding: 10px; background: var(--card-soft); text-align: center; }}
    .score-triplet b {{ display: block; font-size: 1.25rem; }}
    .score-triplet small {{ color: var(--muted); text-transform: uppercase; font-size: .72rem; letter-spacing: .08em; }}
    .insight-grid {{ display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 10px; margin-top: 14px; }}
    .insight-grid div {{ background: var(--card-soft); border: 1px solid var(--line); border-radius: 14px; padding: 12px; min-width: 0; }}
    .insight-grid strong {{ display: block; font-size: .78rem; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); margin-bottom: 6px; }}
    .insight-grid span {{ line-height: 1.45; overflow-wrap: anywhere; }}
    .delta-details {{ margin-top: 12px; border: 1px solid var(--line); border-radius: 14px; padding: 10px 12px; background: rgba(255,255,255,.52); }}
    .delta-details summary {{ cursor: pointer; font-weight: 700; }}
    .delta-details dl {{ margin: 12px 0 0; display: grid; gap: 8px; }}
    .delta-row {{ display: grid; grid-template-columns: 180px minmax(0,1fr); gap: 12px; }}
    .delta-row dt {{ color: var(--muted); font-weight: 700; }}
    .delta-row dd {{ margin: 0; line-height: 1.45; overflow-wrap: anywhere; }}
    .empty {{ padding: 16px; border: 1px dashed var(--line); border-radius: 14px; color: var(--muted); background: rgba(255,255,255,.55); }}
    .compact-empty {{ margin-top: 12px; }}
    @media (max-width: 980px) {{
      .metrics {{ grid-template-columns: repeat(2, minmax(0,1fr)); }}
      .score-card-head, .insight-grid {{ grid-template-columns: 1fr; }}
      .score-triplet {{ grid-template-columns: repeat(3, minmax(0,1fr)); }}
      .bar-row {{ grid-template-columns: 1fr; }}
      .delta-row {{ grid-template-columns: 1fr; gap: 4px; }}
    }}
    @media (max-width: 520px) {{
      .metrics {{ grid-template-columns: 1fr; }}
      .score-triplet {{ grid-template-columns: 1fr; }}
    }}
  </style>
</head>
<body>
  <main class="shell">
    <nav class="nav" aria-label="Dashboard navigation">
      <a href="{index_href}">Project Dashboard</a>
      <a class="active" href="bake-scores.html">BAKE Score Trends</a>
    </nav>
    <section class="hero">
      <div class="eyebrow">BAKE score trends</div>
      <h1>Code quality movement, not just task status.</h1>
      <p>This page collects BAKE closeout score triplets and code-delta summaries so each sprint shows where the code started, how well the work was done, and where the baked codebase landed.</p>
      <div class="generated-at">{escape(generated_at)}</div>
    </section>
    <section class="metrics">{metric_cards}</section>
    <section class="panel">
      <h2>Score Trend</h2>
      <p class="panel-note">{escape(trend_note)}</p>
      {render_trend_svg(records)}
    </section>
    <section class="panel">
      <h2>Pre-to-Baked Improvement</h2>
      <p class="panel-note">Positive bars mean the resulting codebase score improved over the incoming slice.</p>
      <div class="bars">{render_improvement_bars(records)}</div>
    </section>
    <section class="score-list">{score_cards}</section>
  </main>
</body>
</html>
"""


def write_dashboard(output_path: Path = DEFAULT_OUTPUT) -> Path:
    resolved_output = output_path.resolve()
    score_output = resolved_output.parent / DEFAULT_SCORE_OUTPUT_NAME
    projects = load_projects()
    html_text = render_dashboard(projects, resolved_output)
    score_html = render_score_dashboard(projects, score_output)
    resolved_output.parent.mkdir(parents=True, exist_ok=True)
    resolved_output.write_text(html_text, encoding="utf-8")
    score_output.write_text(score_html, encoding="utf-8")
    print(f"Wrote dashboard to {resolved_output}")
    print(f"Wrote BAKE score dashboard to {score_output}")
    print(f"Projects: {len(projects)}")
    print(f"Sprints: {sum(len(project.sprints) for project in projects)}")
    print(f"Scored sprints: {len(collect_score_records(projects))}")
    return resolved_output


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Render the harness project dashboard HTML.")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="Output HTML path")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    write_dashboard(args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
