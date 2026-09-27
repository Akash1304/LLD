# Online Bookstore LLD

A bookstore catalog with search, stock-aware ordering, and cancellation.
Two implementations live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleBookstoreInterview.java`) — a
  single ~100-line file covering search, ordering with a stock check, and
  cancellation, for when you only have 15-20 minutes or want a warm-up
  before the full design.

## Problem statement

> Design an online bookstore: customers can search the catalog by title,
> author, or subject, place an order for one or more books (subject to
> stock availability), and cancel an order to restore stock.

## Clarifying questions to ask in the first 10 minutes

- Search by title/author/subject — exact match or substring/fuzzy match,
  and can criteria be combined? (Modeled here: case-insensitive substring
  match per field as composable `BookSpecification`s — `author AND
  subject AND price < 50` is built from leaf specs, not a new class.)
- What happens if an order contains multiple books and only some are in
  stock — reject the whole order, or partially fulfill it? (Modeled here:
  **reject the whole order** and roll back any partial reservations — an
  all-or-nothing transaction.)
- Is pricing flat per book, or are there discounts (bulk, member, coupon)?
  (Modeled here: pluggable `PricingStrategy`, with a bulk-quantity discount
  as the second implementation.)
- Does cancelling an order restore stock immediately, and can any order be
  cancelled or only recently-placed ones? (Modeled here: any `PLACED`
  order can be cancelled, which restores stock immediately; a fulfilled
  order can't be re-cancelled — see Known Gaps for what "fulfilled" would
  actually require.)
- Does inventory management include restocking from a supplier, or just
  decrementing on sale? (Modeled here: both — `InventoryService.restock`
  is used both by cancellation and would back a supplier restock flow.)

## Class design

```
model/
  Book          isbn, title, author, subject, price, mutable stock
  Customer      id, name, email
  OrderItem     book, quantity, unitPrice (snapshotted at order time), lineTotal
  Order         id, customer, items, total, placedAt, mutable OrderStatus
  OrderStatus   enum: PLACED, CANCELLED, FULFILLED

service/
  CatalogService     + InMemoryCatalogService    add/get/search/list books
  InventoryService   + InMemoryInventoryService  reserve/restock/check stock;
                                                    depends on CatalogService
  OrderService       + InMemoryOrderService      place/cancel orders, all-or-
                                                    nothing stock reservation,
                                                    pricing via PricingStrategy;
                                                    includes InsufficientStockException

specification/
  BookSpecification    isSatisfiedBy(Book) + default and()/or()/not()
  BookSpecifications   leaf specs: titleContains, authorContains,
                         subjectContains, priceBelow, inStock

strategy/
  PricingStrategy   + StandardPricingStrategy / BulkDiscountPricingStrategy

driver/
  BookstoreDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Specification** | `BookSpecification` (composable predicate) + `BookSpecifications` (leaf specs); `CatalogService.search(spec)` | An earlier version had three near-identical `*SearchStrategy` classes that couldn't be combined — "author AND subject" meant a fourth class, and every new filter meant another. A specification is a predicate with `and`/`or`/`not`, so any query the UI can express composes from a handful of one-line leaves, and the catalog sees one interface. **Why not Strategy here:** Strategy swaps *one* algorithm; search criteria need to be *combined*, which Strategy has no vocabulary for. |
| **Strategy** | `PricingStrategy` (standard/bulk-discount) | Pricing *is* one interchangeable algorithm over the order — the correct use of Strategy. |
| **Repository-ish interface + impl** | `CatalogService`/`InMemoryCatalogService`, similarly for `InventoryService`/`OrderService` | Storage sits behind an interface so a real DB-backed catalog later doesn't change callers — same DIP shape as the calendar/parking-lot LLDs. |
| **Saga-style rollback** | `InMemoryOrderService.placeOrder` | Reserves stock item-by-item; if any item fails, it un-reserves everything already taken for that order before rethrowing — keeps the multi-item order atomic without a real transaction manager. |
| **Constructor injection** | `InMemoryInventoryService(CatalogService)`, `InMemoryOrderService(CatalogService, InventoryService, PricingStrategy)` | No service reaches for a static/global catalog; dependencies are explicit and swappable in tests. |

## SOLID mapping

- **SRP** — `CatalogService` only searches/stores books; `InventoryService`
  only manages stock counts; `OrderService` only orchestrates
  order placement/cancellation; pricing math lives only in
  `PricingStrategy` impls.
- **OCP** — a new discount (e.g. `MemberDiscountPricingStrategy`) or a new
  search criterion (a new leaf in `BookSpecifications`) can be added
  without touching `OrderService` or `CatalogService`.
- **LSP** — every `BookSpecification` (leaf or composed) is a
  `Book -> boolean`; every `PricingStrategy` takes `List<OrderItem>` and
  returns a `double` — `CatalogService` and `InMemoryOrderService` treat
  all implementations identically.
- **ISP** — `InventoryService` exposes three narrow stock operations
  instead of one catch-all `updateInventory(...)`.
- **DIP** — `InMemoryInventoryService` and `InMemoryOrderService` depend
  on `CatalogService` (interface), not `InMemoryCatalogService` (impl).

## Core algorithms

### Order placement (`InMemoryOrderService.placeOrder`)

1. For each `(isbn, quantity)` in the cart, look up the `Book` and call
   `InventoryService.reserveStock` — this decrements stock immediately if
   enough is available, or returns `false`.
2. If any reservation fails, **roll back** every reservation already made
   for this order (restock what was taken) and throw
   `InsufficientStockException` — the customer sees one all-or-nothing
   failure, not a partially-filled cart.
3. Otherwise, compute the total via the injected `PricingStrategy` and
   create the `Order`.

