# E-commerce Website LLD

A product catalog, stock-checked multi-item order placement, pluggable
payment methods (including deferred payment for cash-on-delivery), a
discount policy, and an order fulfillment lifecycle. Two implementations
live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleEcommerceInterview.java`) — a
  single ~100-line file covering stock-checked ordering with a discount
  and cancellation, for when you only have 15-20 minutes or want a
  warm-up before the full design.

## Problem statement

> Design an e-commerce website: browse a product catalog, place an order
> for multiple products (subject to stock), pay via one of several
> payment methods, and track the order through shipping and delivery,
> with support for cancellation.

## Clarifying questions to ask in the first 10 minutes

- Multiple payment methods, and do they all behave the same way (charge
  immediately)? (Modeled here: **no** — `PaymentStrategy.isPayNow()`
  distinguishes immediate-charge methods (credit card, wallet) from
  cash-on-delivery, which defers payment and leaves the order in
  `PLACED` rather than `PAID` until delivery.)
- Is a multi-item order all-or-nothing on stock, like the Bookstore LLD's
  ordering, or can it partially fulfill? (Modeled here: **all-or-nothing**
  — the same reserve-then-rollback-on-failure shape as the Bookstore
  LLD's `placeOrder`, extended here to also roll back if *payment* fails
  after stock was already reserved.)
- What's the order lifecycle, and which transitions are valid? (Modeled
  here: `PLACED → PAID → SHIPPED → DELIVERED`, with `CANCELLED` reachable
  from `PLACED` or `PAID` but not after shipping — each transition method
  validates the current status first.)
- Are discounts applied per-item or to the whole order subtotal? (Modeled
  here: **whole-order subtotal**, via a pluggable `DiscountStrategy`,
  applied once after all stock is reserved and before payment is
  charged.)
- Should payment failure roll back the stock reservation? (Modeled here:
  **yes** — if `PaymentStrategy.pay` throws, every product reserved for
  that order is restocked before the exception propagates.)

## Class design

```
model/
  Product        id, name, category, price, mutable stock
  Address        street, city, zip
  OrderItem      product, quantity, unitPrice (snapshotted), lineTotal
  OrderStatus    enum: PLACED, PAID, SHIPPED, DELIVERED, CANCELLED
  Order          id, customerId, items, shippingAddress, totalAmount,
                   paymentMethod (name), mutable status

strategy/
  PaymentStrategy    isPayNow(), pay(amount), getName()
                       + CreditCardPaymentStrategy / WalletPaymentStrategy
                         (both isPayNow() == true)
                       + CashOnDeliveryPaymentStrategy (isPayNow() == false)
  DiscountStrategy   apply(subtotal) -> discounted total
                       + NoDiscountStrategy / PercentageOffDiscountStrategy

service/
  ProductCatalogService   + InMemoryProductCatalogService   add/search/get products
  OrderService            + InMemoryOrderService   placeOrder (reserve stock
                                                       + discount + pay, with
                                                       rollback on failure),
                                                       shipOrder, deliverOrder,
                                                       cancelOrder, order
                                                       history;
                                                       OrderPlacementException /
                                                       InvalidOrderStateException

driver/
  EcommerceDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Builder** | `Order.builder()` | Seven args including two adjacent `String`s (`customerId`, `paymentMethod`) and a status that should default to `PLACED`. Named setters, a default for the status, and one-place validation (non-empty items, non-negative total) replace a constructor that was easy to call wrong and impossible to call partially. |
