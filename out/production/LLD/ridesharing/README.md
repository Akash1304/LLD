# Ride-Sharing Service LLD

Drivers, riders, nearest-driver matching, and distance-based (optionally
surged) fares. Two implementations live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleRideSharingInterview.java`) — a
  single ~100-line file covering nearest-driver matching, ride completion
  with a fare, and cancellation, for when you only have 15-20 minutes or
  want a warm-up before the full design.

## Problem statement

> Design a ride-sharing service: a rider requests a ride between two
> locations, the system matches them with a nearby available driver, and
> a fare is calculated (with support for pricing that varies, e.g. by
> demand) when the ride completes.

## Clarifying questions to ask in the first 10 minutes

- How is "nearest driver" defined — straight-line distance, or real road
  distance/ETA? (Modeled here: **straight-line (Euclidean) distance on a
  flat plane** — a deliberate simplification; see Known Gaps for what a
  production system would use instead.)
- Is a driver locked to one ride at a time, or can they queue up multiple
  pending requests? (Modeled here: **one at a time** — matching a driver
  immediately flips them to unavailable.)
- Does pricing vary by demand (surge), or is it a flat rate + per-distance
  charge? (Modeled here: both — a `StandardFareStrategy` and a
  `SurgePricingFareStrategy` that wraps any base strategy with a
  multiplier.)
- What happens if a ride is cancelled mid-trip vs. before pickup? (Modeled
  here: cancellation is only meaningful while `ONGOING` — the ride is
  cancelled and the driver freed; no distinction between "cancelled before
  pickup" and "cancelled after pickup" is modeled — see Known Gaps.)
- Can a rider request a specific driver, or is matching always automatic?
  (Modeled here: always automatic, via a pluggable `MatchingStrategy`.)

## Class design

```
model/
  Location     x, y (flat-plane coordinates); distanceTo() via Euclidean distance
  Driver       id, name, mutable Location, mutable available flag
  Rider        id, name
  RideStatus   enum: REQUESTED, ONGOING, COMPLETED, CANCELLED
  Ride         id, rider, driver, pickup, dropoff, mutable status, mutable fare

service/
  DriverRegistry   + InMemoryDriverRegistry   register/locate/toggle
                                                availability, list available drivers
  RideService      + InMemoryRideService      requestRide (matches + locks a
                                                driver), completeRide (prices
                                                + frees the driver), cancelRide,
                                                list a rider's rides; depends
                                                on DriverRegistry;
                                                NoDriverAvailableException /
                                                InvalidRideStateException

strategy/
  MatchingStrategy   + NearestDriverStrategy
  FareStrategy       + StandardFareStrategy / SurgePricingFareStrategy
                        (SurgePricingFareStrategy wraps another FareStrategy
                        and multiplies its result -- Decorator-shaped)

driver/
  RideSharingDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `MatchingStrategy` (nearest driver), `FareStrategy` (standard/surge) | Both "who do we match" and "what do we charge" are the two headline design questions in this problem — isolating each behind a one-method interface keeps `RideService` ignorant of the actual algorithms. |
| **Decorator (via composition, not inheritance)** | `SurgePricingFareStrategy` wraps a `FareStrategy` and multiplies its result | Surge pricing is a *modifier* on top of any base fare calculation, not a separate pricing scheme — wrapping instead of reimplementing means it automatically works with `StandardFareStrategy` or any future base strategy without duplicating the distance math. |
| **Repository-ish interface + impl** | `DriverRegistry`/`InMemoryDriverRegistry`, `RideService`/`InMemoryRideService` | Same DIP shape as the rest of this repo's LLDs. |
| **Constructor injection** | `InMemoryRideService(DriverRegistry)` | `RideService` doesn't reach for a global driver pool — it's handed one, so a test could substitute a registry with fixed, predictable driver positions. |

## SOLID mapping

