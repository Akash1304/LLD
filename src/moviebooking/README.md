# Movie Ticket Booking System LLD

Movies, screens, and shows, with seat-level booking, pluggable seat
selection, and cancellation. Two implementations live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleMovieBookingInterview.java`) — a
  single ~110-line file covering best-available (contiguous) seat booking
  and cancellation for one show, for when you only have 15-20 minutes or
  want a warm-up before the full design.

## Problem statement

> Design a movie ticket booking system: list shows for a movie, book N
> seats for a show (or specific seats), and support cancellation, with
> different seat tiers priced differently.

## Clarifying questions to ask in the first 10 minutes

- Book a specific set of seats, or "give me N seats" and let the system
  pick? (Modeled here: **both** — `bookSpecificSeats` for an exact seat
  list, `bookSeats(count, strategy)` for system-picked seats via a
  pluggable `SeatSelectionStrategy`.)
- If the system picks seats, should it try to keep a group together
  (contiguous, same row), or just grab any N free seats? (Modeled here:
  `BestAvailableSeatStrategy` tries contiguous-in-a-row first, falling
  back to `FirstAvailableSeatStrategy` if no row has enough room.)
- Are all seats priced the same, or do tiers (regular/premium) cost
  differently? (Modeled here: `SeatType` carries a price multiplier
  applied to the show's base price.)
- Is there a temporary "hold" while a user is mid-checkout, or is booking
  atomic and immediate? (Modeled here: **atomic and immediate** — no hold
  step; see Known Gaps for what a hold/expiry would add.)
- Can a cancelled seat be rebooked by someone else? (Modeled here: yes —
  `cancelBooking` releases the seats back to the show's available pool
  immediately.)

## Class design

```
model/
  Movie            id, title, durationMinutes
  Seat             id, row, number, SeatType
  SeatType         enum: REGULAR (1.0x), PREMIUM (1.5x) — price multiplier
  Screen           id, name, List<Seat> (the fixed physical layout)
  Show             id, movie, screen, startTime, basePrice, Set<bookedSeatId>;
                    reserveSeats()/releaseSeats() are synchronized
  Booking          id, show, customerId, seats, totalPrice, mutable BookingStatus
  BookingStatus    enum: CONFIRMED, CANCELLED

service/
  CatalogService   + InMemoryCatalogService   movies/screens/shows, find
                                                shows by movie title
  BookingService   + InMemoryBookingService   book by count+strategy, book
                                                specific seats, cancel, list
                                                a customer's bookings;
                                                NotEnoughSeatsException /
                                                SeatUnavailableException

strategy/
  SeatSelectionStrategy   + FirstAvailableSeatStrategy / BestAvailableSeatStrategy

driver/
  MovieBookingDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `SeatSelectionStrategy` (`FirstAvailable`/`BestAvailable`) | "How do we auto-pick seats" is a policy interviewers explicitly call out as a design question — isolating it lets `BookingService` stay ignorant of *how* seats are chosen. |
| **Fallback composition** | `BestAvailableSeatStrategy` delegates to `FirstAvailableSeatStrategy` when no row has a contiguous run | Reuses the simpler strategy instead of duplicating its scan logic — a small but real example of composing strategies rather than only picking one. |
| **Repository-ish interface + impl** | `CatalogService`/`InMemoryCatalogService`, `BookingService`/`InMemoryBookingService` | Same DIP shape as the rest of this repo's LLDs. |
| **Encapsulated invariant on the aggregate root** | `Show.reserveSeats`/`releaseSeats` (synchronized, all-or-nothing) | Seat booking state lives *inside* `Show`, not in `BookingService`, so the "no double-booking" invariant can't be violated by a caller forgetting to check first. |

## SOLID mapping

- **SRP** — `Show` only tracks which seats are booked; `Booking` only
  records what was booked and for how much; `CatalogService` only
  manages movies/screens/shows; seat-picking policy lives only in
  `SeatSelectionStrategy` impls.
- **OCP** — a new seat-selection policy (e.g. "prefer aisle seats") can be
  added without touching `BookingService`.
- **LSP** — every `SeatSelectionStrategy` takes `(Show, List<Seat>, int)`
  and returns `Optional<List<Seat>>`; `InMemoryBookingService` treats both
  implementations identically.
- **ISP** — `BookingService` exposes four narrow operations instead of
  one catch-all `processBookingRequest(...)`.
- **DIP** — `InMemoryBookingService` depends on `CatalogService`
  (interface), not `InMemoryCatalogService` (impl); the seat-selection
  algorithm is injected as a `SeatSelectionStrategy`, not hardcoded.

## Core algorithms

### Seat reservation (`Show.reserveSeats`)

`synchronized`, and checks **every** requested seat is unbooked before
booking **any** of them — an all-or-nothing reservation, so a
partially-successful (and therefore inconsistent) booking can never
happen even under concurrent calls.

### Best-available seat selection (`BestAvailableSeatStrategy`)

1. Group all seats by row, sorted by seat number within each row.
2. Scan each row for a contiguous run of `count` unbooked seats
   (resetting the run whenever a booked seat is hit); return the first
   row that has one.
3. If no row has enough contiguous space, fall back to
   `FirstAvailableSeatStrategy` (any `count` free seats, in seat-list
   order) so the booking still succeeds if physically possible — this is
   a UX trade-off (togetherness vs. best-effort), worth naming explicitly
   in an interview.

### Optimistic retry on selection vs. reservation

