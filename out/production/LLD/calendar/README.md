# Calendar / Meeting Scheduler LLD

A meeting-scheduling system: users, calendars, events, conflict detection,
available-slot search, recurring events, and a pluggable reschedule
strategy. Two implementations live here:

- **Full version** (`driver`, `model`, `service`, `strategy`, `util`
  packages) — what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleCalendarInterview.java`) — a single
  ~130-line file covering just conflict detection + available-slot search,
  for when you only have 20-30 minutes or want a warm-up before the full
  design.

## Problem statement

> Design a calendar system where users can create events on a calendar,
> get notified of scheduling conflicts, find open slots of a given
> duration, and support recurring events.

## Clarifying questions to ask in the first 10 minutes

- Single calendar per user, or can a user have/attend multiple calendars?
  (Modeled here: one `Calendar` owned by one `User`; `Event.attendees` lets
  other users be invited without owning the calendar.)
- Do touching events conflict? i.e. is 9:00–10:00 followed by 10:00–11:00
  a conflict? → decide interval semantics now (this repo uses **half-open
  `[start, end)`**, so touching events do *not* conflict).
- Do we need recurring events? Daily/weekly/monthly? Byday rules (e.g.
  "every Mon/Wed/Fri")?
- Should conflicts be validated per recurrence occurrence, or only against
  the first occurrence? (Current implementation only checks the first
  occurrence — see Known Gaps.)
- Timezones: store in UTC and only convert for display, or model
  per-user timezone? (Current implementation: UTC `Instant` only, no
  timezone-aware display layer.)
- What happens on conflict — reject, force-create, or auto-reschedule?
  (All three are supported: `createEvent` rejects by default,
  `createEvent(event, true)` force-creates, `FindNextAvailableStrategy`
  auto-reschedules.)

## Class design

```
model/
  User            id, name, email
  Calendar        owner (User), name, set<eventId>
  Event           id, calendarId, title, start/end Instant, Optional<RecurrenceRule>,
                   List<Attendee>, description; overlaps() + copyWithStartEnd().
                   Built via Event.builder() / toBuilder() -- constructor is private;
                   build() enforces start < end and required fields
  Attendee        wraps a User + ResponseStatus (INVITED/ACCEPTED/DECLINED)
  RecurrenceRule  Frequency (DAILY/WEEKLY/MONTHLY), interval, count, until, byDay
  TimeSlot        start/end Instant — used for availability gaps

service/
  CalendarService        + InMemoryCalendarService   CRUD for calendars
  EventService            + InMemoryEventService      CRUD, conflict detection,
                                                        recurrence expansion, slot search
  NotificationService      + InMemoryNotificationService  an EventListener: sends
                                                             invites/updates to attendees
                                                             when EventService publishes

observer/
  EventListener   onEventCreated / onEventUpdated / onEventDeleted (default no-ops);
                    EventService.addListener() subscribes, publishes after commit

strategy/
  ConflictResolutionStrategy  + FindNextAvailableStrategy   reschedule-on-conflict policy

util/
  IdGenerator   AtomicLong-backed sequential string IDs

