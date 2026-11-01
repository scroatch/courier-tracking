import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

const COURIERS = 2000;
const RATES = [200, 500, 1000, 2000];   // requests per second
const RAMP_SECONDS = 10;
const HOLD_SECONDS = 60;
const SPREAD = 300;                     // meters around a store
const METERS_PER_DEGREE_LAT = 111320;

const STORES = [
    { lat: 40.9923307, lng: 29.1244229 },
    { lat: 40.986106, lng: 29.1161293 },
    { lat: 41.0066851, lng: 28.6552262 },
    { lat: 41.055783, lng: 29.0210292 },
    { lat: 40.9632463, lng: 29.0630908 },
];

const thresholds = {};
for (const rate of RATES) {
    thresholds[`http_req_duration{rate:${rate}}`] = ['p(95)<200'];
    thresholds[`http_req_failed{rate:${rate}}`] = ['rate<0.001'];
}

export const options = {
    scenarios: {
        stress: {
            executor: 'ramping-arrival-rate',
            startRate: RATES[0],
            timeUnit: '1s',
            preAllocatedVUs: 200,
            maxVUs: 1000,
            stages: RATES.flatMap((rate) => [
                { target: rate, duration: `${RAMP_SECONDS}s` },
                { target: rate, duration: `${HOLD_SECONDS}s` },
            ]),
        },
    },
    thresholds,
    summaryTrendStats: ['med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
    return { runId: `run${Date.now()}`, startMs: Date.now() - 3600 * 1000 };
}

export default function (data) {
    const i = exec.scenario.iterationInTest;
    const courier = i % COURIERS;
    const step = Math.floor(i / COURIERS);
    const store = STORES[courier % STORES.length];

    const north = (Math.random() * 2 - 1) * SPREAD;
    const east = (Math.random() * 2 - 1) * SPREAD;
    const body = JSON.stringify({
        courierId: `${data.runId}-s${courier}`,
        lat: store.lat + north / METERS_PER_DEGREE_LAT,
        lng: store.lng + east / (METERS_PER_DEGREE_LAT * Math.cos(store.lat * Math.PI / 180)),
        time: new Date(data.startMs + step * 1000).toISOString(),
    });

    const res = http.post(`${BASE_URL}/api/v1/locations`, body, {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'record', rate: currentRate() },
    });
    check(res, { 'location accepted': (r) => r.status === 204 });
}

// "200", "500", ... while a rate is held; "ramp" while moving between rates
function currentRate() {
    const elapsed = exec.instance.currentTestRunDuration / 1000;
    const block = RAMP_SECONDS + HOLD_SECONDS;
    const index = Math.floor(elapsed / block);
    if (index >= RATES.length || elapsed % block < RAMP_SECONDS) {
        return 'ramp';
    }
    return String(RATES[index]);
}