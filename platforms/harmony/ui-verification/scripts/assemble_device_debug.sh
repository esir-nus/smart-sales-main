#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$PROJECT_DIR/../../.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLCHAIN_DIR="$REPO_ROOT/platforms/harmony/commandline-tools-linux-x64-6.0.2.650/command-line-tools"
TOOLCHAIN_LIB="$TOOLCHAIN_DIR/sdk/default/openharmony/toolchains/lib"
HVIGORW="$TOOLCHAIN_DIR/bin/hvigorw"
HAP_SIGN_TOOL="$TOOLCHAIN_LIB/hap-sign-tool.jar"
OPENHARMONY_KEYSTORE="$TOOLCHAIN_LIB/OpenHarmony.p12"
OPENHARMONY_PROFILE_CERT="$TOOLCHAIN_LIB/OpenHarmonyProfileDebug.pem"
UNSIGNED_PROFILE_TEMPLATE="$TOOLCHAIN_LIB/UnsgnedDebugProfileTemplate.json"
LOCAL_PROPERTIES="$REPO_ROOT/local.properties"
LEGACY_DEVICE_PROFILE="$HOME/.ohos/tingwu-container-signing/agc-debug/smartsales-testDebug.p7b"

# 目标包配置：ui-verification 和 mini-lab
UI_PROJECT_DIR="$REPO_ROOT/platforms/harmony/ui-verification"
UI_BUNDLE_NAME="smartsales.HOS.ui"
UI_AGC_SIGNING_DIR_DEFAULT="$HOME/.ohos/ui-verification-signing/agc-debug"

MINILAB_PROJECT_DIR="$REPO_ROOT/platforms/harmony/tingwu-container"
MINILAB_BUNDLE_NAME="smartsales.HOS.test"
MINILAB_AGC_SIGNING_DIR_DEFAULT="$HOME/.ohos/tingwu-container-signing/agc-debug"

# 默认使用 ui-verification 目标（向后兼容）
TARGET_PACKAGE=""

BUNDLE_NAME=""
ABILITY_NAME="EntryAbility"
LOCAL_APP_KEY_ALIAS="uiAppDebug"
LOCAL_APP_KEY_PASSWORD="UiVerifyLocalSigningPassphrase20260411"
OPENHARMONY_STORE_PASSWORD="123456"

SIGN_MODE=""
INSTALL_AFTER_BUILD=0
START_AFTER_INSTALL=0

print_usage() {
  cat <<EOF
Usage: $(basename "$0") [--target ui|minilab] [--mode local|agc] [--install] [--start]

Targets:
  ui       Build/sign the ui-verification package (smartsales.HOS.ui).
           This is the default when no --target is given.
  minilab  Build/sign the backend mini-lab package (smartsales.HOS.test).

Both targets share the same .p12 credential and key alias.
Each target uses its own package-specific .p7b profile.

Modes:
  local  Build and locally sign a HAP with the OpenHarmony debug chain.
         This is for repeatable local package verification only and is not
         accepted as device-ready proof on the attached Harmony device.
  agc    Build and sign with AGC-backed assets for the selected target.
         This is the only mode allowed for --install or --start.

Environment overrides for AGC mode:
  HARMONY_UI_AGC_SIGNING_DIR
  HARMONY_UI_AGC_CREDENTIALS_FILE
  HARMONY_UI_AGC_P12
  HARMONY_UI_AGC_CERT
  HARMONY_UI_AGC_PROFILE
  HARMONY_UI_AGC_ALIAS
  HARMONY_UI_AGC_STORE_PASSWORD
  HARMONY_UI_AGC_KEY_PASSWORD
  HARMONY_UI_DEBUG_DEVICE_IDS
  HARMONY_UI_CONNECT_KEY
EOF
}

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --target)
      shift
      if [[ "$#" -eq 0 ]]; then
        echo "Missing value for --target" >&2
        print_usage >&2
        exit 1
      fi
      TARGET_PACKAGE="$1"
      ;;
    --target=*)
      TARGET_PACKAGE="${1#*=}"
      ;;
    --mode)
      shift
      if [[ "$#" -eq 0 ]]; then
        echo "Missing value for --mode" >&2
        print_usage >&2
        exit 1
      fi
      SIGN_MODE="$1"
      ;;
    --mode=*)
      SIGN_MODE="${1#*=}"
      ;;
    --install)
      INSTALL_AFTER_BUILD=1
      ;;
    --start)
      INSTALL_AFTER_BUILD=1
      START_AFTER_INSTALL=1
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      print_usage >&2
      exit 1
      ;;
  esac
  shift
