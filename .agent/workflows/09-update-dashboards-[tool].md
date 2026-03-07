---
description: Update all Dashboard HTML pages (Topology, Dataflow, Estimates)
---

# Update Dashboards Workflow

This workflow regenerates the 3 separated dashboard HTML pages (`dashboard-topology.html`, `dashboard-dataflow.html`, and `dashboard-estimate.html`) using `docs/tools/generate_map.py`. This ensures the visualizations are fully synchronized with the most recent `docs/cerb/interface-map.md` and `PRODUCTION_CODE_ESTIMATE.md` architecture blueprints.

> **Golden Rule**: NEVER edit `docs/cerb/dashboard-*.html` manually. They are purely output artifacts of the generation script.

## When to Use

Run this workflow whenever you make deliberate changes to `docs/cerb/interface-map.md` or `PRODUCTION_CODE_ESTIMATE.md`.

## Step 1: Execute Generator Script

The python generator parses the markdown tables and outputs the 3 HTML dashboards using no external dependencies.
// turbo
```bash
python3 docs/tools/generate_map.py
```

## Step 2: Serve Dashboards Locally

To make the dashboards accessible to other devices on the same Wi-Fi network (like a phone or tablet), start a simple HTTP server in the `docs/cerb` directory.
// turbo
```bash
cd docs/cerb && python3 -m http.server 8000 &
```

## Step 3: Present Result

Once successfully generated and served, notify the user. The user can view the dashboards in their browser by opening the following local network links (replace `[YOUR_IP]` with the machine's local IP address, e.g., `192.168.1.34`):
- `http://[YOUR_IP]:8000/dashboard-topology.html`
- `http://[YOUR_IP]:8000/dashboard-dataflow.html`
- `http://[YOUR_IP]:8000/dashboard-estimate.html`
