import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

const COURIERS = 200;
const STEPS = 300;                 // one location per second for 5 minutes
const SPEED = 10;                  // m/s, a motorbike courier
const TURN_DISTANCE = 500;         // meters from the store where the courier turns back
const PERIOD = 4 * TURN_DISTANCE / SPEED; // 200 s: south -> north -> south
const GPS_NOISE = 15;              // meters, typical urban GPS error
const DUPLICATE_RATE = 0.05;       // network retries
const OUT_OF_ORDER_RATE = 0.02;    // late points from an offline buffer
const METERS_PER_DEGREE_LAT = 111320;

// The courier crosses the 100 m radius at steps 40, 140 and 240 and spends 78+ s
// outside between crossings, so every crossing is one entrance. Boundary jitter
// caused by GPS noise must be absorbed by the 60 s re-entry rule.
const EXPECTED_ENTRANCES = 3;

const STORES = [
    { lat: 40.9923307, lng: 29.1244229 }, // Ataşehir MMM
    { lat: 40.986106, lng: 29.1161293 },  // Novada MMM
    { lat: 41.0066851, lng: 28.6552262 }, // Beylikdüzü 5M
    { lat: 41.055783, lng: 29.0210292 },  // Ortaköy MMM
    { lat: 40.9632463, lng: 29.0630908 }, // Caddebostan MMM
];

export const options = {
    scenarios: {
        couriers: {
            executor: 'per-vu-iterations',
            vus: COURIERS,
            iterations: STEPS,
            maxDuration: '10m',
        },
    },
    thresholds: {
        'http_req_failed{name:record}': ['rate<0.001'],
        'http_req_duration{name:record}': ['p(95)<200'],
        'checks{type:entrances}': ['rate==1'],
    },
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
    return { runId: `run${Date.now()}`, startMs: Date.now() };
}

export default function (data) {
    const courierId = `${data.runId}-c${__VU}`;
    const store = STORES[(__VU - 1) % STORES.length];
    const step = __ITER;

    const body = locationBody(courierId, store, step, data.startMs);
    post(body);

    if (Math.random() < DUPLICATE_RATE) {
        post(body);
    }
    if (step >= 10 && Math.random() < OUT_OF_ORDER_RATE) {
        post(locationBody(courierId, store, step - 10, data.startMs));
    }

    sleep(1);
}

export function teardown(data) {
    for (let first = 1; first <= COURIERS; first += 50) {
        const requests = [];
        for (let vu = first; vu < first + 50 && vu <= COURIERS; vu++) {
            requests.push(['GET', `${BASE_URL}/api/v1/couriers/${data.runId}-c${vu}/entrances?limit=10`,
                null, { tags: { name: 'verify' } }]);
        }
        for (const res of http.batch(requests)) {
            check(res, {
                'every courier has exactly 3 entrances': (r) => r.status === 200 && r.json().length === EXPECTED_ENTRANCES,
            }, { type: 'entrances' });
        }
    }
}

function post(body) {
    const res = http.post(`${BASE_URL}/api/v1/locations`, body, {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'record' },
    });
    check(res, { 'location accepted': (r) => r.status === 204 }, { type: 'record' });
}

function locationBody(courierId, store, step, startMs) {
    const north = metersNorthOfStore(step) + noise();
    const east = noise();
    return JSON.stringify({
        courierId,
        lat: store.lat + north / METERS_PER_DEGREE_LAT,
        lng: store.lng + east / (METERS_PER_DEGREE_LAT * Math.cos(store.lat * Math.PI / 180)),
        time: new Date(startMs + step * 1000).toISOString(),
    });
}

function metersNorthOfStore(step) {
    const phase = step % PERIOD;
    const half = PERIOD / 2;
    return phase <= half
        ? -TURN_DISTANCE + SPEED * phase
        : TURN_DISTANCE - SPEED * (phase - half);
}

function noise() {
    return (Math.random() * 2 - 1) * GPS_NOISE;
}