done

if [[ -z "$SIGN_MODE" ]]; then
  if [[ "$INSTALL_AFTER_BUILD" -eq 1 ]]; then
    SIGN_MODE="agc"
  else
    SIGN_MODE="local"
  fi
fi

case "$SIGN_MODE" in
  local|agc)
    ;;
  *)
    echo "Unsupported --mode value: $SIGN_MODE" >&2
    print_usage >&2
    exit 1
    ;;
esac

# 根据目标包解析项目目录、包名和 AGC 签名目录
if [[ -z "$TARGET_PACKAGE" ]]; then
  TARGET_PACKAGE="ui"
fi

case "$TARGET_PACKAGE" in
  ui)
    PROJECT_DIR="$UI_PROJECT_DIR"
    BUNDLE_NAME="$UI_BUNDLE_NAME"
    AGC_SIGNING_DIR_DEFAULT="$UI_AGC_SIGNING_DIR_DEFAULT"
    ;;
  minilab)
    PROJECT_DIR="$MINILAB_PROJECT_DIR"
    BUNDLE_NAME="$MINILAB_BUNDLE_NAME"
    AGC_SIGNING_DIR_DEFAULT="$MINILAB_AGC_SIGNING_DIR_DEFAULT"
    ;;
  *)
    echo "Unsupported --target value: $TARGET_PACKAGE (expected ui or minilab)" >&2
    print_usage >&2
    exit 1
    ;;
esac

UNSIGNED_HAP="$PROJECT_DIR/entry/build/default/outputs/default/entry-default-unsigned.hap"
LOCAL_SIGNING_DIR="$PROJECT_DIR/.signing/local-debug"
LOCAL_ROOT_CA_CERT="$LOCAL_SIGNING_DIR/openharmony-root-ca.cer"
LOCAL_APP_CA_CERT="$LOCAL_SIGNING_DIR/openharmony-app-ca.cer"
LOCAL_APP_KEYSTORE="$LOCAL_SIGNING_DIR/ui-app-keypair.p12"
LOCAL_APP_CERT_CHAIN="$LOCAL_SIGNING_DIR/ui-app-debug.cer"
LOCAL_APP_LEAF_CERT="$LOCAL_SIGNING_DIR/ui-app-leaf.pem"
LOCAL_PROFILE_JSON="$LOCAL_SIGNING_DIR/ui-profile.json"
LOCAL_SIGNED_PROFILE="$LOCAL_SIGNING_DIR/ui-profile-signed.p7b"
LOCAL_VERIFY_PROFILE_JSON="$LOCAL_SIGNING_DIR/ui-profile-verify.json"
LOCAL_VERIFY_CERT_CHAIN="$LOCAL_SIGNING_DIR/ui-app-verify-cert-chain.cer"
LOCAL_VERIFY_PROFILE_OUT="$LOCAL_SIGNING_DIR/ui-app-verify-profile.p7b"
LOCAL_SIGNED_OUTPUT_DIR="$PROJECT_DIR/entry/build/deviceDebug/outputs/deviceDebug"
LOCAL_SIGNED_HAP="$LOCAL_SIGNED_OUTPUT_DIR/entry-deviceDebug-signed.hap"

