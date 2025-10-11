#!/bin/bash

# Branch Protection Setup Script
# This script configures main branch protection with required status checks

set -e

REPO="vicle1908/kafka-transactional-microservices"
BRANCH="main"

echo "🔒 Setting up branch protection for $REPO:$BRANCH"

# Required status checks based on our workflows
REQUIRED_CHECKS=(
  "build"
  "docs" 
  "integration-test"
)

# Create the branch protection rule
gh api \
  --method PUT \
  "repos/$REPO/branches/$BRANCH/protection" \
  --field required_status_checks='{"strict":true,"contexts":["build","docs","integration-test"]}' \
  --field enforce_admins=true \
  --field required_pull_request_reviews='{"required_approving_review_count":1,"dismiss_stale_reviews":true,"require_code_owner_reviews":false}' \
  --field restrictions=null \
  --field required_linear_history=true \
  --field allow_force_pushes=false \
  --field allow_deletions=false

echo "✅ Branch protection configured successfully!"

echo "📊 Current protection status:"
gh api "repos/$REPO/branches/$BRANCH/protection" --jq '
{
  "required_status_checks": .required_status_checks.contexts,
  "required_reviews": .required_pull_request_reviews.required_approving_review_count,
  "enforce_admins": .enforce_admins.enabled,
  "linear_history": .required_linear_history.enabled
}'

echo ""
echo "🚀 Branch protection is now active with the following rules:"
echo "   - Require status checks: build, docs, integration-test"
echo "   - Require pull request reviews (1 approving review)"
echo "   - Dismiss stale reviews when new commits are pushed"
echo "   - Require linear history (no merge commits)"
echo "   - Prevent force pushes and branch deletion"
echo "   - Apply rules to administrators"