`InMemoryBookingService.bookSeats` calls `strategy.selectSeats` (a
read-only scan) and *then* `show.reserveSeats` (the actual booking) —
two steps, not one atomic operation, so the selected seats are only a
*candidate*. If two threads select overlapping seats concurrently,
`reserveSeats` (atomic and all-or-nothing under `Show`'s own monitor)
rejects the losing caller; rather than surfacing that as a booking
failure, `bookSeats` loops and re-selects with fresh availability, up to
`MAX_BOOKING_ATTEMPTS` — the same optimistic-retry shape used by
`ParkingLot.parkVehicle`. See the Concurrency section below for the full
reasoning.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/moviebooking -name "*.java")

java -cp out moviebooking.driver.MovieBookingDriver
java -cp out moviebooking.interview.SimpleMovieBookingInterview
```

`MovieBookingDriver` demonstrates: one movie, one 3-row/5-seat screen (row
1 premium, rows 2-3 regular) → look up shows by title → book 3 seats
together (lands in row 1) → book 3 more (spills into row 2) → attempt to
book 10 seats (fails, not enough left) → cancel the first booking → book 3
seats again and confirm the freed row-1 seats are reused.

`SimpleMovieBookingInterview` is the same golden path (contiguous booking
→ overbooking failure → cancel → rebook) in one file, with no interfaces,
pricing tiers, or catalog — useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How do you prevent double-booking under concurrent requests?"** —
  `Show.reserveSeats` is `synchronized` and checks-then-books all seats
  atomically within that lock, so two callers can never both succeed on
  an overlapping seat set. The remaining two-step select-then-reserve
  race in `BookingService` (see above) is closed with an optimistic
  retry loop rather than a bigger lock — booking one show never blocks
  booking a different show, which a coarser fix (e.g. one lock per
  `BookingService`) would cause.
- **"What about a temporary seat hold while the user is checking out
  (e.g. entering payment info)?"** — Not modeled; you'd add a
  `HELD`-with-expiry state between "available" and "booked", plus a
  background sweep (or lazy check on next access) to release holds that
  time out — this is the single biggest gap versus a production seat map.
- **"How would you scale seat maps to a multiplex with thousands of
  shows?"** — `Show.bookedSeatIds` as an in-memory `Set` is fine per show;
  at scale you'd shard by show (each show's seat map is independent, so
  this is embarrassingly parallel) and back it with a store that supports
  atomic conditional writes (e.g. a DB row per seat, or a distributed
  lock per show).
- **"How do you handle refunds on cancellation?"** — `cancelBooking`
  currently only releases seats; a `PaymentService.refund(booking)` call
  would slot in right before or after that release, symmetric to how
  `bookSeats` would call a payment charge before confirming.
- **"Dynamic pricing (surge for popular shows)?"** — `basePrice` is fixed
  per `Show`; you'd introduce a `PricingStrategy` (mirroring the pattern
  used in the Bookstore and Parking Lot LLDs) that factors in demand,
  time-to-showtime, etc., instead of a flat field.

## Concurrency & thread-safety

- **`Show.reserveSeats`/`releaseSeats`/`isBooked` are `synchronized`** —
  one monitor per show. `reserveSeats` checks every requested seat is
  free and books all of them atomically (all-or-nothing), so a partial
  reservation — and therefore a torn, inconsistent seat map — can never
  happen, even under heavy concurrent booking of the same show.
- **Optimistic retry, not a bigger lock, for select-then-reserve** —
  `bookSeats` re-runs `strategy.selectSeats` and retries
  `show.reserveSeats` up to `MAX_BOOKING_ATTEMPTS` (5) whenever a
  candidate seat was claimed by a concurrent booking first. This keeps
  `SeatSelectionStrategy` a pure, side-effect-free read (easy to test in
  isolation) while still closing the race — the same trade-off `ParkingLot`
  makes, and for the same reason: locking the *whole* selection+reservation
  sequence would serialize every booking for a show behind one operation,
  when only the specific contended seats need to be retried.
- **`Booking.tryCancel`** — atomic "cancel if still confirmed," so two
  concurrent cancellation requests for the same booking (e.g. a
  double-submitted click) can't both pass the check and both call
  `Show.releaseSeats`, which would double-release the same seats back
  into the available pool.
- **`ConcurrentHashMap` for `bookings`** — safe for concurrent
  `getBookingsForCustomer` reads alongside new bookings being created.
- **Bounded retry is a probabilistic guarantee, not absolute** — same
  caveat as `ParkingLot`: a caller could in principle exhaust
  `MAX_BOOKING_ATTEMPTS` while seats are still available elsewhere in the
  show, trading a small false-negative rate for bounded worst-case
  latency instead of retrying forever.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Seats booked via `Set<String>` on `Show`, not a field on `Seat` | A `booked` boolean directly on each `Seat` object | `Seat` describes the physical layout (shared across every show on that screen); booking state is *per-show*, so it belongs on `Show`, not on the reusable `Seat`. |
| `BestAvailableSeatStrategy` falls back to `FirstAvailableSeatStrategy` | Fail outright if no contiguous run exists | Prioritizes actually completing the booking (physically possible) over the softer "sit together" preference — a product decision worth naming rather than assuming. |
| Optimistic retry over one big lock for select-then-reserve | A single method/lock spanning selection and reservation together | Keeps `SeatSelectionStrategy` a pure, side-effect-free read and scopes contention to only the seats two bookings actually collided on, rather than serializing every booking attempt for a show. |
| Price computed from `SeatType` multiplier × show's base price | A fixed price stored per seat | One `basePrice` per show (which can vary show-to-show, e.g. weekend pricing) times a small, fixed set of tier multipliers is far less data to manage than per-seat pricing. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — catalog/shows/bookings reset on JVM exit.
- No temporary seat hold / checkout expiry window.
- Select-then-reserve isn't a single atomic operation (see follow-ups).
- No payment processing or refunds.
- No search by city/date, only by exact movie title.
- No show-level capacity/overbooking alerts or waitlists.
- No dynamic/surge pricing — a single flat `basePrice` per show.
