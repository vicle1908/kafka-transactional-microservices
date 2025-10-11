# Polyglot Datastore Runbook

## Overview

This document provides operational guidance for managing polyglot datastores in the microservices architecture. The system uses multiple types of databases to best fit the needs of different services:

- PostgreSQL for relational data with ACID properties
- Redis for caching and temporary data storage
- Potential future datastores for specialized use cases

## Architecture

The polyglot datastore architecture includes:

- **PostgreSQL**: Primary relational database for core business data
- **Redis**: In-memory data store for caching and session management
- **Specialized datastores**: Additional databases for specific use cases (e.g., document stores, graph databases, time-series databases)

## PostgreSQL

### Configuration (PostgreSQL)

PostgreSQL is configured with the following settings for optimal performance in a microservices environment:

```yaml
# Docker Compose configuration
postgres:
  image: postgres:18
  environment:
    POSTGRES_USER: app
    POSTGRES_PASSWORD: app
    POSTGRES_DB: orders
  command:
    - "postgres"
    - "-c"
    - "wal_level=logical"
    - "-c"
    - "max_wal_senders=10"
    - "-c"
    - "max_replication_slots=10"
  volumes:
    - pg-data:/var/lib/postgresql/data
    - ./infra/postgres/init:/docker-entrypoint-initdb.d:ro
```

### Key Settings (PostgreSQL)

1. **wal_level=logical**: Enables logical replication for Debezium CDC
2. **max_wal_senders=10**: Allows up to 10 concurrent replication connections
3. **max_replication_slots=10**: Provides replication slots for Debezium connectors

### Database Initialization

Initialization scripts are located in `./infra/postgres/init/`:

- `01-create-dbs.sql`: Creates databases for each service
- Additional scripts for schema setup and initial data

### Common Operations (PostgreSQL)

#### Creating a New Database

To create a new database for a service, add a `CREATE DATABASE` statement to the `infra/postgres/init/01-create-dbs.sql` script. This script is automatically executed when the PostgreSQL container starts, ensuring that the database is created as part of the environment setup.

For example, to add a database for a "new-service":

```sql
CREATE DATABASE "new-service" OWNER app;
```

After adding the statement, restart the Docker Compose environment to apply the changes.

#### Backup and Restore

1. **Backup**:

   ```bash
   docker exec postgres pg_dump -U app -d database_name > backup.sql
   ```

2. **Restore**:

   ```bash
   docker exec -i postgres psql -U app -d database_name < backup.sql
   ```

#### Monitoring

Key PostgreSQL metrics to monitor:

- Connection count
- Transaction rate
- Query performance
- Disk space usage
- Replication lag

### Troubleshooting (PostgreSQL)

#### Connection Issues

**Symptoms**: Applications cannot connect to the database.

**Possible Causes**:

1. Incorrect connection string
2. Database not accepting connections
3. Network issues

**Solutions**:

1. Verify connection string parameters
2. Check PostgreSQL logs for connection errors
3. Ensure network connectivity between applications and database

#### Performance Issues (PostgreSQL)

**Symptoms**: Slow query performance or high latency.

**Possible Causes**:

1. Missing indexes
2. Inefficient queries
3. Resource constraints

**Solutions**:

1. Analyze query execution plans
2. Add appropriate indexes
3. Optimize queries
4. Scale resources if needed

## Redis

### Configuration

Redis is configured with the following settings:

```yaml
# Docker Compose configuration
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data
    - ./infra/redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
  command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
```

### Key Settings

The Redis configuration file (`./infra/redis/redis.conf`) includes:

- Memory management policies
- Persistence settings
- Security configurations
- Performance tuning parameters

### Common Operations

#### Cache Management

1. **Flushing cache**:

   ```bash
   redis-cli FLUSHALL
   ```

2. **Checking cache size**:

   ```bash
   redis-cli INFO memory
   ```

3. **Monitoring keys**:

   ```bash
   redis-cli KEYS "*"
   ```

#### Performance Tuning

1. **Adjust memory policy**:

   ```text
   maxmemory 256mb
   maxmemory-policy allkeys-lru
   ```

2. **Configure persistence**:

   ```text
   save 900 1
   save 300 10
   save 60 10000
   ```

### Monitoring (Redis)

Key Redis metrics to monitor:

- Memory usage
- Hit/miss ratio
- Connected clients
- Commands per second
- Network I/O

### Troubleshooting (Redis)

#### Memory Issues

**Symptoms**: High memory usage or out-of-memory errors.

**Possible Causes**:

1. Large dataset
2. Inefficient key expiration
3. Memory leaks

**Solutions**:

1. Optimize data structures
2. Set appropriate expiration times
3. Use Redis eviction policies
4. Scale Redis instance

#### Performance Issues

**Symptoms**: Slow response times or timeouts.

**Possible Causes**:

1. Blocking operations
2. Large requests
3. Network latency

**Solutions**:

1. Avoid blocking commands (e.g., KEYS, FLUSHALL)
2. Batch operations when possible
3. Optimize network configuration

## Specialized Datastores

### Adding New Datastores

When adding new datastores to the architecture:

1. **Evaluate Requirements**:
   - Data model and access patterns
   - Performance requirements
   - Scalability needs
   - Consistency requirements

2. **Select Appropriate Technology**:
   - Document databases (MongoDB, Couchbase) for flexible schemas
   - Graph databases (Neo4j) for relationship-heavy data
   - Time-series databases (InfluxDB) for temporal data
   - Search engines (Elasticsearch) for full-text search

