# OpenTelemetry Tracing Runbook

## Overview

This document provides operational guidance for implementing and managing OpenTelemetry distributed tracing in the microservices architecture. OpenTelemetry provides a standardized way to collect, process, and export telemetry data (traces, metrics, and logs) across services.

## Architecture

The OpenTelemetry implementation includes:
- **OpenTelemetry SDK**: Integrated into each microservice
- **OpenTelemetry Collector**: Centralized agent for telemetry processing
- **Jaeger**: Distributed tracing backend for trace storage and visualization
- **Integration with existing monitoring stack**: Prometheus for metrics, Grafana for visualization

## Components

### OpenTelemetry SDK

Each service includes the OpenTelemetry SDK with the following components:
- Tracer provider for creating and managing spans
- Propagators for context propagation across services
- Exporters for sending telemetry data to the collector

### OpenTelemetry Collector

The collector is responsible for:
- Receiving telemetry data from services
- Processing and transforming telemetry data
- Exporting data to backend systems (Jaeger, Prometheus)

### Jaeger

Jaeger provides:
- Trace storage and querying
- UI for trace visualization
- Trace analysis and debugging capabilities

## Configuration

### Service Configuration

Each microservice is configured with the OpenTelemetry SDK:

```yaml
# application.yml
opentelemetry:
  traces:
    exporter: otlp
    endpoint: http://otel-collector:4317
  metrics:
    exporter: prometheus
    endpoint: http://otel-collector:9090
```

### OpenTelemetry Collector Configuration

The collector is configured with receivers, processors, and exporters:

```yaml
# otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
      http:

processors:
  batch:

exporters:
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true
  prometheus:
    endpoint: "0.0.0.0:9090"

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [jaeger]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus]
```

### Instrumentation

Services are instrumented with manual and automatic instrumentation:

1. **Automatic Instrumentation**:
   - HTTP client/server spans
   - Database query spans
   - Kafka producer/consumer spans

2. **Manual Instrumentation**:
   - Business logic spans
   - Custom attributes and events
   - Error handling and exception recording

## Common Operations

### Adding New Spans

To add custom spans to your service:

```java
@RestController
public class OrderController {
    
    @Autowired
    private Tracer tracer;
    
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        Span span = tracer.spanBuilder("createOrder")
            .setAttribute("order.customerId", request.getCustomerId())
            .setAttribute("order.itemCount", request.getItems().size())
            .startSpan();
            
        try (Scope scope = span.makeCurrent()) {
            // Business logic here
            Order order = orderService.createOrder(request);
            span.setAttribute("order.id", order.getId());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Adding Custom Attributes

To add custom attributes to existing spans:

```java
@GetMapping("/orders/{id}")
public ResponseEntity<Order> getOrder(@PathVariable String id) {
    Span currentSpan = Span.current();
    currentSpan.setAttribute("order.id", id);
    
    Order order = orderService.getOrder(id);
    currentSpan.setAttribute("order.status", order.getStatus());
    
    return ResponseEntity.ok(order);
}
```

### Context Propagation

Context is automatically propagated across service boundaries:

```java
// HTTP client example
WebClient webClient = WebClient.builder()
    .baseUrl("http://payment-service")
    .build();

Mono<PaymentResponse> response = webClient
    .post()
    .uri("/payments")
    .bodyValue(paymentRequest)
    .retrieve()
    .bodyToMono(PaymentResponse.class);
