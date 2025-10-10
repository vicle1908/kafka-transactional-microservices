# Health Checks Implementation Guide

## Overview
This document describes the health checks implemented for all services in the Docker Compose files. Health checks are essential for ensuring that services are running correctly and for enabling Docker's service dependency management.

## Implemented Health Checks

### Kafka
**Health Check Command:**
```bash
kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1
```

**Configuration:**
```yaml
healthcheck:
  test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Explanation:**
This command attempts to connect to the Kafka broker and retrieve the supported API versions. If the broker is not ready or accessible, the command will fail and exit with a non-zero status.

### Schema Registry
**Health Check Command:**
```bash
curl -f http://localhost:8081/subjects
```

**Configuration:**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8081/subjects"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Explanation:**
This command makes an HTTP request to the Schema Registry's subjects endpoint. A successful response indicates that the Schema Registry is running and can communicate with Kafka.

### PostgreSQL
**Health Check Command:**
```bash
pg_isready -U <username> -d <database>
```

**Configuration:**
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U app -d orders"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Explanation:**
The `pg_isready` utility checks the connection status of a PostgreSQL server. It returns 0 if the server is accepting connections, 1 if it's rejecting them, and 2 if there's no response.

### Redis
**Health Check Command:**
```bash
redis-cli ping
```

**Configuration:**
```yaml
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 10s
  timeout: 3s
  retries: 3
```

**Explanation:**
The Redis PING command is used to test if the Redis server is responsive. It returns "PONG" if the server is running correctly.

### Debezium Connect
**Health Check Command:**
```bash
curl -f http://localhost:8083/
```

**Configuration:**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8083/"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Explanation:**
This command makes an HTTP request to the root endpoint of the Kafka Connect REST API. A successful response indicates that the Debezium Connect service is running.

### AKHQ (Apache Kafka GUI)
**Health Check Command:**
```bash
curl -f http://localhost:8080/health
```

**Configuration:**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Explanation:**
This command makes an HTTP request to the AKHQ health endpoint. A successful response indicates that the AKHQ service is running correctly.

### Grafana
**Health Check Command:**
```bash
curl -f http://localhost:3000/api/health
```

**Configuration:**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Explanation:**
This command makes an HTTP request to Grafana's health API endpoint. A successful response indicates that Grafana is running correctly.

### Prometheus
**Health Check Command:**
```bash
wget --spider http://localhost:9090/-/healthy
```

**Configuration:**
```yaml
healthcheck:
  test: ["CMD", "wget", "--spider", "http://localhost:9090/-/healthy"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Explanation:**
This command makes an HTTP request to Prometheus's health endpoint. A successful response indicates that Prometheus is running correctly.

## Health Check Parameters

All health checks use the following standard parameters:

- **interval**: The time between health checks (10 seconds for most services)
- **timeout**: The time to wait for a response before considering the check failed (3-5 seconds)
- **retries**: The number of times to retry a failed health check before marking the container as unhealthy (3-5 retries)

## Using Health Checks

Health checks can be used in several ways:

1. **Service Dependencies**: Use `depends_on` with `condition: service_healthy` to ensure services start in the correct order:
   ```yaml
   depends_on:
     kafka:
       condition: service_healthy
   ```

2. **Monitoring**: Health check status is visible in Docker CLI commands:
   ```bash
   docker ps
   docker inspect <container_name>
   ```

3. **Orchestration**: Container orchestrators like Kubernetes can use health check information to manage service availability and restart unhealthy containers.

## Best Practices

1. **Keep health checks lightweight**: Health checks should not consume significant resources or take a long time to execute.

2. **Test actual functionality**: Health checks should verify that the service is actually working, not just that the process is running.

3. **Use appropriate timeouts**: Set timeouts that are long enough for the service to respond under normal conditions but short enough to detect problems quickly.

4. **Configure retry logic**: Use retries to avoid marking containers as unhealthy due to temporary issues.

5. **Match production environments**: Health checks in development and test environments should match those used in production.

## Troubleshooting

If a service is marked as unhealthy:

1. Check the service logs:
   ```bash
   docker logs <container_name>
   ```

2. Test the health check command manually:
   ```bash
   docker exec <container_name> <health_check_command>
   ```

3. Verify service configuration and connectivity to dependent services.

## References

- Docker Compose documentation on health checks
- Service-specific documentation for health check endpoints
- Docker best practices for health checks