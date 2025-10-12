# Repository Administration Runbook

This runbook documents the standard branch protection and required checks for the microservices repo.

## Branch Protection (main)

- Require pull request reviews: at least 1 approval
- Dismiss stale pull request approvals when new commits are pushed
- Require branches to be up to date before merging
- Restrict who can push to matching branches: disallow direct pushes to main (merge via PR only)

## Required Status Checks (must pass)

- CI (CI workflow)
- Workflow Validation (actionlint + yamllint)
- Dependency Review (fail-on-severity: high)
- Schema Compatibility (schemaCompatibilityCheck)
- CodeQL (init/autobuild/analyze)
- Security Scan (Trivy reports can be scheduled/non-blocking; optionally gate on a separate job)
- Gitleaks (secrets scanning)

## Recommended Settings

- Enforce conversation resolution before merging
- Require signed commits if your organization mandates it
- Automatically delete head branches after merge

## Maintenance

- Keep actions pinned to full-length commit SHAs (supply-chain hardening)
- Add step-security/harden-runner to all jobs (audit → enforce)
- Review and update required checks when workflows change
- Rotate tokens and review secrets regularly; prefer OIDC + ephemeral tokens

## References

- WARP/AGENTS.md CI/CD guidance
- reports/gha-audit.md for workflow audit and citations
