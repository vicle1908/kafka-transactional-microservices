#!/bin/bash

# Test script to verify observability stack startup
# This script starts the observability components and checks their health

set -e

echo "🔍 Testing Observability Stack Startup"
echo "=================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if we're in the right directory
if [ ! -f "infra/compose.yml" ]; then
    echo "❌ Please run this script from the project root directory."
    exit 1
fi

# Check if .env file exists
if [ ! -f "infra/.env" ]; then
    echo "📝 Creating .env file from example..."
    cp infra/.env.example infra/.env
fi

echo "🚀 Starting observability stack..."

# Start the observability components
docker compose --env-file infra/.env -f infra/compose.yml --profile local up -d

echo "⏳ Waiting for services to be healthy..."

# Function to check service health
check_service_health() {
    local service_name=$1
    local max_attempts=30
    local attempt=1

    echo "🔍 Checking $service_name health..."

    while [ $attempt -le $max_attempts ]; do
        if docker compose --env-file infra/.env -f infra/compose.yml ps $service_name | grep -q "healthy\|Up"; then
            echo "✅ $service_name is healthy!"
            return 0
        fi

        echo "   Attempt $attempt/$max_attempts: $service_name not ready yet..."
        sleep 5
        ((attempt++))
    done

    echo "❌ $service_name failed to become healthy within timeout."
    return 1
}

# Check each observability component
services=(
    "elasticsearch"
    "logstash"
    "kibana"
    "prometheus"
    "grafana"
    "otel-collector"
    "jaeger"
)

all_healthy=true

for service in "${services[@]}"; do
    if ! check_service_health "$service"; then
        all_healthy=false
    fi
done

echo ""
echo "📊 Service Status Summary:"
echo "========================="

docker compose --env-file infra/.env -f infra/compose.yml ps

echo ""
echo "🌐 Access URLs:"
echo "=============="
echo "• Kibana Dashboard: http://localhost:5601"
echo "• Grafana Dashboard: http://localhost:3000 (admin/admin)"
echo "• Jaeger Tracing: http://localhost:16686"
echo "• Prometheus: http://localhost:9090"
echo "• Elasticsearch API: http://localhost:9200"

if [ "$all_healthy" = true ]; then
    echo ""
    echo "🎉 All observability components are running successfully!"
    echo ""
    echo "📝 Next steps:"
    echo "1. Open Kibana to create index patterns for logs"
    echo "2. Import the service logs dashboard from infra/kibana/dashboards/"
    echo "3. Configure Grafana datasources for Prometheus"
    echo "4. Start microservices to generate traces and logs"
else
    echo ""
    echo "⚠️  Some services may not be fully healthy. Check the logs above."
    echo ""
    echo "🔧 Troubleshooting:"
    echo "• Check individual service logs: docker compose logs <service-name>"
    echo "• Verify port availability: lsof -i :<port>"
    echo "• Check system resources: docker system df"
fi

echo ""
echo "🛑 To stop the observability stack:"
echo "docker compose --env-file infra/.env -f infra/compose.yml --profile local down"