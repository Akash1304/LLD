# Elevator System LLD

A bank of elevators running the classic SCAN ("elevator algorithm")
scheduling, with pluggable hall-call dispatch. Two implementations live
here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleElevatorInterview.java`) — a
  single ~70-line file covering one elevator's SCAN scheduling over a mix
  of hall and car calls, for when you only have 15-20 minutes or want a
  warm-up before the full design.

## Problem statement

> Design an elevator system for a building: multiple elevators serve hall
> calls (someone on a floor pressing up/down) and car calls (a passenger
> inside pressing a floor button), and elevators should be scheduled
> efficiently rather than naively (e.g. first-come-first-served).

## Clarifying questions to ask in the first 10 minutes

- How many elevators, and do they share a dispatch system or operate
  independently? (Modeled here: N elevators under one
  `ElevatorControlSystem` that dispatches hall calls to the best
  candidate.)
- What scheduling algorithm — naive FCFS, or something that avoids
  unnecessary direction reversals? (Modeled here: **SCAN** — each
  elevator keeps moving in its current direction, picking up every
  pending stop along the way, only reversing once nothing is left ahead.
  This is the textbook-expected answer and avoids the "elevator ping-pongs
  between two floors" pathology of naive scheduling.)
- How is an elevator picked for a hall call — nearest one, or load
  balancing across the bank? (Modeled here: both, as swappable
  strategies — `NearestElevatorStrategy` and `LeastLoadedElevatorStrategy`.)
- Should a hall call prefer an elevator already moving toward it in the
  same direction (so it can be picked up "on the way"), or just the
  physically nearest one regardless of direction? (Modeled here:
  `NearestElevatorStrategy` heavily deprioritizes elevators moving away
  or in the wrong direction, not just raw distance.)
- Capacity limits (max passengers/weight)? (Not modeled — see Known Gaps.)

## Class design

```
model/
  Direction    enum: UP, DOWN, IDLE
  HallCall     floor + requested Direction (a call from a hallway button)
  Elevator     id, currentFloor, Direction, TreeSet<Integer> upStops/downStops;
                requestStop(floor) enqueues a stop; step() advances one floor
                and runs the SCAN reversal logic

strategy/
  ElevatorSelectionStrategy   + NearestElevatorStrategy / LeastLoadedElevatorStrategy

command/
  ElevatorCommand    execute(system), describe()
  HallCallCommand    floor + direction -> system.dispatchHallCall
  CarCallCommand     elevatorId + floor -> system.dispatchCarCall

service/
  ElevatorControlSystem   + InMemoryElevatorControlSystem
      submit(command)    enqueue onto a LinkedBlockingQueue (any thread)
      step()             drain the queue in arrival order, executing each
                           command, then advance every elevator one floor
      dispatchHallCall / dispatchCarCall   the primitives commands call

driver/
  ElevatorDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Command** | `ElevatorCommand` + `HallCallCommand`/`CarCallCommand`; `ElevatorControlSystem.submit()` queues them, `step()` executes them | A button press is a *request object*, not a method call. Requests arrive from many buttons (threads) at once but must be applied in a defined order — so they're queued as commands and drained by the control loop. Because a request is an object it can be queued (`BlockingQueue`), logged for audit, replayed after a controller restart, prioritized (a fire-alarm recall jumps the queue), or cancelled. **Why not just call `dispatchHallCall` from the button:** none of those are possible with a bare method call, and every button thread would be reaching into elevator state directly instead of through one serialized consumer. |
| **Strategy** | `ElevatorSelectionStrategy` (`Nearest`/`LeastLoaded`) | "Which elevator answers this call" is a genuinely interchangeable algorithm over the same inputs — Strategy is exactly right for this one seam. |
| **Two sorted sets as a scheduling queue** | `Elevator.upStops`/`downStops` (`TreeSet<Integer>`) | `TreeSet.first()`/iteration order gives the next stop in the current direction for free — no manual sort needed on every step, and `O(log n)` insert/remove. |
| **Encapsulated state machine inside the entity** | `Elevator.step()` | Direction transitions (UP → IDLE, UP → DOWN, etc.) live entirely inside `Elevator`, not scattered across the control system — the entity can never end up in an inconsistent state (e.g. `direction=UP` with no up-stops and non-empty down-stops). |
| **Repository-ish interface + impl** | `ElevatorControlSystem`/`InMemoryElevatorControlSystem` | Same DIP shape as the rest of this repo's LLDs — a future networked/persisted control system wouldn't change the driver. |