AGC_SIGNING_DIR="${HARMONY_UI_AGC_SIGNING_DIR:-$AGC_SIGNING_DIR_DEFAULT}"
AGC_CREDENTIALS_FILE="${HARMONY_UI_AGC_CREDENTIALS_FILE:-$AGC_SIGNING_DIR/credentials.txt}"
AGC_VERIFY_DIR="$PROJECT_DIR/.signing/agc-debug"
AGC_VERIFY_PROFILE_JSON="$AGC_VERIFY_DIR/ui-profile-verify.json"
AGC_VERIFY_CERT_CHAIN="$AGC_VERIFY_DIR/ui-app-verify-cert-chain.cer"
AGC_VERIFY_PROFILE_OUT="$AGC_VERIFY_DIR/ui-app-verify-profile.p7b"
AGC_SIGNED_OUTPUT_DIR="$PROJECT_DIR/entry/build/agcDeviceDebug/outputs/agcDeviceDebug"
AGC_SIGNED_HAP="$AGC_SIGNED_OUTPUT_DIR/entry-agcDeviceDebug-signed.hap"

echo "Target: $TARGET_PACKAGE ($BUNDLE_NAME)"
echo "Project: $PROJECT_DIR"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Missing required file: $path" >&2
    exit 1
  fi
}

read_key_value_file() {
  local file_path="$1"
  local requested_key="$2"
  if [[ ! -f "$file_path" ]]; then
    return 0
  fi
  python3 - <<'PY' "$file_path" "$requested_key"
import sys

path, requested_key = sys.argv[1:3]
for line in open(path, 'r', encoding='utf-8'):
    stripped = line.strip()
    if not stripped or stripped.startswith('#') or stripped.startswith('!') or '=' not in line:
        continue
    key, value = line.split('=', 1)
    if key.strip() == requested_key:
        print(value.strip())
        break
PY
}

read_local_property_device_ids() {
  local value
  value="$(read_key_value_file "$LOCAL_PROPERTIES" "HARMONY_UI_DEBUG_DEVICE_IDS")"
  if [[ -n "$value" ]]; then
    printf '%s\n' "$value"
    return 0
  fi
  read_key_value_file "$LOCAL_PROPERTIES" "HARMONY_DEBUG_DEVICE_IDS"
}

read_connected_targets() {
  if ! command -v hdc >/dev/null 2>&1; then
    return 0
  fi
  hdc list targets 2>/dev/null | awk 'NF && $0 ~ /^[A-Fa-f0-9]{64}$/ { print $0 }'
}

read_install_targets() {
  if ! command -v hdc >/dev/null 2>&1; then
    return 0
  fi
  hdc list targets 2>/dev/null | awk 'NF && $0 !~ /^\[/ { print $0 }'
}

decode_profile_json() {
  local profile_path="$1"
  local decoded_json="$2"
  run_sign_tool verify-profile -inFile "$profile_path" -outFile "$decoded_json" >/dev/null
}

read_profile_device_ids() {
  local profile_path="$1"
  if [[ ! -f "$profile_path" ]]; then
    return 0
  fi
  local tmp_json
  tmp_json="$(mktemp --suffix=.json)"
  decode_profile_json "$profile_path" "$tmp_json"
  python3 - <<'PY' "$tmp_json"
import json
import sys

decoded = json.load(open(sys.argv[1], 'r', encoding='utf-8'))
for value in decoded.get('content', {}).get('debug-info', {}).get('device-ids', []):
    print(value)
PY
  rm -f "$tmp_json"
}

resolve_device_ids() {
  {
    if [[ -n "${HARMONY_UI_DEBUG_DEVICE_IDS:-}" ]]; then
      printf '%s\n' "$HARMONY_UI_DEBUG_DEVICE_IDS"
    fi
    read_local_property_device_ids
    read_connected_targets
    read_profile_device_ids "${HARMONY_UI_AGC_PROFILE:-}"
    read_profile_device_ids "$LEGACY_DEVICE_PROFILE"
  } | tr ',' '\n' | awk 'NF && !seen[$0]++ { print $0 }'
}

