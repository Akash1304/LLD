# Parking Lot LLD

A multi-floor parking lot: pluggable spot-selection and fee strategies,
vehicle-size-aware spot fitting, ticket issuance, and fee calculation on
exit. Two implementations live here:

- **Full version** (`ParkingLot`, `ParkingLotDriver`, `entities`, `strategy`,
  `vehicle` packages) — what you'd build across the full 60-minute coding
  window.
- **Compact version** (`interview/SimpleParkingLotInterview.java`) — a
  single ~130-line file covering best-fit spot assignment, parking,
  unparking, and hourly fee calculation, for when you only have 20-30
  minutes or want a warm-up before the full design.

## Problem statement

> Design a parking lot with multiple floors and spot sizes, that can park
> and unpark vehicles of different sizes, pick a spot according to some
> policy, and charge a fee based on how long the vehicle stayed.

## Clarifying questions to ask in the first 10 minutes

- How many vehicle sizes / spot sizes? (Modeled here: `SMALL`, `MEDIUM`,
  `LARGE`, one enum shared by both vehicles and spots.)
- Can a smaller vehicle use a larger spot? (Yes — a `MEDIUM` vehicle fits a
  `MEDIUM` or `LARGE` spot; see `ParkingSpot.canFitVehicle`.)
- Is the lot a singleton (one physical lot per process) or do we need
  multiple independent lots (e.g. multi-location)? (Modeled here as a
  **singleton** — flagged as a trade-off below since it doesn't generalize
  to multiple lots without rework.)
- What's the spot-picking policy — nearest entrance, best fit, or
  farthest first (e.g. to spread wear, or keep spots near the entrance free
  for quick errands)? (All three implemented as swappable strategies.)
- Flat fee or size-based tiered fee? Partial-hour rounding? (Both fee
  strategies implemented; partial hours always round **up**.)
- Do we need reservations, payment processing, or just spot
  tracking + fee calculation? (Scope here: spot tracking + fee calculation
  only — see Known Gaps.)

## Class design