## SOLID mapping

- **SRP** — `Elevator` only tracks its own position/direction/stops;
  `ElevatorControlSystem` only dispatches calls and steps the simulation;
  dispatch policy lives only in `ElevatorSelectionStrategy` impls.
- **OCP** — a new dispatch policy (e.g. "prefer the elevator with the
  shortest estimated wait time") can be added without touching
  `InMemoryElevatorControlSystem` or `Elevator`.
- **LSP** — every `ElevatorSelectionStrategy` takes
  `(List<Elevator>, HallCall)` and returns an `Elevator`; the control
  system treats both implementations identically.
- **ISP** — `ElevatorControlSystem` exposes four narrow operations
  instead of one catch-all `handleRequest(...)`.
- **DIP** — `InMemoryElevatorControlSystem` depends on
  `ElevatorSelectionStrategy` (interface), injected via constructor, not
  a hardcoded dispatch rule.

## Core algorithm

### SCAN scheduling (`Elevator.step`)

1. If `direction == IDLE`, do nothing (there's nothing pending — see
   `requestStop` for how a request sets the initial direction).
2. If moving `UP`: advance one floor; if that floor is a pending up-stop,
   service it (remove it); if `upStops` is now empty, switch to `DOWN` if
   there are pending down-stops, else go `IDLE`.
3. Symmetric logic for `DOWN`.
4. `requestStop(floor)` adds the floor to `upStops` or `downStops`
   depending on whether it's above or below the current floor, and only
   sets an initial `direction` if the elevator was `IDLE` — a moving
   elevator finishes its current sweep before reversing, which is exactly
   what SCAN means by not reversing early.

This guarantees monotonic progress in one direction until nothing is left
ahead, avoiding the "reverses every time a new request comes in" failure
mode of naive scheduling.

### Dispatch (`NearestElevatorStrategy.score`)

Scores each elevator by distance to the call floor, but adds a large
penalty (1000) to any elevator that is moving and *not* already headed
toward the call in the requested direction — so an idle nearby elevator or
one already sweeping toward the caller wins over a closer elevator that
would have to finish its current sweep and reverse first.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/elevator -name "*.java")

