# Food Delivery System LLD

Restaurants with menus, an order lifecycle from placement through
delivery, and pluggable delivery-agent assignment. Two implementations
live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleFoodDeliveryInterview.java`) — a
  single ~90-line file covering nearest-agent assignment and delivery
  completion, for when you only have 15-20 minutes or want a warm-up
  before the full design.

## Problem statement

> Design a food delivery system: customers place an order from a
> restaurant's menu, the restaurant prepares it, a delivery agent is
> assigned and delivers it, and the system tracks the order through each
> stage.

## Clarifying questions to ask in the first 10 minutes

- What are the distinct stages an order moves through, and can they be
  skipped? (Modeled here: **`PLACED → PREPARING → OUT_FOR_DELIVERY →
  DELIVERED`**, strictly in order — each transition method checks the
  order is in exactly the expected prior status before advancing it.)
- How is a delivery agent picked — nearest to the restaurant, or load-
  balanced across the fleet? (Modeled here: **both**, as swappable
  `DeliveryAssignmentStrategy` implementations — `NearestAgentStrategy`
  and `LeastBusyAgentStrategy`, mirroring the nearest-vs-load-balanced
  duality also seen in this repo's Parking Lot and Elevator LLDs.)
- Is a delivery agent assigned at order placement, or only once the
  restaurant confirms it's ready? (Modeled here: **only once
  `PREPARING`** — `assignDelivery` requires the order already be marked
  preparing, so an agent isn't sent to wait at a restaurant before the
  food exists.)
- Can an order be cancelled after an agent is already delivering it?
  (Modeled here: **no** — `cancelOrder` only succeeds from `PLACED` or
  `PREPARING`, matching the same shape as `OrderStatus` guards in the
  Bookstore and E-commerce LLDs.)
- One agent per delivery, or could a single agent batch multiple orders?
  (Modeled here: **one delivery at a time per agent** — an assigned agent
  is marked unavailable until `completeDelivery`; see Known Gaps for
  batching.)

## Class design

```
model/
  Location            x, y (flat-plane coordinates); distanceTo() via Euclidean distance
  MenuItem            id, name, price
  Restaurant          id, name, Location, Map<itemId, MenuItem>
  DeliveryAgent       id, name, mutable Location, mutable available flag,
                        completedDeliveries counter (for load-balanced assignment)
  OrderStatus         enum: PLACED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
  FoodOrderItem       menuItem, quantity, lineTotal
  FoodOrder           id, customerId, restaurant, deliveryLocation, items,
                        totalPrice, mutable status, mutable assignedAgent

strategy/
  DeliveryAssignmentStrategy   + NearestAgentStrategy / LeastBusyAgentStrategy

observer/
  OrderStatusListener        onStatusChanged(order, from, to)
  OrderEventPublisher        subscribe/publish (the subject)
  CustomerTrackingListener   one concrete observer: the customer's app screen

service/
  RestaurantService        + InMemoryRestaurantService        add/get restaurants
  DeliveryAgentRegistry    + InMemoryDeliveryAgentRegistry     register/locate/
                                                                  toggle agent
                                                                  availability
  OrderService             + InMemoryOrderService              full order
                                                                  lifecycle:
                                                                  placeOrder,
                                                                  markPreparing,
                                                                  assignDelivery,
                                                                  completeDelivery,
                                                                  cancelOrder;
                                                                  depends on
                                                                  RestaurantService +
                                                                  DeliveryAgentRegistry;
                                                                  OrderException

driver/
  FoodDeliveryDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Observer** | `OrderEventPublisher` + `OrderStatusListener`; `OrderService` publishes after every successful status CAS; `CustomerTrackingListener` subscribes | "Real-time order tracking" is literally in the prompt. Polling `getOrder()` on a timer is the naive answer; pushing each committed transition to subscribers (customer app, kitchen display, agent app) is the real one, and `OrderService` doesn't know any of them exist. **Why publish after the CAS, not before:** exactly one thread wins `tryTransition`, so exactly one notification goes out per change — the CAS is the dedup. |