```
ParkingLot (singleton)        floors: List<ParkingFloor>, activeTickets: Map<license, ParkingTicket>,
                               feeStrategy, parkingStrategy (both swappable via setters)

entities/
  ParkingFloor    floorNumber, Map<spotId, ParkingSpot>; findAvailableSpot(), displayAvailability()
  ParkingSpot     spotId, size, occupied flag, current vehicle; canFitVehicle(), park/unpark
  ParkingTicket   UUID ticketId, vehicle, spot, entry/exit timestamps (immutable except exit time)

strategy/parking/
  ParkingStrategy       + BestFitStrategy / NearestFirstStrategy / FarthestFirstStrategy

strategy/fee/
  FeeStrategy           + FlatRateFeeStrategy / VehicleBasedFeeStrategy

vehicle/
  Vehicle (abstract)    licenseNumber, VehicleSize
  Bike (SMALL) / Car (MEDIUM) / Truck (LARGE)
  VehicleType           enum: BIKE, CAR, TRUCK (what the gate actually reads)

factory/
  VehicleFactory        create(VehicleType, license) -> Vehicle; the only
                          place concrete Vehicle classes are named

observer/
  ParkingEventListener  onVehicleParked / onVehicleUnparked
  DisplayBoard          one concrete observer: the entrance board, refreshed
                          on every event; ParkingLot.addListener() subscribes

ParkingLotDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Singleton** | `ParkingLot.getInstance()` (double-checked locking) | One physical lot instance shared across the process — the classic textbook use case, though see the trade-off note below. |
| **Strategy** | `ParkingStrategy` (`BestFit`/`NearestFirst`/`FarthestFirst`), `FeeStrategy` (`FlatRate`/`VehicleBased`) | Spot-selection policy and pricing policy both vary independently of the rest of the lot; swappable via `setParkingStrategy`/`setFeeStrategy` without touching `ParkingLot`'s core logic. |
| **Facade** | `ParkingLot` | Callers only see `parkVehicle`/`unparkVehicle`; floors, tickets, and strategy internals are hidden behind it. |
| **Factory** | `VehicleFactory.create(VehicleType, license)` | The entry gate reads a type off a ticket machine or plate scanner — an enum, not a class. The factory is the one place `type -> Vehicle` lives; the gate, the driver, and any API layer never `new Car(...)`. Adding an `ELECTRIC_CAR` that needs a charging spot is one new case plus one subclass. **Why not let callers construct vehicles:** every caller then duplicates the type-switch, and a new vehicle type means touching all of them. |
| **Observer** | `ParkingEventListener` + `ParkingLot.addListener`; `DisplayBoard` subscribes | The classic follow-up: "the display board at the entrance must update when a spot frees." Before, the driver called `floor.displayAvailability()` by hand after every park — forget once and the board is stale. Now the lot publishes after a park/unpark commits (outside the spot's monitor) and the board, a "lot full" sign, a revenue counter can each subscribe without `ParkingLot` knowing them. |
| **Template-ish size matching** | `ParkingSpot.canFitVehicle` (switch on vehicle size) | Centralizes the "which spot sizes fit which vehicle sizes" rule in one place instead of scattering it across strategies. |

## SOLID mapping

- **SRP** — `ParkingSpot` only tracks occupancy; `ParkingTicket` only holds
  entry/exit + refs; fee math lives only in `FeeStrategy` impls; spot
  selection lives only in `ParkingStrategy` impls.
- **OCP** — add a new pricing model (e.g. `WeekendSurchargeFeeStrategy`) or
  a new spot-selection policy (e.g. `RandomAvailableStrategy`) without
  touching `ParkingLot`.
- **LSP** — every `ParkingStrategy` returns `Optional<ParkingSpot>` and
  every `FeeStrategy` returns a `double`; `ParkingLot` treats all
  implementations identically.
- **ISP** — `FeeStrategy` and `ParkingStrategy` are each a single-method
  interface; no implementation is forced to support behavior it doesn't
  need.
- **DIP** — `ParkingLot` holds `FeeStrategy`/`ParkingStrategy` as
  interface-typed fields, injected via setters, not constructed internally.

## Core algorithms

### Spot fitting (`ParkingSpot.canFitVehicle`)

`SMALL` vehicle → only `SMALL` spots. `MEDIUM` vehicle → `MEDIUM` or
`LARGE` spots. `LARGE` vehicle → only `LARGE` spots. (Note: this is
hand-written per case, *not* `vehicle.size.ordinal() <= spot.size.ordinal()`
— except `parkVehicle` itself *does* use the ordinal comparison as a second,
redundant guard. Worth pointing out in an interview: two different
techniques for the same rule living in two different methods is a small
consistency smell, not a bug, since both currently agree.)

### Spot selection strategies

- **BestFitStrategy** — scans *all* floors, picks the smallest spot that
  still fits (minimizes wasted capacity — a small vehicle won't hog a large
  spot).
- **NearestFirstStrategy** — first fitting spot in floor order (greedy,
  cheapest to compute, favors low floors).
- **FarthestFirstStrategy** — reverses floor order first, then greedy
  (e.g. to keep spots near the entrance open for quick turnover).

### Fee calculation

`hours = ceil((exitTs - entryTs) / 1hr)` (implemented as integer-divide
then `+1`, so any partial hour — including a few seconds — rounds up to a
full hour). `FlatRateFeeStrategy` charges a fixed rate regardless of size;
`VehicleBasedFeeStrategy` looks up a per-size hourly rate from an immutable
`Map.of(...)`.

## Concurrency & thread-safety

This LLD is written to survive concurrent `parkVehicle`/`unparkVehicle`
calls from multiple threads correctly, not just look correct in a
single-threaded demo — verified with an ad hoc stress test (200 threads
racing over 20 spots via a 32-thread pool, repeated runs) that confirms
exactly as many vehicles park as there are spots, with zero double-bookings.

- **Singleton via double-checked locking** (`ParkingLot.getInstance`) —
  `instance` is `volatile` so a thread that observes the non-null fast
  path is guaranteed to see a *fully constructed* `ParkingLot`, not one
  whose constructor writes haven't been published yet. The `synchronized`
  block is only entered on the (rare) first call; every call after that
  is lock-free.
- **Atomic claim on `ParkingSpot`** (`tryPark`) — `canFitVehicle` (the
  check) and marking the spot occupied (the act) happen under the same
  monitor, so "is this spot free" and "claim it" can never be split by
  another thread the way a separate `isOccupied()`-then-`parkVehicle()`
  pair could be split.
- **Optimistic retry, not a lot-wide lock** (`ParkingLot.parkVehicle`) —
  `ParkingStrategy.findSpot` is a lock-free, best-effort scan across
  floors backed by `ConcurrentHashMap`s, so the spot it returns is only a
  *candidate*. Rather than wrapping the whole find-then-park sequence in
  one big lock (which would serialize every parking attempt lot-wide,
  even for unrelated floors), the loop re-scans and tries the next
  candidate whenever `tryPark` loses the race on a specific spot.
  Contention is scoped to individual `ParkingSpot` monitors, which is why
  the stress test above shows no throughput collapse even at 200 racing
  threads.
- **Bounded retry is a real trade-off, not a free lunch** — the retry
  loop caps at `MAX_PARK_ATTEMPTS` (5). Under this repo's stress test
  that was always enough, but it's a probabilistic guarantee, not a
  mathematical one: a thread could in principle lose the race 5 times in
  a row while a spot is still free elsewhere, and get told "no spot
  available" when one exists. The honest fix is either a higher/adaptive
  cap, or accepting a small false-negative rate in exchange for bounded
  worst-case latency — worth naming explicitly if an interviewer pushes
  on "what if retries run out."
- **`volatile` on swappable strategies** (`feeStrategy`, `parkingStrategy`)
  — without `volatile`, a strategy swapped in by one thread (e.g. an
  admin action) isn't guaranteed to become visible to `parkVehicle` calls
  running on other threads promptly; `volatile` gives that visibility
  without needing a full lock just to read a reference.
- **`CopyOnWriteArrayList` for `floors`** — floors are added at setup
  time and read very frequently (every `findSpot` scan) but essentially
  never mutated once the lot is running; `CopyOnWriteArrayList` makes
  reads lock-free and safe under concurrent iteration, paying the
  (rarely incurred) copy cost only on the infrequent `addFloor` call.

**What's still coarse-grained on purpose:** `unparkVehicle` relies on
`ConcurrentHashMap.remove` being atomic — only one thread can ever
successfully remove a given license plate's ticket, so everything after
that (`setExitTimestamp`, `unparkVehicle`, fee calculation) safely
operates on an object now exclusively owned by that thread, with no
additional locking needed.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/parkinglot -name "*.java")

java -cp out parkinglot.ParkingLotDriver
java -cp out parkinglot.interview.SimpleParkingLotInterview
```