java -cp out elevator.driver.ElevatorDriver
java -cp out elevator.interview.SimpleElevatorInterview
```

`ElevatorDriver` demonstrates: 3 elevators (at floors 1, 5, 10 in a
10-floor building) → a hall call at floor 3 going up (dispatched to the
elevator at floor 1, since it's already positioned to sweep upward through
it) → a hall call at floor 8 going down (dispatched to the elevator at
floor 10) → a car call to floor 6 on the first elevator → step the
simulation, printing every elevator's position each tick, until all are
idle.

`SimpleElevatorInterview` is the same SCAN logic for a single elevator
handling a mix of car and hall calls, in one file with no interfaces or
dispatch strategy — useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"Why SCAN instead of first-come-first-served?"** — FCFS can make an
  elevator reverse direction repeatedly for requests that arrive
  out-of-order, wasting time and energy; SCAN guarantees it finishes
  sweeping one direction before reversing, which is both fairer and more
  efficient — the standard justification interviewers are listening for.
- **"How would you avoid starvation (a far request waiting forever if
  closer requests keep coming in)?"** — Not structurally prevented in
  SCAN as implemented (a variant called LOOK/C-SCAN with aging could
  prioritize long-waiting requests); worth naming as a known limitation
  of pure SCAN rather than claiming it's solved.
- **"How do you handle elevator capacity (max passengers/weight)?"** —
  Not modeled; you'd add a `currentLoad` field to `Elevator` and have
  `requestStop` (or a new `boardPassenger` step) reject/queue requests
  once capacity is hit — a real capacity model also needs to distinguish
  "car call from inside" (already boarded, must be honored) from "hall
  call" (can be deferred to the next elevator).
- **"Concurrent hall calls from multiple threads?"** — Every mutable
  field on `Elevator` (`currentFloor`, `direction`, the two `TreeSet`
  stop queues) is guarded by a `synchronized` method on that elevator, so
  `requestStop`/`step` on the same elevator can't interleave into a torn
  state, and dispatching to elevator A never blocks a concurrent dispatch
  to elevator B — see the Concurrency section below.
- **"How would you scale to a 50-story building with 20 elevators?"** —
  The scoring in `NearestElevatorStrategy` is `O(elevators)` per call,
  which is fine at that scale; the real scaling concern is dispatch
  *latency* under high call volume, which would push toward
  batching/zoning elevators (e.g. odd/even floor zones) rather than a
  single shared strategy evaluating every elevator for every call.

## Concurrency & thread-safety

- **Per-elevator monitor, not a control-system-wide lock** — every method
  on `Elevator` that touches `currentFloor`/`direction`/the stop queues
  (`requestStop`, `step`, and the getters used for both dispatch scoring
  and printing) is `synchronized` on that specific elevator instance.
  Two hall calls dispatched to *different* elevators run fully in
  parallel; only calls that land on the *same* elevator (a hall call and
  the simulation's `step()` ticking it, say) are serialized against each
  other.
- **No retry loop needed here, unlike Parking Lot/Movie Booking** — a
  hall call just enqueues a stop; there's no scarce resource being
  claimed and no way for `requestStop` to "lose" to a concurrent call the
  way claiming the last parking spot or seat can fail. Per-elevator
  synchronization is sufficient on its own — it exists purely to prevent
  the multi-field state (`currentFloor`+`direction`+stop sets) from being
  read or written half-updated, not to arbitrate contention over a
  shared resource.
- **Elevator selection is a best-effort read, not a locked snapshot** —
  `ElevatorSelectionStrategy.selectElevator` reads each candidate
  elevator's state through its synchronized getters independently (not
  all elevators locked together at once), so the state it scores against
  could change by the time `requestStop` runs on the chosen elevator.
  That's fine here precisely because `requestStop` can't fail — the
  worst case is a slightly stale dispatch decision, not a correctness
  violation.
- **The elevator list itself is immutable at runtime** — no
  add/remove-elevator API exists, so wrapping it `Collections.unmodifiableList`
  at construction is sufficient; no locking is needed for the bank
  membership itself, only for each elevator's internal state.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Per-elevator lock over one control-system-wide lock | `synchronized` on `InMemoryElevatorControlSystem`'s dispatch methods | A control-system-wide lock would serialize every hall/car call across the entire bank; per-elevator locking means only calls landing on the *same* elevator ever contend. |
| Two `TreeSet<Integer>` per elevator (up/down stops) | One sorted list with a direction flag and manual scan logic | `TreeSet` gives sorted iteration and O(log n) insert/remove for free, and splitting by direction makes the SCAN "am I done with this sweep" check a simple `isEmpty()`. |
| `step()` moves one floor per call, not "jump straight to the next stop" | Compute the next stop and teleport the elevator there | One-floor-per-step matches how the driver demo visualizes movement floor-by-floor; a real simulation would additionally track time-per-floor, which this intentionally omits (see Known Gaps). |
| Dispatch heavily penalizes (not eliminates) elevators moving the "wrong way" | Filter them out entirely | Falling back to a wrong-direction elevator is still better than no elevator at all if every other elevator is farther away — a hard filter could pick a worse elevator when the penalized ones are actually closest. |
| `HallCall` carries a direction, but `Elevator.requestStop` doesn't use it | Have the elevator itself reason about hall-call direction when queuing the stop | The direction is only needed for *dispatch* (deciding which elevator answers); once an elevator commits to a stop, SCAN doesn't care why the floor was requested — simplifies `Elevator`'s internal model. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — elevator/request state resets on JVM exit.
- No capacity limits (max passengers/weight per elevator).
- No starvation prevention (pure SCAN, not LOOK-with-aging).
- No door-open/close timing or dwell time at a stop — a stop is
  instantaneous in this model.
- No distinction between "must honor" car calls (passenger already
  aboard) and "can defer" hall calls when computing load for dispatch.
- No zoning/sectoring for very large buildings.
