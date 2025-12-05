import http from 'k6/http';
import { check, sleep } from 'k6';

const DEFAULT_LEVEL = 'medium';
const LEVEL_TARGETS = {
  light: 4,
  medium: 8,
  heavy: 16,
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

const BASE_URL = __ENV.LOAD_TEST_BASE_URL || 'http://inventory-service:8083';

export default function () {
  const sku = `sku-${(__VU % 5) + 1}`;
  const action = __ITER % 4;
  let res;
  if (action === 0) {
    res = reserve();
  } else {
    res = http.get(`${BASE_URL}/inventory/${sku}`);
  }
  check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });
  sleep(0.5);
}

function reserve() {
  const payload = JSON.stringify({
    orderId: `order-${__VU}-${Date.now()}`,
    items: [
      { sku: `sku-${(__VU % 5) + 1}`, quantity: 1 },
    ],
  });
  const params = {
    headers: { 'Content-Type': 'application/json' },
  };
  return http.post(`${BASE_URL}/inventory/reserve`, payload, params);
}
