#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"
PID_FILE="$PROJECT_ROOT/run/login-auth-demo.pid"
LOG_FILE="$PROJECT_ROOT/logs/login-auth-demo.log"
JAR_FILE="$PROJECT_ROOT/target/login-auth-demo.jar"
HEALTH_URL="http://127.0.0.1:18080/actuator/health"

cd "$PROJECT_ROOT"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing environment file: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$PROJECT_ROOT/.env"
set +a

REQUIRED_VARIABLES=(
  SPRING_PROFILES_ACTIVE
  DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD
  ADMIN_USERNAME ADMIN_PASSWORD
  LLM_API_KEY LLM_BASE_URL LLM_MODEL
)

for variable in "${REQUIRED_VARIABLES[@]}"; do
  if [[ -z "${!variable:-}" ]]; then
    echo "Missing required environment variable: $variable" >&2
    exit 1
  fi
done

if [[ ",$SPRING_PROFILES_ACTIVE," != *",prod,"* ]]; then
  echo "SPRING_PROFILES_ACTIVE must include prod" >&2
  exit 1
fi

if ! command -v nc >/dev/null 2>&1; then
  echo "Database connectivity check requires nc (netcat)" >&2
  exit 1
fi
if ! nc -z -w 3 "$DB_HOST" "$DB_PORT" >/dev/null 2>&1; then
  echo "Database is not reachable at ${DB_HOST}:${DB_PORT}" >&2
  exit 1
fi

mkdir -p "$PROJECT_ROOT/run"
if [[ -f "$PID_FILE" ]]; then
  PID="$(tr -d '[:space:]' < "$PID_FILE")"
  if [[ "$PID" =~ ^[0-9]+$ ]] && kill -0 "$PID" 2>/dev/null; then
    COMMAND="$(ps -p "$PID" -o command= 2>/dev/null || true)"
    if [[ "$COMMAND" == *"login-auth-demo.jar"* ]]; then
      echo "login-auth-demo is already started (PID: $PID)"
      exit 0
    fi
    echo "Ignoring stale PID file that points to another process" >&2
  fi
  rm -f "$PID_FILE"
fi

./mvnw clean verify

if [[ ! -f "$JAR_FILE" ]]; then
  echo "Build completed but Jar was not found: $JAR_FILE" >&2
  exit 1
fi

mkdir -p "$PROJECT_ROOT/logs" "$PROJECT_ROOT/run"
nohup java \
  -Xms128m \
  -Xmx512m \
  -jar target/login-auth-demo.jar \
  > "$LOG_FILE" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"

for _ in {1..30}; do
  if ! kill -0 "$PID" 2>/dev/null; then
    break
  fi
  HEALTH="$(curl --connect-timeout 2 --max-time 3 --silent "$HEALTH_URL" 2>/dev/null || true)"
  if [[ "$HEALTH" == *'"status":"UP"'* ]]; then
    echo "login-auth-demo started"
    echo "PID: $PID"
    echo "Health: UP"
    exit 0
  fi
  sleep 1
done

echo "login-auth-demo failed to start; recent application log:" >&2
tail -n 50 "$LOG_FILE" >&2 || true
if kill -0 "$PID" 2>/dev/null; then
  COMMAND="$(ps -p "$PID" -o command= 2>/dev/null || true)"
  if [[ "$COMMAND" == *"login-auth-demo.jar"* ]]; then
    kill "$PID" 2>/dev/null || true
    for _ in {1..10}; do
      kill -0 "$PID" 2>/dev/null || break
      sleep 1
    done
    kill -9 "$PID" 2>/dev/null || true
  fi
fi
rm -f "$PID_FILE"
exit 1
