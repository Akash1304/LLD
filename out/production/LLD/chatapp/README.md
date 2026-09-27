# Chat Application LLD

Chat rooms (group and 1:1), membership-checked message sending, and
observer-style real-time delivery to subscribed listeners. Two
implementations live here:

- **Full version** (`model`, `service`, `driver` packages) — what you'd
  build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleChatAppInterview.java`) — a
  single ~75-line file covering membership-checked sending and a
  listener-based real-time push, for when you only have 15-20 minutes or
  want a warm-up before the full design.

## Problem statement

> Design a chat application: users can create rooms (group or 1:1), send
> and receive messages within a room, and messages should be delivered to
> other participants in real time.

## Clarifying questions to ask in the first 10 minutes

- Group rooms and 1:1 direct messages — are they the same underlying
  concept, or fundamentally different? (Modeled here: **the same
  concept** — a `ChatRoom` with a member set; a DM is just a room with
  exactly 2 members, created via a convenience `createDirectMessage`
  method.)
- How is "real time" simulated without an actual network layer? (Modeled
  here: the **Observer pattern** — a `ChatRoom` holds `MessageListener`
  subscribers and notifies them synchronously the instant a message is
  appended, standing in for a websocket/push fan-out.)
- Can anyone send to any room, or only members? (Modeled here: **members
  only** — `MessageService.sendMessage` checks `ChatRoom.isMember` before
  accepting a message.)
- Is authentication in scope? (Not modeled — sender identity is passed in
  as a plain string; see Known Gaps.)
- Should message history be paginated/limited? (Modeled here:
  `getHistory(roomId, limit)` returns only the most recent `limit`
  messages.)

## Class design

```
model/
  User               id, name
  Message            id, chatRoomId, senderId, content, sentAt
  MessageListener    (interface) onMessage(Message) -- the Observer contract
  ChatRoom           id, name, Set<memberId>, List<Message>, List<MessageListener>;
                       appendMessage() stores the message AND notifies every
                       subscribed listener (the Observer "subject")

service/
  ChatRoomService   + InMemoryChatRoomService   create group/DM rooms, add
                                                   members, subscribe a
                                                   listener to a room
  MessageService    + InMemoryMessageService     sendMessage (membership-
                                                   checked), getHistory;
                                                   depends on ChatRoomService;
                                                   NotAMemberException

driver/
  ChatAppDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Observer** | `ChatRoom` (subject) + `MessageListener` (observer) | This is *the* pattern the "real-time delivery" requirement is asking for: rather than clients polling for new messages, each connected client (or device) subscribes once and is pushed every new message the instant it's appended — no polling loop needed anywhere in this codebase. |
| **Unified room model for group chat and DMs** | `ChatRoom` used by both `createRoom` and `createDirectMessage` | A DM is structurally identical to a group room (a member set + message history) — modeling it as a special case of the same class avoids a parallel, mostly-duplicate `DirectMessage` type. |
| **Repository-ish interface + impl** | `ChatRoomService`/`InMemoryChatRoomService`, `MessageService`/`InMemoryMessageService` | Same DIP shape as the rest of this repo's LLDs. |
| **Constructor injection** | `InMemoryMessageService(ChatRoomService)` | `MessageService` doesn't reach for a global room registry — sending a message and checking membership both go through the injected `ChatRoomService`. |

## SOLID mapping

- **SRP** — `ChatRoom` only tracks membership/history/subscribers;
  `MessageService` only validates and records sends; `ChatRoomService`
  only manages room lifecycle and subscriptions.
- **OCP** — a new kind of listener (e.g. one that forwards to an email
  digest instead of a push notification) can subscribe to a room without
  any change to `ChatRoom` or `MessageService` — it only needs to
  implement `MessageListener`.
- **LSP** — every `MessageListener` implementation is invoked identically
  by `ChatRoom.appendMessage`; the room doesn't know or care what kind of
  client is on the other end.
- **ISP** — `MessageListener` is a single-method interface — a listener
  that only cares about new messages doesn't have to implement unrelated
  callbacks (e.g. typing indicators, read receipts).
- **DIP** — `InMemoryMessageService` depends on `ChatRoomService`
  (interface), not `InMemoryChatRoomService` (impl).

## Core algorithms

### Real-time delivery (`ChatRoom.appendMessage`)

```java
messages.add(message);
for (MessageListener listener : listeners) listener.onMessage(message);
```

Synchronous, in-process fan-out to every subscriber — the direct
analogue of a server pushing a new message down every open websocket
connection for that room. In this demo it happens on the same thread as
`sendMessage`, which is exactly the trade-off called out in the follow-ups
below.

### Membership check (`InMemoryMessageService.sendMessage`)

`ChatRoom.isMember` is an O(1) `Set` lookup, so validating a sender before
accepting a message doesn't add meaningful overhead even for large rooms.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/chatapp -name "*.java")

