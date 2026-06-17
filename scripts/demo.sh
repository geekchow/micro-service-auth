#!/usr/bin/env bash
# Interactive demo mode:
# - shell tracing is enabled with `set -x`
# - each major step pauses and waits for the user to type `yes`
# - any other input stops the script cleanly
set -euo pipefail
set -x

confirm_continue() {
  local answer

  set +x
  printf 'Step complete. Type yes to continue: '
  read -r answer

  if [[ "$answer" != "yes" ]]; then
    printf 'Stopping at user request.\n'
    exit 0
  fi

  set -x
}

run_step() {
  local name="$1"
  local function_name="$2"

  set +x
  printf '\n=== %s ===\n' "$name"
  set -x

  "$function_name"
  confirm_continue
}

wait_for_status() {
  local name="$1"
  local url="$2"
  local expected="$3"
  local attempts="${4:-60}"
  local code=""

  for ((i = 1; i <= attempts; i++)); do
    code=$(curl -s -o /dev/null -w '%{http_code}' "$url" || true)
    if [[ "$code" == "$expected" ]]; then
      return 0
    fi
    sleep 2
  done

  printf 'Timed out waiting for %s at %s, last status: %s\n' "$name" "$url" "$code" >&2
  return 1
}

resolve_compose_network() {
  local container_id
  container_id=$(docker compose ps -q keycloak)

  if [[ -z "$container_id" ]]; then
    printf 'Could not resolve keycloak container for Compose network lookup\n' >&2
    return 1
  fi

  docker inspect -f '{{range $name, $_ := .NetworkSettings.Networks}}{{printf "%s\n" $name}}{{end}}' "$container_id" | awk 'NR==1 {print; exit}'
}

compose_curl() {
  docker run --rm --network "$COMPOSE_NETWORK" curlimages/curl:8.8.0 "$@"
}

wait_for_compose_status() {
  local name="$1"
  local url="$2"
  local expected="$3"
  local attempts="${4:-60}"
  local code=""

  for ((i = 1; i <= attempts; i++)); do
    code=$(compose_curl -s -o /dev/null -w '%{http_code}' "$url" || true)
    if [[ "$code" == "$expected" ]]; then
      return 0
    fi
    sleep 2
  done

  printf 'Timed out waiting for %s at %s, last status: %s\n' "$name" "$url" "$code" >&2
  return 1
}

require_status() {
  local name="$1"
  local expected="$2"
  local url="$3"
  shift 3

  local output
  output=$(curl -i -sS "$url" "$@")
  local status
  status=$(printf '%s' "$output" | awk 'NR==1 {print $2}')

  if [[ "$status" != "$expected" ]]; then
    printf 'Check failed for %s: expected %s, got %s\n' "$name" "$expected" "$status" >&2
    printf '%s\n' "$output" >&2
    return 1
  fi

  printf '%s -> %s\n' "$name" "$status"
}

get_token() {
  local username="$1"
  local password="$2"

  curl -sS -X POST "http://localhost:9081/realms/banking-poc/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=password" \
    --data-urlencode "client_id=mobile-banking-app" \
    --data-urlencode "username=${username}" \
    --data-urlencode "password=${password}"
}

step_wait_for_dependencies() {
  wait_for_status "Keycloak realm" "http://localhost:9081/realms/banking-poc/.well-known/openid-configuration" "200"
  COMPOSE_NETWORK=$(resolve_compose_network)

  wait_for_compose_status "identity-bootstrap-service" "http://identity-bootstrap-service:8080/demo/users" "405"
  wait_for_status "Kong protected route" "http://localhost:8000/api/accounts/A-1001" "401"
}

step_create_alice() {
  printf 'Creating alice\n'
  compose_curl --fail-with-body -sS -X POST "http://identity-bootstrap-service:8080/demo/users" \
    -H "X-Demo-Bootstrap-Secret: ${DEMO_BOOTSTRAP_SECRET}" \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"Password123!","role":"customer","customerId":"C-1001","accountIds":["A-1001"]}' >/dev/null
}

step_create_ops_admin() {
  printf 'Creating ops-admin\n'
  compose_curl --fail-with-body -sS -X POST "http://identity-bootstrap-service:8080/demo/users" \
    -H "X-Demo-Bootstrap-Secret: ${DEMO_BOOTSTRAP_SECRET}" \
    -H "Content-Type: application/json" \
    -d '{"username":"ops-admin","password":"Password123!","role":"ops-admin","customerId":"C-9999","accountIds":["A-1001","A-2001"]}' >/dev/null
}

step_get_alice_token() {
  ALICE_TOKEN=$(get_token "alice" "Password123!" | jq -r '.access_token')

  if [[ -z "$ALICE_TOKEN" || "$ALICE_TOKEN" == "null" ]]; then
    printf 'Failed to obtain token for alice\n' >&2
    exit 1
  fi
}

step_get_ops_admin_token() {
  OPS_TOKEN=$(get_token "ops-admin" "Password123!" | jq -r '.access_token')

  if [[ -z "$OPS_TOKEN" || "$OPS_TOKEN" == "null" ]]; then
    printf 'Failed to obtain token for ops-admin\n' >&2
    exit 1
  fi
}

step_verify_alice_own_account() {
  require_status "alice own account" "200" "http://localhost:8000/api/accounts/A-1001" \
    -H "Authorization: Bearer ${ALICE_TOKEN}"
}

step_verify_alice_foreign_account() {
  require_status "alice foreign account" "403" "http://localhost:8000/api/accounts/A-2001" \
    -H "Authorization: Bearer ${ALICE_TOKEN}"
}

step_verify_ops_admin_account_access() {
  require_status "ops-admin account access" "200" "http://localhost:8000/api/accounts/A-2001" \
    -H "Authorization: Bearer ${OPS_TOKEN}"
}

step_verify_missing_token() {
  require_status "missing token" "401" "http://localhost:8000/api/accounts/A-1001"
}

step_verify_tampered_token() {
  TAMPERED_TOKEN="${ALICE_TOKEN}x"
  require_status "tampered token" "401" "http://localhost:8000/api/accounts/A-1001" \
    -H "Authorization: Bearer ${TAMPERED_TOKEN}"
}

DEMO_BOOTSTRAP_SECRET="${DEMO_BOOTSTRAP_SECRET:-demo-bootstrap-secret}"

run_step "Wait for dependencies" step_wait_for_dependencies
run_step "Create alice demo user" step_create_alice
run_step "Create ops-admin demo user" step_create_ops_admin
run_step "Get alice token" step_get_alice_token
run_step "Get ops-admin token" step_get_ops_admin_token
run_step "Verify alice own account access" step_verify_alice_own_account
run_step "Verify alice foreign account denial" step_verify_alice_foreign_account
run_step "Verify ops-admin account access" step_verify_ops_admin_account_access
run_step "Verify missing token rejection" step_verify_missing_token

set +x
printf '\n=== Verify tampered token rejection ===\n'
set -x
step_verify_tampered_token

set +x
printf 'Demo complete.\n'
