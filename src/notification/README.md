# Notification System LLD

Multi-channel notification delivery (push/email/SMS) with per-user
fallback ordering and per-channel retry. Two implementations live here:

- **Full version** (`model`, `channel`, `decorator`, `factory`, `chain`,
  `service`, `driver` packages) — what you'd build across the full
  60-minute coding window.
- **Compact version** (`interview/SimpleNotificationInterview.java`) — a
  single ~75-line file covering channel fallback and per-channel retry
  with a deterministic flaky channel, for when you only have 15-20
  minutes or want a warm-up before the full design.

## Problem statement

> Design a notification system: notify a user via one of several channels
> (push, email, SMS), and ensure delivery is reliable — retrying on
> transient failure and falling back to another channel if one is
> unavailable or exhausted.

## Clarifying questions to ask in the first 10 minutes

- Does every user use the same channel, or can each user have their own
  preferred channel (and a fallback order)? (Modeled here: **per-user** —
  `User.channelPreferenceOrder` is a list the chain is built from.)
- What counts as "reliable delivery" — retrying the same channel a few
  times, falling back to a different channel, or both? (Modeled here:
  **both**, as two separate concerns: `RetryingChannel` (a decorator)
  owns retry, `ChannelHandler` (a chain link) owns fallback.)
- Should a channel that structurally can't work for a user (no email on
  file) count as a "failure to retry," or be skipped immediately?
  (Modeled here: it's a delivery failure like any other; the retry
  decorator will burn its attempts on it. A permanent-vs-transient
  distinction on the exception is the natural next step — see follow-ups.)
- Is notification content channel-specific, or one message rendered the
  same everywhere? (Modeled here: **one message** — see Known Gaps.)
- Fire-and-forget, or does the caller need the final outcome? (Modeled
  here: `send` returns a `Notification` with a final status and which
  channel delivered it.)

## Class design

```
model/
  NotificationChannelType   enum: PUSH, EMAIL, SMS
  NotificationStatus        enum: PENDING, SENT, FAILED
  User                      id, name, email, phone, ordered List<NotificationChannelType>
  Notification              id, userId, title, message, createdAt,
                              mutable status, mutable deliveredVia

channel/                     -- the delivery mechanisms
  NotificationChannel   (interface) getType(), send(User, Notification)
                          throws ChannelDeliveryException
  EmailChannel / SmsChannel   fail if the user has no contact info
  PushChannel                 deterministic flaky channel: fails its first N
                              attempts per notification, then succeeds

decorator/
  RetryingChannel       wraps ANY NotificationChannel, retries up to N times,
                          rethrows once exhausted

factory/
  ChannelFactory        NotificationChannelType -> NotificationChannel;
                          the only place concrete channel classes are named;
                          instances cached per type

chain/
  ChannelHandler        one link: try my channel, on failure delegate to next
  ChannelChain          builds a per-user chain from preference order:
                          factory.create(type) -> wrap in RetryingChannel
                          -> link

service/
  UserDirectory         + InMemoryUserDirectory
  NotificationService   + InMemoryNotificationService   send(userId, title,
                          message, maxAttemptsPerChannel): build the chain
                          for this user, run it, record the outcome

driver/
  NotificationDriver   end-to-end demo
```

## Design patterns used — and why these, not Strategy

An earlier version of this LLD modeled everything as Strategy: channels as
one strategy family, retry as a `RetryPolicy` "strategy" whose entire
contract was `int getMaxAttempts()`, and fallback as a `for` loop in the
service. That works, but it's the wrong shape for the problem, and an
interviewer will say so. Each concern below maps to a pattern that fits
its actual behavior:

| Concern | Pattern | Where | Why this pattern (and why not Strategy) |
|---|---|---|---|
| **Try channels in order until one works** | **Chain of Responsibility** | `ChannelHandler` + `ChannelChain` | The problem *is* "pass the request down a line of handlers." A `for` loop in the service hard-wires the fallback decision into the orchestrator; with chain links, each handler decides what to do on its own failure, and the service doesn't change when that decision changes (e.g. a handler that short-circuits the chain on a permanent error instead of falling through). |
| **Retry a failed delivery** | **Decorator** | `RetryingChannel` | Retry is a property of a *delivery attempt*, not of the orchestration layer. `RetryingChannel` implements `NotificationChannel` and wraps one, so it composes with every channel, can sit inside the chain transparently, and can itself be wrapped again (rate limiting, metrics). A "retry strategy" object handed to the service forces the service to *implement* the retry loop; a decorator removes that loop from the service entirely. |
| **Turn a channel type into a channel** | **Factory** | `ChannelFactory` | Callers work in terms of the `NotificationChannelType` enum and the `NotificationChannel` interface; only the factory names `EmailChannel`/`PushChannel`. Adding `SlackChannel` is one new `case`. The factory also owns instance lifetime (cached per type), which matters because real channels hold connection pools. |
| **Deliver via a specific medium** | **Strategy** (still the right fit here) | `NotificationChannel` implementations | The channels genuinely *are* interchangeable algorithms for one operation (`send`). Strategy is correct for this one seam — it was wrong for retry and fallback. |
| **Reproducible failure for the demo** | Deterministic fault injection | `PushChannel(failuresBeforeSuccess)` | The retry path can be shown and verified without randomness or timing. |
| **Storage behind an interface** | Repository-ish interface + impl | `UserDirectory`, `NotificationService` | Same DIP shape as the rest of this repo. |

