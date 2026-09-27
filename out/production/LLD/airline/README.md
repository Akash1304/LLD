# Airline Reservation System LLD

Flights with class-partitioned seat maps, pluggable seat assignment,
demand-based pricing, and cancellation. Two implementations live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleAirlineInterview.java`) — a
  single ~100-line file covering demand-based pricing and booking/
  cancellation for one seat class, for when you only have 15-20 minutes
  or want a warm-up before the full design.

## Problem statement

> Design an airline reservation system: search flights between two
> airports, book a seat of a given class (auto-assigned or with a
> preference like window seating), price the seat (which can vary with
> demand), and support cancellation.

## Clarifying questions to ask in the first 10 minutes

- Book any available seat in a class, or support seating preferences
  (window/aisle)? (Modeled here: **both** — a pluggable
  `SeatAssignmentStrategy`, with `FirstAvailableSeatAssignmentStrategy`
  and `WindowSeatPreferredStrategy`.)
- Is pricing flat per class, or does it change as the flight fills up
  (real airline dynamic pricing)? (Modeled here: **both** — a flat
  `FlatClassPricingStrategy` and a `DemandBasedPricingStrategy` that adds
  a surcharge proportional to how full that seat class already is.)
- Should price be locked in at the moment of booking (based on demand
  *then*), or recalculated at checkout? (Modeled here: **computed once,
  right before the seat is reserved** — see Core Algorithms for why the
  ordering matters.)
- Can a passenger book multiple seats on the same flight in one call, or
  one at a time? (Modeled here: **one seat per `bookSeat` call** — a
  multi-seat booking would be a thin loop over this, not a redesign.)
- What happens on cancellation — refund handling, cutoff windows? (Not
  modeled — see Known Gaps.)

## Class design

```
model/
  SeatClass       enum: ECONOMY (1.0x), BUSINESS (2.5x), FIRST (4.0x) -- price multiplier
  Seat            id, row, column, SeatClass; isWindow() (column A or F)
  Flight          id, flightNumber, origin, destination, departureTime,
                    List<Seat>, basePrice, Set<bookedSeatId>;
                    reserveSeat()/releaseSeat() are synchronized;
                    getBookedCountForClass() for demand pricing
  Passenger       passportNumber, name
  BookingStatus   enum: CONFIRMED, CANCELLED
  Booking         id, flight, passenger, seat, price, mutable status

strategy/
  SeatAssignmentStrategy   + FirstAvailableSeatAssignmentStrategy / WindowSeatPreferredStrategy
  PricingStrategy          + FlatClassPricingStrategy / DemandBasedPricingStrategy

service/
  FlightService    + InMemoryFlightService    add flights, search by
                                                 origin/destination, get by id
  BookingService   + InMemoryBookingService   bookSeat (assign + price +
                                                 reserve), cancelBooking, list
                                                 a passenger's bookings;
                                                 depends on FlightService;
                                                 NoSeatAvailableException

driver/
  AirlineDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Builder** | `Flight.builder()` | Seven positional args, four of them `String`s — swapping `origin`/`destination` or `id`/`flightNumber` compiles fine and is only caught at runtime. Named setters make the call site self-describing, and `build()` enforces the invariants (origin ≠ destination, non-empty seat map, positive base price) in exactly one place, so an invalid `Flight` can't be constructed. |
| **Strategy** | `SeatAssignmentStrategy` (first-available/window-preferred) and `PricingStrategy` (flat/demand-based) | "Which seat do we give them" and "what do we charge" are the two headline design questions the problem statement calls out explicitly — isolating both keeps `BookingService` ignorant of the actual algorithms, same shape as the Movie Booking LLD's seat/pricing split. |
| **Encapsulated invariant on the aggregate root** | `Flight.reserveSeat`/`releaseSeat` (synchronized) | Seat-booking state and occupancy counts live *inside* `Flight`, not in `BookingService` — the "no double-booking" invariant, and the occupancy numbers demand pricing depends on, can't drift out of sync with reality. |
| **Repository-ish interface + impl** | `FlightService`/`InMemoryFlightService`, `BookingService`/`InMemoryBookingService` | Same DIP shape as the rest of this repo's LLDs. |
| **Price computed from live occupancy, not cached** | `DemandBasedPricingStrategy.calculatePrice` reads `flight.getBookedCountForClass` at call time | The price for the *next* seat always reflects the *current* state of the cabin — no separate "current price" field to keep in sync as bookings/cancellations happen. |

