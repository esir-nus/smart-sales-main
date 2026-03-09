# Platform Anchoring Report: Prism UI Layer

> **Date**: 2026-01-26  
> **Target**: Android (Compose)  
> **Test Harness**: `MinimalChatScreen.kt` + Component Previews

---

## Calibration Targets

| Property | Components | Status |
|----------|------------|--------|
| Background Colors | All cards | ✅ Extracted |
| Text Colors | All components | ✅ Extracted |
| Accent Colors | Buttons, links | ✅ Extracted |
| Border Radius | Cards | ✅ 12.dp standard |
| Opacity | Not used (solid colors) | ⏭️ Skip |

---

## Color Token Extraction

### Background Colors (Container)

| Token Name | Hex Value | Compose Value | Component |
|------------|-----------|---------------|-----------|
| `surface.thinking` | `#1A1A2E` | `Color(0xFF1A1A2E)` | ThinkingBox |
| `surface.plan` | `#1E3A5F` | `Color(0xFF1E3A5F)` | PlanCard |
| `surface.conflict` | `#3D2020` | `Color(0xFF3D2020)` | ConflictResolver |
| `surface.response` | `#2A2A40` | `Color(0xFF2A2A40)` | ResponseBubble |
| `surface.input` | `#1A1A2E` | `Color(0xFF1A1A2E)` | InputBar |
| `surface.screen` | `#0D0D1A` | `Color(0xFF0D0D1A)` | MinimalChatScreen |

### Text Colors

| Token Name | Hex Value | Compose Value | Usage |
|------------|-----------|---------------|-------|
| `text.primary` | `#FFFFFF` | `Color.White` | Main content |
| `text.secondary` | `#888888` | `Color(0xFF888888)` | Muted, completed items |
| `text.thinking` | `#AAFFAA` | `Color(0xFFAAFFAA)` | Thinking trace (monospace) |
| `text.warning` | `#FFAA00` | `Color(0xFFFFAA00)` | Conflict title |
| `text.error` | `#FF6B6B` | `Color(0xFFFF6B6B)` | Error messages |
| `text.info` | `#88CCFF` | `Color(0xFF88CCFF)` | Plan completion summary |

### Accent Colors

| Token Name | Hex Value | Compose Value | Usage |
|------------|-----------|---------------|-------|
| `accent.primary` | `#4FC3F7` | `Color(0xFF4FC3F7)` | Send button, [运行] links, cursor |
| `accent.warning` | `#FFAA00` | `Color(0xFFFFAA00)` | Confirm button (conflict) |
| `accent.disabled` | `#555555` | `Color(0xFF555555)` | Disabled buttons |
| `accent.selected` | `#3A3A5A` | `Color(0xFF3A3A5A)` | Mode toggle selected |

### Divider Colors

| Token Name | Hex Value | Component |
|------------|-----------|-----------|
| `divider.thinking` | `#333333` | ThinkingBox |
| `divider.plan` | `#3A5F8A` | PlanCard |
| `divider.conflict` | `#5D3030` | ConflictResolver |

---

## Shape Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `radius.card` | `12.dp` | All Card components |
| `radius.input` | `24.dp` | InputBar |
| `radius.toggle` | `8.dp` / `6.dp` | ModeToggleBar |

---

## Typography

| Style | Size | Weight | Family |
|-------|------|--------|--------|
| Card Title | 16.sp | Normal | Default |
| Card Body | 14.sp | Normal | Default |
| Thinking Trace | 12.sp | Normal | Monospace |
| Summary | 12.sp | Normal | Default |
| Emoji Icons | 16-18.sp | — | System |

---

## Calibration Recommendations

| Issue | Current | Recommendation |
|-------|---------|----------------|
| Dark mode only | All hardcoded dark colors | Add light mode variants in Phase 3 |
| No elevation/shadow | Cards use flat color | Consider subtle elevation (2.dp) |
| Divider deprecation | Using `Divider()` | Migrate to `HorizontalDivider()` |

---

## Next Steps

1. **Create `PrismColors.kt`** — centralize all color tokens
2. **Create `PrismTheme.kt`** — wrap with `CompositionLocalProvider`
3. **Device calibration** — run on physical device, capture screenshots
4. **Update `ui-alignment-table.md`** — add token references

---

## Evidence

V1 Legacy component references (`ThinkingBox`, `PlanCard`, `ConflictResolver`, `MinimalChatScreen`) have been formally deprecated following the unified pipeline refactor.

New UI architecture and tokens are defined in the Agent Intelligence `cerb-ui` specifications:
- **UX/Visual Specs**: [Agent Intelligence UI Spec](file:///home/cslh-frank/main_app/docs/cerb-ui/agent-intelligence/spec.md)
- **RAM Contracts**: [Agent Intelligence UI Interface](file:///home/cslh-frank/main_app/docs/cerb-ui/agent-intelligence/interface.md)

Shipped components available in:
- [ResponseBubble.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/components/ResponseBubble.kt)
- [AgentChatScreen.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/AgentChatScreen.kt)
