# LLD — Low-Level Design Interview Practice

Java implementations of common Low-Level Design (LLD) interview questions.
Each problem lives in its own package under `src/`, has an in-memory-only
implementation (no database, no framework), and comes with its own README
covering the requirements, design decisions, and interview follow-ups.

## Interview format this repo is built around

| Phase | Time |
|---|---|
| Requirements discussion / clarifying questions | 10 min |
| Coding | 60 min |
| Follow-up questions & wrap-up | 20 min |
| **Total** | **1.5 hrs** |

## Ground rules used across every LLD here

- **SOLID first.** Every design favors composition over inheritance, small
  interfaces, and dependency injection via constructors so behavior can be
  swapped without touching call sites.
- **In-memory storage only.** No database — use `Map`/`List` collections
  (`HashMap`, `ConcurrentHashMap`) to simulate persistence, since that's what's
  expected in a coding-round timebox.
- **The pattern fits the problem, not the other way round.** Every LLD
  picks the pattern its behavior actually calls for and says why — and
  why the obvious alternative was rejected. Strategy for interchangeable
  algorithms, Observer where the prompt says "notify"/"real-time",
  Builder for fat constructors, Chain of Responsibility for "try handlers
  in order", Command for queued requests, State for a lifecycle with
  per-state behavior. See the decision guide below.
- **State entities are dumb; services hold behavior.** Model classes
  (`Event`, `ParkingSpot`, ...) hold data and small invariants; `*Service` /
  facade classes own orchestration.
- Each LLD's README explicitly calls out **which design patterns are used and
  why**, plus the trade-offs made under interview time pressure.

## LLDs in this repo

