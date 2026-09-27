# ATM System LLD

A textbook State-pattern implementation: card insertion, PIN
authentication with a lockout, withdrawal with denomination-aware cash
dispensing, and balance inquiry. Two implementations live here:

- **Full version** (`model`, `state`, `service`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleATMInterview.java`) — a single
  ~100-line file covering the same state machine collapsed into a
  `switch`-based state field, for when you only have 15-20 minutes or
  want a warm-up before the full design.

## Problem statement

> Design an ATM: a user inserts a card, enters a PIN (with a lockout
> after too many wrong attempts), and can then check their balance or
> withdraw cash — the machine must dispense an achievable combination of
> bills and never allow an operation that doesn't make sense for the
> current stage of the interaction (e.g. withdrawing before authenticating).

## Clarifying questions to ask in the first 10 minutes

- What are the distinct stages of an ATM session, and what's allowed in
  each? (Modeled here: **`IDLE` → `HAS_CARD` → `AUTHENTICATED`**, each a
  concrete `ATMState`; an operation invalid for the current state is
  rejected with a clear message rather than silently ignored or crashing.)
- How many wrong PIN attempts before the card is ejected/locked? (Modeled
  here: **3**, tracked as `failedPinAttempts` on the machine and reset on
  every new card insertion or successful PIN entry.)
- Does withdrawal need to account for which bills are physically
  available, or just check the balance? (Modeled here: **both** —
  `CashDispenser` tracks bill counts per denomination and a withdrawal
  fails if the amount can't be represented with what's left, even if the
  account balance covers it.)
- What happens if the balance check passes but the physical dispense
  fails (e.g. no bills of the right denomination)? (Modeled here: **the
  debit is rolled back** — see Core Algorithms.)
- Multiple accounts per card, or one account per card? (Modeled here:
  **one** — `Card.accountId` is a single reference; see Known Gaps.)

## Class design

```
model/
  Card             cardNumber, accountId, pin; isPinCorrect(int)
  Account          id, mutable balance; withdraw()/deposit()
  CashDispenser    TreeMap<denomination, count> (largest first); dispense(amount)
                     does a greedy largest-first breakdown, throwing if the
                     amount can't be represented exactly with what's on hand

state/   (the State pattern)
  ATMState         (interface, default methods reject unsupported ops)
                     insertCard / enterPin / selectWithdrawal /
                     selectBalanceInquiry / ejectCard
  IdleState        only insertCard() is valid
  HasCardState     only enterPin() (leads to AUTHENTICATED or ejects on
                     lockout) and ejectCard() are valid
  AuthenticatedState   selectWithdrawal(), selectBalanceInquiry(), and
                         ejectCard() are valid

service/
  BankService   + InMemoryBankService   account balance operations;
                                           InsufficientFundsException
  ATMMachine    the State-pattern "context" -- holds the current ATMState,
                  the inserted card, and failed-attempt count; every public
                  method delegates to currentState

driver/
  ATMDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **State** | `ATMState` (interface) + `IdleState`/`HasCardState`/`AuthenticatedState`, driven by `ATMMachine` | This is *the* canonical textbook example of the State pattern, and it's the answer interviewers are listening for: instead of one giant method full of `if (state == X)` branches for every operation, each state is its own class that only implements the transitions valid from it — invalid operations fall through to a single shared rejection path via default interface methods. |
| **Context delegates, never branches on state itself** | `ATMMachine.insertCard/enterPin/selectWithdrawal/...` | Every public method on `ATMMachine` is a one-line delegation to `currentState`. The machine never asks "what state am I in" with a conditional — that's precisely what the State pattern is designed to eliminate. |
| **Compensating action on partial failure** | `AuthenticatedState.selectWithdrawal` rolling back the bank debit if the dispenser can't fulfill the physical cash | A two-step operation (debit the account, then dispense bills) that can fail *after* the first step succeeds needs an explicit undo, rather than leaving the account debited with no cash dispensed. |
| **Repository-ish interface + impl** | `BankService`/`InMemoryBankService` | Same DIP shape as the rest of this repo's LLDs. |

## SOLID mapping

- **SRP** — `Account` only tracks balance; `CashDispenser` only tracks
  physical bill inventory and breakdown math; each `ATMState` only
  implements the transitions valid from that one stage of the session.
- **OCP** — a new stage (e.g. a `SelectingTransactionState` that splits
  "authenticated" into a menu step and a per-transaction step) can be
  added as a new class implementing `ATMState`, without editing the
  existing states or `ATMMachine`'s delegation methods.
- **LSP** — `ATMMachine` calls the same five methods on `currentState`
  regardless of which concrete `ATMState` is active; every state honors
  the same default-reject contract for operations it doesn't support.
- **ISP** — `ATMState`'s default methods mean a state that only supports
  one operation (e.g. `IdleState` only supports `insertCard`) isn't
  forced to write empty/no-op overrides for the other four.
- **DIP** — `ATMMachine` depends on `BankService` (interface), not
  `InMemoryBankService` (impl); state transitions go through
  `atm.getIdleState()`/`getHasCardState()`/`getAuthenticatedState()`
  accessors rather than states `new`-ing up their successors directly (so
  the machine owns exactly one instance of each state).

## Core algorithms

### PIN lockout (`HasCardState.enterPin`)

Wrong PIN increments `failedPinAttempts` and stays in `HAS_CARD` (letting
the user retry) until `MAX_PIN_ATTEMPTS` (3) is reached, at which point the
card is ejected and the machine returns to `IDLE` — matching real ATM
behavior of confiscating/locking a card after too many failures rather
than allowing unlimited guesses.

### Withdrawal with rollback (`AuthenticatedState.selectWithdrawal`)

1. Debit the account via `BankService.withdraw` (fails fast if the
   balance is insufficient — no dispenser interaction needed).
2. Attempt to physically dispense the amount via
   `CashDispenser.dispense`, which throws if the amount can't be built
   from the denominations currently loaded (e.g. asking for $15 when only
   $20 and $100 bills remain).
3. If the dispense fails, **credit the account back** via
   `BankService.deposit` before reporting the failure — so a physical
   dispenser limitation never leaves a customer's balance debited with no
   cash in hand.

### Greedy denomination breakdown (`CashDispenser.dispense`)

Walks denominations largest-first, taking as many bills of each as are
both available and needed, decrementing the running remainder. Standard
greedy change-making — correct here because the denominations in play
(100, 20, ...) are the typical "canonical" set for which greedy is optimal,
not because greedy is optimal for arbitrary denominations in general
(worth naming if an interviewer probes this).

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/atm -name "*.java")

