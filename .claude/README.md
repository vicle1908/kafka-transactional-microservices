# Project-Level Claude Code Hooks

This directory contains project-specific Claude Code configuration and hooks.

## Structure

```text
.claude/
├── settings.json          # Project-level Claude settings
├── scripts/
│   └── linting_hook.sh    # Linting automation script
├── logs/
│   └── linting.log       # Hook execution logs
└── README.md              # This file
```

## Configuration

### `settings.json`

Configures PostToolUse hooks that automatically run after `Write` or `Edit` operations on relevant files:

- **Trigger**: `Write|Edit` operations
- **Files**: `.md`, `.yml`, `.yaml` files and files in `.github/` or `infra/` directories
- **Tools**: markdownlint, yamllint, actionlint

### `linting_hook.sh`

Comprehensive linting script that:

1. **Markdown Files (.md)**
   - Runs `markdownlint` for formatting validation
   - Detects heading issues, list formatting, trailing spaces

2. **YAML Files (.yml, .yaml)**
   - Runs `yamllint` for syntax and style validation
   - Special handling for GitHub Actions workflows

3. **GitHub Actions Workflows**
   - Runs `actionlint` for workflow validation
   - Detects outdated actions, syntax issues, best practices

4. **Infrastructure Files**
   - Additional yamllint validation for `infra/` directory

## Usage

The hooks run automatically whenever you edit relevant files. You'll see output like:

```text
📝 Running markdownlint on: example.md
✅ Markdownlint passed

✅ All linting checks passed!
```

Or if issues are found:

```text
📝 Running markdownlint on: example.md
❌ Markdownlint found issues:
example.md:5:1 MD018/no-missing-space-atx No space after hash

⚠️ Some linting issues were found. Please review the output above.
💡 Tip: You can run the linting tools manually to fix issues:
   markdownlint "docs/**/*.md"
```

## Logging

Hook execution is logged to `.claude/logs/linting.log` with timestamps and file processing details.

## Manual Commands

If you need to run linting manually:

```bash
# Markdown linting
markdownlint "docs/**/*.md"

# YAML linting
yamllint .github/**/*.yml infra/**/*.yml

# GitHub Actions linting
actionlint .github/workflows/*.yml
```

## Customization

To modify hook behavior:

1. Edit `.claude/settings.json` to change trigger patterns
2. Modify `.claude/scripts/linting_hook.sh` to adjust linting rules or add new tools
3. Check `.claude/logs/linting.log` for debugging information

## Benefits

- **Immediate Feedback**: Get linting results right after editing files
- **Consistent Quality**: Enforce formatting standards across the project
- **CI/CD Alignment**: Uses same tools as CI pipeline
- **Project-Specific**: Configuration travels with the project for team consistency
