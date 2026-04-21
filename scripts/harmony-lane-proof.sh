#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_DEVICE="4NY0225613001090"

usage() {
  cat <<'EOF'
Usage:
  scripts/harmony-lane-proof.sh <lane> [--device <target>] [--path-only]

Supported lanes:
  hs-test
  backend-mini-lab
  smartsales.HOS.test
  hui
  smartsales.HOS.ui
  app
  com.smartsales.harmony.app

Behavior:
  - resolves the newest signed .hap for the requested lane
  - prints the exact artifact path
  - prints matching hdc install / aa start / hilog commands

Notes:
  - unsigned .hap files are ignored
  - if no signed .hap exists for the lane, the script exits non-zero
  - this helper resolves artifact paths; it does not replace the device-proof rules
    documented in docs/platforms/harmony/test-signing-ledger.md
EOF
}

lane="${1:-}"
if [[ -z "${lane}" ]]; then
  usage
  exit 1
fi
shift || true

device="${DEFAULT_DEVICE}"
path_only="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --device" >&2
        exit 1
      fi
      device="$2"
      shift 2
      ;;
    --path-only)
      path_only="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

bundle_id=""
app_dir=""
artifact_patterns=()

case "${lane}" in
  hs-test|backend-mini-lab|smartsales.HOS.test)
    bundle_id="smartsales.HOS.test"
    app_dir="${ROOT_DIR}/platforms/harmony/smartsales-app"
    artifact_patterns=(
      "*signed-phase2c-current.hap"
      "*signed-hs006.hap"
      "*signed-test-profile-hs006.hap"
      "*signed-test-profile*.hap"
    )
    ;;
  hui|smartsales.HOS.ui)
    bundle_id="smartsales.HOS.ui"
    app_dir="${ROOT_DIR}/platforms/harmony/ui-verification"
    artifact_patterns=(
      "*smartsales.HOS.ui*.hap"
      "*signed-ui*.hap"
    )
    ;;
  app|com.smartsales.harmony.app)
    bundle_id="com.smartsales.harmony.app"
    app_dir="${ROOT_DIR}/platforms/harmony/smartsales-app"
    artifact_patterns=(
      "*com.smartsales.harmony.app*.hap"
      "*signed-app*.hap"
      "*signed-smartsales-app*.hap"
    )
    ;;
  *)
    echo "Unsupported lane: ${lane}" >&2
    usage
    exit 1
    ;;
esac

if [[ ! -d "${app_dir}" ]]; then
  echo "Lane root not found: ${app_dir}" >&2
  exit 2
fi

mapfile -t candidates < <(
  while IFS= read -r path; do
    base_name="$(basename "${path}")"
    for pattern in "${artifact_patterns[@]}"; do
      if [[ "${base_name}" == ${pattern} ]]; then
        stat -c '%Y %n' "${path}"
        break
      fi
    done
  done < <(find "${app_dir}" -type f -name '*.hap' ! -name '*unsigned*' -print 2>/dev/null) \
    | sort -rn \
    | awk '{ $1=""; sub(/^ /, ""); print }'
)

if [[ "${#candidates[@]}" -eq 0 ]]; then
  echo "No signed .hap artifacts matched lane ${lane} under ${app_dir}" >&2
  exit 3
fi

artifact_path="${candidates[0]}"

if [[ "${path_only}" == "true" ]]; then
  printf '%s\n' "${artifact_path}"
  exit 0
fi

cat <<EOF
Lane: ${lane}
Bundle: ${bundle_id}
Device: ${device}
Artifact: ${artifact_path}

Install:
hdc -t ${device} install -r ${artifact_path}

Launch:
hdc -t ${device} shell aa start -a EntryAbility -b ${bundle_id}

Logs:
hdc -t ${device} shell hilog -r
hdc -t ${device} shell hilog -G 16M
hdc -t ${device} shell hilog -x | rg 'HS-006|Scheduler|Reminder'
EOF