```

## Monitoring and Metrics

### Key Metrics to Monitor

1. **Trace Quality**:
   - Span creation rate
   - Missing spans
   - Trace completeness

2. **Performance**:
   - Average trace duration
   - Span duration percentiles
   - Error rates by service

3. **Resource Usage**:
   - Collector CPU and memory usage
   - Exporter throughput
   - Queue depths

### Grafana Dashboards

Grafana dashboards are available for monitoring OpenTelemetry metrics:
- Trace volume and latency by service
- Error rates and success percentages
- Resource usage of OpenTelemetry components

### Jaeger UI

The Jaeger UI provides:
- Trace search and filtering capabilities
- Detailed trace visualization
- Service dependency graphs
- Performance analysis tools

## Troubleshooting

### Common Issues and Solutions

#### Missing Traces

**Symptoms**: Traces are not appearing in Jaeger UI.

**Possible Causes**:
1. Incorrect collector endpoint configuration
2. Network connectivity issues
3. Exporter misconfiguration

**Solutions**:
1. Verify collector endpoint configuration in services
2. Check network connectivity between services and collector
3. Review exporter configuration and logs

#### High Latency

**Symptoms**: Increased latency in service responses.

**Possible Causes**:
1. Over-instrumentation
2. Collector performance issues
3. Network latency

**Solutions**:
1. Review and optimize instrumentation
2. Scale collector resources
3. Investigate network issues

#### Context Not Propagated

**Symptoms**: Distributed traces are broken across service boundaries.

**Possible Causes**:
1. Missing context propagation in custom code
2. Incorrect HTTP header handling
3. Middleware interfering with headers

**Solutions**:
1. Ensure proper context propagation in custom code
2. Verify HTTP header handling
3. Review middleware configuration

### Debugging Steps

1. **Check Service Logs**: Look for OpenTelemetry-related error messages
2. **Verify Configuration**: Ensure all services have correct OTel configuration
3. **Test Connectivity**: Verify network connectivity between services and collector
4. **Monitor Metrics**: Check Grafana dashboards for anomalies
5. **Review Spans**: Examine individual spans in Jaeger for issues

### Useful Commands

```bash
# Check collector health
curl http://otel-collector:13133

# View collector logs
docker logs otel-collector

# Check trace metrics in Prometheus
curl http://prometheus:9090/api/v1/query?query=traces_span_duration_milliseconds_count
```

## Security Considerations

### Data Protection

1. **PII Handling**:
   - Avoid including PII in span attributes
   - Sanitize sensitive data before exporting
   - Use attribute filtering in the collector

2. **Data Encryption**:
   - Use TLS for data in transit
   - Configure mutual TLS between components
   - Protect collector endpoints

### Access Control

1. **Authentication**:
   - Secure collector endpoints
   - Implement API key authentication where appropriate
   - Use service accounts for inter-service communication

2. **Authorization**:
   - Restrict access to tracing UI
   - Implement role-based access control
   - Audit access to sensitive traces

## Maintenance

### Regular Tasks

1. **Collector Updates**: Regularly update OpenTelemetry Collector to latest stable version
2. **Configuration Reviews**: Periodically review and optimize configuration
3. **Performance Tuning**: Monitor and optimize collector performance
4. **Security Updates**: Apply security patches promptly

### Backup and Recovery

1. **Configuration Backup**: Regularly backup collector configuration files
2. **Data Retention**: Configure appropriate data retention policies in Jaeger
3. **Disaster Recovery**: Document procedures for recovering from tracing system failures

## Scaling

### Horizontal Scaling

The OpenTelemetry system can be scaled horizontally by:
1. Increasing the number of collector instances
2. Load balancing traffic across collectors
3. Scaling Jaeger components

### Performance Tuning

1. **Batch Processing**: Configure appropriate batch sizes in the collector
2. **Resource Allocation**: Allocate sufficient CPU and memory to collector instances
3. **Queue Management**: Configure appropriate queue sizes and processing intervals

## Integration with Other Components

### Service Mesh

OpenTelemetry integrates with the Istio service mesh:
- Both provide distributed tracing capabilities
- Can be used together for enhanced observability
- Trace context is propagated across both systems

### Monitoring Stack

OpenTelemetry integrates with the existing monitoring stack:
- Metrics are exported to Prometheus
- Traces are sent to Jaeger
- Logs can be integrated with centralized logging

### CI/CD Pipeline

OpenTelemetry instrumentation is part of the CI/CD pipeline:
- Instrumentation is validated in automated tests
- Configuration is checked in version control
- Updates are deployed through standard processes

## Change Management

### Deployment Process

1. **Configuration Changes**: 
   - Make changes in a development environment first
   - Test thoroughly before promoting to production
   - Use blue-green deployment to minimize downtime

2. **Versioning**:
   - Maintain versioned configuration files
   - Document breaking changes
   - Provide migration guides for major updates

### Rollback Procedures

1. **Quick Rollback**:
   - Keep previous configuration versions readily available
   - Use automated rollback scripts when possible
   - Monitor for issues after rollback

2. **Gradual Rollback**:
   - For complex changes, rollback in stages
   - Communicate rollback plans to stakeholders
   - Document lessons learned from failed deployments

## References

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Distributed Tracing Best Practices](https://opentelemetry.io/docs/concepts/signals/traces/)
- [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java)