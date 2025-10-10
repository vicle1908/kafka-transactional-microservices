# CI/CD Operations Runbook

## Overview
This runbook covers the operation of the GitHub Actions CI/CD workflows for the Kafka Transactional Microservices platform.

## Workflows Overview

### 1. CI Workflow (ci.yml)
- **Purpose**: Build, test, and validate code changes
- **Trigger**: Push to main branch and pull requests
- **Components**: 
  - JDK 25 setup
  - Gradle caching
  - Version compatibility check
  - Schema compatibility validation
  - Build and test execution
  - Documentation linting

### 2. Integration Test Workflow (integration-test.yml)
- **Purpose**: Validate end-to-end functionality with full Kafka/DB environment
- **Trigger**: Push to main branch and pull requests
- **Components**:
  - PostgreSQL, Kafka, Schema Registry services
  - Debezium Connect for outbox pattern
  - Service mesh integration testing
  - Exactly-once semantics validation

### 3. Schema Compatibility Workflow (schema-compatibility.yml)
- **Purpose**: Validate Avro schema changes for backward compatibility
- **Trigger**: Changes to common-events-avro module
- **Components**:
  - Avro schema export
  - Compatibility checking

### 4. Security Scanning Workflow (security-scan.yml)
- **Purpose**: Security and vulnerability assessment
- **Trigger**: Push to main/develop, pull requests, weekly schedule
- **Components**:
  - OWASP Dependency Check
  - Trivy vulnerability scanning
  - CodeQL SAST analysis
  - SpotBugs and ErrorProne static analysis

### 5. Dependency Review Workflow (dependency-review.yml)
- **Purpose**: Dependency vulnerability and license checking
- **Trigger**: Pull requests to main
- **Components**:
  - Dependency resolution reports
  - License compliance checking
  - Vulnerability identification

### 6. Infrastructure Validation Workflow (infrastructure-validation.yml)
- **Purpose**: Validate infrastructure as code and configuration
- **Trigger**: Changes to infra/ directory
- **Components**:
  - Docker Compose validation
  - Infrastructure security scanning
  - Terraform validation (if applicable)
  - Kubernetes manifest validation

### 7. Container Publishing Workflow (container-publish.yml)
- **Purpose**: Build and publish Docker images for services
- **Trigger**: Tagged releases (v*.*.*)
- **Components**:
  - Multi-stage Docker builds
  - Image tagging and metadata
  - GHCR publishing
  - Security scanning

### 8. Canary Deployment Workflow (canary-deployment.yml)
- **Purpose**: Progressive delivery with traffic splitting
- **Trigger**: Push to main branch
- **Components**:
  - Canary deployment strategy
  - Health checks and smoke tests
  - Traffic splitting (Istio/Linkerd)
  - Rollback automation

### 9. Load Testing Workflow (load-test.yml)
- **Purpose**: Performance validation under load
- **Trigger**: Weekly schedule or manual
- **Components**:
  - Gatling/JMeter/k6 load testing
  - Outbox table depth monitoring
  - EOS behavior validation
  - Performance metrics collection

### 10. Chaos Engineering Workflow (chaos-engineering.yml)
- **Purpose**: Resilience validation through failure injection
- **Trigger**: Weekly schedule or manual
- **Components**:
  - Broker restart simulation
  - DB failover testing
  - Service mesh failure simulation
  - Cache outage testing

## Common Operations

### Running Workflows Manually
1. Navigate to the Actions tab in GitHub repository
2. Select the workflow to run
3. Click "Run workflow" button
4. Configure any required parameters

### Troubleshooting Failed Workflows

#### Common CI Issues
- **Dependency Resolution**: Check internet connectivity and proxy settings
- **Version Mismatch**: Verify versionCheck task in root build.gradle.kts
- **Test Failures**: Check test logs for specific errors

#### Common Integration Test Issues
- **Service Startup**: Verify Docker resources and ports availability
- **Database Connection**: Check PostgreSQL service health
- **Kafka Connectivity**: Verify KRaft cluster health

#### Common Container Publishing Issues
- **Registry Access**: Check GITHUB_TOKEN permissions
- **Image Size**: Optimize Dockerfile for smaller images
- **Build Failures**: Verify JAR files exist before Docker build

## Monitoring and Alerting

### Key Metrics to Monitor
- **Build Success Rate**: Percentage of successful builds
- **Average Build Time**: Time to complete builds
- **Test Coverage**: Code coverage metrics
- **Security Scan Results**: Number of vulnerabilities by severity

### Alert Conditions
- Build failure rate > 5% over 1 hour
- Security vulnerabilities with CRITICAL severity
- Integration test failure rate > 10%
- Container publish failure for tagged releases

## Maintenance Tasks

### Updating Workflow Definitions
1. Update GitHub Actions workflow files in .github/workflows/
2. Test changes in a feature branch
3. Review and approve through pull request process

### Updating Base Images
1. Update Docker images in workflow files
2. Test with integration test workflow
3. Update related documentation if needed

### Security Updates
1. Regularly update action versions in workflow files
2. Monitor security advisories for used actions
3. Update dependencies and test workflows

## Troubleshooting Scenarios

### Scenario: Container Publishing Fails
**Symptoms**: Container publishing workflow fails during image push
**Diagnosis**:
- Check GHCR permissions in repository settings
- Verify GITHUB_TOKEN has packages:write scope
- Confirm Docker image tagging is correct
**Resolution**: Verify repository secrets and permissions

### Scenario: Canary Deployment Rollback
**Symptoms**: Canary deployment triggers rollback
**Diagnosis**:
- Check smoke test results
- Review application logs
- Verify health endpoints
**Resolution**: Fix underlying issue and re-trigger deployment

### Scenario: Load Test Performance Degradation
**Symptoms**: Load test shows performance degradation
**Diagnosis**:
- Compare metrics with baseline
- Check resource utilization
- Review application logs for errors
**Resolution**: Identify bottlenecks and optimize

## Emergency Procedures

### Rolling Back Breaking Changes
1. If a breaking change is merged, immediately pause affected deployments
2. Identify the change causing issues
3. Revert the change or implement a fix
4. Resume workflows only after verification

### Security Incident Response
1. If security scan detects critical vulnerability, immediately pause deployments
2. Create security issue with details
3. Implement security patch
4. Re-run security scans before resuming

### Infrastructure Failures
1. For CI infrastructure failures, check GitHub status page
2. For self-hosted runner issues, contact infrastructure team
3. Document failures for post-incident review