resolve_connect_key() {
  if [[ -n "${HARMONY_UI_CONNECT_KEY:-}" ]]; then
    printf '%s\n' "$HARMONY_UI_CONNECT_KEY"
    return 0
  fi
  read_install_targets | head -n 1
}

run_sign_tool() {
  java -jar "$HAP_SIGN_TOOL" "$@"
}

build_unsigned_hap() {
  (cd "$PROJECT_DIR" && "$HVIGORW" --mode module -p product=default -p buildMode=debug assembleHap --stacktrace)
}

export_cert() {
  local alias="$1"
  local output="$2"
  if [[ -f "$output" ]]; then
    return 0
  fi
  keytool -exportcert -rfc -storetype pkcs12 -keystore "$OPENHARMONY_KEYSTORE" -storepass "$OPENHARMONY_STORE_PASSWORD" -alias "$alias" -file "$output" >/dev/null
}

ensure_app_keypair() {
  if [[ -f "$LOCAL_APP_KEYSTORE" ]]; then
    return 0
  fi
  run_sign_tool generate-keypair \
    -keyAlias "$LOCAL_APP_KEY_ALIAS" \
    -keyPwd "$LOCAL_APP_KEY_PASSWORD" \
    -keyAlg ECC \
    -keySize NIST-P-256 \
    -keystoreFile "$LOCAL_APP_KEYSTORE" \
    -keystorePwd "$LOCAL_APP_KEY_PASSWORD"
}

ensure_app_cert_chain() {
  if [[ -f "$LOCAL_APP_CERT_CHAIN" ]]; then
    return 0
  fi
  run_sign_tool generate-app-cert \
    -keyAlias "$LOCAL_APP_KEY_ALIAS" \
    -keyPwd "$LOCAL_APP_KEY_PASSWORD" \
    -issuer "C=CN,O=OpenHarmony,OU=OpenHarmony Team,CN=OpenHarmony Application CA" \
    -issuerKeyAlias "openharmony application ca" \
    -issuerKeyPwd "$OPENHARMONY_STORE_PASSWORD" \
    -subject "C=CN,O=SmartSales,OU=HarmonyOS,CN=UI Verification Debug" \
    -validity 365 \
    -signAlg SHA256withECDSA \
    -rootCaCertFile "$LOCAL_ROOT_CA_CERT" \
    -subCaCertFile "$LOCAL_APP_CA_CERT" \
    -keystoreFile "$LOCAL_APP_KEYSTORE" \
    -keystorePwd "$LOCAL_APP_KEY_PASSWORD" \
    -issuerKeystoreFile "$OPENHARMONY_KEYSTORE" \
    -issuerKeystorePwd "$OPENHARMONY_STORE_PASSWORD" \
    -outForm certChain \
    -outFile "$LOCAL_APP_CERT_CHAIN"
}

extract_leaf_cert() {
  python3 - <<'PY' "$LOCAL_APP_CERT_CHAIN" "$LOCAL_APP_LEAF_CERT"
import re
import sys

chain_path, out_path = sys.argv[1:3]
text = open(chain_path, 'r', encoding='utf-8').read()
match = re.search(r'-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----', text, re.S)
if not match:
    raise SystemExit('failed to extract UI leaf certificate')
open(out_path, 'w', encoding='utf-8').write(match.group(0) + '\n')
PY
}

