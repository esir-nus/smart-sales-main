# SIM User Center Transplant Note

## Faithful (Must Keep)
- **Geometry & Physics**: The side-entering slab from the right edge with a width of `330px`. The dark frosted Scrim (`rgba(0,0,0,0.4)`) must blur the shell behind it.
- **Surface Material**: The drawer uses `var(--surface-drawer)` (dense blur) and the section cards use `var(--surface-card)` (lighter blur with `0.5px` subtle inner borders).
- **Row Rhythm**: Rows MUST adhere to the `52px` height with `16px` horizontal padding. The divider is extremely faint `rgba(255,255,255,0.05)`.
- **Logout Constraint**: The logout button is NOT inside a standard row card. It sits independently at the bottom in a `rgba(255, 69, 58, 0.08)` button with `#FF453A` text.

## Deferred (Can wait for V2)
- **Edit Profile Route**: Tapping the Profile Hero can just toggle a Toast or no-op for now. 
- **Sub-page Navigation**: For Wave 1 implementation, rows can just be representational if their respective features (like AI Lab settings) aren't built yet.
- **Dynamic Badge Logic**: The "PRO" badge can be hardcoded or hidden if the `subscriptionTier` API lacks data.
