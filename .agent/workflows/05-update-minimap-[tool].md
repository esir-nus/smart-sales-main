---
description: Update the HTML game minimap from the markdown interface map
---

# Update Minimap Workflow

This workflow ensures that the HTML visualization of our architecture ("The Game Minimap") is always perfectly synchronized with the pure Computer Science source of truth (`interface-map.md`).

> **Golden Rule**: NEVER edit `docs/cerb/dashboard.html` manually. It is purely an output artifact.

## When to Use

Run this workflow whenever you make a deliberate change to `docs/cerb/interface-map.md`.

## Step 1: Ensure Target Exists

// turbo
```bash
mkdir -p docs/tools
```

## Step 2: Execute Generator Script

The generator parses the markdown tables and builds the visually encodided HTML using no external dependencies.
// turbo
```bash
python3 docs/tools/generate_map.py
```

## Step 3: Present Result

Once successfully generated, notify the user. The user can view the absolute path in their browser by opening `file:///absolute/path/to/docs/cerb/dashboard.html`.
