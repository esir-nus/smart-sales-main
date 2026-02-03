# User Center Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L703-731

---

## Overview

Settings and profile management sheet. Accessed from History Drawer footer.

---

## Layout Structure

```
┌─────────────────────────────────────────────────────┐
│  [👤 PROFILE CARD]                                  │
│  Avatar | Name | Position | Level | Plan Badge     │
│                            [ Edit Profile ]         │
├─────────────────────────────────────────────────────┤
│  § Preferences                                      │
│    [ Theme: Dark / Light / System ]                 │
│    [ AI Lab: Memory & Learning ]                    │
│    [ Notifications & Pop-ups ]                      │
├─────────────────────────────────────────────────────┤
│  § Storage                                          │
│    [ Used: 120MB ]  [ Clear Cache ]                 │
├─────────────────────────────────────────────────────┤
│  § Security                                         │
│    [ Change Password ]  [ Biometric ]               │
│    [ Logout All Devices ]                           │
├─────────────────────────────────────────────────────┤
│  § Support                                          │
│    [ Help Center ]  [ Contact & Feedback ]          │
├─────────────────────────────────────────────────────┤
│  § About SmartSales                                 │
│    v1.0.0  [ Updates ]  [ Privacy ]  [ Licenses ]   │
└─────────────────────────────────────────────────────┘
                      [ Log Out ]
```

---

## Sections

| Section | Contents |
|---------|----------|
| **Profile Card** | Avatar, Name, Position, Level, Plan Badge, Edit |
| **Preferences** | Theme, AI Lab, Notifications |
| **Storage** | Usage display, Clear Cache |
| **Security** | Password, Biometric, Logout All |
| **Support** | Help Center, Contact & Feedback |
| **About** | Version, Updates, Privacy, Licenses |