## SOLID mapping

- **SRP** — `Flight` only tracks seats/occupancy; `Booking` only records
  what was booked and for how much; `FlightService` only manages the
  flight catalog; seat-picking and pricing policy live only in their
  respective strategy implementations.
- **OCP** — a new seat-assignment policy (e.g. "seat families together")
  or a new pricing model (e.g. time-to-departure surcharge) can be added
  without touching `BookingService`.
- **LSP** — every `SeatAssignmentStrategy` takes `(Flight, SeatClass)`
  and returns `Optional<Seat>`; every `PricingStrategy` takes `(Flight,
  Seat)` and returns a `double` — `InMemoryBookingService` treats every
  implementation of each interchangeably.
- **ISP** — `BookingService` exposes three narrow operations instead of
  one catch-all `processReservationRequest(...)`.
- **DIP** — `InMemoryBookingService` depends on `FlightService`
  (interface), not `InMemoryFlightService` (impl); both strategies are
  injected per call, not hardcoded.

## Core algorithms

### Book-then-price ordering (`InMemoryBookingService.bookSeat`)

1. Ask the `SeatAssignmentStrategy` to pick a seat (a read-only scan of
   the class's unbooked seats).
2. **Compute the price before reserving the seat** — `DemandBasedPricingStrategy`
   reads the *current* occupancy count, which doesn't yet include this
   booking. This ordering is deliberate: the price quoted for seat N+1
   reflects "N seats already booked," not "N+1," matching how a real
   pricing engine would quote before committing the sale.
3. Reserve the seat (`Flight.reserveSeat`, an atomic add to a `Set`) —
   if it was just taken by a concurrent caller between steps 1 and 3, the
   reservation fails and the whole booking is rejected rather than
   silently charging for a seat that turned out to be unavailable.

### Demand-based pricing (`DemandBasedPricingStrategy`)

```
occupancyRatio = bookedInClass / totalInClass
price = basePrice * seatClass.multiplier * (1 + occupancyRatio * MAX_SURCHARGE)
```

A linear ramp from the base class price (empty) up to a 50%-higher price
(that class fully booked) — simple and transparent, not a real airline's
proprietary revenue-management model (see Known Gaps).

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/airline -name "*.java")

