# Wave 2 Rationale

The internal hierarchy uses the dark frosted slab material (`surface-card`) to segment functional blocks while avoiding generic settings-app layouts.

- **Profile Hero**: Uses pill-shaped badges (`Industry, Experience, Platform, Tier`) to compactly convey metadata, letting the avatar and name serve as the primary visual anchors without requiring a deep edit page hierarchy immediately.
- **Section Rhythm**: Sections are grouped tightly with low-contrast dividers (`border-subtle`), prioritizing legibility while feeling like a native overlay rather than a standalone app.
- **Iconography**: Icons are simple stroke-based SVGs from the SIM design system, maintaining a lightweight visual weight.
- **Logout Constraint**: Placed independently at the bottom in a subdued dark card (`rgba(255,69,58,0.1)`) with `color: var(--color-delete)`. It is unmistakably destructive but restrained enough not to scream for attention.
