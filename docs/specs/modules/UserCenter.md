# User Center Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L703-731

---

## Overview

Full-app settings and profile management overlay. Accessed from the main profile entry and the History Drawer footer.

- presentation remains a centered overlay inside `AgentShell`, not a side drawer
- the full-app host must keep a protected top clearance so the hero sits clearly below Android status icons without a separate header row
- the backdrop scrim and sheet material should mute underlying history into context-only background rather than readable competing content
- light-theme sheet treatment should read as a calm, mostly solid slab with denser section and row rhythm instead of a highly translucent glass panel
- the full-app surface does not expose a visible top-left close affordance; dismissal remains scrim tap plus system back

---

## Layout Structure

```
┌─────────────────────────────────────────────────────┐
│  [Centered Hero Sheet]                              │
│  Avatar | Name | Position | Metadata Chips         │
│                   [ Edit Profile ]                  │
├─────────────────────────────────────────────────────┤
│  § Preferences                                      │
│    [ Theme: Dark / Light / System ]                 │
│    [ AI Lab ]                                       │
│    [ Message Notifications ]                        │
├─────────────────────────────────────────────────────┤
│  § Space Management                                 │
│    [ Used Space: 128MB ]                            │
│    [ Clear Cache ]                                  │
├─────────────────────────────────────────────────────┤
│  § Security & Privacy                               │
│    [ Change Password ]                              │
├─────────────────────────────────────────────────────┤
│  § About                                            │
│    [ Help Center ]  [ Version ]                     │
└─────────────────────────────────────────────────────┘
                      [ Log Out ]
```

---

## Sections

| Section | Contents |
|---------|----------|
| **Profile Card** | Avatar, Name, Position, Metadata Chips, Edit |
| **Preferences** | Theme, AI Lab, Message Notifications |
| **Storage** | Used Space, Clear Cache |
| **Security** | Change Password |
| **About** | Help Center, Version |

## Current IA Freeze

- the current full-app `User Center` IA is fixed to `Preferences / Storage / Security / About / Logout`
- the older separate `Support` section is retired in the full-app path
- `Help Center` currently lives under `About`
- biometric / face ID no longer ships on the full-app `User Center` surface
- storage is presented as two rows: `已用空间` and `清除缓存`

## Theme Behavior

- `Theme` is a real single-choice selector with `Dark / Light / System`
- the choice is persisted locally on-device
- the full-app host applies the stored selection immediately
- the full-app overlay is presented inside `AgentShell` and must remain legal from both the main profile entry and the history-footer route
- the SIM settings drawer can edit and display the same stored preference
- the normal SIM launcher-core path now applies the stored selection immediately
- before any explicit choice is saved, the SIM launcher path defaults to dark on first startup while the shared app store still treats its unsaved fallback as `System`
- SIM scheduler/audio/connectivity/onboarding surfaces remain deferred for later light-theme parity and must not be over-claimed
