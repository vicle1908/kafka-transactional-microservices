# Temporal Observability Runbook

## Overview

This document provides operational guidance for monitoring and observing Temporal workflows and activities in the microservices architecture. Temporal is used as the orchestrator for complex, multi-domain sagas.

## Architecture

The Temporal implementation uses a distributed worker model:
- A dedicated workflow service (`temporal-pilot`) hosts the workflow logic, ensuring the orchestrator is isolated from other service deployments.
- Each participating microservice (`payments-service`, `inventory-service`, etc.) runs its own worker to process activities on a dedicated task queue.

## Components

### Temporal Server
The Temporal server provides:
- Workflow execution and state management
- Task queue management
- Visibility and history storage
- Metrics and tracing capabilities

### Temporal Workers
Workers are responsible for:
- Executing workflow and activity code
- Polling task queues for tasks
- Reporting task completion/failure
- Exporting metrics and traces

### Temporal Client
The client is used for:
- Starting and interacting with workflows
- Querying workflow state
- Sending signals to workflows

## Configuration

### Micrometer Metrics Export

Temporal SDK can export Micrometer metrics to Prometheus:

```yaml
# application.yml
temporal:
  metrics:
    prometheus:
      endpoint: http://localhost:9090
      namespace: temporal
```

### OpenTelemetry Tracing

Temporal SDK supports OpenTelemetry tracing:

```java
// Workflow configuration with OpenTelemetry
WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
    .setTracingOptions(
        TracingOptions.newBuilder()
            .withTracerProvider(OpenTelemetry.getGlobalTracerProvider())
            .build())
    .build();
```

## Common Operations

### Monitoring Temporal Metrics

Key metrics to monitor include:
- Workflow start/success/failure rates
- Activity start/success/failure rates
- Task queue depths
- Workflow execution latencies
- Activity execution latencies
- Worker poller latencies

### Creating Grafana Dashboard

A Grafana dashboard should visualize:
- Workflow latency percentiles
- Activity failure rates
- Task queue depths
- Worker utilization
- Retry rates

### Configuring Alerts

Alerts should be configured for:
- High workflow failure rates
- High activity failure rates
- Long workflow execution times
- Large task queue backlogs
- Worker downtime

## Monitoring and Metrics

### Key Metrics to Monitor

1. **Workflow Metrics**:
   - `workflow_start_count`: Number of workflows started
   - `workflow_completed_count`: Number of workflows completed successfully
   - `workflow_failed_count`: Number of workflows failed
   - `workflow_canceled_count`: Number of workflows canceled
   - `workflow_continue_as_new_count`: Number of workflows continued as new

2. **Activity Metrics**:
   - `activity_execution_failed`: Number of activity executions failed
   - `activity_schedule_to_start_latency`: Time between activity scheduled and started
   - `activity_start_to_close_latency`: Time between activity started and completed

3. **Task Queue Metrics**:
   - `task_queue_start_to_close_latency`: Time between task started and completed
   - `task_queue_poll_succeed`: Number of successful polls
   - `task_queue_poll_fail`: Number of failed polls

4. **Worker Metrics**:
   - `worker_start`: Number of workers started
   - `worker_task_slots_available`: Number of available task slots
   - `worker_task_slots_used`: Number of used task slots

### Grafana Dashboard

Create a Grafana dashboard with panels for:
- Workflow execution rates (success, failure, timeout)
- Activity execution rates (success, failure, timeout)
- Task queue depths by queue name
- Workflow execution duration percentiles
- Activity execution duration percentiles
- Worker utilization and availability

## Troubleshooting

### Common Issues and Solutions

#### Workflow Execution Failures

**Symptoms**: Workflows failing with errors or timeouts.

**Possible Causes**:
1. Activity implementation errors
2. Resource constraints on workers
3. Network connectivity issues
4. Incorrect workflow logic

**Solutions**:
1. Check activity logs for error details
2. Monitor worker resource usage
3. Verify network connectivity between components
4. Review workflow code for logical errors

#### High Task Queue Backlog

**Symptoms**: Large number of pending tasks in queues.

**Possible Causes**:
1. Insufficient worker capacity
2. Slow activity execution
3. Worker failures or restarts

