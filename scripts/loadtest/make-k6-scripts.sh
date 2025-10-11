#!/usr/bin/env bash
set -euo pipefail

mkdir -p load-tests

# Determine load parameters based on input env vars
VUS=10
DURATION="${LOAD_TEST_DURATION:-10}m"
RAMP_UP="60s"
case "${LOAD_LEVEL:-medium}" in
  light)
    VUS=5
    DURATION="2m"
    RAMP_UP="30s"
    ;;
  medium)
    VUS=20
    DURATION="5m"
    RAMP_UP="60s"
    ;;
  heavy)
    VUS=50
    DURATION="10m"
    RAMP_UP="120s"
    ;;
esac

# Create primary load test script
cat > load-tests/microservices-load-test.js << EOF
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const failureRate = new Rate('failures');
const orderResponseTime = new Trend('order_response_time');
const paymentResponseTime = new Trend('payment_response_time');

export const options = {
  stages: [
    { duration: '${RAMP_UP}', target: ${VUS} },
    { duration: '${DURATION}', target: ${VUS} },
    { duration: '${RAMP_UP}', target: 0 },
  ],
  thresholds: {
    'http_req_failed': ['rate<0.1'],
    'http_req_duration': ['p(95)<3000'],
    'http_req_waiting': ['p(95)<2500'],
    'failures': ['rate<0.1'],
    'order_response_time': ['p(95)<2000'],
    'payment_response_time': ['p(95)<2500'],
  },
};

const BASE_URL = 'http://localhost';

function generateUniqueId() {
  return Math.random().toString(36).substr(2, 9) + Date.now();
}

export function setup() {
  console.log('Load test setup complete');
  console.log('Configuration:');
  console.log('- VUs: ${VUS}');
  console.log('- Duration: ${DURATION}');
  console.log('- Ramp-up: ${RAMP_UP}');

  const response = http.get(`${BASE_URL}:8081/actuator/health`);
  check(response, { 'orders service is accessible': (r) => r.status === 200 }) || console.error('Orders service is not accessible');

  return { startTime: new Date().toISOString() };
}

export default function (data) {
  const uniqueId = generateUniqueId();

  // Test Orders Service
  const orderPayload = JSON.stringify({
    customerId: "test-customer-" + uniqueId,
    orderItems: [
      { productId: "product-1", quantity: 2, price: 29.99 },
      { productId: "product-2", quantity: 1, price: 49.99 }
    ]
  });

  const orderStart = Date.now();
  const orderResponse = http.post(`${BASE_URL}:8081/orders`, orderPayload, {
    headers: { 'Content-Type': 'application/json', 'X-Request-ID': uniqueId },
    timeout: '10s',
  });
  const orderEnd = Date.now();

  orderResponseTime.add(orderEnd - orderStart);

  const orderChecks = check(orderResponse, {
    'create order status is 200 or 201': (r) => r.status >= 200 && r.status < 300,
    'create order response has orderId': (r) => r.json('orderId') !== undefined,
    'order response time < 2s': (r) => r.timings.duration < 2000,
  });

  if (orderChecks && orderResponse.json('orderId')) {
    sleep(1);
    const paymentPayload = JSON.stringify({
      orderId: orderResponse.json('orderId'),
      amount: 109.97,
      currency: 'USD',
      paymentMethod: 'credit_card'
    });

    const paymentStart = Date.now();
    const paymentResponse = http.post(`${BASE_URL}:8082/payments`, paymentPayload, {
      headers: { 'Content-Type': 'application/json', 'X-Request-ID': uniqueId + '-payment' },
      timeout: '15s',
    });
    const paymentEnd = Date.now();

    paymentResponseTime.add(paymentEnd - paymentStart);

    check(paymentResponse, {
      'payment status is 200 or 201': (r) => r.status >= 200 && r.status < 300,
      'payment response has paymentId': (r) => r.json('paymentId') !== undefined,
      'payment response time < 2.5s': (r) => r.timings.duration < 2500,
    });
  }

  failureRate.add(!orderChecks);
  sleep(Math.random() * 2 + 1);
}

export function teardown(data) {
  console.log('Load test teardown started');
  console.log('Test started at:', data.startTime);
  console.log('Test completed at:', new Date().toISOString());
}
EOF

# Create health check test
cat > load-tests/health-check.js << EOF
import http from 'k6/http';
import { check } from 'k6';

export default function () {
  const responses = http.batch([
    ['GET', 'http://localhost:8081/actuator/health'],
    ['GET', 'http://localhost:8082/actuator/health'],
  ]);

  check(responses[0], { 'orders service health is 200': (r) => r.status === 200 });
  check(responses[1], { 'payments service health is 200': (r) => r.status === 200 });
}
EOF
