set -euo pipefail

# Function to lookup SHA for a specific action@tag
lookup_sha() {
  local repo="$1" tag="$2"
  local url="https://github.com/${repo}.git"
  
  # Try different ref formats
  for ref in "refs/tags/${tag}" "refs/heads/${tag}"; do
    if output=$(git ls-remote "$url" "$ref" 2>/dev/null | head -n1); then
      sha=$(echo "$output" | awk "{print \$1}")
      if [[ -n "$sha" && ${#sha} -eq 40 ]]; then
        echo "$sha"
        return 0
      fi
    fi
  done
  
  echo "ERROR: Could not resolve $repo@$tag" >&2
  return 1
}

# Lookup all actions
declare -A ACTIONS=(
  ["actions/checkout"]="v4"
  ["actions/setup-java"]="v4" 
  ["gradle/actions/wrapper-validation"]="v3"
  ["gradle/actions/setup-gradle"]="v4"
  ["actions/setup-node"]="v4"
  ["docker/metadata-action"]="v5"
  ["docker/setup-buildx-action"]="v3"
  ["docker/login-action"]="v3"
  ["actions/cache"]="v4"
  ["docker/build-push-action"]="v5"
  ["aquasecurity/trivy-action"]="0.33.1"
  ["actions/upload-artifact"]="v4"
  ["docker/setup-compose-action"]="v1"
  ["hashicorp/setup-terraform"]="v3"
  ["bmuschko/setup-kubeconform"]="v1"
  ["github/codeql-action/init"]="v3"
  ["github/codeql-action/autobuild"]="v3" 
  ["github/codeql-action/analyze"]="v3"
  ["actions/dependency-review-action"]="v4"
  ["step-security/harden-runner"]="v2"
)

echo "Resolving action SHAs..."
for action in "${!ACTIONS[@]}"; do
  tag="${ACTIONS[$action]}"
  if sha=$(lookup_sha "$action" "$tag"); then
    echo "$action@$tag -> $sha"
  else
    echo "FAILED: $action@$tag" >&2
  fi
done
