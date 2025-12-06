import http from 'k6/http';
import { check, sleep } from 'k6';

const DEFAULT_LEVEL = 'medium';
const LEVEL_TARGETS = {
  light: 3,
  medium: 6,
  heavy: 12,
};

function parseDurationMinutes(raw) {
  const value = Number(raw);
  if (Number.isFinite(value) && value > 0) {
    return value;
  }
  return 3;
}

function formatDuration(seconds) {
  if (seconds <= 0) {
    return '1s';
  }
  if (seconds % 60 === 0) {
    return `${seconds / 60}m`;
  }
  return `${seconds}s`;
}

function buildStages() {
  const durationMinutes = parseDurationMinutes(__ENV.LOAD_TEST_DURATION || __ENV.DURATION_MINUTES || 3);
  const totalSeconds = Math.max(90, Math.round(durationMinutes * 60));
  const rampSeconds = Math.max(30, Math.round(totalSeconds * 0.2));
  const steadySeconds = Math.max(30, totalSeconds - rampSeconds * 2);
  const rampDownSeconds = Math.max(30, totalSeconds - rampSeconds - steadySeconds);
  const target = LEVEL_TARGETS[(__ENV.LOAD_LEVEL || DEFAULT_LEVEL).toLowerCase()] || LEVEL_TARGETS[DEFAULT_LEVEL];

  return [
    { duration: formatDuration(rampSeconds), target },
    { duration: formatDuration(steadySeconds), target },
    { duration: formatDuration(rampDownSeconds), target: 0 },
  ];
}

export const options = {
  stages: buildStages(),
  thresholds: {
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.95'],
  },
};

const BASE_URL = __ENV.LOAD_TEST_BASE_URL || 'http://payments-service:8080';

export default function () {
  const payload = JSON.stringify({
    paymentId: `load-test-${__VU}-${Date.now()}`,
    orderId: `order-${__VU}-${Date.now()}`,
    amount: 1999,
    currency: 'USD',
    customerId: `customer-${__VU}`,
  });
  const params = {
    headers: { 'Content-Type': 'application/json' },
  };
  const res = http.post(`${BASE_URL}/payments`, payload, params);
  check(res, {
    'status is 202/200': (r) => r.status === 200 || r.status === 202,
  });
  sleep(1);
}
