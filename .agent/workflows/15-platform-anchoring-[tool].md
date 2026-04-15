---
description: Calibrate visual values across platforms with controlled experiments
last_reviewed: 2026-04-13
---

# Platform Anchoring Workflow

When invoked, adopt the persona of a **meticulous visual calibration engineer** who:

- Runs controlled experiments before making assumptions
- Documents visual evidence (screenshots) for every calibration
- Updates the UI Communication Contract with verified mappings
- **Does NOT guess** — tests and observes

---

## 🎯 Purpose

Eliminate blind translation between web prototype values and native platform values by running controlled visual experiments.

---

## When to Use

- Before a major UI transplant (first time porting a component)
- When previous iterations produce unexpected visual results
- When translating effects with no 1:1 mapping (blur, shadow, blend modes)
- When a value "looks wrong" despite "correct" translation

---

## Protocol

### Phase 1: Identify Calibration Targets

List the visual properties that need calibration:

```markdown
## Calibration Targets
- [ ] Opacity/Alpha (for glass effects)
- [ ] Shadow elevation (for depth cues)
- [ ] Border radius (rarely needs calibration)
- [ ] Typography (direct mapping usually works)
```

---

### Phase 2: Generate Test Harness

Create a minimal Compose screen with test swatches.

**Example: Opacity Calibration**

```kotlin
@Composable
fun OpacityCalibrationScreen() {
    val opacityLevels = listOf(0.02f, 0.05f, 0.10f, 0.25f, 0.50f, 0.65f, 0.80f, 1.0f)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opacityLevels.forEach { alpha ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alpha: $alpha", style = MaterialTheme.typography.bodySmall)
                Box(
                    modifier = Modifier
                        .size(100.dp, 48.dp)
                        .background(Color(0xFF007AFF).copy(alpha = alpha))
                )
            }
        }
    }
}
```

---

### Phase 3: Capture Evidence

1. Build and deploy to physical device (or emulator matching target)
2. Capture screenshot of each swatch
3. Store in `docs/design/platform-calibration/android/`

**Naming Convention**:
```
docs/design/platform-calibration/
├── android/
│   ├── opacity_scale.png
│   ├── shadow_scale.png
│   └── blur_fallback.png
├── ios/
│   └── (future)
└── harmony/
    └── (future)
```

---

### Phase 4: Build Translation Table

Compare prototype CSS values with actual rendered output:

```markdown
## Opacity Calibration Results

| CSS Value | Compose Value | Visual Match? | Adjustment |
|-----------|---------------|---------------|------------|
| opacity: 0.65 | alpha = 0.65f | ❌ Too dark | Use 0.45f |
| opacity: 0.10 | alpha = 0.10f | ✅ Correct | -- |
| opacity: 0.02 | alpha = 0.02f | ⚠️ Invisible | Use 0.05f |
```

---

### Phase 5: Update Contracts

1. Update `docs/design/UI-communication-contract.md` with calibrated values
2. Update `design-tokens.json` with platform-specific overrides
3. Update `AppColors.kt` if new tokens are needed

---

## Output Format

```markdown
## Platform Anchoring Report: [Property Name]

### Experiment Setup
- **Device**: [Device model, Android version]
- **Date**: [YYYY-MM-DD]
- **Test Harness**: [Link to composable or screenshot]

### Results

| Web Value | Native Value | Visual Match | Calibrated Value |
|-----------|--------------|--------------|------------------|
| ... | ... | ... | ... |

### Evidence
![Opacity Scale](file:///path/to/screenshot.png)

### Recommendations
- Use calibrated values in `UI-communication-contract.md`
- Add platform override to `design-tokens.json`
```

---

## Integration Points

| Step | Workflow |
|------|----------|
| Before transplant | Run `/15-platform-anchoring` for unknown properties |
| During transplant | Reference `UI-communication-contract.md` |
| After transplant | If mismatch found, run anchoring again |

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| Assume CSS value = Compose value | Test first, then translate |
| Guess "maybe 0.65 is too dark" | Measure: capture screenshot, compare |
| Skip calibration for "simple" properties | Calibrate at least opacity and shadow |
| Ignore platform differences | Document differences in contract |

---

## Cross-References

| Document | Purpose |
|----------|---------|
| [UI-communication-contract.md](file:///home/cslh-frank/main_app/docs/design/UI-communication-contract.md) | Translation tables |
| [design-tokens.json](file:///home/cslh-frank/main_app/docs/design/design-tokens.json) | Token values |
| `/14-ui-transplant` | Gap analysis (uses this for calibration) |
| `/ui-ux-pro-max` | Executor (uses calibrated values) |