`ParkingLotDriver` demonstrates: build a 2-floor lot (floor 1: one `SMALL`,
one `MEDIUM`, one `LARGE`; floor 2: two `MEDIUM`) → set
`VehicleBasedFeeStrategy` → park a bike/car/truck (best-fit picks the
tightest matching spot) → show updated availability → park a second car
(overflows to floor 2 since floor 1 has no `MEDIUM` left) → attempt a
second bike (fails — no `SMALL` spots left) → unpark the first car and
print the calculated fee → show final availability.

`SimpleParkingLotInterview` is the same golden path (park → overflow to
next floor → fail on no matching spot → unpark with fee) in one file, with
no interfaces, singleton, or thread-safety — useful as a live-coding
warm-up, and a good prompt for "now make this thread-safe" as a follow-up
exercise pointing at the full version's `tryPark`/optimistic-retry design.

## Follow-up questions to expect (and how this design answers them)

- **"Why singleton — what if we have multiple parking lots (different
  locations)?"** — Honest answer: `ParkingLot.getInstance()` doesn't
  generalize to multiple lots as written; you'd drop the singleton and let
  a `ParkingLotManager` hold `Map<lotId, ParkingLot>`, constructing each
  `ParkingLot` normally. Singleton was chosen here purely because the
  problem statement implies one lot per process — good to flag this
  limitation proactively rather than waiting to be asked.
