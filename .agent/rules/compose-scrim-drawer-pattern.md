---
description: Compose Modal/Drawer scrim pattern - separate animations for scrim and content
trigger: compose drawer scrim AnimatedVisibility modal overlay
---

# Scrim + Drawer Pattern (Compose)

Scrim and drawer must use **separate AnimatedVisibility** wrappers. Scrim fades, drawer slides. Drawer content must consume pointer events.

Full pattern with code examples: `docs/reference/compose-scrim-drawer-pattern.md`
