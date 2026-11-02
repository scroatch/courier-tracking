# Courier Tracking: technical notes

How to run the service and its API are in the [README](../README.md).

## How a location is processed

Everything below runs in one transaction (`LocationRecorder.record`):

1. Reject a `time` more than 5 minutes ahead of the server clock (`400`).
2. Lock the courier row (`PESSIMISTIC_WRITE`, a `SELECT ... FOR UPDATE`). Locations of the same
   courier are processed one after another; different couriers do not wait on each other's lock.
   A new courier is created with this location as its starting point and distance 0,
   then processing continues at step 6.
3. Same courier and same `time` as a stored location: ignore it.
4. `time` older than the courier's latest location: store it with `out_of_order = true` and stop.
   It changes neither the distance nor the entrances.
5. Add the segment to the courier's total distance, after the checks in [Distance](#distance).
6. Store the location, compare it with every store and record an entrance if there is one
   (see [Store entrances](#store-entrances)).

Each entrance publishes an event. Two listeners run after the commit: one writes a log line,
the other increments the `courier.store.entrances` counter (tag `store`). Because they run
after the commit, an entrance that was rolled back is never logged or counted.

## Store entrances

Stores are read from `src/main/resources/stores.json` at startup. A duplicate store name stops
the application, because the name is the store's key.

For every accepted location the service calculates the distance to each store; 100 m or less
is inside. It keeps one row per courier and store (`courier_store_presence`) and compares the
new state with it:

| Before | Now | Result |
|---|---|---|
| no row | outside | nothing |
| no row | inside | entrance, row created |
| inside | inside | nothing |
| inside | outside | exit: `inside = false`, `last_exit_at = time` |
| outside | outside | nothing |
| outside | inside | entrance if `time - last_exit_at >= 60 s`, otherwise back inside without an entrance |

An entrance is a move from outside to inside, so a courier waiting at a store is one entrance,
however long they wait. The case says: *"Reentries to the same store's circumference over
1 minute should not count as 'entrance'."* I read this as "within one minute after the courier
left".
Counting from the last entrance instead would count a courier who waits 5 minutes, steps out
and comes back after 10 seconds twice. Measured from the exit, the rule also absorbs GPS noise
around the 100 m line. `last_exit_at` is the time of the first location outside the radius;
the real exit happened somewhere between two locations.

The scenario in `LocationServiceTest.documentedScenario_recordsTwoEntrances`, one courier near
Ataşehir MMM:

| Time | Distance to store | Entrance | Why |
|---|---|---|---|
| 12:00:00 | 500 m | no | outside |
| 12:01:00 | 80 m | yes | first entrance |
| 12:01:10 | 50 m | no | already inside |
| 12:05:00 | 60 m | no | waiting is one entrance |
| 12:06:00 | 150 m | no | exit at 12:06:00 |
| 12:06:40 | 90 m | no | 40 s after the exit |
| 12:08:00 | 200 m | no | exit at 12:08:00 |
| 12:09:30 | 70 m | yes | 90 s after the exit |

Edge cases: exactly 100 m is inside; coming back exactly 60 s after the exit counts; an
out-of-order location never creates an entrance, even inside the radius; each store has its
own presence row, so two stores closer than 100 m would both work (the closest pair in the data
is about 980 m apart). A courier whose first location is already inside a store gets an
entrance. Couriers often start a shift at a store, so whether that should count is a business
decision; I kept it as an entrance.

## Distance

Distances are in meters everywhere. The default model is the WGS84 ellipsoid (Karney's
algorithm from GeographicLib), the same model GPS coordinates are given in.
`courier.distance.strategy: HAVERSINE` switches to a sphere; on the store coordinates the two
agree within 0.5% (`DistanceModelConsistencyTest`). The total is the sum of straight-line
(geodesic) segments between accepted GPS locations, not a road distance.

The total is kept on the courier row and grows with each counted segment; it is not
recalculated on read. Each location row also stores its own segment
(`segment_distance_meters`), so the total can be rebuilt or split by day later.

Summing raw GPS points gives the wrong number, so a segment is counted only after three checks
(`LocationRecorder.advance`). The "reference" is the last counted position. The "previous
location" is the latest one that was neither a duplicate nor out of order, counted or not; the
gap is measured from it. "Confirmed" is the last time a location was less than 25 m from the
reference.

| Check | Rule | Why | Cost |
|---|---|---|---|
| Gap | more than 30 min since the previous location: no distance, the new location becomes the reference | a straight line across a shift break or a night is not distance travelled | real movement during a signal loss longer than 30 min is lost |
| Jitter | less than 25 m from the reference: no distance, the reference stays | a standing courier drifts 5-15 m, which adds kilometers per hour; slow movement adds up from the reference and is counted once it passes 25 m | up to 25 m cut at each turn |
| Jump | faster than 150 km/h from the reference: no distance, the reference stays | one multipath measurement can land hundreds of meters away, and counting it adds the way there and back | a real move that fast is not counted |

The jump speed is `(distance - 25 m) / time since the last confirmed location`. A location
less than 25 m from the reference confirms that the courier is still there; a rejected jump does not.
My first version measured from the last counted location. A courier waiting at a store for a
minute then made a 1.5 km jump look like 88 km/h, and about 3 km was added
(`gpsSpikeWhileWaiting_addsNoDistance`). The 25 m is subtracted because the courier could have
been up to 25 m away at the confirmed time; without it a courier at 80 km/h looks like a jump
(`fastCourier_isNotMistakenForSpike`).

Filtered locations are not logged one by one. They increment `courier.locations.filtered`
(tag `reason`: `duplicate`, `out_of_order`, `gap`, `jump`); DEBUG logging shows each one.
Jitter is not counted there: a standing courier is the normal case, not bad data.

What the checks do not fix: with ±15 m of independent noise per location, the load test
measured 3,223-3,824 m (median 3,456 m) for a 2,990 m route, about 16% too much. Each counted
segment picks up some sideways noise; the jitter check reduces this but cannot remove it.
Real GPS errors are correlated between consecutive locations, so this is a pessimistic case.
Removing the rest needs smoothing, for example a Kalman filter, which I left out.

If the very first location of a courier, or the first one after a gap, is a jump, the
reference starts in the wrong place and one wrong segment is counted once the time is long
enough to look plausible.

The 30 min, 25 m and 150 km/h values are my estimates, not tuned on real data. All three are
in `application.yaml` under `courier.distance`.

## Concurrency

Locations of one courier have to be processed one at a time: each one reads the reference
point and the presence rows and writes them back. Two at once could lose a segment or record
the same entrance twice.

- The courier row is locked with `PESSIMISTIC_WRITE` at the start of the transaction. The lock
  lives in the database, so it still holds with several instances on a shared database.
  Different couriers never wait on each other's lock, but they share the database connections:
  requests waiting on one courier's lock hold a connection too (see
  [Known limits](#known-limits-and-things-left-out)).
- I chose a lock over optimistic versioning because concurrent writes for one courier do
  happen (a device retrying, or flushing its offline buffer), and an optimistic conflict would
  rerun the whole flow, presence updates included. The lock is held for a few milliseconds.
  A real courier sends about one location per second; one locked courier still took about
  490 writes per second in the load test.
- A new courier has no row to lock yet. If its first two locations arrive together, both try
  to insert it and one fails on the primary key. `LocationService` catches the
  `DataIntegrityViolationException` and runs the transaction once more; the second run finds
  the row and waits on its lock. `LocationRecorder` is a separate bean, so the retry starts a
  new transaction. A second failure is not retried and becomes a `500`.

`LocationServiceConcurrencyTest` covers both cases: the first-location race, and ten parallel
locations for one courier whose total must equal the sum of the stored segments.
`load-test/concurrency.js` repeats the race against the running app: 100 new couriers with
two simultaneous first locations each, 17 collisions, all resolved by the retry, and exactly
one entrance per courier.

## Design decisions

Decisions not already explained above:

| Decision | Why | Cost |
|---|---|---|
| H2 file database, Flyway migrations, `ddl-auto: validate` | runs with no setup and keeps data across restarts; the schema is versioned and Hibernate only checks it | one instance; the load test numbers are better than a networked database would give |
| Total distance of all time | the case does not name a period | no daily or weekly totals yet; `segment_distance_meters` keeps that possible |
| Total stored on the courier row | a read is one primary-key lookup | changing a filter does not fix old totals; they would be rebuilt from `courier_location` |
| The device's `time`, not the arrival time | late data is judged by when it was measured, and tests are deterministic | a device with a wrong clock is trusted, except more than 5 minutes ahead |
| Duplicate = same courier and `time`, with a unique constraint | a network retry never adds distance twice | a second, different position with the same timestamp is dropped |
| Out-of-order locations stored but not counted | no errors for late data, and the raw data is kept | late data never improves the distance; there is no re-ordering |
| Store name as the key | `stores.json` has no ids | renaming a store breaks its history |
| Entrances newest first, `limit` 1-500, default 50 | the `(courier_id, entered_at)` index returns the first rows without sorting | no cursor paging |
| Metric tag `store`, not `courierId` | the number of stores is small and fixed; courier ids would create a time series per courier | per-courier entrance counts come from the database |
| A `Clock` bean instead of `Instant.now()` | tests fix the current time for the future-time rule | none |

## Design patterns

| Pattern | Where | What it gives |
|---|---|---|
| Strategy | `DistanceCalculator` with `KarneyDistanceCalculator` and `HaversineDistanceCalculator`, picked in `DistanceCalculatorConfig` from `courier.distance.strategy` | the model changes by configuration; callers do not know which one runs |
| Adapter | `KarneyDistanceCalculator` puts GeographicLib's `Geodesic` behind our interface | library types stay out of the rest of the code |
| Observer | `CourierEnteredStoreEvent`, `StoreEntranceLogListener`, `StoreEntranceMetricsListener` (`@TransactionalEventListener`, after commit) | entrance detection does not know what reacts to it; a new reaction is a new listener |

Apart from these, the entrance rule is a plain class with no database access
(`StoreEntranceDetector`), so the whole transition table is unit tested without Spring
(`StoreEntranceDetectorTest`).

## Configuration

All values are in `application.yaml` and can be overridden like any Spring property, for
example `COURIER_DISTANCE_MAX_GAP=45m` as an environment variable.

| Property | Default | Meaning |
|---|---|---|
| `courier.stores-file` | `classpath:stores.json` | store list; `file:/path/stores.json` also works |
| `courier.entrance.radius-meters` | `100` | inside a store at this distance or less |
| `courier.entrance.reentry-interval` | `60s` | time after an exit before a new entrance counts |
| `courier.distance.strategy` | `KARNEY_WGS84` | or `HAVERSINE` |
| `courier.distance.max-gap` | `30m` | longer gaps add no distance |
| `courier.distance.min-movement-meters` | `25` | jitter threshold |
| `courier.distance.max-speed-kmh` | `150` | jump threshold |

The values are validated at startup, so for example a negative radius stops the application.
`/actuator/health` and `/actuator/metrics` are exposed; the two counters are
`courier.store.entrances` and `courier.locations.filtered`.

## Tests

`./mvnw verify` runs 81 tests in three layers:

| Layer | Classes | What they check |
|---|---|---|
| Unit, no Spring | `HaversineDistanceCalculatorTest`, `KarneyDistanceCalculatorTest`, `DistanceModelConsistencyTest`, `StoreEntranceDetectorTest`, `StoreLoaderTest`, `StoreEntranceMetricsListenerTest`, `DistanceCalculatorConfigTest` | distance values against reference numbers, the full entrance transition table including the 100 m and 60 s edges, store loading, strategy selection |
| Web slice (`@WebMvcTest`) | `LocationControllerTest`, `CourierControllerTest` | validation, status codes, error bodies, `limit` bounds; the service is mocked |
| Integration (`@SpringBootTest`, in-memory H2) | `LocationServiceTest`, `CourierQueryServiceTest`, `LocationServiceConcurrencyTest`, `StoreEntranceEventIntegrationTest`, `CourierTrackingApiTest` | duplicates, out-of-order data, the three distance checks, the entrance scenario above, locking and the first-location retry, after-commit events, one end-to-end flow over HTTP |

Integration tests run the Flyway migrations and Hibernate validates the entities against
them, so a mismatch between schema and code fails the build. `LocationServiceTest` replaces
the `Clock` bean with a fixed time.

GitHub Actions runs the same `./mvnw verify` on every push, on a clean Linux machine
(`.github/workflows/build.yml`).

## Load tests

The [k6](https://k6.io) scripts are in `load-test/` and run in Docker next to the app, so no
local install is needed. Each scenario was run on an empty database:

```bash
docker compose down -v                          # deletes the data volume
docker compose up -d --build courier-tracking
docker compose run --rm k6 run /scripts/courier-simulation.js   # or concurrency.js, stress.js
```

In Git Bash on Windows, run `export MSYS_NO_PATHCONV=1` first, or Git Bash rewrites
`/scripts/...` into a Windows path.

The app was limited to 2 CPUs and 1 GB by `docker-compose.yml`, on my laptop (Windows,
Docker Desktop), with H2 in file mode and k6 2.3.0. Raw outputs are in `load-test/results/`.

| Scenario | Load | Median | p95 | p99 | Errors | Checked |
|---|---|---|---|---|---|---|
| Steady load (`courier-simulation.js`) | 200 couriers, 1 location/s each for 5 min, ±15 m noise, 5% duplicates, 2% out of order; 64,188 location requests | 1.11 ms | 3.77 ms | 8.63 ms | 0 | every courier has exactly 3 entrances; measured distance 3,223-3,824 m for a 2,990 m route |
| Ramp to 2000 requests/s (`stress.js`) | 200, 500, 1000 and 2000 requests/s, 60 s each after a 10 s ramp; 250,000 requests | 0.35-0.51 ms | 0.45-1.06 ms | 1.7-11.0 ms | 0 | no dropped iterations; the breaking point was not reached on this machine |
| First-location race (`concurrency.js`) | 100 new couriers, 2 simultaneous first locations each | - | - | - | 0 | 17 collisions, all resolved by the retry; one entrance per courier; slowest request 1.2 s |
| One busy courier (`concurrency.js`) | 1 courier, 20 concurrent writers, 600 locations | 13.8 ms | 163 ms | 429 ms | 0 | about 490 writes/s, serialized by the lock |

What the numbers say:

- The limit is concurrency, not throughput. 2000 requests/s spread evenly hardly overlap
  (p95 0.9 ms). In the first-location race 200 requests arrive at the same moment and the
  slowest took 1.2 s. That run started on a fresh JVM, so this is most likely warm-up plus
  queueing for Hikari's 10 connections; I did not separate the two.
- These are upper bounds. Embedded H2 has no network round trip and does not sync to disk on
  every commit; real capacity should be measured on Postgres.
- `stress.js` sends each request to a random point within 300 m of a store, so 79% of them are
  rejected as jumps. It measures request handling, not the distance rules.
- That test also found a logging problem. Each rejected jump wrote an INFO line: 197,241 lines,
  99% of the log, and a p99 of 24.4 ms at 2000/s. After moving them to the
  `courier.locations.filtered` counter the run logged 2,094 lines and p99 was 11.0 ms.
- Single slow requests (about 1.2 s at the start of the steady run, one 260 ms stall in a ramp)
  are most likely JVM warm-up. The busy-courier p95 varies between runs (78-98 ms in an
  earlier run).
- In the first run k6 reached the app through Docker Desktop's Windows port forwarding and got
  14 `dial: i/o timeout` errors. k6 now runs in the same Docker network and there are none.

## Known limits and things left out

Security and privacy:

- No authentication. Anyone can send locations for any `courierId` and read any courier's
  distance and entrances. In production the id should come from the caller's token, and reads
  should be limited to the couriers the caller may see.
- Swagger UI, `/v3/api-docs` and `/actuator/metrics` are open. In production they go behind
  authentication or a separate management port.
- Courier locations are personal data (KVKK). A real system needs a retention period
  (deletion or anonymisation) and access control. Logs contain no coordinates.

Scale:

- Every location is checked against every store. That is fine for 5 stores, not for
  thousands: a bounding-box prefilter, geohash or PostGIS would pick the nearby ones. The
  candidates must also include stores the courier is currently inside, or exits are missed.
- One instance on H2. The state and the lock are in the database, so on Postgres the service
  should run as several instances without code changes; this is not tested. Metrics are per
  instance and would be summed by the monitoring system. With a message stream (Kafka),
  `courierId` as the partition key would keep one courier's locations in order.
- There is no lock timeout. Many parallel requests for one courier wait on its lock while
  holding database connections, so a client sending the same courier id in parallel can use
  up the pool (10 connections) and slow down every other courier. A lock timeout and a
  per-courier rate limit would stop that.
- `courier_location` grows forever; it needs partitioning or archiving.
- Event listeners run synchronously after the commit. Anything heavy (notifications) needs
  `@Async` or a queue.

Data quality:

- A rejected jump still counts for entrances. One that lands inside a store's radius creates
  a false entrance; one that lands outside creates a false exit (the courier is back within
  60 s, so there is no extra entrance, but the exit time moves). Leaving rejected jumps out of
  the presence check, or requiring two locations inside in a row, would fix it.
- A single location up to 5 minutes in the future, for example after a device clock jump,
  becomes the courier's latest location. Real locations for the next few minutes are then out
  of order: they add no distance and no entrances. A short re-ordering buffer or a smaller
  tolerance would reduce this.
- Entering and leaving use the same 100 m line, so a courier standing on it flips presence
  often. That adds no entrances, only writes; entering at 100 m and leaving beyond 120 m would
  stop it.
- Late locations are stored but never re-ordered. A batch endpoint that takes a device's
  offline buffer and processes it sorted in one transaction would recover them. The device's
  accuracy value is not part of the API; it could be used to drop poor fixes.
- There is no maximum age; very old locations are accepted.
- GPS noise still adds about 16% in the simulation (see [Distance](#distance)).

Product:

- Distance is for all time. There is no daily total, list of entrances per store, exit event,
  `GET /couriers/{id}` or `GET /stores`, and the entrance list has a limit but no paging.
- The entrance counter is in memory and starts at 0 after a restart; the durable count is the
  `store_entrance` table.