- **"How do you prevent two threads from grabbing the same spot?"** —
  Solved via `ParkingSpot.tryPark`, an atomic check-and-claim under the
  spot's own monitor, plus an optimistic-retry loop in
  `ParkingLot.parkVehicle` that re-scans if a candidate spot was claimed
  by someone else first — see the Concurrency section above for the full
  reasoning and a stress-test result.
- **"How would you add reservations?"** — Add a `Reserved` state to
  `ParkingSpot` distinct from `Occupied`, and a `reserve(spotId,
  until)`/`releaseReservation` API on `ParkingFloor`; `findAvailableSpot`
  would need to exclude reserved-but-unoccupied spots.
- **"How would you support EV charging spots or handicap spots?"** —
  Extend `VehicleSize`-based fitting to a more general
  `SpotFeature`/`SpotType` tag set on `ParkingSpot`, and have
  `ParkingStrategy` accept a required-feature filter alongside vehicle
  size.
- **"What about payment failures / disputes?"** — Currently
  `unparkVehicle` calculates and returns a fee but never charges anything
  or persists a receipt; you'd insert a `PaymentService` call between fee
  calculation and ticket removal, and only free the spot after payment
  succeeds (or free it eagerly and handle payment async, depending on
  requirements).
- **"How does this scale to a lot with 10,000 spots?"** — `findSpot`
  strategies are all `O(spots)` linear scans per floor. At scale you'd
  index spots by size (e.g. `Map<VehicleSize, Deque<ParkingSpot>>` per
  floor) so BestFit/NearestFirst become near O(1) pops instead of stream
  filters.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| `ParkingLot` as singleton | Plain instance passed around / DI container | Matches the "one lot" scope of the problem and is the pattern interviewers usually expect you to name explicitly; documented above as a limitation if the scope grows. |
| `ConcurrentHashMap` for spots/tickets | Plain `HashMap` + external locking | Cheap thread-safety for individual map operations; combined with `ParkingSpot.tryPark`'s atomic claim and the optimistic-retry loop, this closes the find-then-reserve race entirely — see the Concurrency section above. |
| Optimistic retry over a lot-wide lock for `parkVehicle` | One `synchronized` block wrapping the whole find-then-park sequence | A lot-wide lock would serialize *every* parking attempt across the entire lot, even onto unrelated floors; per-spot locking plus retry keeps contention scoped to the specific spot two threads actually collided on. |
| `VehicleSize` enum shared by vehicles and spots, compared by identity/switch | Numeric capacity fields | An enum keeps the size hierarchy explicit and exhaustive (compiler warns on missing switch cases) versus magic numbers. |
| Fee always rounds partial hours up | Round to nearest / prorate per minute | Standard real-world parking billing behavior (matches how most physical garages charge). |
| `ParkingTicket.ticketId` via `UUID.randomUUID()` | Sequential `IdGenerator` (used in the calendar LLD) | No coordination needed across floors/threads to hand out unique ticket IDs; sequential IDs would require a shared counter, adding a contention point for no real benefit here. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — lot state resets on JVM exit.
- No payment processing — fee is calculated but never charged.
- No reservation system, no VIP/handicap/EV spot types.
- No revenue reporting or occupancy metrics.
- No history/audit trail — once a ticket is removed from `activeTickets` on
  exit, there's no record it ever existed.
- No re-entry detection — the same vehicle can be "parked" twice with no
  check that it's already inside.
- No level-based or time-of-day pricing (surge pricing, off-peak
  discounts).
