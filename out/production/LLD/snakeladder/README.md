# Snake & Ladder LLD

A turn-based board game for N players: dice rolls, snake/ladder jumps, and
a win condition on exact landing. Two implementations live here:

- **Full version** (`GameStatus`, `models`, `service`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleSnakeLadderInterview.java`) — a
  single ~60-line file covering the same golden path with no interfaces,
  for when you only have 15-20 minutes or want a warm-up before the full
  design.

## Problem statement

> Design a Snake & Ladder game for N players on a configurable board with
> snakes and ladders, that rolls dice, moves the current player, applies
> any snake/ladder at the landing square, and declares a winner.

## Clarifying questions to ask in the first 10 minutes

- Board size and dice range fixed at 100 / 1-6, or configurable? (Modeled
  here: both configurable — `Board(size, entities)`, `Dice(min, max)`.)
- What happens on a roll that would overshoot the last square — bounce
  back, or just stay put? (Modeled here: **stay put**, no bounce-back;
  see Known Gaps for the alternative.)
- Must a player land exactly on the final square to win, or does any
  roll that reaches/exceeds it count? (Modeled here: **exact landing**
  required — this is the standard rule and the more common interview
  expectation.)
- Can two players occupy the same square, and does landing on an
  opponent send them back to start (a "capture" rule)? (Modeled here: no
  capture — squares can be shared; see Known Gaps.)
- Turn order: strict round robin, or does rolling a 6 grant an extra turn
  (a common house rule)? (Modeled here: strict round robin, no extra
  turns.)

## Class design

```
GameStatus                    enum: NOT_STARTED, IN_PROGRESS, FINISHED

models/
  Player          name, position (mutable)
  Dice            min/max value; roll()
  BoardEntity     abstract start/end pair shared by Snake and Ladder
  Snake           BoardEntity, validated start > end (head above tail)
  Ladder          BoardEntity, validated start < end (bottom below top)
  Board           size, Map<start, end> built from a List<BoardEntity>;
                   getFinalPosition(pos) resolves one snake/ladder hop

service/
  GameService              + InMemoryGameService   turn order, dice roll,
                                                      movement, win check

driver/
  SnakeLadderDriver   end-to-end demo: build board, play until a winner
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Repository-ish interface + impl** | `GameService`/`InMemoryGameService` | Turn/movement logic sits behind an interface so a future networked or persisted game state doesn't change the driver — same DIP shape as `CalendarService`/`ParkingLot`'s strategies in the other two LLDs. |
| **Shared abstract base for symmetric variants** | `BoardEntity` → `Snake`/`Ladder` | Snakes and ladders are structurally identical (a start/end jump) and differ only in a direction *invariant*, so the validation lives in each subclass constructor rather than as an `if/else` in `Board`. |
| **Lookup table over conditional logic** | `Board.getFinalPosition` (`Map<Integer,Integer>`) | Resolving a snake/ladder is one `Map.getOrDefault` instead of scanning a list of entities per move — O(1) per lookup. |

## SOLID mapping

- **SRP** — `Player` only tracks name/position; `Dice` only rolls;
  `Board` only resolves positions; `InMemoryGameService` only owns turn
  order and win detection. No class does two of these.
- **OCP** — a new board effect (e.g. a "teleport pad" that isn't
  strictly a snake or ladder) can extend `BoardEntity` without touching
  `Board` or `InMemoryGameService`.
- **LSP** — `Board` treats every `BoardEntity` identically via
  `getStart()`/`getEnd()`; `Snake` and `Ladder` only add constructor
  validation, no behavioral surprises for callers.
- **ISP** — `GameService` exposes three narrow methods (`getStatus`,
  `playTurn`, `getWinner`) rather than one catch-all `execute(...)`.
- **DIP** — `SnakeLadderDriver` depends on `GameService` (interface), not
  `InMemoryGameService` (impl) directly.

## Core algorithm

### Turn resolution (`InMemoryGameService.playTurn`)

1. Roll the dice for the current player.
2. If `position + roll > board.size`, the roll overshoots — the player
   stays put and the turn passes (no bounce-back).
3. Otherwise resolve `board.getFinalPosition(target)` — a plain map
   lookup, `O(1)`.
4. If the resolved position equals `board.size`, that player wins and the
   game ends immediately (checked *after* applying the snake/ladder, so
   landing exactly on a ladder that ends at square 100 still counts as a
   win).
5. Advance `currentPlayerIndex` round robin.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/snakeladder -name "*.java")

java -cp out snakeladder.driver.SnakeLadderDriver
java -cp out snakeladder.interview.SimpleSnakeLadderInterview
```

`SnakeLadderDriver` demonstrates: build a 100-square board with 3 ladders
and 3 snakes → two players (Alice, Bob) → play turns until someone lands
exactly on square 100, printing each roll, jump, and overshoot along the
way.

`SimpleSnakeLadderInterview` is the same golden path in one file, with a
seeded `Random` (so output is reproducible run-to-run) and no interfaces —
useful as a live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"What if a roll overshoots — bounce back or stay?"** — Currently
  stays put; bounce-back would change one branch in `playTurn`
  (`target = board.size - (target - board.size)`), worth naming as a
  one-line change rather than a redesign.
- **"How would you support N players instead of 2?"** — Already
  supported — `InMemoryGameService` takes a `List<Player>` and cycles
  `currentPlayerIndex % players.size()`; the driver just seeds more
  players.
- **"What about a 'roll a 6, go again' house rule?"** — `playTurn` would
  skip the `currentPlayerIndex` advance when `roll == 6`; natural
  extension, not a redesign.
- **"How do you prevent an infinite game (a snake/ladder loop)?"** — Not
  structurally prevented; the driver has a hardcoded `turnLimit` safety
  valve for the demo. A real fix would validate at `Board` construction
  time that no cycle exists among `snakesAndLadders` entries.
- **"Concurrent moves from two clients?"** — `playTurn`/`getStatus`/
  `getWinner` are all `synchronized` on the game instance (see the
  Concurrency section below), so two threads calling `playTurn()` at once
  are serialized correctly rather than interleaving `currentPlayerIndex`
  reads and writes. A single game has one shared sequential turn order —
  there's no finer resource to split a lock across the way a shared pool
  of parking spots or seats would allow.

## Concurrency & thread-safety

`InMemoryGameService.playTurn`/`getStatus`/`getWinner` are `synchronized`
on the service instance — one lock per game. Unlike LLDs with a shared
pool of independent resources (parking spots, movie seats, hotel rooms),
a board game's entire state (turn order, positions) is one indivisible
unit that must change atomically and strictly in sequence anyway, so a
single per-game lock is already the correct granularity — there's no
finer-grained split that would reduce contention without breaking turn
ordering.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Exact-landing win rule | Any roll reaching/exceeding the board size wins | Matches the traditional board game rule and is the variant interviewers usually expect unless they say otherwise. |
| `Map<Integer,Integer>` flattening snakes+ladders in `Board` | Keep `List<BoardEntity>` and scan per move | O(1) lookup per move vs. O(entities) scan; the list is only needed once, at construction. |
| No capture / shared-square rule | Sending a player back to start when landed on | Keeps the golden path simple; flagged as a natural extension if asked. |
| `turnLimit` safety valve in the driver | No limit, trust the board has no cycles | A demo shouldn't hang forever if the sample board data has a bug; a real implementation would validate the board instead. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — game state resets on JVM exit.
- No bounce-back-on-overshoot variant (stay-put only).
- No "extra turn on a 6" or other house rules.
- No cycle detection on the board's snake/ladder graph.
- No capture rule when players share a square.
