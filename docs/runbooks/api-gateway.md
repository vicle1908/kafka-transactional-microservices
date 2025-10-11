# API Gateway Runbook

## Overview

This document provides operational guidance for the API Gateway component in the microservices architecture. The API Gateway serves as the single entry point for all client requests and provides cross-cutting concerns such as authentication, rate limiting, request/response transformation, and routing to downstream services.

## Architecture

The API Gateway is implemented using Spring Cloud Gateway and is responsible for:

- Routing requests to appropriate backend services
- Authentication and authorization
- Rate limiting
- Request/response transformation
- SSL termination
- Load balancing
- Circuit breaking
- Logging and monitoring

## Configuration

### Routes Configuration

Routes are configured in the `application.yml` file:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: orders-service
          uri: lb://orders-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=2
            
        - id: payments-service
          uri: lb://payments-service
          predicates:
            - Path=/api/payments/**
          filters:
            - StripPrefix=2
            
        - id: inventory-service
          uri: lb://inventory-service
          predicates:
            - Path=/api/inventory/**
          filters:
            - StripPrefix=2
            
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/notifications/**
          filters:
            - StripPrefix=2
```

### Security Configuration

Authentication is handled via JWT tokens:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: secured-route
          uri: lb://orders-service
          predicates:
            - Path=/api/secure/**
          filters:
            - TokenRelay=
```

### Rate Limiting

Rate limiting is configured using Redis:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate-limited-route
          uri: lb://orders-service
          predicates:
            - Path=/api/orders/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

## Common Operations

### Deploying a New Route

1. Update the `application.yml` file to add a new route configuration
2. Deploy the updated configuration to the gateway service
3. Verify the route is working by making a test request

### Updating Rate Limits

1. Modify the rate limiting configuration in `application.yml`
2. Deploy the updated configuration
3. Monitor the rate limiting metrics to ensure the new limits are effective

### Adding Authentication to a Route

1. Add the `TokenRelay` filter to the route configuration
2. Ensure the downstream service is configured to validate JWT tokens
3. Test the authentication flow with a valid and invalid token

## Monitoring and Metrics

### Key Metrics to Monitor

1. **Request Rate**: Number of requests per second
2. **Response Time**: Latency of requests
3. **Error Rate**: Percentage of failed requests
4. **Rate Limiting**: Number of requests rejected due to rate limiting
5. **Circuit Breaker**: Status of circuit breakers

### Grafana Dashboard

A Grafana dashboard is available to visualize API Gateway metrics. The dashboard includes panels for:

- Request rate by route
- Response time percentiles
- Error rate by route
- Rate limiting statistics
- Circuit breaker status

## Troubleshooting

### Common Issues and Solutions

#### 503 Service Unavailable

**Symptoms**: Clients receive 503 errors when making requests to services.

**Possible Causes**:

1. Downstream service is down
2. Network connectivity issues
3. Service discovery problems

**Solutions**:

1. Check the health of the downstream service
2. Verify network connectivity between the gateway and the service
3. Check service discovery (Eureka/Consul) for registration issues

#### 404 Not Found

**Symptoms**: Clients receive 404 errors for valid endpoints.

**Possible Causes**:

1. Incorrect route configuration
2. Path matching issues
3. StripPrefix filter misconfiguration

**Solutions**:

1. Verify the route configuration in `application.yml`
2. Check that the path matches the configured predicates
3. Validate StripPrefix filter configuration

#### 429 Too Many Requests

**Symptoms**: Clients receive 429 errors due to rate limiting.

**Possible Causes**:

1. Legitimate traffic spike
2. Incorrect rate limiting configuration
3. Malicious traffic

**Solutions**:

1. Review and adjust rate limiting configuration if needed
2. Implement IP-based rate limiting for better granularity
3. Consider increasing burst capacity for high-traffic routes

### Debugging Steps

1. **Check Gateway Logs**: Look for error messages or warnings in the gateway logs
2. **Verify Service Health**: Ensure downstream services are healthy and accessible
3. **Test Route Configuration**: Use curl or Postman to test routes directly
4. **Monitor Metrics**: Check Grafana dashboards for anomalies
5. **Review Configuration**: Verify that all configuration files are correct and up-to-date

## Security Considerations

### Authentication

JWT tokens are used for authentication. Ensure that:

- Tokens are properly validated
- Token expiration is configured appropriately
- Refresh token mechanisms are in place

### Authorization

Role-based access control (RBAC) is implemented:

- Routes can be protected based on user roles
- Fine-grained permissions can be configured
- Audit logging is enabled for security-sensitive operations

### Rate Limiting (Security)

Rate limiting helps prevent abuse and DoS attacks:

- Configure appropriate limits for different types of requests
- Implement IP-based rate limiting for additional protection
- Monitor rate limiting metrics for potential attacks

## Maintenance

### Regular Tasks

1. **Log Rotation**: Ensure gateway logs are rotated regularly to prevent disk space issues
2. **Certificate Renewal**: Update SSL certificates before they expire
3. **Configuration Updates**: Regularly review and update route configurations
4. **Performance Tuning**: Monitor and optimize gateway performance

### Backup and Recovery

1. **Configuration Backup**: Regularly backup gateway configuration files
2. **Disaster Recovery Plan**: Document procedures for recovering from gateway failures
3. **Rollback Procedures**: Maintain procedures for rolling back configuration changes

## Scaling

### Horizontal Scaling

The API Gateway can be scaled horizontally by:

1. Deploying multiple instances behind a load balancer
2. Using sticky sessions if needed for session affinity
3. Ensuring shared state (e.g., rate limiting) is properly synchronized

### Performance Tuning

1. **Connection Pooling**: Configure appropriate connection pool sizes
2. **Caching**: Implement caching for frequently requested data
3. **Compression**: Enable response compression to reduce bandwidth usage

## Integration with Other Components

### Service Mesh

The API Gateway integrates with Istio service mesh:

- Traffic management policies are applied at both gateway and mesh levels
- Mutual TLS is used for secure service-to-service communication
- Observability data is collected from both gateway and mesh

### Monitoring Stack

The API Gateway integrates with the monitoring stack:

- Metrics are exported to Prometheus
- Logs are shipped to the centralized logging system
- Traces are sent to the distributed tracing system

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

- [Spring Cloud Gateway Documentation](https://cloud.spring.io/spring-cloud-gateway/reference/html/)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
- [Security Best Practices](https://owasp.org/www-project-api-security/)