### Cancellation (`InMemoryOrderService.cancelOrder`)

Only `PLACED` orders can be cancelled; cancelling flips the status to
`CANCELLED` and restocks every item — symmetric with the reservation step
above.

### Search

`CatalogService.search(spec)` is a linear filter of the catalog through
the composed `BookSpecification` — `O(n)` per query. Fine for an interview-sized
catalog; see the trade-off table for what a real system would do instead.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/bookstore -name "*.java")

java -cp out bookstore.driver.BookstoreDriver
java -cp out bookstore.interview.SimpleBookstoreInterview
```

`BookstoreDriver` demonstrates: seed a 3-book catalog → search by author →
place an order for 2 books that crosses the bulk-discount threshold (5+
total units) → attempt to over-order a nearly-sold-out book (fails,
nothing is charged) → cancel the first order and confirm stock is
restored.

`SimpleBookstoreInterview` is the same golden path (search → order with
stock check → over-order failure → cancel-and-restock) in one file, with
no interfaces or discount tiers — useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How would you scale search to millions of books?"** — Replace the
  linear specification filter with a real search index (e.g.
  Elasticsearch, or at minimum an in-memory inverted index
  `Map<word, Set<isbn>>` built at ingest time) — `CatalogService`'s
  interface wouldn't need to change, only `InMemoryCatalogService`'s
  internals.
- **"What about concurrent orders for the last copy of a book?"** —
  `Book.tryReserve` is `synchronized` *on the `Book` instance*, so the
  check-then-decrement is atomic and two threads can't both reserve the
  last unit — one wins, the other gets `false`. This is deliberately a
  per-book lock, not a lock on the whole `InventoryService`: reserving
  stock for one ISBN never blocks a concurrent reservation for a
  different ISBN, unlike an earlier version of this design that
  synchronized the whole service (see the Concurrency section below).
- **"How would you add partial fulfillment instead of all-or-nothing?"**
  — `OrderItem` would need a per-line status (`FULFILLED`/`BACKORDERED`)
  instead of one order-wide status, and `placeOrder` would reserve what it
  can rather than rolling back on the first shortfall — a bigger design
  change than a tweak, worth naming as such rather than hand-waving.
- **"How do you handle payment failure after stock is reserved?"** — This
  design reserves stock synchronously with order placement and has no
  payment step; a real flow would reserve stock, attempt payment, and only
  then finalize the order — release the reservation (same `restock` path
  used by cancellation) if payment fails.
- **"Multiple warehouses/regions?"** — `Book.stock` is a single global
  counter; scaling to per-warehouse stock means `InventoryService` keyed
  by `(isbn, warehouseId)` and an added step to pick a warehouse per order
  — `CatalogService`/`OrderService`'s external contracts wouldn't need to
  change.

## Concurrency & thread-safety

- **Per-book atomic reservation** (`Book.tryReserve`) — the stock check
  and the decrement happen under the same monitor, one per `Book`
  instance, so concurrent reservations for *different* books never
  contend, and concurrent reservations for the *same* book can't both
  pass the check for the last copy.
- **`InventoryService`/`CatalogService` no longer lock the whole
  service** — `reserveStock`/`restock` delegate straight to `Book`'s own
  atomic methods; the service itself holds no lock, so it can't
  accidentally serialize unrelated books behind one monitor the way a
  `synchronized` service method would.
- **`ConcurrentHashMap` for the book catalog and the order store** — both
  `booksByIsbn` (in `InMemoryCatalogService`) and `orders` (in
  `InMemoryOrderService`) are `ConcurrentHashMap`s rather than
  `HashMap`/`LinkedHashMap`, since both are read and written from
  multiple call paths (catalog browsing concurrently with new orders).
- **What's still a known trade-off:** a multi-item order's reservations
  happen one book at a time, each atomically, but the order *as a whole*
  isn't reserved atomically across items — a concurrent order for a
  different customer could interleave its own reservations between this
  order's items. That's fine here because rollback on any single-item
  failure restores exactly what this order reserved (see reserve-then-
  rollback below); it would matter more if two items needed to be
  reserved-or-neither *together* for reasons beyond stock (e.g. a
  bundle deal), which isn't a requirement this problem has.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Reserve-then-rollback for multi-item orders | Two-phase check-then-commit across all items | Simpler to implement correctly under interview time pressure; rollback only runs on the (rare) failure path, so the common case stays a single pass. |
| Per-book lock (`Book.tryReserve`) over a whole-service lock | `synchronized` on `InventoryService`'s reserve/restock methods | A service-wide lock serializes every reservation in the store behind one monitor, even for unrelated books; a per-book lock scopes contention to only the specific book two orders are actually racing over. |
| `PricingStrategy` computed once, at order placement | Store a discount rate on `Order` and recompute later | Total is locked in at purchase time, matching how real checkouts snapshot price — a later change to `BulkDiscountPricingStrategy`'s threshold shouldn't retroactively change past orders. |
| Unit price snapshotted on `OrderItem` at construction | Always read `book.getPrice()` live | If `Book.price` changes after the order is placed (a price update), historical orders shouldn't silently reprice. |
| Linear-scan `BookSpecification` filter | Pre-built inverted index per field | Matches the O(n) scope expected in a coding round; flagged explicitly as the first thing to change at scale (see follow-ups). |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — catalog/orders reset on JVM exit.
- No payment processing — an order is "placed" with no charge step.
- No partial fulfillment / backorder support.
- No per-warehouse or regional inventory.
- `OrderStatus.FULFILLED` is defined but nothing in the driver transitions
  an order into it (no shipping/fulfillment flow is modeled).
- Catalog search is O(n) per query — fine at interview scale, not at
  real-catalog scale (see follow-ups).
