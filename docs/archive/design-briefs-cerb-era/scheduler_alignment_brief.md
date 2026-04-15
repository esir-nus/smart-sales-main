# Design Brief: Scheduler UI Alignment (Visual Only)

> **Status**: Waiting for Approval
> **Owner**: @[/12-ui-director]
> **Scope**: Static Web Prototype Design ONLY

## 1. User Goal
Update the **Scheduler Drawer** web prototype to match the "Sleek Glass" Pro Max aesthetic. Create the visual "Golden Master" for future implementation.

## 2. In-Scope (Visual Only)
### Sleek Glass Aesthetic
- **Drawer Surface**: `SurfaceCard` (FrostedIce = High Blur + White Border).
- **Background**: `glassBlurHuge` (30dp) with `glassSaturation` (1.8x).
- **Scrim**: `Alpha=0.4` Black.

### Components
- **Handle**: Standardized 120dp wide Pill shape.
- **Calendar**: Visual updates to month carousel & grid styling.
- **Timeline**: Adaptive stack layout with "Zero-Chrome" (no visible scrollbars).
- **Typography**: Verify `Type.kt` alignment (font weights/sizes).

## 3. Visual Tokens (Strict Adherence)
| Element | Token / Spec | Note |
|---------|--------------|------|
| **Background** | `SurfaceCard` (FrostedIce) | `glassBlurHuge` + `BorderDefault` |
| **Scrim** | `Scrim` (Z=3.0) | `Alpha=0.4` Black. |
| **Drawer Layout**| `Drawer` (Z=4.0) | Top-anchored. |
| **Handle** | `Scheduler Handle` | 120dp width, Pill shape. |
| **Card** | `SurfaceCard` | 12-16dp radius, `Elevation.small`. |

## 4. Acceptance Criteria (Visual)
- [ ] Drawer visuals match "Frosted Ice" look (blur + border) in browser.
- [ ] No visible browser scrollbars (Zero-Chrome).
- [ ] Handle is exactly 120dp.
- [ ] Selected date has clear visual styling (Ring/Fill).
