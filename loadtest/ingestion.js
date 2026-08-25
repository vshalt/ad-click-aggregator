import http from 'k6/http';
import { check } from 'k6';
import { randomItem, randomString } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

const RATE = Number(__ENV.RATE || 1000);
const DURATION = __ENV.DURATION || '30s';
const URL = __ENV.URL || 'http://localhost:8080/api/v1/logs/';

const services = ['auth', 'billing', 'search', 'cart', 'recommendation', 'gateway'];
const levels = ['INFO', 'WARN', 'ERROR', 'DEBUG'];

export const options = {
    scenarios: {
        ingest: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: Math.ceil(RATE / 20),
            maxVUs: Math.ceil(RATE/2),
        }
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500']
    }
};

export default function() {
    const payload = JSON.stringify({
        service: randomItem(services),
        level: randomItem(levels),
        message: `evt-${randomString(12)} value=${Math.floor(Math.random() * 1000000)}`,
        timestamp: new Date().toISOString()
    });
    const res = http.post(URL, payload, {
        headers: {'Content-Type': 'application/json'}
    });
    check(res, {'status is 2xx': (r) => r.status >= 200 && r.status < 300});
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    '/scripts/results/summary.json': JSON.stringify(data, null, 2),
  };
}