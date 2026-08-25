#!/usr/bin/env bash
set -euo pipefail

JAR_PATH="${1:-target/login-auth-demo.jar}"
if [[ ! -f "$JAR_PATH" ]]; then
  echo "Jar not found: $JAR_PATH" >&2
  exit 1
fi

install -o login-auth-demo -g login-auth-demo -m 0644 "$JAR_PATH" /opt/login-auth-demo/login-auth-demo.jar
systemctl daemon-reload
systemctl restart login-auth-demo
systemctl --no-pager --full status login-auth-demo