- **SRP** — `Driver`/`Rider` are near-pure data; `DriverRegistry` only
  tracks driver location/availability; `RideService` only orchestrates
  the ride lifecycle; matching and pricing policy live only in their
  respective strategy impls.
- **OCP** — a new matching policy (e.g. "prefer drivers with a higher
  rating, tie-broken by distance") or a new fare modifier (e.g. a
  promo-discount strategy, composable the same way as surge pricing) can
  be added without touching `RideService`.
- **LSP** — every `MatchingStrategy` takes `(List<Driver>, Location)` and
  returns `Optional<Driver>`; every `FareStrategy` takes `(Location,
  Location)` and returns a `double` — `InMemoryRideService` treats every
  implementation of each interchangeably.
- **ISP** — `DriverRegistry` and `RideService` each expose a handful of
  narrow, single-purpose methods instead of one catch-all
  `handleDriverEvent(...)`/`handleRideEvent(...)`.
- **DIP** — `InMemoryRideService` depends on `DriverRegistry` (interface),
  not `InMemoryDriverRegistry` (impl); matching/fare algorithms are
  injected per call, not hardcoded.

## Core algorithms

### Matching (`NearestDriverStrategy`)

`O(available drivers)` linear scan for the minimum `distanceTo(pickup)` —
straightforward and sufficient for a coding-round-sized driver pool; see
the follow-ups for how this changes at real scale.

### Ride lifecycle (`InMemoryRideService`)

1. `requestRide` — matches via the injected `MatchingStrategy` (a
   lock-free read-only scan), then attempts `Driver.tryClaim()` — an
   atomic check-and-flip-to-unavailable on that specific driver. If
   another request claimed the same driver first, `requestRide` re-scans
   and retries against fresh availability rather than failing outright
   (see the Concurrency section below).
2. `completeRide` — atomically transitions the ride from `ONGOING` to
   `COMPLETED` via `Ride.tryTransition`, prices it via the injected
   `FareStrategy`, and frees the driver **at the dropoff location** — so
   the next match considers the driver's new position, not their stale
   pickup-time location.
3. `cancelRide` — atomically transitions `ONGOING` → `CANCELLED`; frees
   the driver without charging a fare.

### Surge pricing composition

`SurgePricingFareStrategy.calculateFare` delegates to a wrapped
`FareStrategy` and multiplies the result — so `new
SurgePricingFareStrategy(new StandardFareStrategy(), 1.5)` is "standard
pricing, 1.5x," and swapping in a different base strategy later doesn't
require touching the surge logic at all.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/ridesharing -name "*.java")

