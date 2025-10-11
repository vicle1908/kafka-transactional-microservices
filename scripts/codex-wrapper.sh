#!/bin/bash

# Codex CLI wrapper script for clink integration
# This script ensures proper argument forwarding for codex CLI

set -euo pipefail

# Project root directory
PROJECT_DIR="/Users/vinhlekhanh/Library/Mobile Documents/com~apple~CloudDocs/project/microservices"

# Build codex command with all required flags
CMD=(
    codex
    exec
    --cd "$PROJECT_DIR"
    --skip-git-repo-check
    --json
    --dangerously-bypass-approvals-and-sandbox
)

# Add any additional arguments passed to this script
if [ $# -gt 0 ]; then
    CMD+=("$@")
fi

# Execute the command
echo "Running: ${CMD[*]}" >&2
exec "${CMD[@]}"