# Dependabot Configuration Guide

This document explains the Dependabot configuration for the Kafka Transactional Microservices project, which ensures automated dependency updates across all package ecosystems.

## Overview

The `.github/dependabot.yml` file is configured to monitor and update dependencies for:

- **Gradle** (multi-module project with version catalog)
- **Docker** (infrastructure and service images)
- **GitHub Actions** (CI/CD workflows)
- **npm** (documentation tooling and scripts)

## Configuration Structure

### 1. Gradle Configuration

**Scope**: Root directory with version catalog (`gradle/libs.versions.toml`)

**Schedule**: Every Monday at 09:00 UTC

**Groups**:
- **spring-boot**: Spring ecosystem (Spring Boot, Spring Kafka, Spring Framework)
- **kotlin**: Kotlin language tools and KSP
- **testing**: Testing frameworks (JUnit, Testcontainers, Mockito, MockK, Awaitility)
- **static-analysis**: Code quality tools (Detekt, KtLint, Versions)
- **serialization**: Messaging and serialization (Avro, Protobuf, gRPC, Kotlinx Serialization)
- **observability**: Monitoring and tracing (OpenTelemetry, Temporal)
- **database**: Database drivers and migrations (PostgreSQL, Flyway, H2)

### 2. GitHub Actions Configuration

**Scope**: `.github/workflows/` directory

**Schedule**: Every Monday at 09:00 UTC

**Groups**:
- **security**: Security-related actions (harden-runner, dependency review)
- **build**: Build and CI actions (checkout, setup-java, gradle actions)
- **deployment**: Container and deployment actions

### 3. Docker Configuration

**Scope**: Multiple directories
- Root directory (`/`) - Infrastructure images
- Services directory (`/services/`) - Application base images
- Temporal pilot (`/temporal-pilot`) - Temporal-specific images

**Schedule**: Every Monday at 09:00 UTC

**Groups by Directory**:

**Root Directory Groups**:
- **kafka**: Apache Kafka, Confluent Schema Registry, Debezium
- **database**: PostgreSQL, Redis
- **observability**: Prometheus, Grafana, Jaeger, OpenTelemetry Collector
- **elastic**: Elastic Stack (Elasticsearch, Logstash, Kibana, Filebeat)

**Services Directory Groups**:
- **base**: Base images (OpenJDK, Eclipse Temurin, Ubuntu, Alpine)

**Temporal Pilot Groups**:
- **temporal**: Temporal.io images

### 4. npm Configuration

**Scope**: Root directory (documentation and tooling)

**Schedule**: Every Monday at 09:00 UTC

**Groups**:
- **docs**: Documentation tools (MkDocs, VuePress, GitBook)
- **dev-tools**: Development tools (ESLint, Prettier, TypeScript, Vite, Webpack)

## Key Features

### Smart Grouping

Dependencies are grouped by functionality to reduce PR noise and make updates easier to review:

- Related packages are updated together in a single PR
- Each group has meaningful naming for easy identification
- Commit messages are prefixed with ecosystem type for clarity

### Reviewer and Assignee Configuration

All Dependabot PRs automatically:
- Assign to `vicle1908`
- Request review from `vicle1908`
- Use prefixed commit messages for easy filtering

### Controlled Update Frequency

- **Weekly updates** on Mondays at 09:00 UTC
- Reasonable PR limits to avoid overwhelming the repository
- Direct dependency updates only (transitive dependencies excluded for better control)

### Version Catalog Support

The Gradle configuration specifically supports our version catalog approach:
- Updates to `gradle/libs.versions.toml` are properly detected
- Version references are maintained across modules
- Gradle version constraints are respected

## Tech Stack Coverage

### Supported Technologies

**Backend Framework**:
- Spring Boot 3.5.x
- Spring Framework 6.x
- Spring for Apache Kafka 3.3.x

**Language & Runtime**:
- Kotlin 2.2.x
- Java 25 (with fallback to Java 21/23)
- Gradle 9.1.x with Kotlin DSL

**Messaging & Serialization**:
- Apache Kafka 4.1.x
- Apache Avro 1.12.x
- Protocol Buffers 4.32.x
- gRPC 1.76.x
- Kotlinx Serialization 1.9.x

**Database**:
- PostgreSQL 18.x
- Flyway 11.x
- H2 Database 2.x

**Observability**:
- OpenTelemetry 1.54.x
- Jaeger 1.74.x
- Prometheus v3.6.x
- Grafana (main branch)
- Elastic Stack 8.19.x

**Testing**:
- JUnit 6.x (Jupiter)
- Testcontainers 1.21.x
- Mockito Kotlin 5.4.x
- MockK 1.14.x
- Awaitility 4.3.x

**Infrastructure**:
- Redis 8.x
- Docker (latest stable)
- Temporal 1.31.x

**Code Quality**:
- Detekt 1.23.x
- KtLint 1.7.x (CLI) / 13.1.x (Plugin)

## Maintenance

### Monitoring Dependabot Activity

1. **Dependency Graph Tab**: Check the Dependabot tab in GitHub's dependency graph
2. **PR Review**: Review and merge Dependabot PRs regularly
3. **CI/CD Validation**: Ensure all Dependabot updates pass CI checks

### Handling Conflicts

If Dependabot PRs have conflicts:
1. Merge recent PRs first
2. Rebase remaining PRs
3. Dependabot will automatically retry on next scheduled run

### Customizing Updates

To temporarily disable updates for specific dependencies:
1. Add `ignore` rules to the Dependabot configuration
2. Use wildcards for broad exclusions
3. Specify version ranges to block

## Security Integration

The configuration works seamlessly with GitHub's security features:
- **Vulnerability Alerts**: Enabled for automatic security updates
- **Dependency Review**: Integrated with GitHub Actions workflow
- **Advanced Security**: Available (repository is public)

## Best Practices Implemented

1. **Consistent Schedule**: All ecosystems update on the same schedule
2. **Logical Grouping**: Related dependencies updated together
3. **Controlled Scope**: Direct dependencies only for better control
4. **Clear Communication**: Prefixed commit messages and reviewer assignments
5. **Future-Proofing**: Registry configuration ready for private registries
6. **Comprehensive Coverage**: All package ecosystems in the project are monitored

## Troubleshooting

### Common Issues

1. **Missing Updates**: Check if dependencies are in ignore rules
2. **Failed Updates**: Review CI logs for specific failure reasons
3. **Too Many PRs**: Adjust `open-pull-requests-limit` or tighten grouping
4. **Update Conflicts**: Merge successful PRs before rebasing others

### Getting Help

- [Dependabot Documentation](https://docs.github.com/en/code-security/dependabot)
- [Dependabot Options Reference](https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference)
- [Supported Ecosystems](https://docs.github.com/en/code-security/dependabot/ecosystems-supported-by-dependabot/supported-ecosystems-and-repositories)

---

This configuration ensures our Kafka Transactional Microservices project stays up-to-date with the latest security patches and feature updates while maintaining stability through controlled, grouped updates.
