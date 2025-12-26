#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Pause-marker experiment tool (Phase 1, non-LLM)

Input:  raw transcription JSON (xfyun/tingwu-like)
Output: two comparable renderings:
  1) EXPERIMENT: same dialogue text + inline suspicious markers based on gap_ms
  2) CONTROL:    same dialogue text without markers

Markers are purely deterministic and driven by pause classes (P0..P4) derived from gap_ms.
"""

from __future__ import annotations

import argparse
import dataclasses
import html
import json
import re
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple


# -----------------------------
# Data model
# -----------------------------

@dataclasses.dataclass(frozen=True)
class Word:
    start: int
    end: int
    text: str


@dataclasses.dataclass
class RenderToken:
    text: str
    start: int
    end: int
    para_idx: int
    tok_idx: int


@dataclasses.dataclass
class Paragraph:
    speaker_id: str
    words: List[Word]


# -----------------------------
# Parsing
# -----------------------------

def load_paragraphs(json_path: Path) -> List[Paragraph]:
    data = json.loads(json_path.read_text(encoding="utf-8"))
    tx = (data or {}).get("Transcription") or {}
    paras = tx.get("Paragraphs") or []

    out: List[Paragraph] = []
    for p in paras:
        speaker_id = str(p.get("SpeakerId", ""))
        words_raw = p.get("Words") or []
        words: List[Word] = []
        for w in words_raw:
            try:
                start = int(w.get("Start"))
                end = int(w.get("End"))
            except Exception:
                continue
            text = str(w.get("Text", ""))
            if text == "":
                continue
            words.append(Word(start=start, end=end, text=text))
        if not words:
            continue
        out.append(Paragraph(speaker_id=speaker_id, words=words))
    return out


# -----------------------------
# Cleaning / normalization
# -----------------------------

_WS_RE = re.compile(r"\s+")

def normalize_text(s: str) -> str:
    s = s.replace("\u3000", " ")
    s = _WS_RE.sub(" ", s)
    return s.strip()


_ASCII_CORE_RE = re.compile(r"^([A-Za-z0-9]+)(.*)$", re.DOTALL)

def split_ascii_core(text: str) -> Tuple[Optional[str], str]:
    m = _ASCII_CORE_RE.match(text)
    if not m:
        return None, text
    return m.group(1), (m.group(2) or "")


def merge_spelled_out_ascii(words: List[Word]) -> List[Word]:
    """
    Merge sequences like: "A" "P" "P，" -> "APP，"
    Also works for "H" "T" "T" "P" -> "HTTP"
    """
    out: List[Word] = []
    buf: List[Tuple[Word, str, str]] = []  # (word, core, suffix)

    def flush():
        nonlocal buf
        if not buf:
            return
        start = buf[0][0].start
        end = buf[-1][0].end
        core_join = "".join([b[1] for b in buf])
        suffix = buf[-1][2]
        out.append(Word(start=start, end=end, text=core_join + suffix))
        buf = []

    for w in words:
        raw = normalize_text(w.text)
        if raw == "":
            continue

        core, suffix = split_ascii_core(raw)
        is_merge_candidate = core is not None and len(core) == 1

        if is_merge_candidate:
            buf.append((Word(w.start, w.end, raw), core, suffix))
        else:
            flush()
            out.append(Word(start=w.start, end=w.end, text=raw))

    flush()
    return out


def paragraph_to_tokens(words: List[Word]) -> List[Word]:
    # Phase-1 cleaning is intentionally conservative and deterministic.
    return merge_spelled_out_ascii(words)


# -----------------------------
# Pause classes / markers
# -----------------------------

def parse_thresholds(s: str) -> List[int]:
    parts = [p.strip() for p in s.split(",") if p.strip() != ""]
    if len(parts) != 4:
        raise ValueError("pause thresholds must be 4 comma-separated integers, e.g. 120,300,800,2000")
    vals = [int(x) for x in parts]
    if any(v < 0 for v in vals):
        raise ValueError("pause thresholds must be non-negative")
    if vals != sorted(vals):
        raise ValueError("pause thresholds must be non-decreasing")
    return vals


def classify_pause(gap_ms: int, t: List[int]) -> int:
    if gap_ms < t[0]:
        return 0
    if gap_ms < t[1]:
        return 1
    if gap_ms < t[2]:
        return 2
    if gap_ms < t[3]:
        return 3
    return 4


_ANSI = {
    0: "\033[90m",  # gray
    1: "\033[36m",  # cyan
    2: "\033[33m",  # yellow
    3: "\033[35m",  # magenta
    4: "\033[31m",  # red
}
_ANSI_RESET = "\033[0m"

_HTML_COLORS = {
    0: "#6b7280",
    1: "#0891b2",
    2: "#ca8a04",
    3: "#a855f7",
    4: "#ef4444",
}

def format_marker(idx: int, pause_class: int, gap_ms: int) -> str:
    return f"sus{idx}_P{pause_class} <{gap_ms}>"


# -----------------------------
# Rendering
# -----------------------------

def build_render_model(paragraphs: List[Paragraph]) -> Tuple[List[List[Word]], List[RenderToken]]:
    per_para: List[List[Word]] = []
    global_tokens: List[RenderToken] = []

    for pi, p in enumerate(paragraphs):
        cleaned = paragraph_to_tokens(p.words)
        per_para.append(cleaned)
        for ti, w in enumerate(cleaned):
            global_tokens.append(RenderToken(text=w.text, start=w.start, end=w.end, para_idx=pi, tok_idx=ti))

    global_tokens.sort(key=lambda x: (x.start, x.end, x.para_idx, x.tok_idx))
    return per_para, global_tokens


def attach_boundary_markers(
    paragraphs: List[Paragraph],
    per_para_words: List[List[Word]],
    boundary_short_ms: int,
) -> Dict[Tuple[int, int], List[Tuple[int, int, int]]]:
    """
    Mark suspicious gaps only at speaker-turn boundaries (S1↔S2).
    
    Only marks boundaries where SpeakerId[i] != SpeakerId[i+1] and gap_ms < boundary_short_ms.
    All markers use constant P0 class.
    """
    markers: Dict[Tuple[int, int], List[Tuple[int, int, int]]] = {}
    sus_idx = 1

    for pi in range(len(paragraphs) - 1):
        # Only mark boundaries where speaker changes
        if paragraphs[pi].speaker_id == paragraphs[pi + 1].speaker_id:
            continue
        
        # Get last word of current paragraph
        para_a_words = per_para_words[pi]
        if not para_a_words:
            continue
        a_end = para_a_words[-1].end
        
        # Get first word of next paragraph
        para_b_words = per_para_words[pi + 1]
        if not para_b_words:
            continue
        b_start = para_b_words[0].start
        
        # Compute boundary gap
        gap_ms = b_start - a_end
        if gap_ms < 0:
            gap_ms = 0  # Clamp negative gaps (overlap) to 0
        
        # Mark if gap is suspiciously short
        if gap_ms < boundary_short_ms:
            # Attach marker to last token of preceding paragraph
            last_tok_idx = len(para_a_words) - 1
            key = (pi, last_tok_idx)
            markers.setdefault(key, []).append((sus_idx, 0, gap_ms))  # pcls=0 (constant)
            sus_idx += 1

    return markers


def render_dialogues(
    paragraphs: List[Paragraph],
    per_para_words: List[List[Word]],
    markers: Optional[Dict[Tuple[int, int], List[Tuple[int, int, int]]]],
    color_mode: str,
    speaker_prefix: str,
) -> str:
    lines: List[str] = []
    for pi, p in enumerate(paragraphs):
        words = per_para_words[pi]
        parts: List[str] = []
        for ti, w in enumerate(words):
            parts.append(w.text)
            if markers:
                for (idx, pcls, gap) in (markers.get((pi, ti)) or []):
                    marker_text = format_marker(idx, pcls, gap)
                    if color_mode == "ansi":
                        marker_text = f"{_ANSI.get(pcls, '')}{marker_text}{_ANSI_RESET}"
                    elif color_mode == "html":
                        c = _HTML_COLORS.get(pcls, "#111827")
                        marker_text = f'<span style="color:{c}; font-weight:600">{html.escape(marker_text)}</span>'
                    parts.append(" " + marker_text)

        body = "".join(parts)
        speaker = f"{speaker_prefix}{p.speaker_id}" if p.speaker_id != "" else f"{speaker_prefix}?"
        if color_mode == "html":
            lines.append(f"{html.escape(speaker)}: {body}")
        else:
            lines.append(f"{speaker}: {normalize_text(body)}")
    return "\n".join(lines)


def wrap_html(experiment_body: str, control_body: str, title: str) -> str:
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>{html.escape(title)}</title>
<style>
  body {{ font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; margin: 16px; }}
  .grid {{ display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }}
  .panel {{ border: 1px solid #e5e7eb; border-radius: 8px; padding: 12px; }}
  .h {{ font-weight: 700; margin-bottom: 8px; }}
  pre {{ white-space: pre-wrap; word-wrap: break-word; margin: 0; }}
</style>
</head>
<body>
<div class="grid">
  <div class="panel">
    <div class="h">EXPERIMENT</div>
    <pre>{experiment_body}</pre>
  </div>
  <div class="panel">
    <div class="h">CONTROL</div>
    <pre>{control_body}</pre>
  </div>
</div>
</body>
</html>"""


