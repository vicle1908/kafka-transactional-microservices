# GitHub Actions Audit (Validated via Research)

Date: 2025-10-11
Repo: microservices

Summary
- Validated earlier findings against docs and reputable sources. This report consolidates errors, warnings, and recommendations, with citations.

Key findings and validations

1) Step vs job timeouts
- GitHub Actions officially documents timeout-minutes at the job level. No official support for step-level timeout-minutes.
- Action: Remove step-level timeouts; set job-level timeout-minutes.
- Sources:
  - Workflow syntax (timeout-minutes default 360): https://docs.github.com/actions/reference/workflow-syntax-for-github-actions

2) CodeQL required permissions
- Advanced setup requires permissions: security-events: write (and contents: read for private repos).
- Action: Add permissions for CodeQL job/workflow in security-scan.yml.
- Sources:
  - codeql-action README: https://github.com/github/codeql-action#usage

3) Pin actions to full-length commit SHAs (supply chain)
- Pinning to a full SHA is the only immutable release mechanism; best practice for security hardening.
- Action: Pin all actions to SHAs; optionally add step-security/harden-runner to audit/enforce network egress.
- Sources:
  - GitHub Docs – Secure use reference: https://docs.github.com/en/actions/reference/security/secure-use
  - Harden-Runner (marketplace/readme): https://github.com/step-security/harden-runner

4) Dependency Review gating
- dependency-review-action supports fail-on-severity to fail PRs based on severity threshold.
- Action: Run on pull_request (currently push to main). Remove warn-only (prefer failing gate with fail-on-severity: high) to enforce policy, per WARP baseline.
- Sources:
  - Action README: https://github.com/actions/dependency-review-action
  - Docs: https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/configuring-the-dependency-review-action

5) Docker Compose on GitHub runners
- Compose v1 is deprecated; hosted runners have Compose v2. docker/setup-compose-action exists and will install/skip as needed.
- Action: Prefer docker compose (v2). Keep setup-compose action if you want explicit control.
- Sources:
  - Changelog: https://github.blog/changelog/2024-04-10-github-hosted-runner-images-deprecation-notice-docker-compose-v1/
  - Setup action: https://github.com/docker/setup-compose-action

6) Debezium Connect with Avro Converter
- Debezium containers do not include Confluent Avro converter jars by default.
- Action (integration-test.yml): Either switch to JSON converters or include Confluent Avro converter into the Connect image when using Avro converters.
- Sources:
  - Debezium docs: https://debezium.io/documentation/reference/stable/configuration/avro.html

7) Apache Kafka image/tag
- Official apache/kafka repo exists; verify tag 4.1.0 is available or use a widely adopted image (bitnami/kafka) in CI.
- Action: Verify image tag or switch to bitnami for stability in CI.
- Sources:
  - Docker Hub apache/kafka: https://hub.docker.com/r/apache/kafka/
  - bitnami/kafka: https://hub.docker.com/r/bitnami/kafka

8) actions/checkout persist-credentials
- Default persists token in git config; set persist-credentials: false to avoid token exposure to later steps.
- Action: For PRs from forks, set persist-credentials: false.
- Sources:
  - actions/checkout README: https://github.com/actions/checkout

9) Trivy exit-code gating
- By default trivy exits 0 even with findings; use exit-code to fail.
- Action: Keep scheduled scans non-blocking; add PR-gating job with exit-code and severity thresholds if desired.
- Sources:
  - trivy-action README: https://github.com/aquasecurity/trivy-action

Confirmed issues in current workflows
- container-publish.yml: Trailing quote in upload-artifact path (syntax error). Cache key references gradle/deps.versions.toml (should be gradle/libs.versions.toml). Hardcoded JAR version in build arg.
- ci.yml: Wrapper validation allowed to fail (continue-on-error). Step-level timeouts used; remove. Consider adding harden-runner and jacoco.
- security-scan.yml: Missing permissions: security-events: write for CodeQL. Actions not pinned.
- dependency-review.yml: Trigger only on push to main; warn-only set; if: !private – doesn’t gate PRs. Should run on pull_request with fail-on-severity.
- integration-test.yml: Debezium Avro converter likely missing; image/tag checks for Kafka.
- load-test.yml: docker-compose (prefer docker compose), invalid env var SPRING_KAFKA_BOOTSTRAP-SERVERS (hyphen), duplicate thresholds in k6, Gradle action major mismatch.
- infrastructure-validation.yml: docker/setup-compose-action is valid; optional to keep; check Terraform guards.

Actions to implement (high level)
1) Fix critical syntax & correctness
- container-publish.yml: artifact path quote; cache key path; compute JAR path dynamically; pin actions.
- ci.yml: make wrapper validation required; remove step timeouts; add harden-runner; pin actions; optional jacoco.
- security-scan.yml: add permissions security-events: write; pin actions.
- dependency-review.yml: run on pull_request; remove warn-only; keep fail-on-severity: high.

2) Integration environment correctness
- integration-test.yml: use JSON converters for Debezium or add Avro converter plugin; verify Kafka image tag or switch to bitnami.

3) Load test workflow fixes
- Replace docker-compose with docker compose; fix SPRING_KAFKA_BOOTSTRAP_SERVERS; consolidate k6 thresholds; align Gradle action to v4.

4) Security & supply chain
- Pin actions to SHAs across workflows; add step-security/harden-runner as first step in each job.

5) Optional enhancements
- Add actionlint and yamllint workflows on PRs.
- Add gitleaks on PRs and pushes.
- Restructure to reusable CI workflow; enable branch protection with required checks.

Alignment to WARP/AGENTS.md
- Java 25 + Gradle config cache enforced.
- Schema compatibility check in CI.
- ktlint + detekt.
- Security scans (OWASP/Trivy/CodeQL), infra validation, and only-merge-on-green.

Next steps
- Apply fixes via targeted PRs (grouped by risk), then enforce branch protection to require the updated checks.
