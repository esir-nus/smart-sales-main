Update the single product changelog.

## Flow

1. Run `bash scripts/changelog.sh` from repo root to regenerate `CHANGELOG.html` from `CHANGELOG.md`.
2. Find commits since the latest `### YYYY-MM-DD` header already present in `CHANGELOG.md`.
3. Append new dated sections to the top of `## 最近更新` in `CHANGELOG.md` for `feat:`, `fix:`, `ship:`, and significant `chore:` or `refactor:` commits not yet represented.
4. Run `bash scripts/changelog.sh` again.
5. Report how many entries were added and the covered date range.

## Rules

- This command edits `CHANGELOG.md` only and regenerates `CHANGELOG.html`.
- Single Changelog rule: there is one product changelog.
- Do not maintain or update any parallel product trace log.
- Write changelog prose in Chinese and keep technical jargon in English where the file already does so.