| **Strategy** | `PaymentStrategy` (credit card/wallet/COD) and `DiscountStrategy` (none/percentage-off) | "How do we charge" and "what discount applies" are the two variable policies this problem calls out — isolating each keeps `OrderService` ignorant of the actual payment/discount mechanics, and either can be swapped or added to without touching order placement logic. |
| **Behavioral flag distinguishing strategy variants** | `PaymentStrategy.isPayNow()` | Rather than a type-check (`if (strategy instanceof CashOnDeliveryPaymentStrategy)`), `OrderService` asks the strategy how it behaves — a small but real application of "tell, don't ask," and it means a *new* deferred-payment method (e.g. "pay in 30 days" invoicing) needs zero changes to `InMemoryOrderService`. |
| **Reserve-then-rollback across two failure points** | `InMemoryOrderService.placeOrder` | Extends the Bookstore LLD's single-rollback-point pattern to two: stock reservation can fail (insufficient inventory) *or* payment can fail (declined card) — either failure rolls back everything reserved so far, keeping the operation atomic from the caller's perspective. |
| **Repository-ish interface + impl** | `ProductCatalogService`/`InMemoryProductCatalogService`, `OrderService`/`InMemoryOrderService` | Same DIP shape as the rest of this repo's LLDs. |

## SOLID mapping

- **SRP** — `Product` only tracks catalog data and stock; `OrderService`
  only orchestrates placement/lifecycle; payment math lives only in
  `PaymentStrategy` impls; discount math lives only in `DiscountStrategy`
  impls.
- **OCP** — a new payment method (e.g. `BuyNowPayLaterStrategy`) or
  discount type (e.g. a fixed-amount-off coupon) can be added without
  touching `InMemoryOrderService`.
- **LSP** — every `PaymentStrategy` implements `isPayNow()`/`pay()`/
  `getName()` identically from the caller's perspective; every
  `DiscountStrategy` takes a subtotal and returns a discounted total —
  `InMemoryOrderService` treats every implementation of each
  interchangeably.
- **ISP** — `OrderService` exposes five narrow lifecycle operations
  instead of one catch-all `processOrder(...)`.
- **DIP** — `InMemoryOrderService` depends on `ProductCatalogService`
  (interface), not `InMemoryProductCatalogService` (impl); both
  strategies are injected per call, never hardcoded.

## Core algorithms

### Order placement with two rollback points (`InMemoryOrderService.placeOrder`)

1. For each `(productId, quantity)`, reserve stock (`Product.adjustStock(-qty)`)
   — if any item's stock is insufficient, roll back every reservation
   made so far for this order and throw.
2. Compute the subtotal, apply the injected `DiscountStrategy`.
3. Attempt payment via the injected `PaymentStrategy`. **If payment
   fails, the stock reservations from step 1 are also rolled back** —
   a customer's failed credit card should never leave inventory
   decremented for an order that was never actually completed.
4. Set the order's initial status based on `paymentStrategy.isPayNow()`
   — `PAID` for immediate-charge methods, `PLACED` (awaiting collection)
   for cash-on-delivery.

### Lifecycle transitions

`shipOrder` requires `PAID`; `deliverOrder` requires `SHIPPED`;
`cancelOrder` only works from `PLACED` or `PAID` (not after shipping,
since the product has physically left the warehouse by then) — each
guard mirrors the pattern used by `ReservationStatus` in the Hotel LLD and
`OrderStatus` in the Bookstore LLD.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/ecommerce -name "*.java")

