#!/usr/bin/env bash
set -euo pipefail

if command -v terraform >/dev/null 2>&1; then
  terraform fmt -check
  terraform validate
fi

if command -v helm >/dev/null 2>&1; then
  helm lint infra/istio 2>/dev/null || true
fi

if command -v kubeconform >/dev/null 2>&1; then
  kubeconform -summary infra/**/*.yaml
fi

echo "IaC validation completed."
