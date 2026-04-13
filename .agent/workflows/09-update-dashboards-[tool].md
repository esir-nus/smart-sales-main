---
description: Update all Dashboard HTML pages (Topology, Dataflow, Estimates, Reports)
last_reviewed: 2026-04-13
---

# Update Dashboards Workflow

This workflow regenerates the 4 separated dashboard HTML pages (`dashboard-topology.html`, `dashboard-dataflow.html`, `dashboard-estimate.html`, and `dashboard-reports.html`) using `docs/tools/generate_map.py`. This ensures the visualizations are fully synchronized with the most recent `docs/cerb/interface-map.md`, `PRODUCTION_CODE_ESTIMATE.md`, and `docs/reports/*.md` architectures and assessments.

> **Golden Rule**: NEVER edit `docs/cerb/dashboard-*.html` manually. They are purely output artifacts of the generation script.

## When to Use

Run this workflow whenever you make deliberate changes to `docs/cerb/interface-map.md`, `PRODUCTION_CODE_ESTIMATE.md` or adding new reports to `docs/reports`.

## Step 1: Execute Generator Script

The python generator parses the markdown tables and reports, and outputs the 4 HTML dashboards using no external dependencies.
// turbo
```bash
python3 docs/tools/generate_map.py
```

## Step 2: Serve Dashboards Locally

To make the dashboards accessible to other devices on the same Wi-Fi network (like a phone or tablet), start the custom development server which supports UTF-8 Markdown rendering natively.
// turbo
```bash
python3 docs/tools/serve_dashboard.py &
```

## Step 3: Present Result

Once successfully generated and served, notify the user. The user can view the dashboards in their browser by opening the following local network links (replace `[YOUR_IP]` with the machine's local IP address, e.g., `192.168.1.34`):
- `http://[YOUR_IP]:8000/dashboard-topology.html`
- `http://[YOUR_IP]:8000/dashboard-dataflow.html`
- `http://[YOUR_IP]:8000/dashboard-estimate.html`
- `http://[YOUR_IP]:8000/dashboard-reports.html`
