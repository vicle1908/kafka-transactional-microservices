# Service Mesh Runbook

## Overview

This document provides operational guidance for the Istio service mesh implementation in the microservices architecture. The service mesh handles service-to-service communication, security, observability, and traffic management.

## Architecture

The service mesh is implemented using Istio in ambient mode, which provides:
- Zero-trust security model
- Traffic management capabilities
- Enhanced observability
- Resilience patterns (retries, timeouts, circuit breaking)

## Components

### Control Plane

The Istio control plane consists of:
- **Istiod**: Manages service discovery, configuration, and certificate management
- **Ingress Gateway**: Handles north-south traffic into the mesh
- **Egress Gateway**: Manages outbound traffic from the mesh

### Data Plane

In ambient mode, the data plane uses:
- **Ztunnel**: Node-level proxy that handles L4 authorization and encryption
- **Waypoint Proxies**: Handle L7 processing for specific workloads

## Configuration

### Service Mesh Installation

The service mesh is installed using Helm charts:

```bash
# Install Istio CRDs
helm install istio-base istio/base -n istio-system

# Install Istiod
helm install istiod istio/istiod -n istio-system

# Install Ingress Gateway
helm install istio-ingress istio/gateway -n istio-system
```

### Ambient Profile Configuration

The ambient profile is enabled by default in the installation:

```yaml
profile: ambient
components:
  ingressGateways:
    - name: istio-ingressgateway
      enabled: true
```

### Traffic Management

Traffic policies are defined using Istio CRDs:

#### Virtual Services

Virtual services define routing rules:

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: orders-service
spec:
  hosts:
    - orders-service
  http:
    - route:
        - destination:
            host: orders-service
            subset: v1
          weight: 90
        - destination:
            host: orders-service
            subset: v2
          weight: 10
```

#### Destination Rules

Destination rules define policies for traffic intended for a service:

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: orders-service
spec:
  host: orders-service
  subsets:
    - name: v1
      labels:
        version: v1
      trafficPolicy:
        loadBalancer:
          simple: LEAST_CONN
    - name: v2
      labels:
        version: v2
      trafficPolicy:
        loadBalancer:
          simple: LEAST_CONN
```

#### PeerAuthentication

Peer authentication defines mutual TLS settings:

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
spec:
  mtls:
    mode: STRICT
```

## Common Operations

### Deploying Services to the Mesh

1. Add the Istio sidecar injection label to the namespace:
   ```bash
   kubectl label namespace <namespace> istio-injection=enabled
   ```

2. Deploy services as usual; Istio will automatically inject the sidecar proxy

3. Verify that services are properly injected:
   ```bash
   kubectl get pods -n <namespace>
   ```

### Configuring Traffic Policies

1. Create VirtualService and DestinationRule resources
2. Apply the configuration using kubectl:
   ```bash
   kubectl apply -f traffic-policy.yaml
   ```

3. Verify the configuration:
   ```bash
   kubectl get virtualservices
   kubectl get destinationrules
   ```

### Enabling Mutual TLS

1. Create a PeerAuthentication policy:
   ```yaml
   apiVersion: security.istio.io/v1beta1
   kind: PeerAuthentication
   metadata:
     name: default
   spec:
     mtls:
       mode: STRICT
   ```

2. Apply the policy:
   ```bash
   kubectl apply -f peerauthentication.yaml
   ```

### Configuring Authorization Policies

1. Create an AuthorizationPolicy resource:
   ```yaml
   apiVersion: security.istio.io/v1beta1
   kind: AuthorizationPolicy
   metadata:
     name: orders-service
     namespace: prod
   spec:
     selector:
       matchLabels:
         app: orders-service
     rules:
     - from:
       - source:
           principals: ["cluster.local/ns/prod/sa/some-service-account"]
       to:
       - operation:
           methods: ["GET"]
           paths: ["/api/orders/*"]
   ```

2. Apply the policy:
   ```bash
   kubectl apply -f authorizationpolicy.yaml
   ```

## Monitoring and Metrics

### Key Metrics to Monitor

1. **Request Rate**: Number of requests per second between services
2. **Response Time**: Latency of service-to-service calls
3. **Error Rate**: Percentage of failed requests
4. **Traffic Distribution**: Distribution of traffic across service versions
5. **Circuit Breaker Events**: Number of times circuit breakers have been triggered

### Grafana Dashboards

Several Grafana dashboards are available for monitoring the service mesh:

1. **Istio Service Dashboard**: Provides metrics for individual services
2. **Istio Workload Dashboard**: Shows metrics for individual workloads
3. **Istio Mesh Dashboard**: Provides an overview of the entire mesh
4. **Istio Performance Dashboard**: Focuses on performance metrics
5. **Istio Control Plane Dashboard**: Monitors the Istio control plane components

### Distributed Tracing

Jaeger is used for distributed tracing:
- Traces are automatically generated for service-to-service calls
- Spans include information about request processing time
- Traces can be filtered and searched based on various criteria

## Troubleshooting

### Common Issues and Solutions

#### Services Not Reachable

**Symptoms**: Services in the mesh cannot communicate with each other.

**Possible Causes**:
1. Incorrect service discovery configuration
2. Network policies blocking traffic
3. Misconfigured authorization policies

**Solutions**:
1. Verify that services are properly registered in the service mesh
2. Check network policies to ensure traffic is allowed
3. Review authorization policies for proper configuration

#### High Latency

**Symptoms**: Increased response times for service-to-service calls.

**Possible Causes**:
1. Network congestion
2. Resource constraints on proxy sidecars
3. Misconfigured retry policies

**Solutions**:
1. Monitor network usage and identify bottlenecks
2. Check resource usage on proxy sidecars
3. Review and adjust retry policies

#### Mutual TLS Issues

**Symptoms**: Connection failures with TLS-related error messages.

**Possible Causes**:
1. Incorrect certificate configuration
2. Mismatched TLS modes between services
3. Expired certificates

**Solutions**:
1. Verify certificate configuration in PeerAuthentication policies
2. Ensure consistent TLS modes across services
3. Rotate expired certificates

### Debugging Steps

1. **Check Istio Proxy Logs**: Look for error messages in the Envoy proxy logs
2. **Verify Configuration**: Use istioctl to validate Istio configuration
3. **Test Connectivity**: Use curl from within pods to test service connectivity
4. **Monitor Metrics**: Check Grafana dashboards for anomalies
5. **Review Policies**: Verify that all policies are correctly configured

### Useful Commands

```bash
# Check the status of Istio components
istioctl version

