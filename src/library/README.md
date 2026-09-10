# Library Management System LLD

A library catalog with multiple physical copies per title, checkout/return,
and a pluggable late-fine policy. Two implementations live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleLibraryInterview.java`) — a single
  ~90-line file covering checkout, return, and a flat late fine, for when
  you only have 15-20 minutes or want a warm-up before the full design.

## Problem statement

> Design a library system: members can search the catalog, check out an
> available physical copy of a book, return it, and be charged a fine if
> the return is late.

## Clarifying questions to ask in the first 10 minutes

- Is "Book" the catalog title, or the physical copy on the shelf — and can
  a library own multiple copies of the same title? (Modeled here: **two
  separate entities** — `Book` is the catalog title, `BookItem` is one
  physical, individually-trackable copy with its own barcode and status.)
- How is the loan period determined — fixed for all books, or does it vary
  by book type (e.g. reference books can't leave the building, DVDs get a
  shorter loan)? (Modeled here: a single fixed 14-day period for
  everything — see Known Gaps.)
- Flat late fee, or does the fine escalate the longer a book is overdue?
  (Modeled here: both — `FlatDailyFineStrategy` and `TieredFineStrategy`
  are both implemented as swappable policies.)
- Can a member check out an unlimited number of books, or is there a
  per-member cap? (Modeled here: **no cap** — see Known Gaps.)
- What happens to a lost book — does it just disappear from the catalog,
  or does it accrue a replacement charge? (Modeled here: `BookItemStatus`
  has a `LOST` state defined but nothing transitions an item into it yet —
  flagged as a natural extension.)

## Class design

```
model/
  Book             isbn, title, author — the catalog entry (one per title)
  BookItem         barcode, Book, mutable BookItemStatus — one physical copy
  BookItemStatus   enum: AVAILABLE, LOANED, LOST
  Member           id, name
  Loan             id, bookItem, member, checkoutDate, dueDate, mutable returnDate

service/
  CatalogService   + InMemoryCatalogService   add books/items, search by
                                                title/author, find an
                                                available copy for an ISBN
  LoanService      + InMemoryLoanService      checkout/return, depends on
                                                CatalogService + FineStrategy;
                                                NoAvailableCopyException /
                                                LoanNotFoundException

strategy/
  FineStrategy     + FlatDailyFineStrategy / TieredFineStrategy

driver/
  LibraryDriver    end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `FineStrategy` (`FlatDaily`/`Tiered`) | Late-fee policy is exactly the kind of rule interviewers ask you to change mid-interview ("now make it escalate after a week") — isolating it behind one method keeps that a drop-in swap. |
| **Title vs. copy split** | `Book` vs. `BookItem` | The classic library-system modeling decision: search/browse operates on `Book` (one row per title), while checkout/availability operates on `BookItem` (one row per physical, independently-loanable copy). Conflating the two would make "2 copies, 1 checked out" impossible to represent. |
| **Repository-ish interface + impl** | `CatalogService`/`InMemoryCatalogService`, `LoanService`/`InMemoryLoanService` | Same DIP shape as the other LLDs in this repo — storage swap-out doesn't ripple into callers. |
| **Constructor injection** | `InMemoryLoanService(CatalogService, FineStrategy)` | `LoanService` doesn't construct its own catalog or fine policy — both are handed in, so tests can substitute either independently. |

## SOLID mapping

- **SRP** — `BookItem` only tracks one copy's status; `CatalogService`
  only searches/stores; `LoanService` only orchestrates checkout/return;
  fine math lives only in `FineStrategy` impls.
- **OCP** — a new fine policy (e.g. a flat cap after N days) or a new
  search field (e.g. by ISBN) can be added without touching `LoanService`
  or `CatalogService`.
- **LSP** — every `FineStrategy` takes `(Loan, LocalDate)` and returns a
  `double`; `InMemoryLoanService` treats both implementations identically.
- **ISP** — `LoanService` exposes three narrow operations
  (`checkout`, `returnBook`, `getActiveLoansForMember`) instead of one
  catch-all `processLoan(...)`.
- **DIP** — `InMemoryLoanService` depends on `CatalogService` (interface),
  not `InMemoryCatalogService` (impl).

## Core algorithms

### Checkout (`InMemoryLoanService.checkout`)

1. Ask `CatalogService.findAvailableItem(isbn)` for the first `BookItem`
   in `AVAILABLE` status for that title — `O(copies of that title)`, not
   `O(catalog size)`, since items are already grouped by ISBN.
2. Flip its status to `LOANED` and create a `Loan` with
   `dueDate = checkoutDate + 14 days`.
3. Both this method and `returnBook` are `synchronized`, so two members
   can't both "win" the same last available copy.

### Fine calculation

`FlatDailyFineStrategy`: `max(0, daysLate) * $0.50`.
`TieredFineStrategy`: first 7 late days at $0.25/day, every day beyond
that at $1.00/day — modeling a policy that gets more aggressive the
longer a book sits overdue.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/library -name "*.java")

