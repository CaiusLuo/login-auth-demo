#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-https://127.0.0.1}"
CURL_TLS_ARGS=()
if [[ "${ALLOW_INSECURE_TLS:-false}" == "true" ]]; then
  CURL_TLS_ARGS+=(--insecure)
fi

curl "${CURL_TLS_ARGS[@]}" --fail --silent --show-error "$BASE_URL/actuator/health/readiness"
echo

STATUS="$(curl "${CURL_TLS_ARGS[@]}" --silent --output /dev/null --write-out '%{http_code}' "$BASE_URL/api/app")"
if [[ "$STATUS" != "401" ]]; then
  echo "Expected anonymous /api/app to return 401, got $STATUS" >&2
  exit 1
fi
echo "Smoke test passed: health is ready and anonymous authorization is enforced."
