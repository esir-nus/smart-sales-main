# Wave 1 Rationale

We explored the side-enter support slab direction with two variants to preserve SIM shell context:

- **Variant A (Full-Edge Drawer)**: Matches the `History Drawer` geometry, anchoring directly to the right edge. It provides maximum vertical space for the full blueprint but feels slightly heavier.
- **Variant B (Floating Slab)**: Detaches slightly from the edges (`top/bottom/right: 12px`), reading more clearly as an overlay rather than a rigid navigation pane. It feels more intimate and subordinate to the SIM conversation shell.

Both use the dark frosted material (`var(--surface-drawer)`). Variant A is recommended for consistency with existing overlays (like the History drawer), ensuring the shell's overlay physics remain predictable.
