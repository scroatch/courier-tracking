import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

const RACING_COURIERS = 100;      // two requests per courier at the same moment
const HOT_WRITERS = 20;           // concurrent writers for a single courier
const HOT_LOCATIONS_PER_WRITER = 30;

const ATASEHIR = { lat: 40.9923307, lng: 29.1244229 };
const OUTSIDE = { lat: 41.0, lng: 29.0 }; // no store within 100 m

export const options = {
    scenarios: {
        firstLocationRace: {
            executor: 'per-vu-iterations',
            vus: RACING_COURIERS * 2,
            iterations: 1,
            maxDuration: '30s',
            exec: 'firstLocationRace',
        },
        hotCourier: {
            executor: 'per-vu-iterations',
            vus: HOT_WRITERS,
            iterations: HOT_LOCATIONS_PER_WRITER,
            startTime: '10s',
            maxDuration: '1m',
            exec: 'hotCourier',
        },
    },
    thresholds: {
        'http_req_failed{scenario:firstLocationRace}': ['rate==0'],
        'http_req_failed{scenario:hotCourier}': ['rate==0'],
        'http_req_duration{scenario:hotCourier}': ['p(95)<500'],
        'checks{type:race}': ['rate==1'],
    },
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
    // an hour in the past, so even the hot courier's last location is not in the future
    return { runId: `run${Date.now()}`, startMs: Date.now() - 3600 * 1000 };
}

export function firstLocationRace(data) {
    const i = exec.scenario.iterationInTest;     // 0..199
    const courierId = `${data.runId}-race${Math.floor(i / 2)}`;
    const time = new Date(data.startMs + (i % 2) * 1000).toISOString();
    record(courierId, ATASEHIR.lat, ATASEHIR.lng, time);
}

export function hotCourier(data) {
    const i = exec.scenario.iterationInTest;     // 0..599, every location has a unique time
    const time = new Date(data.startMs + i * 1000).toISOString();
    record(`${data.runId}-hot`, OUTSIDE.lat + (i % 50) * 0.0001, OUTSIDE.lng, time);
}

export function teardown(data) {
    const requests = [];
    for (let c = 0; c < RACING_COURIERS; c++) {
        requests.push(['GET', `${BASE_URL}/api/v1/couriers/${data.runId}-race${c}/entrances`,
            null, { tags: { name: 'verify' } }]);
    }
    for (const res of http.batch(requests)) {
        check(res, {
            'racing courier has exactly 1 entrance': (r) => r.status === 200 && r.json().length === 1,
        }, { type: 'race' });
    }

    const hot = http.get(`${BASE_URL}/api/v1/couriers/${data.runId}-hot/total-distance`, { tags: { name: 'verify' } });
    check(hot, { 'hot courier exists': (r) => r.status === 200 }, { type: 'race' });
}

function record(courierId, lat, lng, time) {
    const res = http.post(`${BASE_URL}/api/v1/locations`,
        JSON.stringify({ courierId, lat, lng, time }),
        { headers: { 'Content-Type': 'application/json' }, tags: { name: 'record' } });
    check(res, { 'location accepted': (r) => r.status === 204 }, { type: 'record' });
}