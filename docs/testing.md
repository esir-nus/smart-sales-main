Testing and Verification
========================

Unit tests (Gradle)
-------------------
- Run all JVM/unit tests: `./gradlew test`
- Module scope (faster focus): `./gradlew :feature:chat:testDebugUnitTest`
- Use module-scoped runs while iterating; run root `test` before commit/PR.

Registry verification (source-repo)
-----------------------------------
- Standard: `./gradlew verifySourceRepoRegistry`
- Strict (WARN becomes FAIL): `./gradlew verifySourceRepoRegistry -PregistryStrict=true`
- Script only: `python3 tools/verify_source_repo_registry.py [--strict]`
- Checks performed:
  - Draft-07 schema validation of `docs/source-repo.json`
  - For every `validationStatus=VALIDATED`: files exist, `testName` matches in file (supports backticked Kotlin names), `searchTerms` found at least once
  - Custom prompt pointers emit WARN (non-strict) and FAIL under `--strict`
- Failure reasons: `MISSING_FILE`, `MISSING_TEST`, `MISSING_SEARCHTERM`, or schema errors.

Notes
-----
- These checks are tooling only; they不改变应用行为。
- 新增 `VALIDATED` 节点时，必须添加 `validatedByTests` 与可 grep 的锚点（searchTerms/mappingLocations）。***
