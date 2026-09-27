# Shopping Cart System LLD

A per-customer, mutable pre-checkout cart: add/update/remove line items,
a running subtotal, pluggable promo/discount rules, and a checkout step
that snapshots the cart into an immutable receipt and clears it. Two
implementations live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleShoppingCartInterview.java`) — a
  single ~90-line file covering line-item mutation, a minimum-spend
  discount, and checkout, for when you only have 15-20 minutes or want a
  warm-up before the full design.

## Problem statement

> Design a shopping cart: a customer can add products to their cart,
> change quantities, remove items, see a running total, and check out —
> applying any active promo/discount — which finalizes the order and
> empties the cart.

## How this differs from the E-commerce Website LLD in this repo

This repo's [E-commerce LLD](../ecommerce/README.md) models placing a
multi-item order *directly* from the catalog in one call, with stock
reservation and payment as first-class concerns. **This LLD instead
models the cart itself as a standalone, mutable, multi-step object** a
customer builds up over several interactions before ever checking out —
no catalog-side stock tracking, no payment step. If an interviewer asks
for the shopping-cart problem specifically, the cart's own CRUD
lifecycle (and what "checkout" means as a transition from mutable cart to
immutable receipt) is the part they're evaluating, not order fulfillment.

## Clarifying questions to ask in the first 10 minutes

- Does adding a product already in the cart increase its quantity, or
  create a duplicate line? (Modeled here: **increases quantity** —
  `Cart.addItem` merges into the existing `CartItem` for that product
  rather than creating a second line for the same product.)
- Should setting quantity to zero remove the item, or is that an
  invalid operation? (Modeled here: **removes it** —
  `Cart.updateQuantity` treats `quantity <= 0` as "remove," matching how
  most real cart UIs let you drag a quantity stepper down to delete a
  line.)
- Is the discount applied to the cart itself (a coupon "attached" to the
  cart) or supplied at the moment of checkout? (Modeled here: **supplied
  at checkout** — `CheckoutService.checkout` takes a `DiscountStrategy`
  parameter, so the same cart could be priced differently depending on
  which promo code is active *right now*, without the cart itself needing
  to know about promos.)
- What happens to the cart after checkout — does it stay as a historical
  record, or reset for the next shopping session? (Modeled here: **reset**
  — `checkout` clears the cart after producing the `Receipt`, so the
  `Receipt` is the durable record, not the cart.)
- Can you check out an empty cart? (Modeled here: **no** —
  `EmptyCartException`.)

## Class design

```
model/
  Product     id, name, price
  CartItem    product, mutable quantity, getLineTotal()
  Cart        customerId, Map<productId, CartItem>; addItem (merges
                quantity if already present), updateQuantity (removes on
                <= 0), removeItem, clear, getSubtotal()
  Receipt     id, customerId, item snapshot, subtotal, total, checkedOutAt;
                getDiscountApplied() = subtotal - total

strategy/
  DiscountStrategy   + NoDiscountStrategy / PercentageDiscountStrategy /
                        MinimumSpendFlatDiscountStrategy ("$15 off orders
                        over $50" -- only applies past a threshold, unlike
                        a percentage discount which always applies)

service/
  CartService        + InMemoryCartService       getOrCreateCart, addItem,
                                                    updateQuantity,
                                                    removeItem, clearCart
  CheckoutService    + InMemoryCheckoutService    checkout(customerId,
                                                    discountStrategy) ->
                                                    Receipt; depends on
                                                    CartService;
                                                    EmptyCartException

driver/
  ShoppingCartDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `DiscountStrategy` (none/percentage/minimum-spend-flat) | Promo rules are exactly the kind of policy an interviewer will ask you to extend live ("now add a buy-2-get-1 rule") — isolating it means `CheckoutService` never needs to change, and the discount is chosen *per checkout call*, not baked into the cart. |
| **Mutable aggregate with an immutable snapshot on completion** | `Cart` (mutable, multi-step) → `Receipt` (immutable, produced once at checkout) | The cart's whole reason to exist is to be edited repeatedly before a decision is finalized; the receipt is the frozen record of that decision. Modeling them as two distinct types (rather than one object that's "sometimes still editable, sometimes not") makes illegal states — like mutating a completed order — unrepresentable. |
| **Merge-on-add semantics encapsulated in the aggregate** | `Cart.addItem` | The "does adding an existing product increase quantity or duplicate the line" rule lives in exactly one place (`Cart`), so `CartService` and any future caller get consistent behavior for free rather than each having to remember to check for an existing line first. |
| **Repository-ish interface + impl** | `CartService`/`InMemoryCartService`, `CheckoutService`/`InMemoryCheckoutService` | Same DIP shape as the rest of this repo's LLDs. |