java -cp out chatapp.driver.ChatAppDriver
java -cp out chatapp.interview.SimpleChatAppInterview
```

`ChatAppDriver` demonstrates: create a group room with Alice and Bob →
subscribe a listener standing in for "Bob's phone" → Alice sends a
message and the listener fires immediately → Carol (not a member) fails
to send → Alice adds Carol to the room and Carol's message now succeeds
(and Bob's listener fires again) → print the room's message history →
create a 1:1 direct message between Alice and Dave and send a message in
it.

`SimpleChatAppInterview` is the same golden path (membership-checked send
→ real-time listener → failure for a non-member) in one file, with no
interfaces or direct-message convenience method — useful as a live-coding
warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How would this actually work over a network (not just in-process
  method calls)?"** — `MessageListener.onMessage` is the seam: a real
  implementation would be a websocket-session-backed listener that
  serializes the message and writes it to a socket, instead of printing
  to stdout — `ChatRoom`'s fan-out logic wouldn't change at all.
- **"What if a listener is slow or throws an exception — does it block
  or break message delivery to everyone else?"** — Yes, as written: the
  `for` loop in `appendMessage` runs *inside* the room's synchronized
  block (see the Concurrency section below), so one bad listener not
  only breaks delivery to the rest but holds the room's lock the whole
  time, blocking any other thread trying to send or subscribe in that
  same room. A real system would either catch-and-log per listener, or
  push notifications onto a queue per subscriber (fire-and-forget from
  `appendMessage`'s perspective) so one slow consumer can't back up
  either message delivery or the lock.
- **"How do you scale to millions of concurrent chat rooms / users?"** —
  In-process `List<MessageListener>` per room only works on a single
  server; at scale you'd replace it with a pub/sub backbone (e.g. Kafka,
  Redis pub/sub) where "subscribe" means joining a topic per room rather
  than registering a Java object.
- **"How would you add read receipts or typing indicators?"** — Both are
  structurally similar to messages: a `TypingEvent`/`ReadReceipt` type
  broadcast the same way through `ChatRoom`'s listener list, possibly via
  a broader `RoomEvent` interface that `Message` becomes one case of.
- **"What about message editing or deletion?"** — Not modeled; `Message`
  is immutable and `ChatRoom.messages` only supports appending. Both
  would need `Message` to carry a mutable `edited`/`deletedAt` flag and a
  new event type broadcast to listeners so connected clients update their
  view of history, not just get new messages appended.
- **"Concurrent sends to the same room from multiple threads?"** — Every
  method on `ChatRoom` that touches `memberIds`/`messages`/`listeners` is
  `synchronized` on that room instance, so two threads calling
  `appendMessage` on the *same* room are serialized (correct append order,
  no corrupted `ArrayList`), while sends to a *different* room proceed
  without waiting at all.

## Concurrency & thread-safety

- **Per-room monitor, not a global chat-server lock** — every mutating
  and reading method on `ChatRoom` (`addMember`, `isMember`,
  `subscribe`/`unsubscribe`, `appendMessage`, and the getters) is
  `synchronized` on that specific room instance. Two rooms never
  contend with each other; only operations on the *same* room serialize.
- **Fan-out happens inside the lock** — `appendMessage` notifies every
  listener while still holding the room's monitor, which is what makes a
  slow/misbehaving listener a real liveness risk for that room (see the
  follow-up above), not just a delivery-order curiosity. This is called
  out as a known, named trade-off rather than hidden — the alternative
  (copy the listener list, release the lock, then notify) avoids holding
  the lock during fan-out at the cost of a listener list snapshot that
  could include a subscriber who unsubscribed a moment earlier.
- **Defensive copies on reads** — `getMessages`/`getMemberIds` return a
  snapshot copy (`new ArrayList<>(messages)` / `new
  LinkedHashSet<>(memberIds)`) taken under the lock, not a live
  unmodifiable *view* over the mutable collection — a view would risk
  `ConcurrentModificationException` (or worse, inconsistent partial
  state) if iterated by a caller while another thread mutates the
  underlying collection concurrently.
- **`ConcurrentHashMap` for the room registry** — `InMemoryChatRoomService.rooms`
  is safe for concurrent room creation alongside lookups from active
  chat sessions.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Fan-out inside the room's lock | Copy the listener list, release the lock, then notify outside it | Simpler to reason about (no risk of notifying a listener that already unsubscribed, no lock-free window); the named cost is that a slow listener holds up the whole room, not just message delivery — see the concurrency follow-up. |
| One `ChatRoom` type for both group chat and DMs | Separate `GroupChat`/`DirectMessage` classes | A DM has no behavior a group chat doesn't already have (both are "a member set + history + subscribers") — a separate type would just duplicate `ChatRoom` with an artificial 2-member constraint. |
| Observer pattern via a plain `MessageListener` interface | A callback `Consumer<Message>` (Java functional interface) directly | Both work equivalently here (the driver even uses a lambda); a named interface documents intent ("this is a real-time subscriber") better than a generic `Consumer`, and leaves room to grow into a richer `RoomEvent` interface later (see follow-ups). |
| Synchronous, in-process notification | An async event bus / message queue | Keeps the demo's cause-and-effect visible in linear stdout output (send → immediately see the listener fire) — the trade-off (one slow listener blocks the rest) is called out explicitly rather than hidden. |
| Membership check happens in `MessageService`, not `ChatRoom` | Push the check into `ChatRoom.appendMessage` itself | `ChatRoom` stays a simple data+notification holder; validation (which can throw a checked exception) belongs in the service layer, consistent with how other LLDs in this repo separate storage/state from validation/orchestration. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — rooms/messages reset on JVM exit.
- No real network layer — `MessageListener` is an in-process callback,
  not a websocket/push abstraction.
- No authentication/authorization beyond room membership.
- No message editing, deletion, or read receipts.
- No typing indicators or presence (online/offline status).
- No pagination cursor for history (only a simple "last N" limit).
