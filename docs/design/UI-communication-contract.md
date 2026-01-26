# UI Communication Contract

> **NOTE**: For Prism project specifics (Tokens, Components), see [Prism UI Registry](prism-ui-registry.md).  
> This document defines the *general methodology* for Platform Anchoring.

> **Purpose**: Define a shared language for translating visual specifications between web prototypes and native platforms.

---

## The Problem

AI agents translate CSS values to native code **symbolically**, not **perceptually**:

| Symbolic Translation | Perceptual Translation |
|----------------------|------------------------|
| `opacity: 0.65` → `alpha = 0.65f` | "What does 0.65 *look like* on Android?" |
| `box-shadow: 8px` → `elevation = 8.dp` | "Does 8.dp produce similar visual depth?" |
| Assumes 1:1 mapping | Validates with evidence |

**Result**: Blind translation produces unexpected visual artifacts (boxes, hard edges, muddy colors).

---

## The Solution: Platform Anchoring

Before transplanting UI, run **controlled experiments** to calibrate values:

```
Web Prototype (CSS)
       │
       ▼
┌─────────────────────────────┐
│  ANCHORING LAYER            │
│  - Controlled Experiments   │
│  - Visual Swatches          │
│  - Platform-specific maps   │
│  - Screenshot evidence      │
└──────────────┬──────────────┘
               ▼
Native Code (Kotlin/SwiftUI/ArkTS)
```

---

## Visual Property Translation Tables

### 1. Opacity / Alpha

| CSS Value | Android (Compose) | iOS (SwiftUI) | Visual Notes |
|-----------|-------------------|---------------|--------------|
| `opacity: 0` | `alpha = 0f` | `.opacity(0)` | Invisible |
| `opacity: 0.02` | `alpha = 0.02f` | `.opacity(0.02)` | Barely visible tint |
| `opacity: 0.05` | `alpha = 0.05f` | `.opacity(0.05)` | Subtle overlay |
| `opacity: 0.10` | `alpha = 0.10f` | `.opacity(0.10)` | Light tint |
| `opacity: 0.25` | `alpha = 0.25f` | `.opacity(0.25)` | Visible overlay |
| `opacity: 0.50` | `alpha = 0.50f` | `.opacity(0.50)` | Semi-transparent |
| `opacity: 0.65` | `alpha = 0.65f` | `.opacity(0.65)` | Frosted glass range |
| `opacity: 0.80` | `alpha = 0.80f` | `.opacity(0.80)` | Nearly opaque |
| `opacity: 1.0` | `alpha = 1f` | `.opacity(1)` | Fully opaque |

> **Calibration Required**: Run `/15-platform-anchoring` to capture actual screenshots.

---

### 2. Shadow / Elevation

| CSS `box-shadow` | Android `Modifier.shadow()` | Visual Notes |
|------------------|----------------------------|--------------|
| `0 2px 4px` | `elevation = 2.dp` | Subtle lift |
| `0 4px 8px` | `elevation = 4.dp` | Card elevation |
| `0 8px 16px` | `elevation = 8.dp` | Modal elevation |
| `0 12px 24px` | `elevation = 12.dp` | Dialog elevation |
| `0 16px 32px` | `elevation = 16.dp` | Heavy lift |

**Android-Specific**:
- `spotColor`: Controls shadow color (default: black)
- `ambientColor`: Background shadow layer
- Use `Color.Transparent` for `ambientColor` to avoid "box" artifacts

---

### 3. Blur Effects

| CSS Effect | Android Equivalent | Notes |
|------------|-------------------|-------|
| `backdrop-filter: blur(20px)` | **No direct equivalent** | Use alpha overlay + tint |
| `filter: blur(8px)` | `Modifier.blur(8.dp)` (API 31+) | Limited support |
| Frosted glass | `Surface(color = X.copy(alpha = 0.65f))` | Approximation |

---

### 4. Border Radius

| CSS | Android (Compose) | Notes |
|-----|-------------------|-------|
| `border-radius: 8px` | `RoundedCornerShape(8.dp)` | Direct mapping |
| `border-radius: 12px` | `RoundedCornerShape(12.dp)` | Standard card |
| `border-radius: 50%` | `CircleShape` | Full circle |

---

### 5. Typography

| CSS Property | Compose Property | Notes |
|--------------|------------------|-------|
| `font-size: 15px` | `fontSize = 15.sp` | Direct mapping |
| `font-weight: 500` | `fontWeight = FontWeight.Medium` | Named weight |
| `font-weight: 600` | `fontWeight = FontWeight.SemiBold` | Named weight |
| `font-weight: 700` | `fontWeight = FontWeight.Bold` | Named weight |
| `letter-spacing: -0.02em` | `letterSpacing = (-0.02).em` | Tight tracking |
| `letter-spacing: 0.05em` | `letterSpacing = 0.05.em` | Loose tracking |

---

### 6. Spacing

| CSS Property | Compose Property | Notes |
|--------------|------------------|-------|
| `padding: 16px` | `Modifier.padding(16.dp)` | Direct mapping |
| `margin: 12px 0` | `Arrangement.spacedBy(12.dp)` | Gap between items |
| `gap: 4px` | `Arrangement.spacedBy(4.dp)` | Flexbox gap |

---

## Anchoring Workflow Reference

When values don't match visually, use `/15-platform-anchoring`:

1. **Identify**: Which property looks wrong?
2. **Experiment**: Build a test harness with extreme values (5%, 50%, 80%)
3. **Capture**: Screenshot each swatch on the target device
4. **Calibrate**: Update this contract with corrected mappings
5. **Document**: Store evidence in `docs/design/platform-calibration/`

---

## Known Platform Differences

### Android-Specific Gotchas

| Issue | Cause | Solution |
|-------|-------|----------|
| "Hard box border" on shadows | `ambientColor` not transparent | Set `ambientColor = Color.Transparent` |
| Opacity looks darker | Surface layering compounds alpha | Use lower alpha values |
| Blur not working | API 31+ only | Use alpha overlay fallback |

### iOS-Specific Gotchas

| Issue | Cause | Solution |
|-------|-------|----------|
| System blur looks different | UIBlurEffect uses system styles | Match to `.systemMaterial` variants |
| Shadow clipping | clipsToBounds on parent | Add shadow to wrapper view |

---

## Integration with Design Tokens

This contract extends `design-tokens.json`:

```json
{
  "platform-overrides": {
    "android": {
      "glass-opacity": 0.45,
      "shadow-ambient-alpha": 0
    },
    "ios": {
      "glass-opacity": 0.65,
      "shadow-ambient-alpha": 0.1
    }
  }
}
```

---

## Cross-References

| Document | Purpose |
|----------|---------|
| [design-tokens.json](file:///home/cslh-frank/main_app/docs/design/design-tokens.json) | Master token values |
| [style-guide.md](file:///home/cslh-frank/main_app/docs/specs/style-guide.md) | Visual design system |
| `/15-platform-anchoring` | Calibration workflow |
| `/14-ui-transplant` | Gap analysis workflow |