java -cp out airline.driver.AirlineDriver
java -cp out airline.interview.SimpleAirlineInterview
```

`AirlineDriver` demonstrates: one flight with a 12-seat economy cabin and
a 2-seat business cabin → search flights by route → book a window economy
seat for Alice (flat base price, cabin still empty) → book 10 more economy
seats to drive up occupancy → book one more economy seat and observe the
price is now noticeably higher due to demand-based pricing → book a
business seat (independent occupancy/pricing from economy) → cancel
Alice's booking and confirm it shows as `CANCELLED` in her booking list.

`SimpleAirlineInterview` is the same demand-based pricing + booking +
cancellation logic for a single seat class, in one file with no
interfaces or seat preferences — useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"Why price before reserving, not after?"** — Pricing *after*
  reserving would mean the seat being booked counts toward its own
  demand calculation, inflating the quoted price by one seat's worth of
  occupancy for every booking — a subtle off-by-one that's worth naming
  proactively (see Core Algorithms).
- **"How do you prevent double-booking under concurrent requests for the
  last seat in a class?"** — `bookSeat` wraps assign-price-reserve for a
  given flight in `synchronized (flight)`, and `Flight.reserveSeat` is
  itself a single atomic `Set.add`. Two bookings for the *same* flight
  are fully serialized (so only one wins the last seat, and demand
  pricing never reads stale occupancy); two bookings for *different*
  flights never wait on each other. See the Concurrency section below.
- **"How would you support multi-city or connecting flights?"** — Not
  modeled; a `Trip` or `Itinerary` type would wrap an ordered list of
  `Flight` legs, and `BookingService` would need to book (or roll back)
  every leg together — a materially bigger design than a single-flight
  booking, worth flagging as such.
- **"How do real airlines actually do dynamic pricing?"** — Far more
  sophisticated than a linear occupancy ramp: time-to-departure, route
  popularity, competitor pricing, and historical demand curves all feed
  into a revenue-management model. `DemandBasedPricingStrategy` is
  explicitly a simplified stand-in, swappable for a more elaborate
  strategy without touching `BookingService`.
- **"What about refunds and cancellation cutoffs (e.g. no refund within
  24h of departure)?"** — Not modeled; `cancelBooking` unconditionally
  releases the seat with no time-based policy or refund calculation —
  this would be a `CancellationPolicy` strategy, mirroring the shape of
  `PricingStrategy`.
- **"Overbooking (airlines often intentionally oversell)?"** — Not
  modeled; this design always books strictly within physical seat
  availability, unlike real airline practice — worth naming as a
  deliberate scope simplification if asked.

## Concurrency & thread-safety

- **Per-flight lock spans the whole assign-price-reserve sequence**
  (`bookSeat`) — unlike the Parking Lot or Movie Booking LLDs (which use
  optimistic retry because their read scans span many independently-
  lockable resources), `SeatAssignmentStrategy.assignSeat` here is
  already scoped to *one* flight. So `bookSeat` synchronizes on that
  specific `Flight` instance for the entire operation: assign a seat,
  price it from current occupancy, and reserve it, all as one atomic
  unit. This is strictly necessary, not just convenient — demand pricing
  reading occupancy *between* two interleaved bookings on the same
  flight (rather than before or after both) would produce an
  inconsistent price for one of them.
- **A booking on flight A never blocks a booking on flight B** — the
  lock is scoped to the specific `Flight` object, not the whole
  `BookingService`, so unrelated flights' bookings run fully in parallel.
- **`Booking.tryCancel`** — atomic "cancel if still confirmed," closing
  the same double-cancellation race handled the same way in the Movie
  Booking LLD.
- **`ConcurrentHashMap` for `bookings` and the flight catalog** — safe
  for concurrent reads (booking history, flight search) alongside writes
  (new bookings, new flights).

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| One lock spanning assign-price-reserve, scoped to the flight | Optimistic retry, as in Parking Lot/Movie Booking | Those LLDs retry because their candidate search spans many independently-lockable resources (spots across floors, seats via a strategy that could be reused across shows); here the search is already confined to one flight, so locking that flight for the whole operation is both simpler and doesn't sacrifice any real parallelism (a different flight's bookings are unaffected either way). |
| Price computed before reservation, from current occupancy | Price computed after reservation | Avoids the seat-counts-itself off-by-one described above; keeps demand pricing an honest reflection of "how full was it when this sale happened." |
| Demand pricing scoped per seat class, not the whole flight | One demand curve across all classes combined | Matches real airline behavior — economy selling out doesn't make business class more expensive, and vice versa; `getBookedCountForClass` is scoped accordingly. |
| `WindowSeatPreferredStrategy` falls back to any available seat if no window is free | Fail the booking if no window seat is available | Prioritizes completing the booking (a seat, just not the preferred kind) over strictly honoring a soft preference — same UX trade-off as the Movie Booking LLD's `BestAvailableSeatStrategy` fallback. |
| One seat class multiplier + one flight base price, not per-seat pricing | Store a price per individual seat | A small, fixed set of class multipliers times one flight-level base price is far less data to manage than pricing every seat independently, while still supporting class-based and demand-based variation. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — flights/bookings reset on JVM exit.
- No multi-leg/connecting itineraries — one flight per booking.
- No overbooking support (always strictly within physical capacity).
- No cancellation policy (refund rules, time-based cutoffs).
- No payment processing.
- No seat-map visualization or adjacent-seat ("seat my family together")
  assignment logic.
- Demand pricing is a simple linear occupancy ramp, not a real
  revenue-management model.