The service is now almost empty — look up the user, build the chain, run
it. That emptiness is the point: retry, fallback, and construction each
live in the one place that owns them.

## SOLID mapping

- **SRP** — each `NotificationChannel` only knows one medium;
  `RetryingChannel` only knows retry; `ChannelHandler` only knows
  "me, then next"; `ChannelFactory` only knows construction; the service
  only orchestrates.
- **OCP** — a new channel is a new class plus one factory case; a new
  cross-cutting behavior (backoff, rate limiting) is a new decorator;
  neither touches the chain or the service.
- **LSP** — `RetryingChannel` is a drop-in `NotificationChannel`: the
  chain treats a wrapped channel and a bare one identically, and any
  channel can be swapped for any other in the chain.
- **ISP** — `NotificationChannel` is two methods; a channel isn't forced
  to know about retries, ordering, or user preferences.
- **DIP** — `ChannelChain` depends on `ChannelFactory` and the
  `NotificationChannel` interface, never on `EmailChannel` et al.; the
  service depends on the chain, not on any channel.

## Core algorithms

### Chain assembly (`ChannelChain.build`)

```
for each type in user.channelPreferenceOrder:
    channel = new RetryingChannel(factory.create(type), maxAttempts)
    handler = new ChannelHandler(channel)
    link handler after the previous one
return head
```

### Delivery (`ChannelHandler.handle`)

Try my channel (which, being a `RetryingChannel`, already retries
internally). On success, return my type. On failure, delegate to `next`,
or return empty if I'm the last link. The service maps that `Optional` to
`SENT`/`FAILED`.

## Concurrency & thread-safety

- **`ConcurrentHashMap` for shared, mutable channel state** —
  `PushChannel.attemptsSoFar` is read and written (via `merge`) on every
  retry of every notification through that shared instance. `merge` is
  atomic per key, so concurrent notifications can't corrupt each other's
  attempt counts.
- **Factory cache via `computeIfAbsent`** — two threads asking for the
  same channel type concurrently get the same instance; the mapping
  function runs exactly once per type.
- **Chains are per-call, not shared** — `ChannelChain.build` creates
  fresh `ChannelHandler`/`RetryingChannel` objects for each `send`, so
  handler state (the `next` links) is never shared across threads. Only
  the underlying channels from the factory are shared, and those are
  stateless except for the guarded map above.
- **Each `send` is independent** — the service holds no mutable state
  across calls beyond the id counter (an `AtomicLong`).

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/notification -name "*.java")

java -cp out notification.driver.NotificationDriver
java -cp out notification.interview.SimpleNotificationInterview
```

`NotificationDriver` demonstrates: Alice prefers push→email, and the push
provider is configured to fail her first 2 attempts — with 3 attempts per
channel, the retry decorator succeeds on the 3rd and the chain never
falls back → Bob has no push device and no phone on file, so push and SMS
each burn their single attempt and the chain hands off to email → sending
to Alice again with only 1 attempt per channel shows push failing once
and the chain falling back to email.

`SimpleNotificationInterview` is the same fallback-with-retry logic in one
file with no patterns — useful as a live-coding warm-up, and a good
"now refactor this into chain + decorator" follow-up.

## Follow-up questions to expect (and how this design answers them)

- **"How do you avoid retrying instantly and hammering a struggling
  provider?"** — Backoff is another decorator: `BackoffChannel` wraps a
  channel and sleeps (or, better, schedules) between attempts. Because
  retry is already a decorator, backoff composes on top —
  `new RetryingChannel(new BackoffChannel(channel), 3)` — with no change
  to the chain or service.
- **"How would you make delivery asynchronous?"** — `send` is synchronous
  for demo clarity; a real system would enqueue the notification and run
  the chain on a worker, returning `PENDING` immediately.
- **"A permanent failure (no email on file) shouldn't burn retries."** —
  Give `ChannelDeliveryException` a `retryable` flag; `RetryingChannel`
  rethrows non-retryable failures immediately. The chain already handles
  the fallthrough. This is the cleanest demonstration of why retry and
  fallback needed to be *separate* objects.
- **"How would you add a Slack channel?"** — `SlackChannel implements
  NotificationChannel`, one new `case` in `ChannelFactory`, one new enum
  value. The chain, the decorator, and the service are untouched.
- **"How do you prevent duplicate sends if the caller retries the whole
  request?"** — Not modeled; a real system needs idempotency keys so a
  duplicate "send this" request doesn't double-deliver.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Chain of Responsibility for fallback | `for` loop over channels in the service | The loop hard-wires the on-failure decision into the orchestrator; chain links own that decision individually and can differ per link. |
| Retry as a Decorator | A `RetryPolicy` object passed to the service | A policy object still makes the *service* run the retry loop; a decorator removes the loop from the service and lets retry budgets differ per channel. |
| Factory caches one instance per type | New channel per `send` | Real channels hold connections; and `PushChannel`'s per-notification attempt counter must persist across the retries of one send. |
| Chain built per call | One long-lived chain per user | Handler links are cheap, and per-call construction means no shared mutable handler state — simpler concurrency for free. |
| Deterministic flaky `PushChannel` | A randomly-failing channel | A reproducible demo is worth more for a reference implementation than realism from randomness. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — users/notifications reset on JVM exit.
- Synchronous, blocking send — no async queue/worker model.
- No backoff between retries.
- No permanent-vs-transient distinction on failures (see follow-ups).
- No user opt-out beyond channel ordering; no rate limiting.
- No idempotency keys for caller-side retries.
- No channel-specific content templating.
