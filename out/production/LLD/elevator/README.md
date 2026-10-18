# Elevator System LLD

Follows the [Hello Interview elevator breakdown](https://www.hellointerview.com/learn/low-level-design/problem-breakdowns/elevator):
a controller dispatching hall calls across a small bank of elevators, each
running **direction-aware SCAN** in a discrete-time `step()` simulation.

- **Full version** (`model`, `controller`, `driver` packages).
- **Compact version** (`interview/SimpleElevatorInterview.java`) — the same
  design in one file, for live-coding warm-up.

## Requirements

**In scope**
- 3 elevators serving 10 floors (0–9), all starting at floor 0.
- A user on a floor requests an elevator with a direction (UP/DOWN) — a
  **hall call**.
- A passenger inside a car selects one or more floors — a **destination**.
- Time advances in discrete ticks via `step()`: each tick an elevator
  either moves one floor, stops at a floor, or changes direction.
- Invalid requests (floor out of range, a DESTINATION sent as a hall call)
  are rejected. A request for the floor the elevator is already serving is
  a no-op.

**Out of scope** — weight/capacity, door mechanics, emergency stops,
dynamic configuration, UI.

> Senior signal: ask up front whether this is a **simulation** (we own the
> clock, `step()`) or **real hardware** (events from sensors). This design
> is a simulation.

## Core entities

| Entity | Responsibility |
|---|---|
| `ElevatorController` | Owns the bank; validates and dispatches hall calls; ticks every elevator |
| `Elevator` | Its own floor, direction and pending stops; advances itself with SCAN |
| `Request` | `(floor, RequestType)` value object |
| `Direction` | `UP`, `DOWN`, `IDLE` |
| `RequestType` | `PICKUP_UP`, `PICKUP_DOWN`, `DESTINATION` |

## Class design

```
ElevatorController
  - elevators: List<Elevator>
  + requestElevator(floor, type) -> boolean   // hall calls only
  + step()                                     // ticks every elevator
  - selectBestElevator(floor, type)            // priority-tier dispatch

Elevator
  - currentFloor: int
  - direction: Direction
  - requests: Set<Request>
  + addRequest(request) -> boolean
  + step()
  + getCurrentFloor(), getDirection()

record Request(int floor, RequestType type)
```

Destinations don't go through the controller — the passenger presses them
on the car's own panel, so the driver calls `elevator.addRequest(new
Request(floor, DESTINATION))` directly.

## Why `Request` carries a type (the key insight)

If an elevator only stored floor numbers (`Set<Integer>`), an elevator
sweeping **up** through floor 5 would stop for someone at 5 who pressed
**down** — they'd board a car going the wrong way. Storing the request type
lets the elevator stop only for:

- moving UP → `PICKUP_UP` or `DESTINATION`
- moving DOWN → `PICKUP_DOWN` or `DESTINATION`

and pass opposite-direction pickups, serving them after it reverses.

## Core algorithm — `Elevator.step()` (SCAN)

Each tick, in order:

1. **No requests** → go `IDLE`, return.
2. **IDLE with requests** → face the nearest request (ties → lowest floor,
   so behavior doesn't depend on `HashSet` iteration order).
3. **Stop check** → remove any `DESTINATION` / matching-direction pickup at
   the current floor. If anything was removed, the tick is spent stopping
   (doors open) — return without moving.
4. **Nothing ahead** in the current direction → reverse, return. The next
   tick re-runs the stop check in the new direction, which is how a
   `PICKUP_DOWN` at the top of an upward sweep gets served.
5. Otherwise **move one floor**.

Boundary floors need no special case: an elevator can only be moving
toward a floor that has a request, and every request is within 0–9.

Why SCAN over FIFO / nearest-first: it minimizes direction changes, and
it's predictable for passengers — the car keeps moving toward them instead
of bouncing back to whatever request arrived last.

## Dispatch — `ElevatorController.selectBestElevator()`

Priority tiers, nearest wins inside a tier:

1. Elevators **already moving toward the floor in the caller's direction**
   (e.g. a `PICKUP_UP` at 7 goes to a car heading UP at floor 3) — it
   picks them up on the way with no extra reversal.
2. Nearest **idle** elevator.
3. Nearest elevator of **any** state — it'll get there after finishing its
   sweep.

Known limitation (worth saying out loud): distance ignores how many stops
are already queued — a tier-3 elevator "2 floors away" might be finishing
a 9-floor sweep first. A better score would estimate time-to-arrival from
the queue.

## Edge cases handled

- Out-of-range floor → `false`. `DESTINATION` passed to `requestElevator` → `false`.
- Request for the current floor while idle → no-op (`true`).
- Duplicate button presses dedupe via `Request` equality in the `Set`.
- Stopping consumes the tick; reversal also consumes the tick.
- Opposite-direction pickups are passed, then served after reversal.
- Can't oscillate forever: a request on the current floor is served in at
  most one reversal (DESTINATION in either direction, a pickup in its own).

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/elevator -name "*.java")

java -cp out elevator.driver.ElevatorDriver
java -cp out elevator.interview.SimpleElevatorInterview
```

`ElevatorDriver` walks through: rejected invalid requests → two hall calls
dispatched to the two nearest idle elevators → a `PICKUP_UP` at 7 going to
E1 because it's already heading up below 7 (tier 1) → E2 rising to 3,
reversing, then serving its `PICKUP_DOWN` → E1's passenger pressing 9 and 2,
with E1 finishing the UP sweep (7, 9) before coming back down for 2.

## Extensibility / follow-ups

- **Express elevators** — add an `isExpress` flag / set of served floors to
  `Elevator`; `addRequest` rejects other floors; dispatch filters to
  elevators that serve the floor (and prefers express for long trips).
- **Cancel a request** — add `removeRequest(request)`. Easy because all
  movement logic lives in `step()`, which just reacts to whatever is in
  the set next tick.
- **Concurrency** — buttons pressed from many threads while `step()` runs:
  either (a) one lock around `requestElevator()` and `step()`, or
  (b) a thread-safe queue of pending requests that `step()` drains at the
  start of each tick, so the elevator state is only ever touched by the
  ticking thread. (b) keeps button presses non-blocking.
- **All elevators busy / smarter dispatch** — immediate dispatch commits a
  hall call to one elevator forever. A central pending-call queue that is
  re-assigned each tick allows rebalancing as conditions change.
- **Starvation** — SCAN bounds wait to roughly two sweeps, so a far floor
  can't wait forever the way it can under nearest-first.

## Expectations by level

- **Junior** — entities, nearest-elevator dispatch, input validation;
  some bouncing acceptable.
- **Mid** — correct SCAN with direction-aware stops, clean state
  transitions, aware of dispatch limitations.
- **Senior** — asks simulation vs hardware upfront, tiered dispatch,
  proactively raises the queue-length blind spot, fluent on trade-offs.