write_profile_json() {
  python3 - <<'PY' "$UNSIGNED_PROFILE_TEMPLATE" "$LOCAL_APP_LEAF_CERT" "$LOCAL_PROFILE_JSON" "$BUNDLE_NAME" "$@"
import json
import sys
import time
import uuid

template_path, leaf_cert_path, out_path, bundle_name, *device_ids = sys.argv[1:]
profile = json.load(open(template_path, 'r', encoding='utf-8'))
leaf_cert = open(leaf_cert_path, 'r', encoding='utf-8').read().strip() + '\n'
now = int(time.time())
profile['uuid'] = str(uuid.uuid4())
profile['validity']['not-before'] = now - 3600
profile['validity']['not-after'] = now + 30 * 24 * 60 * 60
profile['bundle-info']['bundle-name'] = bundle_name
profile['bundle-info']['development-certificate'] = leaf_cert
profile['bundle-info']['developer-id'] = 'OpenHarmony'
profile['debug-info'] = {
    'device-ids': device_ids,
    'device-id-type': 'udid'
}
json.dump(profile, open(out_path, 'w', encoding='utf-8'), indent=2)
open(out_path, 'a', encoding='utf-8').write('\n')
PY
}

sign_local_profile() {
  run_sign_tool sign-profile \
    -mode localSign \
    -keyAlias "openharmony application profile debug" \
    -keyPwd "$OPENHARMONY_STORE_PASSWORD" \
    -profileCertFile "$OPENHARMONY_PROFILE_CERT" \
    -inFile "$LOCAL_PROFILE_JSON" \
    -signAlg SHA256withECDSA \
    -keystoreFile "$OPENHARMONY_KEYSTORE" \
    -keystorePwd "$OPENHARMONY_STORE_PASSWORD" \
    -outFile "$LOCAL_SIGNED_PROFILE"
  run_sign_tool verify-profile -inFile "$LOCAL_SIGNED_PROFILE" -outFile "$LOCAL_VERIFY_PROFILE_JSON" >/dev/null
}

sign_local_hap() {
  mkdir -p "$LOCAL_SIGNED_OUTPUT_DIR"
  run_sign_tool sign-app \
    -mode localSign \
    -keyAlias "$LOCAL_APP_KEY_ALIAS" \
    -keyPwd "$LOCAL_APP_KEY_PASSWORD" \
    -appCertFile "$LOCAL_APP_CERT_CHAIN" \
    -profileFile "$LOCAL_SIGNED_PROFILE" \
    -inFile "$UNSIGNED_HAP" \
    -signAlg SHA256withECDSA \
    -keystoreFile "$LOCAL_APP_KEYSTORE" \
    -keystorePwd "$LOCAL_APP_KEY_PASSWORD" \
    -outFile "$LOCAL_SIGNED_HAP" \
    -compatibleVersion 12 \
    -signCode 1
  run_sign_tool verify-app -inFile "$LOCAL_SIGNED_HAP" -outCertChain "$LOCAL_VERIFY_CERT_CHAIN" -outProfile "$LOCAL_VERIFY_PROFILE_OUT" >/dev/null
}

find_first_file() {
  local search_dir="$1"
  shift
  local pattern
  for pattern in "$@"; do
    while IFS= read -r candidate; do
      if [[ -n "$candidate" ]]; then
        printf '%s\n' "$candidate"
        return 0
      fi
    done < <(compgen -G "$search_dir/$pattern" | sort)
  done
  return 1
}

resolve_agc_file() {
  local explicit_path="$1"
  shift
  if [[ -n "$explicit_path" ]]; then
    printf '%s\n' "$explicit_path"
    return 0
  fi
  if [[ ! -d "$AGC_SIGNING_DIR" ]]; then
    return 1
  fi
  find_first_file "$AGC_SIGNING_DIR" "$@"
}

resolve_agc_credential() {
  local env_value="$1"
  local key_name="$2"
  if [[ -n "$env_value" ]]; then
    printf '%s\n' "$env_value"
    return 0
  fi
  read_key_value_file "$AGC_CREDENTIALS_FILE" "$key_name"
}

