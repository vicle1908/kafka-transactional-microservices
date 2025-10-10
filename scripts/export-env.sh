#!/usr/bin/env bash
# Usage: source scripts/export-env.sh
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ROOT_DIR/.env"
  set +a
  echo "Loaded env from $ROOT_DIR/.env"
else
  echo "No .env found at $ROOT_DIR/.env (skipping). Copy .env.example to .env first."
fi
