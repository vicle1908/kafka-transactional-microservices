# GitHub Repository Settings Setup Guide

This guide provides step-by-step instructions for configuring the required GitHub repository settings for the Kafka Transactional Microservices project.

## Overview

The repository already has comprehensive CI/CD workflows configured. To ensure code quality and security, you need to enable GitHub's built-in branch protection and status checks.

## 🎯 Required Settings to Enable

### 1. Branch Protection Rules

**Navigate to**: Repository → Settings → Branches → Branch protection rule

**Create/Edit Branch Protection Rule for `main` branch:**

#### Basic Settings ✅

- [ ] **Branch name pattern**: `main`
- [ ] **Require status checks to pass before merging**: ✅ (Enable)
- [ ] **Require branches to be up to date before merging**: ✅ (Enable)
- [ ] **Require conversation resolution before merging**: ✅ (Enable - Recommended)
- [ ] **Require pull request reviews before merging**: ✅ (Enable, at least 1 approval)
- [ ] **Dismiss stale pull request approvals when new commits are pushed**: ✅ (Enable)
- [ ] **Limit who can push to matching branches**: ✅ (Enable, restrict to admins + maintainers)
- [ ] **Allow force pushes**: ❌ (Disable)
- [ ] **Allow deletions**: ❌ (Disable)

#### Protect Matching Branches

- [ ] **Include `main` branch**: ✅

### 2. Required Status Checks

**Navigate to**: Branch Protection Rule → Require status checks to pass before merging

#### Required Status Checks ✅

Add the following status checks to the required list:

1. **CI (CI workflow)** - ✅ Already implemented
   - Workflow file: `.github/workflows/ci.yml`
   - Runs on: push, pull_request to main

2. **Workflow Validation** - ✅ Already implemented
   - Workflow file: `.github/workflows/workflow-validation.yml`
   - Includes: actionlint + yamllint validation
   - Runs on: pull_request to main

3. **Dependency Review** - ✅ Already implemented
   - Workflow file: `.github/workflows/dependency-review.yml`
   - Fail-on-severity: `high`
   - Runs on: pull_request to main

4. **Schema Compatibility** - ✅ Already implemented
   - Workflow file: `.github/workflows/schema-compatibility.yml`
   - Validates Avro schema compatibility
   - Runs on: push, pull_request to main

5. **CodeQL** - ✅ Already implemented
   - Workflow file: `.github/workflows/security-scan.yml`
   - Languages: Java/Kotlin
   - Includes security-and-quality queries
   - Runs on: push to main, weekly schedule

6. **Gitleaks (Secrets Scanning)** - ✅ Already implemented
   - Workflow file: `.github/workflows/gitleaks.yml`
   - Custom configuration for detecting secrets
   - Runs on: push, pull_request to main

7. **Security Scan** - ✅ Already implemented
   - Workflow file: `.github/workflows/security-scan.yml`
   - Includes: OWASP Dependency Check, SpotBugs, Error Prone, Trivy, CodeQL
   - Runs on: push to main, weekly schedule

#### Additional Recommended Status Checks ✅

- [ ] **Gradle Wrapper Validation** - Already included in CI
- [ ] **Build & Test** - Already included in CI
- [ ] **Kotlin Lint (ktlint)** - Already included in CI
- [ ] **Static Analysis (Detekt)** - Already included in CI

## 🚀 Already Implemented Features

### Security & Compliance ✅

- **Dependabot**: `.github/dependabot.yml` - Automated dependency updates
- **Step Security Runner**: All workflows include `step-security/harden-runner`
- **Supply Chain Hardening**: Actions pinned to full commit SHAs
- **Vulnerability Scanning**: OWASP Dependency Check, Trivy, CodeQL
- **Secrets Detection**: Gitleaks with custom configuration

### CI/CD Pipeline ✅

- **Multi-language Support**: Kotlin, Java, Docker, JavaScript
- **Parallel Execution**: Jobs run in parallel where possible
- **Configuration Cache**: Gradle configuration and build caching enabled
- **Artifact Upload**: Build artifacts and scan results preserved
- **Failure Handling**: Appropriate continue-on-error settings for non-critical scans

### Code Quality ✅

- **Multiple Linters**: ktlint, Detekt, SpotBugs, Error Prone
- **Schema Validation**: Avro schema compatibility checking
- **Documentation Validation**: Markdown linting and link checking
- **Infrastructure Validation**: Docker Compose and Kubernetes configuration validation

## 📋 Verification Steps

After enabling the settings, verify they work correctly:

1. **Create a Test Pull Request**
   - Make a small change to a source file
   - Create a pull request to `main`
   - Verify all required status checks appear and run

2. **Check Status Checks**
   - Ensure all checks from the "Required Status Checks" section appear
   - Verify each check completes successfully
   - Confirm the PR cannot be merged until all checks pass

3. **Test Branch Protection**
   - Try to push directly to `main` (should be blocked)
   - Verify merge requires PR approval
   - Test stale approval dismissal

## 🔧 Workflow File Status

All required workflow files are already implemented:

- ✅ `.github/workflows/ci.yml` - Main CI pipeline
- ✅ `.github/workflows/workflow-validation.yml` - YAML/Actions validation
- ✅ `.github/workflows/dependency-review.yml` - Dependency security review
- ✅ `.github/workflows/schema-compatibility.yml` - Avro schema validation
- ✅ `.github/workflows/security-scan.yml` - Comprehensive security scanning
- ✅ `.github/workflows/gitleaks.yml` - Secrets detection

## 🎯 Next Steps

1. **Navigate to your repository settings**
2. **Enable Branch Protection** following the checklist above
3. **Configure Required Status Checks** using the listed workflow names
4. **Test the configuration** with a sample pull request
5. **Monitor the first few PRs** to ensure all checks work as expected

## 📞 Support

If you encounter any issues with the GitHub settings or need clarification on any of the configurations:

1. Check that workflow files are present in `.github/workflows/`
2. Verify the workflow names match exactly when adding to required status checks
3. Ensure your GitHub account has appropriate permissions for repository administration
4. Consult the repository administration runbook at `docs/runbooks/repo-admin.md` for detailed guidance

---

*This guide should help you enable all the necessary GitHub settings to work with the existing CI/CD pipeline configuration.*
