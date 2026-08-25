#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$PROJECT_ROOT/run/login-auth-demo.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "login-auth-demo is not running"
  exit 0
fi

PID="$(tr -d '[:space:]' < "$PID_FILE")"
if [[ ! "$PID" =~ ^[0-9]+$ ]] || ! kill -0 "$PID" 2>/dev/null; then
  rm -f "$PID_FILE"
  echo "Removed stale PID file; login-auth-demo is not running"
  exit 0
fi

COMMAND="$(ps -p "$PID" -o command= 2>/dev/null || true)"
if [[ "$COMMAND" != *"login-auth-demo.jar"* ]]; then
  echo "Refusing to stop PID $PID because it is not login-auth-demo.jar" >&2
  exit 1
fi

kill "$PID"
for _ in {1..20}; do
  if ! kill -0 "$PID" 2>/dev/null; then
    rm -f "$PID_FILE"
    echo "login-auth-demo stopped"
    exit 0
  fi
  sleep 1
done

kill -9 "$PID"
for _ in {1..5}; do
  kill -0 "$PID" 2>/dev/null || break
  sleep 1
done

if kill -0 "$PID" 2>/dev/null; then
  echo "Failed to stop login-auth-demo (PID: $PID)" >&2
  exit 1
fi

rm -f "$PID_FILE"
echo "login-auth-demo stopped"