java -cp out library.driver.LibraryDriver
java -cp out library.interview.SimpleLibraryInterview
```

`LibraryDriver` demonstrates: seed one title with 2 physical copies →
search by title → check out both copies to two different members → a
third checkout attempt fails (no copies left) → the first member returns
6 days late and is charged a tiered fine → list a member's active loans →
list every physical copy's current status.

`SimpleLibraryInterview` is the same golden path (checkout both copies →
fail on a third → return late → flat fine) in one file, with no
interfaces or tiered fines — useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How would you support different loan periods per book type (e.g.
  reference books, DVDs)?"** — Add a `LoanPolicy` (or a field on `Book`)
  that maps book type → loan period in days, and have `checkout` read it
  instead of the hardcoded `LOAN_PERIOD_DAYS` constant — same shape as
  swapping `FineStrategy`.
- **"How do you cap the number of books a member can hold at once?"** —
  `getActiveLoansForMember` already returns exactly what you'd need to
  check the count against a cap before allowing a new `checkout` — the
  data's there, just add the guard.
- **"What about holds/reservations when every copy is checked out?"** —
  Not modeled; you'd add a `HoldQueue` per ISBN (a `Queue<Member>`) and,
  on `returnBook`, check the queue before marking the item `AVAILABLE` for
  general checkout.
- **"How do you handle a lost book?"** — `BookItemStatus.LOST` already
  exists as a state but nothing transitions into it; you'd add a
  `reportLost(barcode)` method that sets the status and charges a
  replacement fee, structurally identical to how `returnBook` charges a
  late fee.
- **"Concurrent checkouts for the last copy?"** — `BookItem.tryLoan` is
  `synchronized` per copy, so the find-then-mark-loaned sequence for any
  single copy is atomic — two members racing for the same last copy can't
  both win it. See the Concurrency section below for why this is a
  per-copy lock rather than a lock on the whole service.

## Concurrency & thread-safety

- **Per-copy atomic claim** (`BookItem.tryLoan`/`tryReturn`) — the status
  check and the flip to `LOANED`/`AVAILABLE` happen under the same
  monitor, one per physical copy. `checkout` scans a title's copies (a
  read, safe under `CopyOnWriteArrayList`/`ConcurrentHashMap`) and calls
  `tryLoan()` on each candidate, moving to the next copy whenever another
  thread already won that one — the same optimistic-scan-and-claim shape
  used for `ParkingSpot.tryPark` and `Book.tryReserve` (Bookstore LLD).
  Checkouts for a *different* ISBN never contend with this at all.
- **Per-loan atomic return** (`Loan.tryMarkReturned`) — guards against a
  double-submitted return request for the same loan triggering the fine
  calculation and copy release twice.
- **No service-wide lock** — an earlier version of `InMemoryLoanService`
  synchronized the entire `checkout`/`returnBook` methods, which would
  have serialized checkouts for every title in the library behind one
  monitor. Moving the atomicity down to `BookItem`/`Loan` themselves
  means only genuinely contended copies/loans ever block each other.
- **`ConcurrentHashMap`/`CopyOnWriteArrayList` throughout
  `InMemoryCatalogService`/`InMemoryLoanService`** — replaces
  `LinkedHashMap`/`HashMap`/`ArrayList`, which aren't safe under
  concurrent structural modification (a librarian adding copies while a
  checkout is scanning the same title's list, for instance).

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Per-copy/per-loan locks (`BookItem`/`Loan`) over a whole-service lock | `synchronized` on `InMemoryLoanService`'s checkout/return methods | A service-wide lock serializes checkouts across every title in the library; per-copy locking scopes contention to only the specific copy two members are actually racing over. |
| Separate `Book` (title) and `BookItem` (copy) | One `Book` class with a `copiesAvailable` counter | A counter can't answer "which specific copy is with which member" or support per-copy state (e.g. one copy lost, others fine) — the split models reality directly. |
| `LocalDate` for checkout/due/return dates | `Instant`/`ZonedDateTime` (as used in the Calendar LLD) | Library due dates are calendar-day granular, not time-of-day sensitive — `LocalDate` avoids unnecessary timezone reasoning for a domain that doesn't need it. |
| `TieredFineStrategy` as the driver's default | `FlatDailyFineStrategy` | Demonstrates the more interesting policy in the full demo; the compact interview version intentionally uses the simpler flat strategy to keep that file short. |
| Fine computed on return, not accrued daily | A background job that accrues fines day-by-day | Simpler and sufficient — the fine only needs to exist once, at the moment of return; there's no requirement to show a running balance while a book is still out. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — catalog/loans reset on JVM exit.
- No per-member checkout cap.
- No holds/reservation queue for fully-checked-out titles.
- No lost-book workflow (`BookItemStatus.LOST` is defined but unused).
- No loan renewal (extending a due date before it lapses).
- Single fixed loan period for every book, regardless of type.
- No fine payment tracking — `returnBook` reports a fine amount but
  nothing records whether it was ever paid.