## SOLID mapping

- **SRP** — `Cart` only manages line items and a subtotal; `Receipt` only
  holds a finalized snapshot; `CartService` only manages cart CRUD;
  `CheckoutService` only orchestrates the cart→receipt transition;
  discount math lives only in `DiscountStrategy` impls.
- **OCP** — a new promo rule (e.g. "buy 2 get 1 free," or a coupon-code
  lookup) can be added as a new `DiscountStrategy` without touching
  `CheckoutService` or `Cart`.
- **LSP** — every `DiscountStrategy` takes a `double` subtotal and
  returns a `double` total; `InMemoryCheckoutService` treats every
  implementation identically.
- **ISP** — `CartService` exposes five narrow line-item operations
  instead of one catch-all `updateCart(...)`.
- **DIP** — `InMemoryCheckoutService` depends on `CartService`
  (interface), not `InMemoryCartService` (impl); the discount is injected
  per call, never hardcoded.

## Core algorithms

### Merge-on-add (`Cart.addItem`)

```java
CartItem existing = itemsByProductId.get(product.getId());
if (existing != null) existing.setQuantity(existing.getQuantity() + quantity);
else itemsByProductId.put(product.getId(), new CartItem(product, quantity));
```

O(1) via the `Map<productId, CartItem>` backing structure — adding the
same product twice never produces two separate lines.

### Checkout (`InMemoryCheckoutService.checkout`)

1. Reject an empty cart outright.
2. Compute the subtotal from the cart's current items.
3. Apply the injected `DiscountStrategy` to get the final total.
4. Snapshot the cart's items into an immutable `Receipt`.
5. Clear the cart — the customer's next shopping session starts empty,
   and the `Receipt` is now the only record of what was purchased.

### Minimum-spend discount (`MinimumSpendFlatDiscountStrategy`)

`subtotal >= minimumSpend ? subtotal - flatDiscount : subtotal` — a
threshold-gated promo, structurally different from a percentage discount
(which always applies proportionally): it's an all-or-nothing unlock
based on cart value, a very common real-world cart promo shape ("free
shipping over $50," "$10 off orders over $75").

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/shoppingcart -name "*.java")

