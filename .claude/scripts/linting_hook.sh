#!/bin/bash

# Claude Code Linting Hook
# Runs markdownlint, actionlint, and yamllint after editing relevant files

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Log file for debugging
if [ -n "$CLAUDE_PROJECT_DIR" ]; then
    LOG_FILE="$CLAUDE_PROJECT_DIR/.claude/logs/linting.log"
else
    LOG_FILE="$HOME/.claude/logs/linting.log"
fi
mkdir -p "$(dirname "$LOG_FILE")"

# Function to log messages
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to run linting on a file
run_linting() {
    local file="$1"
    local has_errors=false
    local output=""

    log "Running linting checks for: $file"

    # Skip if file doesn't exist
    if [ ! -f "$file" ]; then
        log "File not found: $file"
        return 0
    fi

    # Get file extension and path
    extension="${file##*.}"
    filename=$(basename "$file")
    dirname=$(dirname "$file")

    # Determine file type for appropriate linting
    case "$extension" in
        md)
            output+="📝 Running markdownlint on: $filename\n"

            if command_exists markdownlint; then
                # Check if it's in docs directory or any markdown file
                if [[ "$file" == *"docs"* ]] || [[ "$extension" == "md" ]]; then
                    markdownlint_output=$(markdownlint "$file" 2>&1)
                    if [ $? -ne 0 ]; then
                        output+="${RED}  ❌ Markdownlint found issues:${NC}\n"
                        output+="$markdownlint_output\n"
                        has_errors=true
                    else
                        output+="${GREEN}  ✅ Markdownlint passed${NC}\n"
                    fi
                fi
            else
                output+="${YELLOW}  ⚠️  markdownlint not available${NC}\n"
            fi
            ;;

        yml|yaml)
            output+="📝 Running YAML linting on: $filename\n"

            # Check for yamllint
            if command_exists yamllint; then
                yamllint_output=$(yamllint "$file" 2>&1)
                if [ $? -ne 0 ]; then
                    output+="${RED}  ❌ Yamllint found issues:${NC}\n"
                    output+="$yamllint_output\n"
                    has_errors=true
                else
                    output+="${GREEN}  ✅ Yamllint passed${NC}\n"
                fi
            else
                output+="${YELLOW}  ⚠️  yamllint not available${NC}\n"
            fi

            # Special handling for GitHub Actions workflows
            if [[ "$file" == *".github/workflows"* ]] || [[ "$file" == *".github/"* ]]; then
                output+="  🔍 Checking GitHub Actions workflow...\n"

                if command_exists actionlint; then
                    actionlint_output=$(actionlint "$file" 2>&1)
                    if [ $? -ne 0 ]; then
                        output+="${RED}  ❌ Actionlint found issues:${NC}\n"
                        output+="$actionlint_output\n"
                        has_errors=true
                    else
                        output+="${GREEN}  ✅ Actionlint passed${NC}\n"
                    fi
                else
                    output+="${YELLOW}  ⚠️  actionlint not available${NC}\n"
                fi
            fi
            ;;

        *)
            # For other files, check if they might be configuration files
            if [[ "$file" == *".github"* ]] || [[ "$file" == *"infra"* ]] || [[ "$file" == *".kube"* ]]; then
                output+="📝 Checking configuration file: $filename\n"

                # Try yamllint for any YAML-like files
                if command_exists yamllint; then
                    yamllint_output=$(yamllint "$file" 2>&1)
                    if [ $? -ne 0 ]; then
                        output+="${RED}  ❌ Yamllint found issues:${NC}\n"
                        output+="$yamllint_output\n"
                        has_errors=true
                    else
                        output+="${GREEN}  ✅ Yamllint passed${NC}\n"
                    fi
                fi
            fi
            ;;
    esac

    # Output the results
    if [ -n "$output" ]; then
        echo -e "$output"
    fi

    return $([ "$has_errors" = true ] && echo 1 || echo 0)
}

# Function to check if we should run linting (avoid excessive runs)
should_run_linting() {
    local file="$1"

    # Skip certain files that don't need linting
    if [[ "$file" == *"/.git/"* ]] || [[ "$file" == *"node_modules"* ]] || [[ "$file" == *".DS_Store"* ]] || [[ "$file" == *"*.log"* ]]; then
        return 1
    fi

    # Only run on relevant file types
    local extension="${file##*.}"
    case "$extension" in
        md|yml|yaml)
            return 0
            ;;
        *)
            # Check if it's in special directories
            if [[ "$file" == *".github"* ]] || [[ "$file" == *"infra"* ]]; then
                return 0
            fi
            return 1
            ;;
    esac
}

# Main execution
log "=== Linting hook started ==="
log "Files to check: $CLAUDE_FILE_PATHS"
log "Tool used: $CLAUDE_TOOL_NAME"

overall_status=0
processed_files=0
relevant_files=0

# Process each file
for file in $CLAUDE_FILE_PATHS; do
    if [ -n "$file" ] && should_run_linting "$file"; then
        ((relevant_files++))
        run_linting "$file"
        if [ $? -ne 0 ]; then
            overall_status=1
        fi
        ((processed_files++))
    fi
done

log "Processed $processed_files relevant files out of $relevant_files candidates, exit status: $overall_status"

# Provide summary
if [ $processed_files -gt 0 ]; then
    echo ""
    if [ $overall_status -eq 0 ]; then
        echo -e "${GREEN}✅ All linting checks passed!${NC}"
    else
        echo -e "${YELLOW}⚠️  Some linting issues were found. Please review the output above.${NC}"
        echo -e "${BLUE}💡 Tip: You can run the linting tools manually to fix issues:${NC}"
        echo -e "   ${BLUE}   markdownlint \"docs/**/*.md\"${NC}"
        echo -e "   ${BLUE}   yamllint .github/**/*.yml infra/**/*.yml${NC}"
        echo -e "   ${BLUE}   actionlint .github/workflows/*.yml${NC}"
    fi
elif [ $relevant_files -gt 0 ]; then
    log "Found $relevant_files relevant files but none were processed"
else
    log "No relevant files found for linting"
fi

exit $overall_status