| **Strategy** | `DeliveryAssignmentStrategy` (`NearestAgent`/`LeastBusyAgent`) | "Which agent gets this delivery" is the headline design question, explicitly called out in the prompt ("algorithms to achieve order assignment... to guarantee timely delivery") — isolating it keeps `OrderService` ignorant of the actual dispatch algorithm, and a fairness-vs-speed trade-off (nearest vs. least-busy) is expressible without touching order logic. |
| **Explicit state machine via enum + guarded transitions** | `OrderStatus` + `markPreparing`/`assignDelivery`/`completeDelivery`/`cancelOrder`, each validating the current status first | Same shape as `ReservationStatus` in the Hotel LLD and `OrderStatus` in the Bookstore/E-commerce LLDs — a linear lifecycle where each step can only be reached from the correct prior step, so an order can't jump straight from `PLACED` to `OUT_FOR_DELIVERY` without ever being prepared. |
| **Registry + business-service split** | `DeliveryAgentRegistry` (who exists, where they are, are they free) separate from `OrderService` (order lifecycle) | Same split as `DriverRegistry`/`RideService` in the Ride-Sharing LLD — agent location/availability tracking is a different concern from order orchestration, and `OrderService` depends on the registry rather than owning agent state itself. |
| **Repository-ish interface + impl** | `RestaurantService`/`InMemoryRestaurantService`, all the above services | Same DIP shape as the rest of this repo's LLDs. |

## SOLID mapping

- **SRP** — `Restaurant` only holds a menu; `DeliveryAgentRegistry` only
  tracks agent location/availability; `OrderService` only orchestrates
  the order lifecycle; assignment policy lives only in
  `DeliveryAssignmentStrategy` impls.
- **OCP** — a new assignment policy (e.g. "prefer an agent already
  delivering to the same neighborhood") can be added without touching
  `InMemoryOrderService`.
- **LSP** — every `DeliveryAssignmentStrategy` takes
  `(List<DeliveryAgent>, Location)` and returns
  `Optional<DeliveryAgent>` — `InMemoryOrderService` treats both
  implementations identically.
- **ISP** — `OrderService` exposes six narrow lifecycle operations
  instead of one catch-all `processOrderEvent(...)`.
- **DIP** — `InMemoryOrderService` depends on `RestaurantService` and
  `DeliveryAgentRegistry` (interfaces), not their `InMemory*`
  implementations; the assignment algorithm is injected per call.

## Core algorithms

### Order lifecycle guards (`InMemoryOrderService`)

Every transition method (`markPreparing`, `assignDelivery`,
`completeDelivery`, `cancelOrder`) goes through
`FoodOrder.tryTransition`/`tryAssignAndTransition` — an atomic
compare-and-set on the order's status — before mutating anything, so
calling `assignDelivery` on an order that's still `PLACED` (not yet
`PREPARING`) fails loudly with a clear message instead of silently
sending an agent to a restaurant with nothing ready to hand off.

### Delivery assignment (`assignDelivery`)

1. Require the order is `PREPARING`.
2. Ask the injected `DeliveryAssignmentStrategy` to pick from
   `agentRegistry.getAvailableAgents()` — an `O(available agents)`,
   lock-free, read-only scan, fine at demo scale.
3. Atomically claim the chosen agent (`DeliveryAgent.tryClaim`) and, if
   that succeeds, atomically attach them to the order and transition it
   to `OUT_FOR_DELIVERY` (`FoodOrder.tryAssignAndTransition`) — see the
   Concurrency section below for why these are two separate atomic steps
   rather than one bigger lock, and what happens if the second step loses
   a race.

### Load-balanced assignment (`LeastBusyAgentStrategy`)

Sorts available agents by `completedDeliveries` ascending (fewest
deliveries first), tie-broken by distance to the restaurant — spreads
work evenly across the fleet over a shift rather than always picking the
single closest agent, which would otherwise get every order near a
popular restaurant.

## Concurrency & thread-safety

- **Per-agent atomic claim** (`DeliveryAgent.tryClaim`) — the
  availability check and the flip to unavailable happen under that
  agent's own monitor, so two orders being assigned concurrently can't
  both claim the same agent from a race in the read-only
  `selectAgent` scan.
- **Per-order atomic transition, with agent release on a lost race**
  (`assignDelivery`) — after claiming an agent, `assignDelivery` still
  has to attach it to the order and flip the order to
  `OUT_FOR_DELIVERY` atomically (`tryAssignAndTransition`). If the
  *order's* status changed out from under it in that narrow window (e.g.
  it was cancelled a moment earlier), the method releases the
  already-claimed agent (`agent.setAvailable(true)`) before failing —
  claiming a resource and then failing to use it must not leak that
  resource as permanently unavailable.