ensure_agc_assets() {
  AGC_P12="$(resolve_agc_file "${HARMONY_UI_AGC_P12:-}" "*.p12" || true)"
  AGC_CERT="$(resolve_agc_file "${HARMONY_UI_AGC_CERT:-}" "*.cer" || true)"
  AGC_PROFILE="$(resolve_agc_file "${HARMONY_UI_AGC_PROFILE:-}" "*Debug.p7b" "*.p7b" || true)"
  AGC_KEY_ALIAS="$(resolve_agc_credential "${HARMONY_UI_AGC_ALIAS:-}" "alias")"
  AGC_STORE_PASSWORD="$(resolve_agc_credential "${HARMONY_UI_AGC_STORE_PASSWORD:-}" "store_password")"
  AGC_KEY_PASSWORD="$(resolve_agc_credential "${HARMONY_UI_AGC_KEY_PASSWORD:-}" "key_password")"

  if [[ -z "$AGC_P12" || -z "$AGC_CERT" || -z "$AGC_PROFILE" || -z "$AGC_KEY_ALIAS" || -z "$AGC_STORE_PASSWORD" || -z "$AGC_KEY_PASSWORD" ]]; then
    {
      echo "AGC signing assets for ${BUNDLE_NAME} (target: ${TARGET_PACKAGE}) are incomplete."
      echo "Expected them under: $AGC_SIGNING_DIR"
      echo "Required: .p12, .cer, .p7b, alias, store_password, key_password"
      echo "Note: .p12 and credentials are shared across targets, but the .p7b must match the target bundle."
    } >&2
    exit 1
  fi

  require_file "$AGC_P12"
  require_file "$AGC_CERT"
  require_file "$AGC_PROFILE"
  mkdir -p "$AGC_VERIFY_DIR"
}

validate_agc_keystore() {
  keytool -list -storetype pkcs12 -keystore "$AGC_P12" -storepass "$AGC_STORE_PASSWORD" -alias "$AGC_KEY_ALIAS" >/dev/null
}

validate_agc_profile() {
  decode_profile_json "$AGC_PROFILE" "$AGC_VERIFY_PROFILE_JSON"
  python3 - <<'PY' "$AGC_VERIFY_PROFILE_JSON" "$BUNDLE_NAME" "$AGC_PROFILE" "$@"
import json
import sys

decoded_path, bundle_name, profile_path, *device_ids = sys.argv[1:]
decoded = json.load(open(decoded_path, 'r', encoding='utf-8'))
content = decoded.get('content', {})
bundle_info = content.get('bundle-info', {})
profile_device_ids = content.get('debug-info', {}).get('device-ids', [])

errors = []
if not decoded.get('verifiedPassed'):
    errors.append('profile verification failed before bundle checks')
if content.get('issuer') != 'app_gallery':
    errors.append(f"expected issuer 'app_gallery' but got {content.get('issuer')!r}")
if bundle_info.get('bundle-name') != bundle_name:
    errors.append(
        f"expected bundle-name {bundle_name!r} but got {bundle_info.get('bundle-name')!r}"
    )
if not bundle_info.get('app-identifier'):
    errors.append('missing AGC app-identifier in signed profile')
if not profile_device_ids:
    errors.append('profile has no debug device ids')
if device_ids and not set(device_ids).intersection(profile_device_ids):
    errors.append(
        'current resolved device ids are not present in the AGC profile: '
        + ', '.join(profile_device_ids)
    )

if errors:
    print(f'AGC profile preflight failed for {profile_path}:', file=sys.stderr)
    for error in errors:
        print(f'  - {error}', file=sys.stderr)
    sys.exit(1)

print(
    'AGC profile validated:',
    bundle_info.get('bundle-name'),
    bundle_info.get('app-identifier'),
    ','.join(profile_device_ids),
)
PY
}

