# User Center Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Local settings module contract beneath `docs/core-flow/base-runtime-ux-surface-governance-flow.md` (`UX.SETTINGS.*`)

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
│    [ Debug Mode ] (debug builds only)               │
│    [ Message Notifications ]                        │
├─────────────────────────────────────────────────────┤
│  § Device Control                                   │
│    [ Badge Voice Volume: 0..100 ]                   │
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
| **Preferences** | Theme, AI Lab, Debug Mode in debug builds, Message Notifications |
| **Device Control** | Badge Voice Volume |
| **Storage** | Used Space, Clear Cache |
| **Security** | Change Password |
| **About** | Help Center, Version |

## Current IA Freeze

- the current full-app `User Center` IA is fixed to `Preferences / Device Control / Storage / Security / About / Logout`
- the older separate `Support` section is retired in the full-app path
- `Help Center` currently lives under `About`
- biometric / face ID no longer ships on the full-app `User Center` surface
- storage is presented as two rows: `已用空间` and `清除缓存`
- `Device Control` currently contains one real row: `徽章语音音量`
- badge voice volume is persisted locally, but the badge command is sent only when the user releases the slider
- duplicate voice-volume writes are skipped only after the same value was confirmed as written to the badge; a disconnected/no-op attempt must remain retryable
- debug builds expose a persisted `调试模式` preference that controls whether page-local debug and simulation controls are visible; release builds must not show the toggle or the controls
- debug controls that claim L2.5 must show deterministic scenario labels and emit assertion telemetry; they validate app-side dataflow only and must not be described as authentic physical badge evidence

## Theme Behavior

- `Theme` is a real single-choice selector with `Dark / Light / System`
- the choice is persisted locally on-device
- the full-app host applies the stored selection immediately
- the full-app overlay is presented inside `AgentShell` and must remain legal from both the main profile entry and the history-footer route
- the SIM settings drawer can edit and display the same stored preference
- the normal SIM launcher-core path now applies the stored selection immediately
- before any explicit choice is saved, the SIM launcher path defaults to dark on first startup while the shared app store still treats its unsaved fallback as `System`
- SIM scheduler/audio/connectivity/onboarding surfaces remain deferred for later light-theme parity and must not be over-claimed