# -----------------------------
# CLI
# -----------------------------

def main(argv: Optional[List[str]] = None) -> int:
    ap = argparse.ArgumentParser(
        prog="pause_marker",
        description="Generate control + experiment dialogue renderings with inline pause markers (Phase 1, non-LLM).",
    )
    ap.add_argument("json_path", type=Path, help="Path to raw transcription JSON dump.")
    ap.add_argument("--boundary-short-ms", type=int, default=250, help="Mark boundary as suspicious if gap_ms < this threshold (default: 250)")
    ap.add_argument("--pause-thresholds", default="120,300,800,2000", help="[Deprecated] Not used in boundary-only mode")
    ap.add_argument("--mark-min-class", type=int, default=2, help="[Deprecated] Not used in boundary-only mode")
    ap.add_argument("--color", choices=["none", "ansi", "html"], default="ansi")
    ap.add_argument("--speaker-prefix", default="S")
    ap.add_argument("--out-dir", type=Path, default=None)
    ap.add_argument("--title", default="Pause Marker Experiment")
    args = ap.parse_args(argv)

    if args.boundary_short_ms < 0:
        raise ValueError("--boundary-short-ms must be non-negative")

    paragraphs = load_paragraphs(args.json_path)
    if not paragraphs:
        print("No paragraphs/words found in input JSON.", file=sys.stderr)
        return 2

    per_para_words, global_tokens = build_render_model(paragraphs)
    marker_map = attach_boundary_markers(paragraphs, per_para_words, args.boundary_short_ms)

    control_text = render_dialogues(paragraphs, per_para_words, markers=None, color_mode="none", speaker_prefix=args.speaker_prefix)

    if args.color == "html":
        exp_html = render_dialogues(paragraphs, per_para_words, markers=marker_map, color_mode="html", speaker_prefix=args.speaker_prefix)
        ctl_html = render_dialogues(paragraphs, per_para_words, markers=None, color_mode="html", speaker_prefix=args.speaker_prefix)
        doc = wrap_html(exp_html, ctl_html, args.title)
        if args.out_dir:
            args.out_dir.mkdir(parents=True, exist_ok=True)
            out_path = args.out_dir / "pause_marker.html"
            out_path.write_text(doc, encoding="utf-8")
            print(f"Wrote: {out_path}")
        else:
            print(doc)
        return 0

    experiment_text = render_dialogues(paragraphs, per_para_words, markers=marker_map, color_mode=args.color, speaker_prefix=args.speaker_prefix)
    payload = "\n".join(["=== EXPERIMENT ===", experiment_text, "", "=== CONTROL ===", control_text])
    print(payload)

    if args.out_dir:
        args.out_dir.mkdir(parents=True, exist_ok=True)
        exp_path = args.out_dir / "experiment.txt"
        ctl_path = args.out_dir / "control.txt"
        exp_plain = render_dialogues(paragraphs, per_para_words, markers=marker_map, color_mode="none", speaker_prefix=args.speaker_prefix)
        exp_path.write_text(exp_plain, encoding="utf-8")
        ctl_path.write_text(control_text, encoding="utf-8")
        print(f"\nWrote: {exp_path}\nWrote: {ctl_path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
