#!/bin/bash

# schema-publish.sh - Script to publish Avro schemas to Schema Registry
# Usage: ./scripts/schema-publish.sh [--subject <subject-name>] [--schema <schema-file>] [--version <version>]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8081}"
SCHEMAS_DIR="$PROJECT_ROOT/common-events-avro/src/main/avro"

HELP_TEXT="Usage: $0 [OPTIONS]

Publish Avro schemas to Schema Registry.

Options:
  --subject <name>         Schema subject name (e.g., orders-value)
  --schema <file>          Path to Avro schema file
  --version <version>      Schema version (defaults to latest)
  --list-subjects          List all registered subjects
  --list-versions <subj>   List versions for a subject
  --help                   Show this help message

Examples:
  $0 --subject orders-value --schema schemas/OrderEvent.avsc
  $0 --list-subjects
  $0 --list-versions orders-value
"

# Default values
SUBJECT_NAME=""
SCHEMA_FILE=""
VERSION=""
LIST_SUBJECTS=false
LIST_VERSIONS=""
ACTION="publish"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --subject)
      SUBJECT_NAME="$2"
      shift 2
      ;;
    --schema)
      SCHEMA_FILE="$2"
      shift 2
      ;;
    --version)
      VERSION="$2"
      shift 2
      ;;
    --list-subjects)
      LIST_SUBJECTS=true
      ACTION="list_subjects"
      shift
      ;;
    --list-versions)
      LIST_VERSIONS="$2"
      ACTION="list_versions"
      shift 2
      ;;
    --help)
      echo "$HELP_TEXT"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "$HELP_TEXT"
      exit 1
      ;;
  esac
done

# Function to check if jq is available
check_jq() {
  if ! command -v jq &> /dev/null; then
    echo "Error: jq is required but not installed"
    echo "Install jq using: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
  fi
}

# Function to list all subjects
list_subjects() {
  echo "Listing registered subjects:"
  curl -s "$SCHEMA_REGISTRY_URL/subjects" | jq -r '.[]'
}

# Function to list versions for a subject
list_versions() {
  local subject="$1"
  echo "Listing versions for subject: $subject"
  curl -s "$SCHEMA_REGISTRY_URL/subjects/$subject/versions" | jq -r '.[]'
}

# Function to publish a schema
publish_schema() {
  local subject="$1"
  local schema_file="$2"
  local version="$3"
  
  # Validate inputs
  if [[ -z "$subject" ]]; then
    echo "Error: Subject name is required"
    exit 1
  fi
  
  if [[ -z "$schema_file" ]]; then
    echo "Error: Schema file is required"
    exit 1
  fi
  
  if [[ ! -f "$schema_file" ]]; then
    echo "Error: Schema file not found: $schema_file"
    exit 1
  fi
  
  echo "Publishing schema to subject: $subject"
  echo "Schema file: $schema_file"
  
  # Read schema content
  local schema_content
  schema_content=$(cat "$schema_file")
  
  # Prepare request payload
  local payload
  payload=$(jq -n --arg schema "$schema_content" '{schema: $schema}')
  
  # Publish schema
  local response
  response=$(curl -s -X POST \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    -d "$payload" \
    "$SCHEMA_REGISTRY_URL/subjects/$subject/versions")
  
  # Check response
  if echo "$response" | jq -e '.id' > /dev/null 2>&1; then
    local schema_id
    schema_id=$(echo "$response" | jq -r '.id')
    echo "Successfully published schema with ID: $schema_id"
  else
    echo "Failed to publish schema:"
    echo "$response" | jq '.'
    exit 1
  fi
}

# Main execution
check_jq

case "$ACTION" in
  "list_subjects")
    list_subjects
    ;;
  "list_versions")
    if [[ -z "$LIST_VERSIONS" ]]; then
      echo "Error: Subject name required for --list-versions"
      echo "$HELP_TEXT"
      exit 1
    fi
    list_versions "$LIST_VERSIONS"
    ;;
  "publish")
    publish_schema "$SUBJECT_NAME" "$SCHEMA_FILE" "$VERSION"
    ;;
  *)
    echo "Unknown action: $ACTION"
    echo "$HELP_TEXT"
    exit 1
    ;;
esac