sign_agc_hap() {
  mkdir -p "$AGC_SIGNED_OUTPUT_DIR"
  run_sign_tool sign-app \
    -mode localSign \
    -keyAlias "$AGC_KEY_ALIAS" \
    -keyPwd "$AGC_KEY_PASSWORD" \
    -appCertFile "$AGC_CERT" \
    -profileFile "$AGC_PROFILE" \
    -inFile "$UNSIGNED_HAP" \
    -signAlg SHA256withECDSA \
    -keystoreFile "$AGC_P12" \
    -keystorePwd "$AGC_STORE_PASSWORD" \
    -outFile "$AGC_SIGNED_HAP" \
    -compatibleVersion 12 \
    -signCode 1
  run_sign_tool verify-app -inFile "$AGC_SIGNED_HAP" -outCertChain "$AGC_VERIFY_CERT_CHAIN" -outProfile "$AGC_VERIFY_PROFILE_OUT" >/dev/null
}

install_and_start() {
  local connect_key="$1"
  local signed_hap="$2"
  hdc -t "$connect_key" install -r "$signed_hap"
  if [[ "$START_AFTER_INSTALL" -eq 1 ]]; then
    hdc -t "$connect_key" shell aa start -a "$ABILITY_NAME" -b "$BUNDLE_NAME"
  fi
}

build_local_lane() {
  mkdir -p "$LOCAL_SIGNING_DIR"
  export_cert "openharmony application root ca" "$LOCAL_ROOT_CA_CERT"
  export_cert "openharmony application ca" "$LOCAL_APP_CA_CERT"
  ensure_app_keypair
  ensure_app_cert_chain
  extract_leaf_cert
  write_profile_json "${DEVICE_IDS[@]}"
  sign_local_profile
  sign_local_hap
  echo "Mode: local"
  echo "Signed HAP ready: $LOCAL_SIGNED_HAP"
  echo "Signed profile: $LOCAL_SIGNED_PROFILE"
  echo "Device ids: ${DEVICE_IDS[*]}"
  echo "Local mode only proves repeatable package signing. It does not prove device acceptance for ${BUNDLE_NAME}."
}

build_agc_lane() {
  ensure_agc_assets
  validate_agc_keystore
  validate_agc_profile "${DEVICE_IDS[@]}"
  sign_agc_hap
  echo "Mode: agc"
  echo "Signed HAP ready: $AGC_SIGNED_HAP"
  echo "Signed profile: $AGC_PROFILE"
  echo "Device ids: ${DEVICE_IDS[*]}"
}

main() {
  require_file "$HVIGORW"
  require_file "$HAP_SIGN_TOOL"
  require_file "$UNSIGNED_PROFILE_TEMPLATE"

  mapfile -t DEVICE_IDS < <(resolve_device_ids)
  if [[ "${#DEVICE_IDS[@]}" -eq 0 ]]; then
    echo "No UI debug device ids found. Set HARMONY_UI_DEBUG_DEVICE_IDS, add the key to local.properties, or provide an AGC profile that includes the target device." >&2
    exit 1
  fi

  build_unsigned_hap
  require_file "$UNSIGNED_HAP"

  case "$SIGN_MODE" in
    local)
      require_file "$OPENHARMONY_KEYSTORE"
      require_file "$OPENHARMONY_PROFILE_CERT"
      build_local_lane
      ;;
    agc)
      build_agc_lane
      ;;
  esac

  if [[ "$INSTALL_AFTER_BUILD" -eq 1 ]]; then
    if [[ "$SIGN_MODE" != "agc" ]]; then
      echo "Refusing to install from local signing mode. Use AGC-backed assets for ${BUNDLE_NAME} device proof." >&2
      exit 2
    fi
    local connect_key
    connect_key="$(resolve_connect_key || true)"
    if [[ -z "$connect_key" ]]; then
      echo "No connected target available for install. Signed HAP was still produced." >&2
      exit 3
    fi
    install_and_start "$connect_key" "$AGC_SIGNED_HAP"
    echo "Installed to target: $connect_key"
  fi
}

main "$@"
