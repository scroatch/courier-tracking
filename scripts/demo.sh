#!/usr/bin/env bash
# Sends five couriers to the running service, each one showing a rule, and compares what the
# service recorded with the expected result. Needs bash and curl; exits with 1 on a difference.
# Usage: scripts/demo.sh [base-url]   (default http://localhost:8080)
set -uo pipefail

BASE_URL="${1:-http://localhost:8080}"
RUN="demo-$(date +%s)"   # new courier ids on every run
FAILED=0

send() { # courier, time, lat, lng, note
  local status
  status=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/api/v1/locations" \
    -H 'Content-Type: application/json' \
    -d "{\"courierId\":\"$1\",\"lat\":$3,\"lng\":$4,\"time\":\"2026-11-01T$2Z\"}" | tail -n 1)
  printf '  %s  %-36s %s\n' "$2" "$5" "$status"
}

check() { # courier, expected entrance count, expected meters, expected entrances as text
  local entrances distance count
  entrances=$(curl -s "$BASE_URL/api/v1/couriers/$1/entrances")
  distance=$(curl -s "$BASE_URL/api/v1/couriers/$1/total-distance" \
    | sed -E 's/.*"totalDistanceMeters":([-0-9.eE+]+).*/\1/')
  count=$(printf '%s' "$entrances" | grep -o '"storeName"' | wc -l | tr -d ' ')
  echo "  entrances: $entrances"
  echo "  distance:  $distance m"
  if [ "$count" = "$2" ] && awk -v a="$distance" -v e="$3" 'BEGIN { exit !(a > e - 1 && a < e + 1) }'; then
    echo "  OK: $4, about $3 m"
  else
    echo "  DIFFERENT: expected $4 ($2 entrances), about $3 m"
    FAILED=1
  fi
  echo
}

if ! curl -sf "$BASE_URL/actuator/health" > /dev/null; then
  echo "The service is not reachable at $BASE_URL. Start it first: docker compose up --build"
  exit 1
fi


echo "Courier $RUN-1: Ataşehir: in, out, back after 40 s (not an entrance), out, back after 90 s"
send "$RUN-1" 12:00:00 40.9968223 29.1244229 "500 m north"
send "$RUN-1" 12:01:00 40.9930493 29.1244229 "80 m, entrance"
send "$RUN-1" 12:01:10 40.9927799 29.1244229 "50 m"
send "$RUN-1" 12:05:00 40.9928697 29.1244229 "60 m, still inside"
send "$RUN-1" 12:06:00 40.9936782 29.1244229 "150 m, exit"
send "$RUN-1" 12:06:40 40.9931392 29.1244229 "90 m, back after 40 s"
send "$RUN-1" 12:08:00 40.9941273 29.1244229 "200 m, exit"
send "$RUN-1" 12:09:30 40.9929595 29.1244229 "70 m, back after 90 s, entrance"
check "$RUN-1" 2 848.0 "Ataşehir 12:01:00 and 12:09:30"

echo "Courier $RUN-2: Novada: waits about 5 minutes with GPS jitter"
send "$RUN-2" 12:00:00 40.9834111 29.1161293 "300 m south"
send "$RUN-2" 12:01:00 40.9859263 29.1161293 "20 m, entrance"
send "$RUN-2" 12:01:30 40.9859263 29.1162007 "waiting, GPS jitter"
send "$RUN-2" 12:02:00 40.9858814 29.1160817 "waiting, GPS jitter"
send "$RUN-2" 12:02:30 40.9859892 29.1161531 "waiting, GPS jitter"
send "$RUN-2" 12:03:00 40.9858994 29.1162007 "waiting, GPS jitter"
send "$RUN-2" 12:03:30 40.9859713 29.1160460 "waiting, GPS jitter"
send "$RUN-2" 12:04:00 40.9858724 29.1161055 "waiting, GPS jitter"
send "$RUN-2" 12:04:30 40.9859623 29.1161888 "waiting, GPS jitter"
send "$RUN-2" 12:05:00 40.9858635 29.1161650 "waiting, GPS jitter"
send "$RUN-2" 12:05:30 40.9859802 29.1160698 "waiting, GPS jitter"
send "$RUN-2" 12:06:00 40.9859084 29.1160579 "waiting, GPS jitter"
send "$RUN-2" 12:08:00 40.9834111 29.1161293 "300 m south, exit"
check "$RUN-2" 1 558.6 "Novada 12:01:00"

echo "Courier $RUN-3: Ataşehir -> Novada -> Ataşehir, starting inside a store"
send "$RUN-3" 12:00:00 40.9923307 29.1244229 "at Ataşehir, entrance"
send "$RUN-3" 12:02:00 40.9861060 29.1161293 "at Novada, entrance"
send "$RUN-3" 12:04:00 40.9923307 29.1244229 "back at Ataşehir, entrance"
check "$RUN-3" 3 1964.6 "Ataşehir 12:00:00, Novada 12:02:00, Ataşehir 12:04:00"

echo "Courier $RUN-4: Caddebostan: one GPS jump while waiting"
send "$RUN-4" 12:00:00 40.9632463 29.0630908 "at Caddebostan, entrance"
send "$RUN-4" 12:00:10 40.9632463 29.0631622 "waiting"
send "$RUN-4" 12:00:20 40.9632014 29.0630432 "waiting"
send "$RUN-4" 12:00:30 40.9633092 29.0631146 "waiting"
send "$RUN-4" 12:00:40 40.9632194 29.0631622 "waiting"
send "$RUN-4" 12:00:50 40.9632912 29.0630075 "waiting"
send "$RUN-4" 12:01:00 40.9631924 29.0630670 "waiting"
send "$RUN-4" 12:01:10 40.9767210 29.0630908 "GPS jump 1.5 km north"
send "$RUN-4" 12:01:20 40.9632822 29.0631503 "waiting"
send "$RUN-4" 12:01:30 40.9631834 29.0631265 "waiting"
send "$RUN-4" 12:01:40 40.9633002 29.0630313 "waiting"
send "$RUN-4" 12:01:50 40.9632283 29.0630194 "waiting"
check "$RUN-4" 1 0.0 "Caddebostan 12:00:00"

echo "Courier $RUN-5: Beylikdüzü: duplicate, late location and a 44-minute signal gap"
send "$RUN-5" 12:00:00 41.0066851 28.6516550 "300 m west"
send "$RUN-5" 12:01:00 41.0066851 28.6552262 "at the store, entrance"
send "$RUN-5" 12:01:00 41.0066851 28.6552262 "same location again (retry)"
send "$RUN-5" 12:00:30 41.0066851 28.6534406 "late location from 12:00:30"
send "$RUN-5" 12:45:00 41.0066851 28.6587974 "300 m east after 44 min, exit"
send "$RUN-5" 12:46:00 41.0066851 28.6552262 "back at the store, entrance"
check "$RUN-5" 2 600.9 "Beylikdüzü 12:01:00 and 12:46:00"

if [ "$FAILED" -ne 0 ]; then
  echo "Some results differ from the expected values."
  exit 1
fi
echo "All five couriers match the expected results."