java -cp out shoppingcart.driver.ShoppingCartDriver
java -cp out shoppingcart.interview.SimpleShoppingCartInterview
```

`ShoppingCartDriver` demonstrates: Alice adds 3 products (merging
quantities where relevant) → she changes her mind, reducing one item's
quantity and removing another → she checks out with a "$15 off orders
over $50" promo (subtotal clears the threshold, discount applies) → her
cart is now empty → a second checkout attempt on the empty cart fails →
Bob checks out a single low-value item that doesn't clear the promo
threshold, so no discount applies.

`SimpleShoppingCartInterview` is the same golden path (add/merge → update
quantity → checkout with a threshold discount → empty-cart failure) in
one file, with no interfaces or a `Receipt` object (just a returned total)
— useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How is this different from just placing an order directly, like the
  E-commerce LLD?"** — Directly addressed above: the cart is a
  standalone, repeatedly-mutated object with its own lifecycle
  independent of catalog/inventory/payment concerns — the interviewer is
  usually specifically probing the CRUD-and-merge semantics of the cart
  itself, not order fulfillment.
- **"How would you support multiple stacked coupons?"** — `checkout`
  currently accepts exactly one `DiscountStrategy`; stacking would need a
  `CompositeDiscountStrategy` that itself implements `DiscountStrategy`
  by chaining several others in sequence — additive, no change needed to
  `CheckoutService`'s interface.
- **"What about stock/inventory checks before checkout?"** — Deliberately
  out of scope here (see the E-commerce LLD, which owns that concern);
  bolting it on would mean `CheckoutService` also depending on an
  inventory service and reserving stock before finalizing the receipt,
  the same shape as `InMemoryOrderService` in the E-commerce LLD.
- **"How do you handle the same customer using two devices/tabs
  simultaneously?"** — Every mutating and reading method on `Cart` is
  `synchronized` on that specific cart instance, so two concurrent
  `addItem` calls from two tabs for the *same* customer are serialized
  correctly (no lost update on `CartItem` quantity), while a different
  customer's cart is completely unaffected. See the Concurrency section
  below, including how `checkout` closes a subtler compound race across
  several cart calls.
- **"Should the cart persist across sessions (saved cart)?"** — Yes in
  most real products; this in-memory model already keeps one `Cart` per
  `customerId` for the life of the process (via
  `getOrCreateCart`), so "persisting" it is really just "back the map
  with a real database" — no structural change to `CartService`'s
  interface.
- **"How would you show cart abandonment / recover an abandoned cart?"**
  — Not modeled; you'd need a `lastModifiedAt` timestamp on `Cart` and a
  background job that flags carts untouched (and non-empty) past some
  threshold — additive to the existing model, not a redesign.

## Concurrency & thread-safety

- **Per-cart monitor** (`Cart`) — every method (`addItem`,
  `updateQuantity`, `removeItem`, `clear`, `isEmpty`, `getItems`,
  `getSubtotal`) is `synchronized` on that specific cart instance. Editing
  customer A's cart never blocks customer B's; only concurrent edits to
  the *same* cart (two open tabs, a retry after a network blip) serialize
  against each other.
- **`CartItem.setQuantity` is package-private** — it's only ever called
  from `Cart`'s own synchronized methods (same package), so a caller
  holding a `CartItem` obtained from `Cart.getItems()` has no way to
  mutate a line item's quantity directly, bypassing the cart's lock.
- **Checkout closes a compound, multi-call race, not just single-method
  races** (`InMemoryCheckoutService.checkout`) — `isEmpty`, `getSubtotal`,
  `getItems`, and `clear` are each individually atomic on `Cart`, but
  checkout needs *all four* to observe the same unchanging snapshot.
  Without an outer lock, a concurrent `addItem` landing between
  `getSubtotal()` and `clear()` could be silently wiped with no receipt
  ever reflecting it, or produce a receipt whose subtotal disagrees with
  its own item list. `checkout` wraps the whole sequence in
  `synchronized (cart)`, which composes safely with `Cart`'s own
  synchronized methods (Java monitors are reentrant) rather than needing
  a second, separate lock.
- **`ConcurrentHashMap` for the per-customer cart registry**
  (`InMemoryCartService.cartsByCustomerId`) — `computeIfAbsent`'s
  atomicity guarantee (the mapping function runs exactly once per key,
  even under concurrent calls) is specific to `ConcurrentHashMap`; a
  plain `HashMap`/`LinkedHashMap` doesn't provide it, so two concurrent
  `getOrCreateCart` calls for a brand-new customer could otherwise
  construct and register two different `Cart` objects for the same
  customer, silently losing whichever one loses the race.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Lock the whole checkout sequence on the cart, not just each Cart method individually | Rely on each of `isEmpty`/`getSubtotal`/`getItems`/`clear` being independently synchronized | Individually-atomic calls don't add up to an atomic *sequence* — a concurrent `addItem` between two of those calls could still corrupt the receipt or lose an item. The outer lock treats checkout as one indivisible operation. |
| Discount supplied at checkout time, not stored on the cart | A `Cart.appliedCoupon` field set whenever a promo code is entered | Keeps `Cart` a pure "what's in the basket" model with no awareness of pricing rules; also means the exact same cart state can be re-priced under different promos without mutating the cart, useful for "preview your total with/without this code" UX. |
| Setting quantity to `<= 0` removes the item | Throw an error and require an explicit `removeItem` call | Matches how most real cart UIs behave (drag a quantity stepper to zero and the line disappears) — one less special case for callers to handle. |
| Checkout clears the cart and produces a separate `Receipt` | Keep the same `Cart` object and just mark it "checked out" | Prevents the awkward "editable sometimes, frozen other times" object described above; a cleared cart is unambiguously ready for the next shopping session. |
| One cart per customer, created lazily (`computeIfAbsent`) | Explicit `createCart` call required before any item operations | Removes a whole category of "cart doesn't exist yet" errors from every caller — the very first `addItem` for a new customer just works. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — carts/receipts reset on JVM exit.
- No stock/inventory checks (see the E-commerce LLD for that concern).
- No payment processing.
- No coupon-code lookup/validation (a `DiscountStrategy` is handed in
  directly, not resolved from a code string).
- No cart expiration / abandoned-cart detection.
- No support for stacking multiple discounts in one checkout call.
