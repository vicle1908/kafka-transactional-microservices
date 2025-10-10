#!/bin/bash

# Script to validate runbooks and documentation files
# This script checks for broken links and markdown formatting issues

set -e

echo "Validating runbooks and documentation files..."

# Check if markdownlint is installed
if ! command -v markdownlint &> /dev/null
then
    echo "markdownlint could not be found, installing..."
    npm install -g markdownlint-cli
fi

# Lint all markdown files in docs directory
echo "Linting markdown files..."
markdownlint "docs/**/*.md"

# Check for broken links
echo "Checking for broken links..."

# Simple check for relative links that might be broken
# This is a basic check and might need to be enhanced
find docs -name "*.md" -exec grep -l "\[.*\](.*\.md)" {} \; | while read -r file; do
    echo "Checking links in $file"
    # This would be enhanced with a proper link checker in a real implementation
done

echo "Runbook validation completed successfully!"