# Courier Tracking

A REST service that receives a stream of courier locations, logs when a courier
enters the 100 m radius of a Migros store, and returns the total distance each
courier has travelled. Java 21, Spring Boot 4.1, H2 (file) and Flyway.

## What the case asks for

| Requirement | Where |
|---|---|
| Take a stream of courier locations (time, courier, lat, lng) | `POST /api/v1/locations`, see [API](#api) |
| Log courier and store when a courier enters 100 m of a Migros store | a log line and a `store_entrance` row, see [Store entrances](docs/DESIGN.md#store-entrances) |
| Do not count reentries to the same store within 1 minute | measured from the exit, see [Store entrances](docs/DESIGN.md#store-entrances) |
| Store locations from `stores.json` | `src/main/resources/stores.json`, loaded at startup |
| Query a courier's total distance, e.g. `getTotalTravelDistance(courierId)` | `CourierQueryService.getTotalTravelDistance` and `GET /api/v1/couriers/{courierId}/total-distance` |
| At least 2 design patterns | Strategy, Adapter and Observer, see [Design patterns](docs/DESIGN.md#design-patterns) |
| Instructions to run and test; a script is nice to have | [Running it](#running-it) and `scripts/demo.sh`, see [Try it](#try-it) |

## Running it

With Docker (nothing else needed):

```bash
docker compose up --build
```

The API listens on port 8080 and Swagger UI is at http://localhost:8080/swagger-ui.html.
Data is kept in the `courier-data` volume between restarts; `docker compose down -v` deletes it.

Without Docker you need JDK 21:

```bash
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Data is written to `./data`. Delete the folder to start from an empty database.

The `dev` profile enables the H2 console at http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:file:./data/courier-tracking`, user `sa`, empty password) and DEBUG logging:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# PowerShell: .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

If port 8080 is taken, change the left side of `"8080:8080"` in `docker-compose.yml`,
or add `-Dspring-boot.run.arguments=--server.port=8081` to the Maven command.

Tests: `./mvnw verify` (81 tests).

## Try it

With the app running, `scripts/demo.sh` sends five couriers, each showing one rule, and
compares what the service recorded with the expected result. It needs only bash and curl
(Git Bash on Windows), prints each courier's id so you can query it afterwards, and exits
with 1 if anything differs. Every run uses new courier ids.

```bash
scripts/demo.sh                 # or scripts/demo.sh http://localhost:8081
```

| Courier | Store | What it shows | Expected |
|---|---|---|---|
| 1 | Ataşehir | in, out, back after 40 s, out, back after 90 s | 2 entrances, 848 m |
| 2 | Novada | waits about 5 minutes with GPS jitter | 1 entrance, 559 m, nothing added while waiting |
| 3 | Ataşehir, Novada | starts inside a store, then visits two stores | 3 entrances, 1,965 m |
| 4 | Caddebostan | a 1.5 km GPS jump while waiting | 1 entrance, 0 m |
| 5 | Beylikdüzü | a duplicate, a late location, a 44-minute gap, back exactly 60 s after leaving | 2 entrances, 601 m |

The expected values were calculated from the rules in [docs/DESIGN.md](docs/DESIGN.md), before
the script was run against the service.

## API

| Method | Path | Response |
|---|---|---|
| `POST` | `/api/v1/locations` | `204`, also for duplicate and out-of-order locations |
| `GET` | `/api/v1/couriers/{courierId}/total-distance` | `200` with the distance in meters, `404` for an unknown courier |
| `GET` | `/api/v1/couriers/{courierId}/entrances?limit=50` | `200` with store entrances, newest first; `limit` is 1–500; `404` for an unknown courier |

The first two endpoints are what the case asks for. I added the third one so the entrance
logic can be checked without opening the database.

### Sending a location

```bash
curl -i -X POST http://localhost:8080/api/v1/locations \
  -H 'Content-Type: application/json' \
  -d '{"courierId":"courier-1","lat":40.9923307,"lng":29.1244229,"time":"2026-11-01T22:00:53Z"}'
```

- `courierId`: required, at most 64 characters.
- `lat` (−90 to 90) and `lng` (−180 to 180): required. A missing coordinate is an error, not 0.
- `time`: required; when the device measured the location, as an ISO-8601 instant with an offset, for
  example `2026-11-01T12:00:00Z` or `2026-11-01T15:00:00+03:00`. Ordering, distance and the
  one-minute rule use this time, not the arrival time. Up to 5 minutes ahead of the server
  clock is accepted for clock skew.

A location with the same `courierId` and `time` as an earlier one is a duplicate and is ignored.
A location older than the courier's latest one is stored but not counted. Both return `204`:
a device that retries a request or sends its offline buffer late should not get errors.
In Swagger this means the second click on the same example does nothing; change `time`.

### Reading the results

After the location above and a second one about 300 m north a minute later:

```bash
curl http://localhost:8080/api/v1/couriers/courier-1/total-distance
```

```json
{"courierId":"courier-1","totalDistanceMeters":299.289952402662}
```

```bash
curl http://localhost:8080/api/v1/couriers/courier-1/entrances
```

```json
[{"storeName":"Ataşehir MMM Migros","enteredAt":"2026-11-01T22:00:53Z"}]
```

The first location was already inside Ataşehir MMM, so it counts as an entrance.

### Errors

Errors use the `application/problem+json` format (RFC 9457). Body validation errors list
the fields (`"courierId":""`, `"lat":91`):

```json
{
  "detail": "Invalid request content",
  "instance": "/api/v1/locations",
  "status": 400,
  "title": "Bad Request",
  "errors": {
    "courierId": "must not be blank",
    "lat": "must be less than or equal to 90"
  }
}
```

| Case | Status | `detail` |
|---|---|---|
| Missing `lat` | 400 | `Invalid request content`, with `errors.lat`: `must not be null` |
| `time` more than 5 minutes ahead | 400 | `Location time 2026-11-01T22:12:53Z is in the future (server time 2026-11-01T22:02:54.366535884Z)` |
| Unreadable `time` or malformed JSON | 400 | `Failed to read request` |
| Unknown courier | 404 | `nobody not found` |
| `limit=0` | 400 | `Validation failure` |
| `limit=abc` | 400 | `Failed to convert 'limit' with value: 'abc'` |
| Unexpected error | 500 | `Unexpected error` |

An unknown courier is a 404 and not 0 m, because 0 would read as a courier that never moved.
Parser errors do not say which field failed: Jackson's message names internal classes, so
it stays in the server. Query parameter errors (`limit`) do not list the field yet, unlike
body errors. A 500 never carries the exception; it is only logged
(`unexpectedError_returnsProblemDetailWithoutInternals`).

## How it works, in short

- Each location is processed in one transaction with the courier row locked, so the locations
  of one courier are handled in order.
- An entrance is a move from outside to inside 100 m of a store; coming back within 60 s of
  leaving does not count.
- Distance is the sum of straight-line segments between locations, after three GPS checks:
  gaps over 30 min, jitter under 25 m and jumps faster than 150 km/h add nothing.
- Load tests: 200 couriers sending a location every second ran with p95 3.8 ms and no errors,
  and the service handled up to 2000 requests/s without errors.
- Known limits include no authentication, H2 as a single instance, and about 16% extra
  distance under heavy GPS noise.

Details, numbers and trade-offs are in [docs/DESIGN.md](docs/DESIGN.md): how a location is
processed, store entrances, distance, concurrency, design decisions and patterns,
configuration, tests, load tests and known limits.
