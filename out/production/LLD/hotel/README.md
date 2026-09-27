# Hotel Management System LLD

Room inventory, date-range-aware booking with no overlap, pluggable
pricing, and a check-in/check-out lifecycle. Two implementations live
here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleHotelInterview.java`) — a single
  ~100-line file covering overlap-checked booking, check-in/out, and a
  flat nightly rate, for when you only have 15-20 minutes or want a
  warm-up before the full design.

## Problem statement

> Design a hotel booking system: guests book a room of a given type for a
> date range (no double-booking the same room for overlapping dates),
> check in and check out, and pricing can vary (e.g. by season).

## Clarifying questions to ask in the first 10 minutes

- Book a specific room, or "book me a DOUBLE for these dates" and let the
  system pick any available one of that type? (Modeled here: **the
  latter** — `bookRoom` takes a `RoomType`, not a specific `Room`; picking
  a specific room isn't in scope but would be a one-line variant.)
- Does a reservation touching another's checkout date conflict (guest A
  checks out the same day guest B checks in)? (Modeled here: **no** —
  half-open date ranges `[checkIn, checkOut)`, same convention as the
  Calendar LLD, so same-day turnover is allowed.)
- Is pricing flat per room type, or does it vary by season/demand?
  (Modeled here: pluggable `PricingStrategy`, with a holiday-season
  surcharge as the second implementation.)
- Can a reservation be cancelled after check-in, or only before?
  (Modeled here: **only while `BOOKED`** — once checked in, the guest is
  physically there; cancellation no longer makes sense. Checkout is the
  correct transition after that point.)
- Overbooking tolerance (airlines/hotels sometimes intentionally
  overbook)? (Not modeled — every booking is strictly against real
  availability; see Known Gaps.)

## Class design

```
model/
  RoomType             enum: SINGLE/DOUBLE/SUITE, each carrying a base nightly rate
  Room                 id, RoomType, floor
  Guest                id, name
  ReservationStatus    enum: BOOKED, CHECKED_IN, CHECKED_OUT, CANCELLED
  Reservation          id, guest, room, checkInDate, checkOutDate, totalPrice,
                        mutable status; overlaps() does half-open date-range overlap

service/
  RoomInventoryService   + InMemoryRoomInventoryService   add rooms, list by
                                                              type, get by id
  ReservationService     + InMemoryReservationService      book/checkIn/
                                                              checkOut/cancel;
                                                              depends on
                                                              RoomInventoryService;
                                                              NoRoomAvailableException /
                                                              InvalidReservationStateException

strategy/
  PricingStrategy   + StandardPricingStrategy / SeasonalPricingStrategy

driver/
  HotelDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Builder** | `Reservation.builder()` | Two adjacent `LocalDate` args (checkIn, checkOut) are the classic transposition bug; naming them and validating `checkIn < checkOut` in `build()` means a reservation with an inverted stay can never exist — the overlap check downstream can then assume well-formed ranges. |
| **Strategy** | `PricingStrategy` (`Standard`/`Seasonal`) | Nightly-rate policy is exactly the kind of rule an interviewer will ask you to extend live ("now add a weekend surcharge") — isolating it keeps that a drop-in swap, same shape as the Parking Lot and Bookstore LLDs. |
| **Half-open interval overlap, reused from the Calendar LLD's convention** | `Reservation.overlaps` | Same `start < otherEnd && otherStart < end` shape as `Event.overlaps` in the Calendar LLD — one battle-tested overlap rule applied consistently across date-range problems in this repo. |
| **Explicit state machine via enum + guarded transitions** | `ReservationStatus` + `checkIn`/`checkOut`/`cancelReservation` | Each transition method validates the *current* status before mutating it (e.g. can't check out a reservation that was never checked in), so `Reservation` can't drift into an invalid lifecycle state. |
| **Repository-ish interface + impl** | `RoomInventoryService`/`InMemoryRoomInventoryService`, `ReservationService`/`InMemoryReservationService` | Same DIP shape as the rest of this repo's LLDs. |

## SOLID mapping

- **SRP** — `Room`/`Guest` are pure data; `RoomInventoryService` only
  manages the room catalog; `ReservationService` only orchestrates
  booking/lifecycle; nightly-rate math lives only in `PricingStrategy`
  impls.
- **OCP** — a new pricing model (e.g. `WeekendSurchargePricingStrategy`)
  or a new room type can be added without touching `ReservationService`.
- **LSP** — every `PricingStrategy` takes `(RoomType, LocalDate,
  LocalDate)` and returns a `double`; `InMemoryReservationService` treats
  both implementations identically.
- **ISP** — `ReservationService` exposes five narrow lifecycle operations
  instead of one catch-all `processReservation(...)`.
- **DIP** — `InMemoryReservationService` depends on `RoomInventoryService`
  (interface), not `InMemoryRoomInventoryService` (impl).

## Core algorithms

### Room assignment (`InMemoryReservationService.bookRoom`)

1. Fetch every `Room` of the requested `RoomType` from
   `RoomInventoryService` — `O(rooms of that type)`, not `O(all rooms)`.
2. For each candidate room, check every *active* (`BOOKED`/`CHECKED_IN`)
   reservation already on that room for a date overlap; the first room
   with none wins.
3. The candidate check and the reservation commit happen inside a lock
   scoped to that *specific room* (`lockFor(room.getId())`), so two
   guests can't both "win" the same room for overlapping dates — while a
   booking attempt for a *different* room proceeds without waiting. See
   the Concurrency section below.

### Overlap check (`Reservation.overlaps`)

Half-open semantics: `existing.checkIn < candidate.checkOut &&
candidate.checkIn < existing.checkOut`. This means a guest checking out on
March 1st and another checking in on March 1st do **not** conflict —
same-day room turnover is standard hotel behavior.

### Seasonal pricing (`SeasonalPricingStrategy`)

Walks each individual night in the stay (not just total nights × a flat
rate) and applies a 25% surcharge to any night falling in December or
January — so a stay spanning New Year's is priced night-by-night rather
than an all-or-nothing surcharge on the whole reservation.

## Concurrency & thread-safety

- **Per-room lock striping for `bookRoom`** — only reservations on the
  *same room* can date-overlap, so the check-then-act (scan this room's
  active reservations for a conflict, then commit a new one) synchronizes
  on a lock keyed by `roomId` (lazily created via `computeIfAbsent`), not
  a lock across the whole hotel. Two guests racing for room 101 are
  correctly serialized; a booking for room 102 runs concurrently,
  untouched.
- **`Reservation.tryTransition`** — a compare-and-set for the status
  field: `checkIn`/`checkOut`/`cancelReservation` all go through it,
  checking the *expected* current status and only committing the new one
  if it still matches, under one monitor. This closes races like a
  check-in racing a cancellation on the same reservation, or a
  double-submitted check-in request succeeding twice.
- **`ConcurrentHashMap`/`CopyOnWriteArrayList` throughout** —
  `reservations`, `activeByRoom`, and the per-room lock map are all
  concurrent-safe collections, since guest lookups, availability checks,
  and new bookings all happen from multiple call paths simultaneously.
- **What's still coarse-grained on purpose:** `bookRoom` iterates
  candidate rooms of the requested type *outside* any lock, only taking
  the per-room lock once it's checking a specific room — so the
  candidate scan itself is lock-free and can't block on rooms it isn't
  currently examining.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/hotel -name "*.java")

