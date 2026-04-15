#!/usr/bin/env bash
set -euo pipefail

# changelog.sh — Render CHANGELOG.md into a polished CHANGELOG.html.
# CHANGELOG.md is the source of truth (hand-curated, Chinese).
# This script reads it and produces an HTML version for PDF/screenshot export.
#
# Usage:
#   bash scripts/changelog.sh
#   bash scripts/changelog.sh --input /path/to/CHANGELOG.md
#   bash scripts/changelog.sh --output /tmp/changelog.html

REPO_ROOT="$(git rev-parse --show-toplevel)"
INPUT="$REPO_ROOT/CHANGELOG.md"
OUTPUT="$REPO_ROOT/CHANGELOG.html"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --input)  INPUT="$2";  shift 2 ;;
        --output) OUTPUT="$2"; shift 2 ;;
        *)        echo "Unknown option: $1" >&2; exit 1 ;;
    esac
done

if [[ ! -f "$INPUT" ]]; then
    echo "Error: $INPUT not found" >&2; exit 1
fi

# ── HTML header ──────────────────────────────────────────────────────────────
cat > "$OUTPUT" <<HTML
<!DOCTYPE html>
<html lang="zh">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>更新日志</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

  * { box-sizing: border-box; margin: 0; padding: 0; }

  body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    background: #f0f2f5;
    color: #1a1a2e;
    min-height: 100vh;
    padding: 48px 24px;
  }

  .page { max-width: 760px; margin: 0 auto; }

  .page-header {
    text-align: center;
    margin-bottom: 48px;
  }
  .page-header h1 {
    font-size: 2.25rem;
    font-weight: 700;
    letter-spacing: -0.03em;
    color: #0f0f1a;
    margin-bottom: 8px;
  }
  .page-header .subtitle {
    font-size: 0.95rem;
    color: #6b7280;
  }
  .page-header .generated {
    display: inline-block;
    margin-top: 12px;
    font-size: 0.78rem;
    color: #9ca3af;
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 20px;
    padding: 4px 14px;
  }

  /* Section headings (## …) */
  .section-title {
    font-size: 1.1rem;
    font-weight: 700;
    color: #111827;
    margin: 40px 0 20px;
    padding-bottom: 10px;
    border-bottom: 2px solid #e5e7eb;
  }

  /* Date sub-headings (### …) */
  .date-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 28px 0 12px;
  }
  .date-header h3 {
    font-size: 0.78rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #9ca3af;
    white-space: nowrap;
  }
  .date-header::after {
    content: '';
    flex: 1;
    height: 1px;
    background: #e5e7eb;
  }

  .entries { display: flex; flex-direction: column; gap: 10px; }

  .entry {
    background: #fff;
    border-radius: 12px;
    padding: 16px 20px;
    display: flex;
    align-items: flex-start;
    gap: 14px;
    border: 1px solid #f0f0f5;
    box-shadow: 0 1px 3px rgba(0,0,0,0.04);
    transition: box-shadow 0.15s ease;
  }
  .entry:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }

  .badge {
    flex-shrink: 0;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 3px 9px;
    border-radius: 6px;
    margin-top: 2px;
  }
  .badge-新增 { background: #eff6ff; color: #2563eb; }
  .badge-修复 { background: #fff7ed; color: #ea580c; }
  .badge-发布 { background: #f0fdf4; color: #16a34a; }
  .badge-other { background: #f3f4f6; color: #6b7280; }

  .entry-msg {
    font-size: 0.92rem;
    color: #374151;
    line-height: 1.6;
  }

  hr.divider {
    border: none;
    border-top: 2px dashed #e5e7eb;
    margin: 40px 0;
  }

  .print-btn {
    display: block;
    margin: 48px auto 0;
    padding: 12px 36px;
    background: #1a1a2e;
    color: #fff;
    border: none;
    border-radius: 10px;
    font-size: 0.9rem;
    font-weight: 500;
    cursor: pointer;
    letter-spacing: 0.02em;
    transition: background 0.15s;
  }
  .print-btn:hover { background: #2d2d50; }

  @media print {
    body { background: #fff; padding: 20px; }
    .print-btn { display: none; }
    .entry { box-shadow: none; border: 1px solid #e5e7eb; break-inside: avoid; }
    .page { max-width: 100%; }
  }
</style>
</head>
<body>
<div class="page">
  <div class="page-header">
    <h1>更新日志</h1>
    <p class="subtitle">产品功能与修复记录</p>
    <span class="generated">生成于 $(date '+%Y年%m月%d日')</span>
  </div>
HTML

# ── Parse CHANGELOG.md line by line ──────────────────────────────────────────
in_entries=false   # true when we're inside a list of entries under a date header
current_section="" # track open section div

close_entries() {
    if $in_entries; then
        echo "  </div>" >> "$OUTPUT"   # close .entries
        in_entries=false
    fi
}

re_badge='^\-[[:space:]]*\*?\*?\[([^]]+)\]\*?\*?[[:space:]]*[-—]*[[:space:]]*(.*)'
re_bold_entry='^\-[[:space:]]*\*\*\[([^]]+)\][[:space:]]*([^*]*)\*\*[[:space:]]*[—-]+[[:space:]]*(.*)'

while IFS= read -r line; do
    # H1 — skip (we render our own page header)
    if [[ "$line" =~ ^#[[:space:]] ]]; then
        continue

    # Blockquote / hr — render as divider
    elif [[ "$line" =~ ^(\>|---) ]]; then
        close_entries
        echo "  <hr class=\"divider\">" >> "$OUTPUT"

    # H2 section title (## …)
    elif [[ "$line" =~ ^##[[:space:]]+(.*) ]]; then
        close_entries
        title="${BASH_REMATCH[1]}"
        title="${title//&/&amp;}"
        echo "  <div class=\"section-title\">${title}</div>" >> "$OUTPUT"

    # H3 date header (### …)
    elif [[ "$line" =~ ^###[[:space:]]+(.*) ]]; then
        close_entries
        date="${BASH_REMATCH[1]}"
        date="${date//&/&amp;}"
        echo "  <div class=\"date-header\"><h3>${date}</h3></div>" >> "$OUTPUT"
        echo "  <div class=\"entries\">" >> "$OUTPUT"
        in_entries=true

    # List entry with bold badge: - **[新增] Title** — desc
    elif [[ "$line" =~ ^\-[[:space:]]*\*\*\[([^]]+)\][[:space:]]*([^*]*)\*\*[[:space:]]*[—-]+[[:space:]]*(.*) ]]; then
        badge="${BASH_REMATCH[1]}"
        title="${BASH_REMATCH[2]}"
        desc="${BASH_REMATCH[3]}"
        badge="${badge//&/&amp;}"
        title="${title//&/&amp;}"
        desc="${desc//&/&amp;}"
        badge_class="badge-${badge}"
        if ! [[ "$badge" =~ ^(新增|修复|发布)$ ]]; then badge_class="badge-other"; fi
        if ! $in_entries; then
            echo "  <div class=\"entries\">" >> "$OUTPUT"
            in_entries=true
        fi
        echo "      <div class=\"entry\">" >> "$OUTPUT"
        echo "        <span class=\"badge ${badge_class}\">${badge}</span>" >> "$OUTPUT"
        echo "        <span class=\"entry-msg\"><strong>${title}</strong> — ${desc}</span>" >> "$OUTPUT"
        echo "      </div>" >> "$OUTPUT"

    # List entry with plain badge: - [新增] message
    elif [[ "$line" =~ ^\-[[:space:]]*\[([^]]+)\][[:space:]]*(.*) ]]; then
        badge="${BASH_REMATCH[1]}"
        msg="${BASH_REMATCH[2]}"
        badge="${badge//&/&amp;}"
        msg="${msg//&/&amp;}"
        badge_class="badge-${badge}"
        if ! [[ "$badge" =~ ^(新增|修复|发布)$ ]]; then badge_class="badge-other"; fi
        if ! $in_entries; then
            echo "  <div class=\"entries\">" >> "$OUTPUT"
            in_entries=true
        fi
        echo "      <div class=\"entry\">" >> "$OUTPUT"
        echo "        <span class=\"badge ${badge_class}\">${badge}</span>" >> "$OUTPUT"
        echo "        <span class=\"entry-msg\">${msg}</span>" >> "$OUTPUT"
        echo "      </div>" >> "$OUTPUT"

    # Empty line — ignore
    elif [[ -z "$line" ]]; then
        continue
    fi

done < "$INPUT"

close_entries

# ── HTML footer ───────────────────────────────────────────────────────────────
cat >> "$OUTPUT" <<'HTML'
  <button class="print-btn" onclick="window.print()">导出 PDF / 打印</button>
</div>
</body>
</html>
HTML

echo "Generated ${OUTPUT}"