**Solutions**:
1. Scale worker deployments
2. Optimize activity implementation
3. Investigate worker health and restart patterns

#### Worker Registration Issues

**Symptoms**: Workers not appearing in Temporal UI or tasks not being processed.

**Possible Causes**:
1. Incorrect task queue names
2. Network connectivity issues
3. Authentication/authorization problems

**Solutions**:
1. Verify task queue names match between workers and workflows
2. Check network connectivity and firewall rules
3. Review authentication configuration

### Debugging Steps

1. **Check Temporal Web UI**: Look for workflow execution details and error messages
2. **Review Worker Logs**: Check for errors or warnings in worker logs
3. **Monitor Metrics**: Use Grafana dashboard to identify anomalies
4. **Verify Configuration**: Ensure all components are properly configured
5. **Test Connectivity**: Verify network connectivity between components

### Useful Commands

```bash
# Check Temporal server status
tctl cluster health

# List workflows
tctl workflow list

# Describe a specific workflow
tctl workflow describe --workflow_id <workflow-id>

# Check task queue status
tctl taskqueue describe --taskqueue <task-queue-name>
```

## Security Considerations

### Authentication and Authorization

1. **Temporal Server Access**:
   - Use TLS encryption for all connections
   - Implement proper authentication for server access
   - Restrict API access with authorization policies

2. **Worker Authentication**:
   - Use secure credentials for worker connections
   - Implement role-based access control
   - Regularly rotate credentials

### Data Protection

1. **Workflow Data**:
   - Avoid storing sensitive data in workflow state
   - Use encryption for sensitive workflow inputs/outputs
   - Implement proper data retention policies

2. **Activity Data**:
   - Sanitize logs to remove sensitive information
   - Encrypt sensitive activity parameters/results
   - Monitor for data access patterns

## Maintenance

### Regular Tasks

1. **Version Updates**: Regularly update Temporal server and SDK versions
2. **Configuration Reviews**: Periodically review and optimize configuration
3. **Performance Tuning**: Monitor and optimize worker performance
4. **Security Updates**: Apply security patches promptly

### Backup and Recovery

1. **Data Backup**: Regularly backup Temporal persistence data
2. **Configuration Backup**: Backup server configuration files
3. **Disaster Recovery**: Document procedures for recovering from failures

## Scaling

### Horizontal Scaling

Temporal can be scaled horizontally by:
1. Increasing the number of worker instances
2. Adding more Temporal server nodes
3. Scaling underlying persistence layer

### Performance Tuning

1. **Worker Configuration**:
   - Adjust poller count based on workload
   - Configure appropriate task queue partitioning
   - Optimize worker resource allocation

2. **Server Configuration**:
   - Tune database connection pools
   - Configure appropriate caching policies
   - Optimize history and visibility storage

## Integration with Other Components

### Observability Stack

Temporal integrates with the existing observability stack:
- Metrics are exported to Prometheus via Micrometer
- Traces are sent to Jaeger via OpenTelemetry
- Logs are shipped to centralized logging system

### Service Mesh

Temporal workers integrate with the Istio service mesh:
- mTLS for secure service-to-service communication
- Traffic management policies
- Observability data collection

### CI/CD Pipeline

Temporal components are part of the CI/CD pipeline:
- Workers are deployed through standard processes
- Configuration is managed via Infrastructure-as-Code
- Updates are validated through automated tests

## Change Management

### Deployment Process

1. **Worker Updates**: 
   - Deploy new worker versions with blue-green deployment
   - Test with non-production workflows first
   - Monitor metrics during rollout

2. **Server Updates**:
   - Follow Temporal upgrade guides
   - Backup data before major version upgrades
   - Test in staging environment first

### Rollback Procedures

1. **Worker Rollback**:
   - Keep previous worker versions available
   - Use deployment mechanisms that support quick rollback
   - Monitor for issues after rollback

2. **Server Rollback**:
   - Maintain database backups
   - Document rollback procedures for each version
   - Test rollback procedures regularly

## References

- [Temporal Documentation](https://docs.temporal.io/)
- [Temporal Metrics Guide](https://docs.temporal.io/references/metrics)
- [Temporal Tracing Guide](https://docs.temporal.io/references/tracing)
- [Temporal Best Practices](https://docs.temporal.io/best-practices)