java -cp out hotel.driver.HotelDriver
java -cp out hotel.interview.SimpleHotelInterview
```

`HotelDriver` demonstrates: 2 `DOUBLE` rooms + 1 `SUITE` → book a `DOUBLE`
over New Year's with `SeasonalPricingStrategy` (surcharge applied) → book
a second overlapping `DOUBLE` (lands on the other room) → a third
overlapping `DOUBLE` booking fails (none left) → the first guest checks in
then checks out → a new, non-overlapping January booking succeeds and can
reuse the now-`CHECKED_OUT` room.

`SimpleHotelInterview` is the same golden path (overlap-checked booking →
overbooking failure → check-in/out → rebook) in one file, with no
interfaces or seasonal pricing — useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How would you let a guest book a specific room instead of any room
  of a type?"** — `bookRoom` already isolates "find a candidate room" (the
  loop over `getRoomsByType`) from "check it's free and reserve it" (the
  overlap check); a `bookSpecificRoom(guest, roomId, dates,
  pricingStrategy)` variant would skip straight to the overlap check for
  one room instead of iterating a type's candidates.
- **"Concurrent bookings for the last room on the same dates?"** —
  `bookRoom` synchronizes the find-then-reserve sequence on a lock scoped
  to the specific room being checked (see the Concurrency section below),
  so two threads racing for the last matching room can't both win it,
  while bookings against unrelated rooms never wait on each other.
- **"How do you handle overbooking (airlines/hotels sometimes
  intentionally overbook past capacity)?"** — Not modeled; this design
  always books strictly within real availability. Overbooking would mean
  relaxing the overlap check by a configurable percentage per room type
  and handling the "more reservations than rooms on a given night"
  case at check-in time (walk a guest to a comparable room or partner
  hotel) — a materially different, riskier design worth flagging as such.
- **"What about no-shows or late cancellations?"** — `cancelReservation`
  exists but has no time-based policy (e.g. "free cancellation up to 24h
  before check-in, otherwise a penalty charge"); that would be another
  natural `CancellationPolicy` strategy, mirroring `PricingStrategy`'s
  shape.
- **"How would you scale room availability lookups to thousands of
  rooms?"** — `isRoomFree` currently scans every active reservation
  *for a given room* (not the whole hotel), so it's already bounded by
  that room's booking history, not total inventory. At real scale you'd
  index reservations by `(roomId, dateRange)` in an interval tree or a
  DB with range queries rather than a linear scan per room, similar to
  the Calendar LLD's discussion of the same trade-off.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Book by `RoomType`, not a specific `Room` | Require the caller to name an exact room | Matches how hotel booking actually works from a guest's perspective ("I want a double," not "I want room 214") — simpler API, and the system still guarantees no double-booking underneath. |
| Per-room lock striping over a whole-service lock | `synchronized` on `InMemoryReservationService.bookRoom` | A whole-service lock serializes every booking in the hotel behind one monitor, even for unrelated rooms; per-room striping scopes contention to only the room two guests are actually racing over. |
| `LocalDate` for check-in/out | `Instant`/`ZonedDateTime` | Hotel stays are calendar-day granular (a "night," not a specific hour) — same reasoning as the Library LLD's use of `LocalDate`. |
| Reservation overlap reuses the Calendar LLD's half-open convention | A hotel-specific "same day is fine either way" rule bolted on separately | Consistency: every date/time-range problem in this repo uses the same half-open rule, so a reader who's seen one understands all of them immediately. |
| Cancellation only allowed while `BOOKED` | Allow cancelling a `CHECKED_IN` reservation too | Once a guest has physically checked in, "cancel" is semantically wrong — the correct next state is `CHECKED_OUT` (possibly early), not `CANCELLED`. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — inventory/reservations reset on JVM exit.
- No payment processing.
- No specific-room booking (only "any room of this type").
- No overbooking support.
- No cancellation policy / no-show handling (e.g. time-based penalties).
- No amenities/preferences matching beyond room type.
- Availability check is a linear scan per room's reservation history —
  fine at interview scale, not at real-hotel-chain scale (see follow-ups).