java -cp out atm.driver.ATMDriver
java -cp out atm.interview.SimpleATMInterview
```

`ATMDriver` demonstrates: entering a PIN before inserting a card is
rejected (`IDLE` state) → insert a card → two wrong PINs followed by the
correct one → check balance → withdraw $140 (dispensed as one $100 + two
$20 bills) → attempt to withdraw $1000 (exceeds balance, rejected) → eject
the card → an attempted withdrawal after ejection is rejected → a second
card is locked out entirely after 3 consecutive wrong PINs.

`SimpleATMInterview` is the same state machine (idle → has-card →
authenticated, with lockout and denomination-aware withdrawal) collapsed
into a single `enum`-and-`switch` implementation instead of separate state
classes — useful as a live-coding warm-up, and a good prompt for "now
refactor this into the State pattern" as a follow-up exercise.

## Follow-up questions to expect (and how this design answers them)

- **"Why the State pattern instead of an enum + switch (like the compact
  version)?"** — The `switch` version is fine for 3 states and 5
  operations, but each new state multiplies the branches in every method;
  the State pattern instead adds one new class per new state, and each
  existing state/method is untouched — that's the concrete OCP payoff
  worth naming when asked why bother.
- **"What if dispensing cash fails but the network call to debit the
  account already succeeded upstream — how do you handle that in a
  distributed system?"** — This design rolls back in-process, in the same
  method call. A real ATM talks to a bank over a network, so debit and
  dispense are two separate operations that can fail independently and
  at different times — that needs a saga/compensating-transaction
  pattern with idempotency keys, not just a local `deposit()` call.
- **"How would you support multiple accounts per card (checking +
  savings)?"** — `Card` would hold a list of account IDs instead of one,
  and `AuthenticatedState` would need an account-selection step before
  `selectWithdrawal`/`selectBalanceInquiry` — a new state
  (`AccountSelectedState`) between `AUTHENTICATED` and the transaction
  states, not a redesign of the pattern itself.
- **"Concurrent transactions on the same account from two ATMs?"** —
  `Account.tryWithdraw` is an atomic check-and-debit synchronized on that
  specific account, so two concurrent withdrawals against the *same*
  account (e.g. the same card used at two machines at once) can't both
  pass the balance check and over-withdraw — while withdrawals against
  *different* accounts never contend. See the Concurrency section below.
- **"What about a deposit flow?"** — `Account.deposit`/`BankService.deposit`
  already exist; a `selectDeposit(ATMMachine, double)` method would be
  added to `ATMState` (with a sensible default rejection) and implemented
  only in `AuthenticatedState`, following the exact same shape as
  withdrawal.

## Concurrency & thread-safety

- **Per-account atomic debit** (`Account.tryWithdraw`) — the balance
  check and the debit happen under one monitor per account, closing the
  classic "two withdrawals both read a sufficient balance, both debit,
  account goes negative" race, while withdrawals on unrelated accounts
  never wait on each other.
- **Per-machine session lock** (`ATMMachine`) — every public entry point
  (`insertCard`, `enterPin`, `selectWithdrawal`, `selectBalanceInquiry`,
  `ejectCard`) and the session-state accessors used internally by
  `ATMState` implementations are `synchronized` on the machine instance.
  A real single ATM has one keypad and naturally serializes input, but
  making the lock explicit protects against a hypothetical multi-channel
  front-end (e.g. a mobile app and the physical keypad both able to drive
  the same session) and guards the State pattern's own invariant — that
  exactly one state transition is ever in flight at a time. Java monitors
  are reentrant, so a state's callback into `atm.setState(...)` from
  inside an already-locked entry point composes safely rather than
  deadlocking.
- **Per-dispenser lock** (`CashDispenser`) — `loadCash`/`dispense`/
  `getTotalCash` are all synchronized on the dispenser instance, since a
  physical cash dispenser could plausibly be shared (a vault refill
  process running while withdrawals are in flight) and `dispense` is a
  compound "plan a breakdown, then deduct it" sequence that must be
  atomic to avoid two withdrawals together dispensing more bills of a
  denomination than physically exist.
- **What's still coarse-grained on purpose:** the ATM session lock
  serializes *all* operations on one machine, including a balance
  inquiry blocking behind an in-progress withdrawal on the same machine
  — correct and desired, since a single ATM genuinely can only do one
  thing at a time for one customer.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Per-account/per-dispenser locks over whole-service locks | `synchronized` on `InMemoryBankService.withdraw` | A whole-service lock serializes every withdrawal across every account in the bank; per-account locking scopes contention to only the account two withdrawal attempts are actually racing over. |
| State pattern (one class per state) | A single `ATM` class with an enum field and `if`/`switch` on every method | The State pattern is the textbook-expected answer for this exact problem and scales better as operations/states grow (see follow-ups); the compact interview version deliberately uses the `switch` approach instead, to show both and let the trade-off be discussed explicitly. |
| Default interface methods reject unsupported operations | Every concrete state overrides every method, most with an explicit "not allowed" body | Cuts boilerplate significantly — `IdleState` only needs to override `insertCard`, not write four near-identical rejection methods. |
| Rollback the debit on dispenser failure | Reserve/hold cash before debiting (debit only after physical dispense succeeds) | Debit-then-rollback-on-failure is simpler to reason about in this single-process demo; a two-phase hold-then-commit would be the production-correct approach in a system where debit and dispense are separate, independently-failing steps (see the distributed-systems follow-up). |
| Greedy largest-first cash breakdown | Dynamic-programming optimal change-making | Greedy is correct and simpler for the canonical denominations ATMs actually use (100/50/20/10); DP would be needed only for arbitrary/non-canonical denomination sets, which is out of scope here but worth naming if asked. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — accounts/cash inventory reset on JVM exit.
- One account per card (no account selection step).
- No deposit flow wired into the driver (the model supports it).
- No PIN hashing/encryption — plain integer comparison.
- No network/distributed-transaction modeling between debit and dispense.
- No receipt printing or transaction history/audit log.
- No daily withdrawal limits.