driver/
  CalendarDriver   end-to-end demo wiring all of the above
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Builder** | `Event.builder()` / `Event.toBuilder()` | `Event` has 8 fields, 3 optional. The old 8-arg constructor put two `Instant`s and an `Optional` side by side — transposing start/end compiles and only fails at runtime. The builder names every field, defaults the optional ones, and validates `start < end` once in `build()`, so an invalid `Event` cannot exist. `toBuilder()` replaces the hand-written copy constructor behind `copyWithStartEnd`. **Why not telescoping constructors or setters:** setters make `Event` mutable (it's shared across threads and used as a map value); telescoping constructors multiply with 3 optionals. |
| **Observer** | `EventListener` + `EventService.addListener`; `InMemoryNotificationService` implements `EventListener` | Before: the driver had to call `notifyInvite()` by hand after every `createEvent()` — forget once and invites silently don't go out. Now `EventService` publishes after the commit, and notifications are one subscriber among any number (audit log, sync to a phone). Published *outside* the per-calendar lock so a slow email send never blocks other bookings. |
| **Strategy** | `ConflictResolutionStrategy` / `FindNextAvailableStrategy` | Reschedule-on-conflict policy is a genuinely interchangeable algorithm ("shift to next slot" vs. "shift to nearest") — the one seam where Strategy is exactly right. |
| **Repository-ish interface + impl** | `CalendarService`/`InMemoryCalendarService`, `EventService`/`InMemoryEventService` | Storage is behind an interface so swapping in-memory for a real DB later doesn't change callers — classic **DIP**. |
| **Constructor injection** | `InMemoryEventService(CalendarService)`, `FindNextAvailableStrategy(EventService, Instant)` | No service reaches for a global/static instance; dependencies are explicit and mockable. |
| **Value object w/ copy-on-write mutation** | `Event.copyWithStartEnd(...)` | `Event` is otherwise immutable; rescheduling produces a new `Event` rather than mutating shared state. |

## SOLID mapping

- **SRP** — `Event` only holds data; `InMemoryEventService` only handles
  storage + conflict/availability logic; `InMemoryNotificationService` only
  prints notifications. No class does two of these.
- **OCP** — new reschedule policies implement `ConflictResolutionStrategy`
  without editing `EventService` or existing strategies.
- **LSP** — any `ConflictResolutionStrategy` can replace
  `FindNextAvailableStrategy` in the driver with no behavior surprises
  (same `apply(Event): Event` contract).
- **ISP** — `EventListener` gives every callback a default no-op, so a
  listener that only cares about creation implements one method, not
  three.
- **DIP** — `InMemoryEventService` depends on `CalendarService` (interface),
  not `InMemoryCalendarService` (impl); same for the strategy depending on
  `EventService`.

## Core algorithms

### Conflict detection

Half-open interval overlap check (`Event.overlaps`):

```java
candidate.start.isBefore(other.end) && other.start.isBefore(candidate.end)
```

This means `[9:00,10:00)` and `[10:00,11:00)` do **not** conflict — an event
ending exactly when another begins is fine. `InMemoryEventService.findConflicts`
reuses this same method, so the boundary rule is defined in exactly one
place.

### Finding available slots (`findAvailableSlots(from, to, duration)`)

1. Collect events intersecting `[from, to]`, clip each to the window:
   `busyStart = max(event.start, from)`, `busyEnd = min(event.end, to)`.
2. Sort busy intervals by start time.
3. Merge overlapping/contiguous busy intervals (prevents double-counting
   overlapping meetings as two separate gaps).
4. Walk the merged list with a cursor starting at `from`: whenever the gap
   before the next busy interval is `>= duration`, emit it as an available
   `TimeSlot`; advance the cursor past the busy interval; after the loop,
   emit the tail gap up to `to` if it's large enough.

Complexity: `O(m log m)` for `m` busy intervals in the window — fine for a
single calendar in an interview; for scale, an interval tree or a
pre-merged availability index would avoid re-scanning on every query.

### Recurrence expansion

`expandRecurring` walks forward from the first occurrence by
`interval` units (day/week/month), with a special case for weekly `byDay`
sets (e.g. "every Mon & Wed") that steps day-by-day to the next matching
weekday rather than jumping a full week. Stops at `count` occurrences or
`until`, whichever comes first.

## Concurrency & thread-safety

- **Per-calendar lock striping for create/update** — only events *within
  the same calendar* can conflict with each other, so
  `InMemoryEventService.createEvent`/`updateEvent` synchronize on a lock
  keyed by `calendarId` (`Map<calendarId, Object>`, lazily populated via
  `computeIfAbsent`) rather than one global lock across every calendar in
  the system. Two threads booking events on *different* calendars never
  block each other; two threads racing to book the same calendar's
  9-10am slot are correctly serialized so both can't pass `findConflicts`
  and both write — the check-then-act race called out in earlier drafts
  of this README is closed.
- **`ConcurrentHashMap` for `events` and `calendars`** — `IdGenerator`
  (an `AtomicLong`) guarantees unique keys, so puts never collide on a
  key, but a plain `HashMap` is still unsafe under concurrent structural
  modification (two `put`s triggering an internal resize at the same
  time can corrupt the bucket structure or silently drop an entry).
- **`Calendar.addEvent`/`removeEvent`/`getEventIds` are `synchronized`**
  — one monitor per `Calendar` instance, so the event-id `Set` mutated by
  `InMemoryEventService` on every create/delete can't be corrupted by
  concurrent structural changes, without contending with unrelated
  calendars.
- **What's still a known gap:** `findAvailableSlots` and `listEvents` are
  unsynchronized reads — they can observe a slightly stale view if a
  write is in flight on the same calendar concurrently (a classic
  read/write trade-off: making reads block on the calendar lock too would
  serialize every availability query behind every write). This is an
  acceptable "eventually consistent read" for a calendar UI refreshing
  its view, not for anything requiring a strict linearizable snapshot.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/calendar -name "*.java")

