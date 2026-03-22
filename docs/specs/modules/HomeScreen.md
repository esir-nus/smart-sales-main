# Home Screen Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: Shipped
> **Role**: Compatibility pointer
> **Current Owner**: [Home Shell UI Spec](../../cerb-ui/home-shell/spec.md)

---

## Overview

This path remains stable for compatibility, but it is no longer the detailed source of truth for the current home empty-state shell.

---

## Current Rule

- Current home empty-state composition is owned by
  [spec.md](../../cerb-ui/home-shell/spec.md).
- Current header-center Dynamic Island behavior is owned by
  [spec.md](../../cerb-ui/dynamic-island/spec.md).
- Shared interaction and layer invariants remain owned by
  [ui_element_registry.md](../ui_element_registry.md).
- Global visual tokens remain owned by
  [style-guide.md](../style-guide.md).

---

## Deprecated Detailed Layout

Older "Executive Desk" descriptions that included a static `New Session` center title,
debug controls, right-toolbar utilities, mode toggles, or artifact-first shell composition
are not current product truth and should not be used for new implementation decisions.
