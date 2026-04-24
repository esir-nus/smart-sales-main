---
name: changelog
description: Update CHANGELOG.md with new entries from git history, regenerate CHANGELOG.html, commit and push. Use when the user says /changelog or asks to update the changelog.
---

# Update Changelog

Update the single product changelog, then commit and push.

## Flow

1. Run `bash scripts/changelog.sh`.
2. Read the latest `### YYYY-MM-DD` header in `CHANGELOG.md`.
3. Gather eligible commits after that date.
4. Append new dated sections to `CHANGELOG.md`.
5. Run `bash scripts/changelog.sh` again.
6. Commit with `docs(changelog): update changelog to YYYY-MM-DD`.
7. Push and report the entry count and date range.

## Rules

- Single Changelog rule: there is one product changelog.
- `CHANGELOG.md` only, with regenerated `CHANGELOG.html`.
- Skip merge commits and trivial non-user-visible changes.
- Never commit directly to `master`.