# Validate Istio configuration
istioctl validate -f <config-file.yaml>

# Check proxy configuration
istioctl proxy-config <proxy-type> <pod-name>

# View logs for a specific proxy
istioctl proxy-status

# Generate a cluster configuration dump
istioctl pc cluster <pod-name> -o json
```

## Security Considerations

### Mutual TLS

Mutual TLS is enabled by default:
- All service-to-service communication is encrypted
- Certificates are automatically rotated
- Strong cryptographic standards are used

### Authorization

Authorization policies provide fine-grained access control:
- Requests can be allowed or denied based on various criteria
- Policies can be applied at namespace or workload level
- Audit logging is available for security-sensitive operations

### Network Security

Network policies restrict traffic flow:
- Only authorized traffic is allowed between services
- Egress traffic can be controlled and monitored
- Network segmentation is enforced

## Maintenance

### Regular Tasks

1. **Certificate Rotation**: Monitor certificate expiration and rotation
2. **Configuration Updates**: Regularly review and update mesh configurations
3. **Performance Tuning**: Monitor and optimize mesh performance
4. **Security Updates**: Apply security patches to Istio components

### Backup and Recovery

1. **Configuration Backup**: Regularly backup Istio configuration files
2. **Certificate Backup**: Backup important certificates and keys
3. **Disaster Recovery Plan**: Document procedures for recovering from mesh failures
4. **Rollback Procedures**: Maintain procedures for rolling back configuration changes

## Scaling

### Horizontal Scaling

The service mesh can be scaled horizontally by:
1. Increasing the number of Istiod replicas
2. Scaling ingress and egress gateways
3. Ensuring proper load distribution

### Performance Tuning

1. **Resource Allocation**: Configure appropriate CPU and memory limits for Istio components
2. **Proxy Configuration**: Tune Envoy proxy settings for optimal performance
3. **Caching**: Implement caching where appropriate to reduce load

## Integration with Other Components

### API Gateway

The service mesh integrates with the API Gateway:
- Traffic management policies are applied at both gateway and mesh levels
- Mutual TLS is used for secure communication between gateway and services
- Observability data is collected from both gateway and mesh

### Monitoring Stack

The service mesh integrates with the monitoring stack:
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

- [Istio Documentation](https://istio.io/latest/docs/)
- [Service Mesh Pattern](https://www.redhat.com/en/topics/microservices/what-is-a-service-mesh)
- [Istio Security Best Practices](https://istio.io/latest/docs/ops/best-practices/security/)