java -cp out ridesharing.driver.RideSharingDriver
java -cp out ridesharing.interview.SimpleRideSharingInterview
```

`RideSharingDriver` demonstrates: 3 drivers at different positions →
request a ride (matches the nearest driver) → request a second ride at
the same pickup (the first match is now busy, so the next-nearest is
picked) → complete the first ride with surge pricing applied → confirm
the driver becomes available again at the dropoff location → request and
complete a third ride at standard pricing.

`SimpleRideSharingInterview` is the same golden path (match → match again
→ fail when no drivers are left → complete with a fare) in one file, with
no interfaces or surge pricing — useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"Why Euclidean distance instead of real road distance?"** — Honest
  trade-off: Euclidean distance is a stand-in for what a real system
  would get from a routing/maps API (which accounts for roads, traffic,
  and ETA, not straight-line distance) — flagged explicitly here rather
  than presented as production-accurate.
- **"How do you prevent two riders from matching the same driver at the
  same time?"** — `Driver.tryClaim()` is an atomic check-and-claim on
  that specific driver; `requestRide` treats a failed claim as "someone
  beat me to it" and re-matches rather than failing the whole request —
  see the Concurrency section below for why this is a retry loop, not a
  bigger lock.
- **"How would you scale driver matching to a whole city with thousands
  of drivers?"** — The current linear scan over all available drivers is
  fine at demo scale; a real system would geo-index drivers (e.g. a grid
  or geohash bucket by location) so matching only scans drivers near the
  pickup point, not the entire fleet.
- **"What if the matched driver rejects the ride?"** — Not modeled;
  `requestRide` treats a match as final. A real flow would add a
  `PENDING_DRIVER_ACCEPTANCE` step with a timeout, falling back to the
  next-best match (via the same `MatchingStrategy`, called again
  excluding the rejecting driver) if the driver doesn't accept in time.
- **"How is surge pricing actually triggered (not just applied
  manually)?"** — In this design, the caller decides which `FareStrategy`
  to pass in; a real system would compute a surge multiplier from
  live supply/demand (e.g. ratio of pending ride requests to available
  drivers in a zone) and have `RideService` select the strategy itself
  rather than accepting it as a parameter — worth naming as the next
  design iteration if pressed.
- **"Concurrent completion and cancellation of the same ride?"** —
  `Ride.tryTransition` is a compare-and-set on the status field: both
  `completeRide` and `cancelRide` go through it, requiring the ride to
  still be `ONGOING` and only one of them can win if both fire for the
  same ride at once.

## Concurrency & thread-safety

- **Optimistic retry for driver matching** (`requestRide`) —
  `MatchingStrategy.selectDriver` is a lock-free scan over
  `driverRegistry.getAvailableDrivers()`, so its result is only a
  candidate; `Driver.tryClaim()` is the atomic primitive that actually
  claims one (check-and-flip-to-unavailable under that driver's own
  monitor). If the claim loses a race, `requestRide` re-scans and retries
  up to `MAX_MATCH_ATTEMPTS` — the same shape as `ParkingLot.parkVehicle`
  and the Movie Booking LLD's `bookSeats`. Matching in one region never
  blocks matching in another, since no lock spans the whole registry.
- **`Ride.tryTransition`** — a compare-and-set for ride status, closing
  the completion-vs-cancellation race called out above.
- **`ConcurrentHashMap` for `rides` and the driver registry** — both are
  read (ride history, availability listing) and written (new rides,
  claims) from multiple call paths concurrently.
- **Bounded retry is probabilistic, not absolute** — same caveat as
  `ParkingLot`/Movie Booking: a request could in principle exhaust
  `MAX_MATCH_ATTEMPTS` while a driver is still available elsewhere,
  trading a small false-negative rate for bounded worst-case latency.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Optimistic retry over a whole-service lock for `requestRide` | `synchronized` on `InMemoryRideService.requestRide` | A whole-service lock serializes every ride request across the entire fleet; per-driver claims plus retry scope contention to only the driver two requests actually collided on. |
| Euclidean distance on a flat `Location` | Haversine formula on real lat/lon | Keeps the matching/fare math trivial to read and test in an interview; the trade-off is called out explicitly rather than hidden, since interviewers will ask about it. |
| Surge pricing as a wrapper (`SurgePricingFareStrategy` holds a `FareStrategy`) | A `SURGE` flag + multiplier field bolted onto `StandardFareStrategy` | Composition keeps each strategy single-purpose and lets surge pricing apply to *any* future base strategy without modification — a direct application of OCP. |
| Driver freed at the ride's dropoff location, not left at pickup | Leave the driver's location unchanged after a ride | Matching the very next rider correctly requires the driver's *current* (post-ride) position — leaving it stale would produce wrong "nearest driver" results immediately after every ride. |
| One ride per driver at a time (immediate unavailability on match) | Let a driver queue multiple accepted rides | Matches how most ride-share products work in practice, and avoids modeling driver routing/scheduling across multiple concurrent passengers, which is out of scope for this problem's depth. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — driver/ride state resets on JVM exit.
- Straight-line distance, not real road distance/ETA.
- No driver-acceptance step (a match is assumed to be accepted
  immediately).
- No geo-indexing — matching scans every available driver linearly.
- No payment processing.
- No rating system for drivers or riders.
- No demand-based automatic surge computation (the multiplier is supplied
  by the caller, not derived from live supply/demand).