- **No single lock spanning "select a candidate agent" and "commit the
  order"** — unlike Airline (which locks one `Flight` for its whole
  assign-price-reserve sequence because the search is already scoped to
  one flight), agent selection here scans *all* available agents across
  the whole fleet — locking that whole scan would serialize every
  assignment fleet-wide. Two independent atomic claims (agent, then
  order) plus a compensating release on failure gets the same
  correctness without that bottleneck.
- **`ConcurrentHashMap` throughout** — `orders`, the agent registry, the
  restaurant store, and each restaurant's menu are all
  `ConcurrentHashMap`s, safe for concurrent reads (order history, agent
  availability, menu lookups) alongside writes.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/fooddelivery -name "*.java")

java -cp out fooddelivery.driver.FoodDeliveryDriver
java -cp out fooddelivery.interview.SimpleFoodDeliveryInterview
```

`FoodDeliveryDriver` demonstrates: one restaurant, two delivery agents →
Alice orders, the restaurant marks it preparing, and the nearest agent
(Sam) is assigned → Bob orders while Sam is busy, so the only remaining
agent (Nina) is assigned via the load-balancing strategy → a third order
from Carol fails to find any agent (both busy) → Sam completes Alice's
delivery and becomes available again → Carol's order can now be assigned
to Sam.

`SimpleFoodDeliveryInterview` is the same golden path (nearest-agent
assignment → both-busy failure → completion frees an agent → retry
succeeds) in one file, with no interfaces, restaurant menus, or full
order lifecycle beyond assignment/delivery — useful as a live-coding
warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How would you estimate and show delivery ETA to the customer?"** —
  Not modeled; you'd compute `agent.location.distanceTo(restaurant)` +
  `restaurant.location.distanceTo(deliveryLocation)` and convert to a
  time estimate via an assumed speed — the distance primitives already
  exist on `Location`, so this is additive, not a redesign.
- **"How do you handle an agent going offline mid-delivery (app crash,
  phone dies)?"** — Not modeled; a real system needs a heartbeat/timeout
  mechanism that detects a stalled `OUT_FOR_DELIVERY` order and
  reassigns it to a new agent — `OrderService` would need a
  `reassignDelivery(orderId, newStrategy)` that releases the stale
  agent and re-runs assignment, structurally similar to `assignDelivery`
  itself.
- **"Can a delivery agent handle multiple orders from the same restaurant
  at once (batching)?"** — Not modeled — one agent is unavailable per
  delivery. Batching would need `DeliveryAgent` to track a *list* of
  in-progress orders instead of a boolean, and the assignment strategy
  would need a capacity check instead of a pure availability filter.
- **"How do you scale agent assignment to a whole city with thousands of
  agents?"** — The current linear scan over all available agents is fine
  at demo scale; a real system would geo-index agents (grid/geohash) so
  assignment only considers agents near the restaurant, the same scaling
  answer as the Ride-Sharing LLD's matching discussion.
- **"What about restaurant capacity — can a restaurant be overwhelmed
  with orders it can't prepare in time?"** — Not modeled; you'd add a
  per-restaurant queue/capacity limit and have `placeOrder` reject or
  queue new orders once a restaurant is at capacity, rather than
  accepting unlimited concurrent `PLACED` orders.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Agent assignment only happens after `PREPARING`, not at placement | Assign an agent immediately at order placement | Avoids an agent sitting idle waiting at a restaurant for food that isn't ready yet — matches how real delivery apps sequence this. |
| `LeastBusyAgentStrategy` uses a lifetime `completedDeliveries` counter | Track "currently active deliveries" (always 0 or 1 per agent here) | Since this model only allows one active delivery per agent at a time, "currently busy" can't differentiate agents — a lifetime counter is the meaningful fairness signal for load-balancing across a shift. |
| Euclidean distance on a flat `Location` | Real road distance/ETA via a routing API | Same simplification as the Ride-Sharing LLD, for the same reason: keeps assignment math trivial to read and test, with the trade-off named explicitly rather than hidden. |
| `DeliveryAgentRegistry` separate from `OrderService` | Fold agent tracking directly into `OrderService` | Mirrors the Ride-Sharing LLD's `DriverRegistry`/`RideService` split — agent state (who exists, where, are they free) is a different concern from order orchestration, and keeping them separate means either can evolve independently. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — restaurants/agents/orders reset on JVM exit.
- Status changes are pushed to the customer via Observer, but there's no
  ETA calculation or live agent-location tracking.
- No agent-offline/stalled-delivery detection or reassignment.
- No delivery batching (one order per agent at a time).
- No restaurant capacity limits.
- No payment processing.
- Euclidean distance, not real road distance/ETA.
