#!/bin/bash
# Install Git hooks for the project
# Usage: ./scripts/install-hooks.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

echo "🔧 Installing Git hooks..."

# Create hooks directory if it doesn't exist
mkdir -p "$HOOKS_DIR"

# Install pre-commit hook
if [ -f "$SCRIPT_DIR/hooks/pre-commit" ]; then
    cp "$SCRIPT_DIR/hooks/pre-commit" "$HOOKS_DIR/pre-commit"
    chmod +x "$HOOKS_DIR/pre-commit"
    echo "✅ Installed pre-commit hook"
else
    echo "❌ pre-commit hook not found at $SCRIPT_DIR/hooks/pre-commit"
    exit 1
fi

echo ""
echo "🎉 Git hooks installed successfully!"
echo ""
echo "The following hooks are now active:"
echo "  - pre-commit: ktlint (auto-fix), markdownlint-cli2 (auto-fix), yamllint, detekt"
echo ""
echo "To uninstall, run: rm .git/hooks/pre-commit"
echo ""
echo "Optional: Install pre-commit framework for additional features:"
echo "  pip install pre-commit && pre-commit install"
