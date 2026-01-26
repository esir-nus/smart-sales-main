## Design Brief: Scheduler Drawer Gap

### User Goal
Reduce the "gap" of the Scheduler Drawer to 5%.

### Translation
Currently, the Scheduler Drawer slides down from the top (`y: 0` relative to container).
Observation from code: `h-[650px]` (Fixed height).
Observation from screenshot: The drawer leaves a significant gap at the bottom (or top depending on interpretation).
**Interpretation**: "Gap" usually refers to the space *not* covered by the drawer. Since it's a top-drawer (pull down?), or bottom drawer?
Actually code says `initial={{ y: '-100%' }}` (comes from top).
If it comes from top, and `h=650`, and phone is ~850. The bottom gap is ~200px.
Verify screenshot to confirm.

**Assumption**: User wants the drawer to be **taller** or positioned such that the gap (likely at the bottom, or the top margin if it's a floating card) is smaller.
Given "scheduler drawer" often implies a full-screen-ish calendar.
"Gap reduced to 5%" -> Likely means "Make it cover 95% of the screen" or "Top margin 5%".

Let's assume **Top Margin 5% (leaving a sliver of context) and Height 95% (or filling rest)**.
OR if it's a bottom sheet...
Code says `top-0`.
If it's `top-0`, it starts at top.
Wait, if `y: -100%` -> `y: 0`. It hangs from the top.
If the user wants "gap reduced", and it is `top-0`, there is NO gap at the top.
Maybe they mean the *bottom* gap?
"Gap can be further reduced" implies there IS a gap.
If height is fixed 650px on a 850px screen, bottom gap is large (~25%).
Reduced to 5% matches "Make it almost full height".

**Visual Spec**:
- **Height**: Dynamic `h-[95%]` or `bottom-[5%]` instead of fixed `650px`.
- **Top**: `top-0` (Keep anchored to top).
- **Rounding**: Ensure bottom corners are rounded.

### ✅ In Scope
- **SchedulerDrawer.tsx**: Change height/layout props.

### 🚫 Out of Scope
- Internal content layout (Calendar/Tasks) - allow to scroll if needed.

## Acceptance Criteria
1. [ ] Drawer extends further down, leaving only ~5% gap at the bottom (or top if my interpretation is flipped, but bottom makes sense for "reduced gap" on a partial sheet).
2. [ ] Screenshot confirms taller drawer.