Sourced from the [LeetCode "Frequently Asked LLD Questions" list](https://leetcode.com/discuss/post/5328221/frequently-asked-low-level-design-lld-qu-l0xk/)
plus a couple of classics (Calendar, Snake & Ladder) added independently.

| LLD | Package | Status | README |
|---|---|---|---|
| Calendar / Meeting Scheduler | `src/calendar` | Complete | [src/calendar/README.md](src/calendar/README.md) |
| Parking Lot | `src/parkinglot` | Complete | [src/parkinglot/README.md](src/parkinglot/README.md) |
| Snake & Ladder | `src/snakeladder` | Complete | [src/snakeladder/README.md](src/snakeladder/README.md) |
| Online Bookstore | `src/bookstore` | Complete | [src/bookstore/README.md](src/bookstore/README.md) |
| Library Management System | `src/library` | Complete | [src/library/README.md](src/library/README.md) |
| Movie Ticket Booking System | `src/moviebooking` | Complete | [src/moviebooking/README.md](src/moviebooking/README.md) |
| Elevator System | `src/elevator` | Complete | [src/elevator/README.md](src/elevator/README.md) |
| Hotel Management System | `src/hotel` | Complete | [src/hotel/README.md](src/hotel/README.md) |
| Ride-Sharing Service | `src/ridesharing` | Complete | [src/ridesharing/README.md](src/ridesharing/README.md) |
| File Storage System (Drive-style) | `src/filestorage` | Complete | [src/filestorage/README.md](src/filestorage/README.md) |
| Chat Application | `src/chatapp` | Complete | [src/chatapp/README.md](src/chatapp/README.md) |
| Social Media Platform | `src/socialmedia` | Complete | [src/socialmedia/README.md](src/socialmedia/README.md) |
| Notification System | `src/notification` | Complete | [src/notification/README.md](src/notification/README.md) |
| Airline Reservation System | `src/airline` | Complete | [src/airline/README.md](src/airline/README.md) |
| ATM System | `src/atm` | Complete | [src/atm/README.md](src/atm/README.md) |
| E-commerce Website | `src/ecommerce` | Complete | [src/ecommerce/README.md](src/ecommerce/README.md) |
| Food Delivery System | `src/fooddelivery` | Complete | [src/fooddelivery/README.md](src/fooddelivery/README.md) |
| Shopping Cart System | `src/shoppingcart` | Complete | [src/shoppingcart/README.md](src/shoppingcart/README.md) |

## Building & running

Each LLD is compiled independently — there's no shared build tool, just
`javac`. Every LLD follows the same two-entry-point convention (see each
package's own README for the exact class names):

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD

javac -d out $(find src/<package> -name "*.java")
java -cp out <package>.<Name>Driver                       # full service-based demo
java -cp out <package>.interview.Simple<Name>Interview     # compact single-file version
```

`src/Main.java` compiles every finished LLD and runs each compact
interview demo back to back — a quick smoke test that everything still
builds and runs. Prefer running each driver's own `main` directly (as
above) when you're iterating on one LLD at a time, since interleaved
output from many demos in one run is harder to read.

## Pattern decision guide

Read the problem statement for the *shape* of the behavior, then pick.
Each row names the LLD in this repo that shows it, and the trap it avoids.

| The problem says… | Pattern | Shown in | Why — and what it's *not* |
|---|---|---|---|
| "pick a spot / price / route by policy X or Y" — one operation, several interchangeable algorithms | **Strategy** | Parking Lot (spot, fee), Hotel (pricing), Elevator (dispatch) | Only for *one* swappable algorithm. If criteria must be *combined*, it's Specification; if the thing varies by *state*, it's State. |
| "search by title AND author under $50" — criteria that compose | **Specification** | Bookstore | Three `*SearchStrategy` classes can't be AND-ed. A predicate with `and/or/not` can. |
| "notify…", "real-time…", "display board updates…", "invalidate cache when…" | **Observer** | Social Media, Food Delivery, Calendar, Parking Lot | Publish the fact once; every reaction subscribes. Not a direct `notificationService.send()` from the domain service — that hard-wires one reaction and grows a parameter per new one. Publish *after* releasing domain locks. |
| "try A, then B, then C until one succeeds" | **Chain of Responsibility** | Notification | A `for` loop puts the on-failure decision in the orchestrator; a chain puts it in each link. |
| "add retry / backoff / rate-limit / metrics around X" without X knowing | **Decorator** | Notification (`RetryingChannel`), Ride-Sharing (surge fare) | Wraps the same interface, stacks. Not a `RetryPolicy` object handed to the service — that still makes the *service* run the loop. |
| "create the right subtype from a type code / string" | **Factory** | Parking Lot (`VehicleFactory`), Notification (`ChannelFactory`) | One place maps type → class; callers never `new` a concrete type. |
| "an object with 6+ fields, some optional, with invariants between them" | **Builder** | Calendar (`Event`), Airline (`Flight`), Hotel, E-commerce, Food Delivery | Named fields + one `build()` validation. Not setters (mutability) or telescoping constructors (combinatorial). |
| "requests arrive from many sources, must be queued / ordered / logged / undone" | **Command** | Elevator | A request that's an object can sit in a `BlockingQueue`; a method call can't. |
| "the same operation means different things depending on what stage we're in" | **State** | ATM | One class per state, each overriding only the transitions valid from it. |
| "a lifecycle enum with guarded transitions" (PLACED→PAID→SHIPPED) | enum + CAS `tryTransition` — **deliberately not State** | Bookstore, Hotel, E-commerce, Ride-Sharing, Food Delivery | State pattern earns its weight when each state has *different behavior for the same call* (ATM: `enterPin` in IDLE vs HAS_CARD). When the only per-state difference is "which next states are legal," an enum plus a compare-and-set is smaller, thread-safe, and just as explicit. Know where the line is. |
| "tree of things where a group behaves like a leaf" | **Composite** | File Storage | Files and folders share identity/ACL; only one holds children. |
| "exactly one lot / one controller per process" | **Singleton** (double-checked, `volatile`) | Parking Lot | And say out loud that it doesn't generalize to multiple lots. |
| "hide a subsystem behind a few calls" | **Facade** | Parking Lot | `parkVehicle`/`unparkVehicle` over floors, spots, tickets, strategies. |

## Adding a new LLD

Structure by *role*; name the pattern package for the pattern it holds
(`observer/`, `command/`, `chain/`, `factory/`, `specification/`,
`strategy/`), so the tree itself says what the design is:

1. `model/` — plain data classes, immutable where practical; a `Builder`
   for anything with more than ~5 fields or an invariant between fields.
2. `service/` (or a single facade class for simpler problems) — interfaces +
   `InMemory*` implementations, injected via constructor.
3. One package per pattern the problem calls for (see the guide above) —
   never default to `strategy/`.
4. `driver/` (or a `*Driver` class at the package root) — a runnable `main`
   that walks through the golden path and a couple of edge cases end to end.
5. A package-level `README.md` covering: requirements & clarifying
   questions, class design, **patterns used, why each, and why not the
   obvious alternative**, SOLID mapping, concurrency & thread-safety, how
   to run, likely interviewer follow-ups, tech decisions/trade-offs, and
   known gaps.
