# Pause Marker Tool

Phase 1 non-LLM pause marker experiment tool for analyzing transcription gaps.

## Purpose

This tool generates two comparable dialogue renderings from a raw transcription JSON:
- **Experiment**: Same dialogue text with inline suspicious markers based on pause gaps
- **Control**: Clean dialogue text without markers

The markers help identify potential transcription issues (drift, merged speakers, dropped clauses, segmentation problems) by highlighting suspicious pauses in the conversation flow.

## Features

- **Deterministic pause classification**: P0-P4 based on configurable thresholds
- **Inline suspicious markers**: `sus{n}_P{p} <{gap_ms}>` format
- **ASCII merging**: Automatically merges spelled-out sequences (e.g., `A P P` → `APP`, `H T T P` → `HTTP`)
- **Multiple output formats**: ANSI colors for terminal, HTML for side-by-side viewing, or plain text
- **Speaker preservation**: Respects original `SpeakerId` and paragraph boundaries exactly
- **Deterministic output**: Same input + parameters = same output (diff-friendly)

## Installation

The script uses only Python standard library modules:
- `json`, `argparse`, `dataclasses`, `re`, `html`, `pathlib`, `sys`

**Requirements**: Python 3.7+ (for `dataclasses` and type hints)

No external dependencies needed.

## Usage

### Basic Command

```bash
python3 pause_marker.py <json_path> [options]
```

### Example with Sample File

```bash
# Terminal output with ANSI colors (default)
python3 pause_marker.py tingwu_62d4da5e48404523ba8765d9a4f7fc3e_session-7826fe23-578d-466c-8a44-cb6c360f4c3f_20251224_184205350.raw.json \
  --pause-thresholds 120,300,800,2000 \
  --mark-min-class 2 \
  --color ansi
```

### Generate HTML Side-by-Side View

```bash
python3 pause_marker.py tingwu_62d4da5e48404523ba8765d9a4f7fc3e_session-7826fe23-578d-466c-8a44-cb6c360f4c3f_20251224_184205350.raw.json \
  --pause-thresholds 120,300,800,2000 \
  --mark-min-class 1 \
  --color html \
  --out-dir out/
```

This creates `out/pause_marker.html` with side-by-side panels.

### Generate Plain Text Files

```bash
python3 pause_marker.py tingwu_62d4da5e48404523ba8765d9a4f7fc3e_session-7826fe23-578d-466c-8a44-cb6c360f4c3f_20251224_184205350.raw.json \
  --pause-thresholds 120,300,800,2000 \
  --mark-min-class 2 \
  --color none \
  --out-dir out/
```

This creates:
- `out/experiment.txt` - With markers
- `out/control.txt` - Without markers

## Parameters

### Required

- `json_path`: Path to raw transcription JSON dump (Tingwu/XFyun format)

### Optional

- `--pause-thresholds` (default: `120,300,800,2000`): Four comma-separated integers defining pause class boundaries in milliseconds
  - P0: `< 120ms`
  - P1: `120-299ms`
  - P2: `300-799ms`
  - P3: `800-1999ms`
  - P4: `>= 2000ms`

- `--mark-min-class` (default: `2`): Minimum pause class to mark (0-4). Only pauses at or above this class will generate markers.

- `--color` (default: `ansi`): Output color mode
  - `ansi`: Terminal colors (P0-P4 mapped to distinct colors)
  - `html`: HTML output with colored markers (side-by-side panels)
  - `none`: Plain text, no colors

- `--speaker-prefix` (default: `S`): Prefix for speaker labels (e.g., `S1`, `S2`)

- `--out-dir`: Optional output directory. If specified:
  - HTML mode: writes `pause_marker.html`
  - Other modes: writes `experiment.txt` and `control.txt`

- `--title` (default: `Pause Marker Experiment`): Title for HTML output

## Output Format

### Marker Format

Markers appear inline after the token where the pause occurred:
```
sus1_P3 <925>
```

- `sus{n}`: Sequential suspicious point index
- `P{p}`: Pause class (0-4)
- `<{gap_ms}>`: Actual gap in milliseconds

### Example Output

**Control (baseline):**
```
S1: 我不打开这个APP，APP我刚才已经退掉了，我估计他就没有办法进行这个传输，就是后台那除非他在后台运行。
S2: 对他如果是APP，他就可以在后台运行。
S1: 我已经关掉了，它，连后台都没有。
```

**Experiment (with markers):**
```
S1: 我不打开这个APP， sus1_P1 <189> APP我刚才已经退掉了，我估计他就没有办法进行这个传输，就是后台那除非他在后台运行。
S2: 对他如果是APP，他就可以在后台运行。 sus2_P3 <925>
S1: 我已经关掉了，它，连后台都没有。
```

## Input JSON Format

The script expects JSON with this structure:

```json
{
  "TaskId": "...",
  "Transcription": {
    "AudioInfo": {...},
    "Paragraphs": [
      {
        "ParagraphId": "...",
        "SpeakerId": "1",
        "Words": [
          {
            "Id": 10,
            "Start": 2420,
            "End": 2978,
            "Text": "我不"
          },
          ...
        ]
      },
      ...
    ]
  }
}
```

## How It Works

1. **Load & Parse**: Reads JSON and extracts paragraphs with word timestamps
2. **Clean**: Normalizes whitespace and merges ASCII sequences (`A P P` → `APP`)
3. **Build Render Model**: Creates chronological token stream across all paragraphs
4. **Detect Gaps**: Computes `gap_ms = next.start - current.end` for adjacent tokens
5. **Classify Pauses**: Maps gaps to P0-P4 based on thresholds
6. **Attach Markers**: Inserts inline markers for gaps meeting `--mark-min-class`
7. **Render**: Produces two outputs (with/without markers) in requested format

## Phase 1 Goal

Validate the signal before any LLM work. If markers reliably cluster around true problem regions, Phase 2 can use these markers to constrain "surgical reasoning" to small ranges.

## Notes

- Markers are not limited to dialogue ends; they flag suspicious gaps anywhere (mid-sentence, between phrases, across turns)
- Negative gaps are clamped to 0
- Same input JSON + same parameters → same outputs (deterministic)
- The script never changes speaker/segmentation in Phase 1; it respects raw boundaries exactly