3. **Configuration**:
   - Add to Docker Compose files
   - Configure security settings
   - Set up monitoring and alerting

4. **Integration**:
   - Implement connection pooling
   - Add health checks
   - Configure backup and recovery procedures

### Example: Adding MongoDB

```yaml
# Docker Compose configuration
mongodb:
  image: mongo:7
  environment:
    MONGO_INITDB_ROOT_USERNAME: app
    MONGO_INITDB_ROOT_PASSWORD: app
  volumes:
    - mongo-data:/data/db
  ports:
    - "27017:27017"
```

## Monitoring and Metrics

### Centralized Monitoring

All datastores export metrics to Prometheus:

- PostgreSQL exporter for database metrics
- Redis exporter for cache metrics
- Specialized exporters for other datastores

### Key Metrics to Monitor

1. **Availability**:
   - Uptime
   - Connection success rate
   - Health check status

2. **Performance**:
   - Query/response latency
   - Throughput (requests/second)
   - Resource utilization (CPU, memory, disk)

3. **Capacity**:
   - Disk space usage
   - Memory usage
   - Connection count

4. **Data Integrity**:
   - Replication lag
   - Backup status
   - Error rates

### Grafana Dashboards

Grafana dashboards are available for each datastore:

- PostgreSQL dashboard with query performance and connection metrics
- Redis dashboard with memory usage and cache hit ratios
- Specialized dashboards for other datastores

## Security Considerations

### Authentication and Authorization

1. **Database Credentials**:
   - Use strong, unique passwords
   - Rotate credentials regularly
   - Store credentials securely (e.g., HashiCorp Vault)

2. **Network Security**:
   - Restrict database access to specific IP ranges
   - Use TLS encryption for data in transit
   - Implement network segmentation

3. **Application Access**:
   - Use least-privilege database users
   - Implement connection pooling
   - Monitor and audit database access

### Encryption

1. **Data in Transit**:
   - Use TLS connections between applications and databases
   - Configure certificate validation

2. **Data at Rest**:
   - Enable encryption for database files
   - Encrypt backups
   - Secure encryption keys

## Backup and Recovery

### PostgreSQL (Backup and Recovery)

1. **Logical Backups**:

   ```bash
   pg_dump -U app -d database_name > backup.sql
   ```

2. **Point-in-Time Recovery**:
   - Configure WAL archiving
   - Maintain base backups
   - Test recovery procedures regularly

### Redis (Backup and Recovery)

1. **RDB Persistence**:
   - Automatic snapshots based on configuration
   - Manual SAVE command

2. **AOF Persistence**:
   - Append-only file for durability
   - AOF rewrite for file size management

### Recovery Procedures

1. **Restore Process**:
   - Stop affected services
   - Restore database from backup
   - Validate data integrity
   - Restart services

2. **Disaster Recovery**:
   - Maintain offsite backups
   - Document recovery procedures
   - Regularly test recovery processes

## Maintenance

### Regular Tasks

1. **Database Maintenance**:
   - Run VACUUM and ANALYZE regularly
   - Monitor and optimize table indexes
   - Update statistics for query planner

2. **Cache Management**:
   - Monitor memory usage
   - Optimize key expiration policies
   - Clear stale cache entries

3. **Security Updates**:
   - Apply security patches promptly
   - Monitor for vulnerabilities
   - Update database software versions

### Performance Tuning (Datastores)

1. **Query Optimization**:
   - Analyze slow query logs
   - Add appropriate indexes
   - Optimize database schema

2. **Resource Allocation**:
   - Monitor resource usage
   - Scale resources as needed
   - Configure connection pooling

## Scaling

### Vertical Scaling

1. **Increase Resources**:
   - Add CPU and memory to database instances
   - Increase storage capacity
   - Optimize database configuration

### Horizontal Scaling

1. **Read Replicas**:
   - Configure read replicas for PostgreSQL
   - Distribute read load across replicas
   - Monitor replication lag

2. **Sharding**:
   - Implement horizontal partitioning
   - Distribute data across multiple instances
   - Manage shard routing

## Integration with Other Components

### Debezium CDC

PostgreSQL integrates with Debezium for change data capture:

- Logical replication enabled for CDC
- Dedicated replication user for Debezium
- Outbox pattern implementation for transactional messaging

### Caching Layer

Redis serves as the caching layer:

- Session storage for web applications
- Cache-aside pattern for database queries
- Distributed caching for microservices

### Monitoring Stack

All datastores integrate with the monitoring stack:

- Metrics exported to Prometheus
- Logs shipped to centralized logging
- Health checks integrated with service discovery

## Change Management

### Deployment Process

1. **Schema Changes**:
   - Use database migration tools (Flyway)
   - Test migrations in development environment
   - Apply migrations during maintenance windows

2. **Configuration Updates**:
   - Make changes in version-controlled configuration files
   - Test changes in staging environment
   - Apply to production with rollback plan

### Rollback Procedures

1. **Database Rollbacks**:
   - Maintain backup copies before major changes
   - Use transactional DDL when possible
   - Document rollback procedures for each change

2. **Configuration Rollbacks**:
   - Keep previous configuration versions
   - Use automated rollback mechanisms
   - Monitor for issues after rollback

## References

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [Debezium Documentation](https://debezium.io/documentation/)
- [Database Security Best Practices](https://owasp.org/www-project-database-security/)