java -cp out calendar.driver.CalendarDriver
java -cp out calendar.interview.SimpleCalendarInterview
```

`CalendarDriver` demonstrates, in order: create calendar → create event →
attempt a conflicting event (caught, conflicts printed) → list available
slots for that duration → auto-reschedule the conflicting event via
`FindNextAvailableStrategy` and create the shifted event → create a weekly
recurring event (Mondays, 3 occurrences) → list events for a day and for a
week window.

`SimpleCalendarInterview` is the same golden path (create → conflict →
show slots → reschedule to first slot) in one file, with no interfaces or
recurrence — useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How would you scale this to millions of events?"** — Swap
  `InMemoryEventService`'s `HashMap` for a real store with an index on
  `(calendarId, start)`; for slot search at scale, maintain a pre-merged
  "busy" interval tree per calendar instead of recomputing from raw events
  on every query.
- **"How do you handle recurring event conflicts?"** — Currently
  `createEvent` only checks the *first* occurrence for conflicts
  (`findConflicts` doesn't call `expandRecurring`). The honest answer: you'd
  expand occurrences within a bounded horizon and run conflict detection
  against each one, which is strictly more expensive — worth discussing the
  horizon trade-off (e.g. only check the next N occurrences or next year).
- **"What about timezones?"** — Everything is stored as UTC `Instant`;
  timezone is a presentation concern that would live on `User` (preferred
  `ZoneId`) and only be applied when rendering, never in storage or
  conflict math.
- **"Concurrent updates from two clients?"** — Solved via per-calendar
  lock striping in `InMemoryEventService` (see the Concurrency section
  above): two threads booking the *same* calendar's overlapping slot are
  serialized correctly; two threads on *different* calendars run in
  parallel with no contention.
- **"How would you add attendee accept/decline?"** — `Attendee` already
  models `ResponseStatus` and has a setter; the missing piece is an
  `EventService.respondToInvite(eventId, userId, status)` entry point and a
  notification hook back to the organizer — natural extension, not a
  redesign.
- **"Undo a reschedule?"** — Because `Event` is immutable and
  `copyWithStartEnd` produces a new object, an undo stack of `Event`
  snapshots is straightforward to bolt on.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| `Instant` (UTC) for all time fields | `LocalDateTime` / `ZonedDateTime` per event | Avoids timezone bugs in conflict math; conversion to a display zone is a UI-layer concern only. |
| Half-open `[start, end)` interval semantics | Closed intervals | Standard for calendar systems — a meeting ending at 10:00 shouldn't block one starting at 10:00. |
| `Map<String, Event>` in-memory store | Real DB | Interview constraint — no DB available; `EventService` interface means swapping the backing store later doesn't touch callers. |
| Per-calendar lock striping over one global lock or optimistic versioning | A single `synchronized` keyword on the whole service; or version-stamped `Event`s with retry-on-conflict | A global lock would serialize bookings across every calendar in the system for no reason (calendars can't conflict with each other); optimistic versioning avoids locking entirely but adds retry-loop complexity this problem's scope doesn't need — per-calendar striping is the middle ground. |
| Separate `ConflictResolutionStrategy` instead of a reschedule method on `EventService` | Hardcode reschedule logic in the service | Keeps `EventService` focused on storage/query; reschedule *policy* is a separate, swappable concern (OCP). |
| `createEvent(event, boolean force)` overload | A separate `forceCreateEvent` method | Minimal surface area; `force` is just a flag on the same operation, not a different operation. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — everything resets on JVM exit.
- Availability/listing reads are unsynchronized (see Concurrency section
  — an intentional read/write trade-off, not an oversight).
- Recurring events are not conflict-checked per occurrence, only on the
  first occurrence.
- No timezone-aware display layer (UTC only).
- `Attendee.setStatus` exists but nothing in the driver calls it — no
  accept/decline flow wired up yet.
- `updateEvent` and `deleteEvent` exist on `EventService` but aren't
  exercised by `CalendarDriver`.
- `calendar.strategy.parking.NearestFirstStrategy` is a stray empty
  placeholder class left over from scaffolding — it belongs in the parking
  lot package, not here, and isn't referenced by anything.