java -cp out ecommerce.driver.EcommerceDriver
java -cp out ecommerce.interview.SimpleEcommerceInterview
```

`EcommerceDriver` demonstrates: seed a 2-product catalog → Alice orders
both products by credit card with a 10% coupon (`PAID` immediately) →
Bob's 5-keyboard order fails (only stock for fewer, nothing charged) →
Bob orders 1 keyboard cash-on-delivery (stays `PLACED`, not `PAID`) →
Alice's order is shipped then delivered → an attempt to ship Bob's still-
`PLACED` COD order fails → Bob's order is cancelled, restoring keyboard
stock → print Alice's order history.

`SimpleEcommerceInterview` is the same golden path (stock-checked order +
discount → over-order failure → cancel-and-restock) in one file, with no
interfaces, payment methods, or order lifecycle beyond placement — useful
as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How do you handle a payment that succeeds but the confirmation is
  lost (network timeout)?"** — Not modeled; `pay()` is a synchronous
  call assumed to either clearly succeed or clearly fail. A real payment
  integration needs idempotency keys and a reconciliation step for the
  ambiguous "did it actually charge" case — worth naming as a real gap
  between this demo and production payment handling.
- **"Why does cash-on-delivery skip straight to `PLACED` instead of some
  `AWAITING_PAYMENT` state?"** — `PLACED` *is* effectively "awaiting
  payment" for COD orders in this model; a real system might want a more
  explicit status to distinguish "awaiting COD collection" from "awaiting
  a retried card charge," which would be a straightforward enum addition,
  not a redesign.
- **"How would you support partial shipments (some items ship before
  others are back in stock)?"** — Not modeled; `Order` has one status for
  the whole order. Real partial-shipment support needs per-`OrderItem`
  status, with the order's overall status derived from its items'
  states — a materially bigger model change worth flagging as such.
- **"Concurrent orders depleting the last unit of a popular product?"**
  — `Product.tryReserve` is an atomic check-and-decrement synchronized on
  that specific product, so two orders racing for the last units can't
  both pass the check and over-sell; orders for a *different* product
  never contend at all. See the Concurrency section below.
- **"How do you prevent a discount from being combined in unintended ways
  (e.g. stacking two coupons)?"** — `placeOrder` accepts exactly one
  `DiscountStrategy` per call; stacking would require a
  `CompositeDiscountStrategy` that itself implements `DiscountStrategy`
  by chaining others — the interface already supports this without
  changes to `OrderService`.

## Concurrency & thread-safety

- **Per-product atomic reservation** (`Product.tryReserve`) — the stock
  check and the decrement happen under one monitor per product, closing
  the race that used to exist between a separate `getStock()` check and
  `adjustStock()` call. Orders touching *different* products never
  contend.
- **`Order.tryTransition`/`tryCancelFrom`** — compare-and-set for order
  status, so `shipOrder`/`deliverOrder`/`cancelOrder` racing each other on
  the same order (e.g. a shipment racing a cancellation) can't both
  succeed and leave inconsistent side effects.
- **`ConcurrentHashMap` for the product catalog and order store** — safe
  for concurrent reads (catalog browsing, order history) alongside writes
  (new orders, stock changes).
- **What's still a known trade-off:** as in the Bookstore LLD, a
  multi-item order reserves each product atomically but the order as a
  whole isn't reserved atomically *across* items — acceptable here since
  rollback on any single-item failure restores exactly what this order
  reserved.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Per-product lock (`Product.tryReserve`) over a whole-service lock | `synchronized` on `InMemoryOrderService.placeOrder` | A service-wide lock would serialize every order in the store behind one monitor, even for unrelated products; per-product locking scopes contention to only the product two orders are actually racing over. |
| `isPayNow()` flag on `PaymentStrategy` instead of type-checking for COD | `instanceof CashOnDeliveryPaymentStrategy` check in `OrderService` | Keeps `OrderService` closed for modification when a new deferred-payment method is added — it only ever asks the strategy how it behaves, never which concrete class it is. |
| Two rollback points (stock, then payment) in one method | Separate "reserve" and "charge" steps exposed as two public API calls | A single `placeOrder` call keeps the all-or-nothing guarantee simple to reason about for callers; splitting it into two calls would push the rollback responsibility onto every caller instead of centralizing it. |
| Discount applied to the whole-order subtotal, once | Per-item discounts | Matches how most real storefront coupons work (a percentage off the cart total) and keeps `DiscountStrategy`'s interface trivial (`double -> double`) rather than needing per-item context. |
| One `OrderService` handling both catalog-backed stock and payment/lifecycle | Split into a separate `InventoryService` (as in the Bookstore LLD) | A deliberate simplification here to keep the file count reasonable — `Product.adjustStock` is called directly rather than through an intermediary service; the Bookstore LLD in this repo shows the alternative (`InventoryService` as its own interface) for comparison. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — catalog/orders reset on JVM exit.
- No partial shipments (one status per whole order, not per item).
- No idempotency handling for ambiguous payment outcomes.
- No coupon-stacking / discount-composition support (though the
  interface would allow it, as noted above).
- No tax calculation or shipping cost — `totalAmount` is purely
  product subtotal minus discount.
- No refund flow tied to cancellation (stock is restored, but no
  payment reversal is modeled for already-`PAID` orders).
