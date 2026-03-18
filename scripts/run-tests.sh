#!/bin/bash

set -euo pipefail

# 中文说明: 统一仓库级测试入口，避免继续依赖零散的 Gradle 命令记忆。
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-all}"

print_help() {
    cat <<'EOF'
Usage: scripts/run-tests.sh [all|app|infra|l2|pipeline|scheduler]

Targets:
  all        Run the curated repo-default unit-test slice (not an exhaustive superset)
  app        Run all app-core unit tests
  infra      Run shared test-infrastructure module checks
  l2         Run the high-value app-core L2 simulated test slice
  pipeline   Run core pipeline unit tests
  scheduler  Run domain scheduler JVM tests
EOF
}

case "$MODE" in
    all)
        TASKS=("testDebugUnitTest")
        ;;
    app)
        TASKS=(":app-core:testDebugUnitTest")
        ;;
    infra)
        TASKS=(":core:test:test" ":core:test-fakes-domain:test" ":core:test-fakes-platform:testDebugUnitTest")
        ;;
    l2)
        TASKS=(":app-core:testDebugUnitTest" "--tests" "com.smartsales.prism.data.real.L2*")
        ;;
    pipeline)
        TASKS=(":core:pipeline:testDebugUnitTest")
        ;;
    scheduler)
        TASKS=(":domain:scheduler:test")
        ;;
    help|-h|--help)
        print_help
        exit 0
        ;;
    *)
        echo "Unknown target: $MODE" >&2
        print_help >&2
        exit 1
        ;;
esac

echo "Running test target: $MODE"
printf '  %s\n' "${TASKS[@]}"
./gradlew "${TASKS[@]}"

if [ "$MODE" = "all" ]; then
    echo
    echo "Note: 'all' is the curated repo-default unit-test slice (Gradle testDebugUnitTest), not an exhaustive aggregate of every runner mode."
    echo
    echo "L3 device validation remains manual. See docs/cerb-e2e-test/testing-protocol.md."
fi
