# Social Media Platform LLD

Users, follow relationships, posts with likes/comments, and a pluggable
feed-ranking algorithm. Two implementations live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleSocialMediaInterview.java`) — a
  single ~75-line file covering follow, post, like/comment counters, and
  chronological vs. engagement feed ordering, for when you only have
  15-20 minutes or want a warm-up before the full design.

## Problem statement

> Design a social media platform: users can follow each other, create
> posts, like and comment on posts, and view a feed of posts from the
> people they follow, ordered by some ranking.

## Clarifying questions to ask in the first 10 minutes

- Is following mutual (like Facebook friends) or one-directional (like
  Twitter follows)? (Modeled here: **one-directional** — `User` tracks
  both `followingIds` and `followerIds` as separate sets, so A following B
  doesn't imply B follows A.)
- Should the feed be strictly chronological, or ranked by
  engagement/relevance? (Modeled here: **both**, as swappable
  `FeedRankingStrategy` implementations.)
- Is the feed computed on read (pull) or precomputed and pushed to each
  follower's timeline on every new post (push/fan-out-on-write)? (Modeled
  here: **pull** — `FeedService.getFeed` gathers posts from followed
  authors at read time; see Known Gaps for the fan-out alternative and
  why real large-scale systems often use it instead.)
- Do likes/comments need their own identity (who liked what, when), or
  just counts? (Modeled here: likes are a `Set<userId>` on each post — so
  "did Alice like this" is answerable and double-likes are naturally
  prevented — while comments are a full list of `Comment` objects with
  author/content/time.)
- Should a private/blocked user's posts be excluded from a follower's
  feed? (Not modeled — see Known Gaps.)

## Class design

```
model/
  User      id, name, Set<followingId>, Set<followerId>
  Comment   id, authorId, content, createdAt
  Post      id, authorId, content, createdAt, Set<likedByUserId>, List<Comment>;
             getEngagementScore() = likes + comments

strategy/
  FeedRankingStrategy   + ChronologicalFeedStrategy / EngagementFeedStrategy

observer/
  SocialEvent            immutable: type (LIKE/COMMENT/FOLLOW), actorId,
                           targetUserId, postId
  SocialEventListener    onEvent(SocialEvent)
  SocialEventPublisher   subscribe/unsubscribe/publish (the subject)
  NotificationListener   one concrete observer: prints a notification to
                           the target user

service/
  UserService   + InMemoryUserService   create users, follow/unfollow
                                           (maintains both directions);
                                           publishes FOLLOW
  PostService   + InMemoryPostService   create posts, like, comment,
                                           fetch posts by a set of authors;
                                           publishes LIKE / COMMENT
  FeedService   + InMemoryFeedService   getFeed(userId, rankingStrategy);
                                           depends on UserService + PostService

driver/
  SocialMediaDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Observer** | `SocialEventPublisher` (subject) + `SocialEventListener`; `PostService`/`UserService` publish, `NotificationListener` subscribes | The prompt says "notifications" — but "invalidate the feed cache" and "emit an analytics event" are the next two asks, and each would otherwise be another hard dependency bolted onto `PostService`. Publishing decouples the *fact* (a like happened) from every *reaction*. **Why not just call a `NotificationService` from `likePost`:** that couples `PostService` to one specific reaction and grows a new parameter every time another reaction appears. Events are published *after* the domain lock is released, so a slow listener never stalls a like. |
| **Strategy** | `FeedRankingStrategy` (`Chronological`/`Engagement`) | Feed ordering is a genuinely interchangeable algorithm over one input — the one place Strategy is exactly right. Isolating it means `FeedService` never changes to support a new ranking. |
| **Bidirectional index maintained at write time** | `User.followingIds` + `User.followerIds`, both updated together in `follow`/`unfollow` | Answering "who does Alice follow" and "who follows Alice" are both O(1) set lookups instead of one being an O(n) scan over every user — a small but real example of trading a bit of write-time bookkeeping for fast reads. |
| **Repository-ish interface + impl** | `UserService`/`InMemoryUserService`, `PostService`/`InMemoryPostService`, `FeedService`/`InMemoryFeedService` | Same DIP shape as the rest of this repo's LLDs. |
| **Composition of two services behind a third** | `InMemoryFeedService` depends on both `UserService` and `PostService` | The feed is a *derived* view (posts ∩ following), not its own data store — modeling it as a service that composes the other two keeps "who to show" (social graph) and "what to show" (content) as separately-owned concerns. |

## SOLID mapping

- **SRP** — `User` only tracks identity and social graph edges; `Post`
  only tracks content and engagement; `PostService` only manages posts;
  `UserService` only manages the social graph; ranking logic lives only
  in `FeedRankingStrategy` impls.
- **OCP** — a new ranking algorithm (e.g. weighting recency and
  engagement together, or personalizing by past interaction) can be
  added without touching `InMemoryFeedService`.
- **LSP** — every `FeedRankingStrategy` takes `List<Post>` and returns
  `List<Post>`; `InMemoryFeedService` treats both implementations
  identically.
- **ISP** — `UserService` and `PostService` each expose a handful of
  narrow, purpose-specific methods instead of one catch-all
  `handleSocialAction(...)`.
- **DIP** — `InMemoryFeedService` depends on `UserService`/`PostService`
  (interfaces), not their `InMemory*` implementations.

## Core algorithms

### Feed assembly (`InMemoryFeedService.getFeed`)

1. Look up the requesting user's `followingIds` — O(1) since it's a
   maintained set, not computed by scanning every user's followers.
2. Ask `PostService.getPostsByAuthors(following)` for every post authored
   by someone in that set — `O(total posts)` in this in-memory
   implementation (a linear scan filtering by author), acceptable at demo
   scale.
3. Hand the candidate list to the injected `FeedRankingStrategy` to sort.

### Engagement ranking (`EngagementFeedStrategy`)

Sorts by `likes + comments` descending, with recency as a tiebreaker —
simple and transparent, deliberately not a black-box ML ranking model
(which would be the honest answer to "how would a real platform do this
better").

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/socialmedia -name "*.java")

java -cp out socialmedia.driver.SocialMediaDriver
java -cp out socialmedia.interview.SimpleSocialMediaInterview
```

`SocialMediaDriver` demonstrates: Alice follows Bob and Carol → Bob posts
twice, Carol posts once → Carol's post gets a like from Alice and Bob plus
a comment from Alice → print Alice's feed chronologically (newest first)
→ print the same feed ranked by engagement (Carol's now-popular post
jumps to the top despite being older) → inspect Carol's post's comment
list directly.

`SimpleSocialMediaInterview` is the same golden path (follow → post →
engagement counters → both feed orderings) in one file, with no
interfaces or a real `Comment` object (just a counter) — useful as a
live-coding warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"How do you scale the feed to millions of users, each following
  thousands of accounts?"** — This design computes the feed on read
  (pull), scanning all posts by followed authors every time — fine at
  small scale, but expensive per-request at scale. The standard answer is
  **fan-out-on-write**: when a user posts, push it into a precomputed
  timeline for each follower immediately, so reads become an O(1)
  timeline fetch — at the cost of write amplification for
  celebrity/high-follower accounts, which usually gets special-cased
  (fan-out-on-read only for accounts above a follower threshold).
- **"How would you personalize ranking per user instead of one global
  algorithm?"** — `FeedRankingStrategy.rank` already takes just the
  candidate posts; a personalized variant would need additional context
  (the requesting user's past interactions) passed into the strategy —
  the interface would need to grow to `rank(List<Post>, User)` or similar,
  a small but real interface change worth naming rather than glossing
  over.
- **"How do you prevent a user from liking the same post twice?"** —
  Already structurally prevented: `Post.likedByUserIds` is a `Set`, so
  calling `like()` twice for the same user is a no-op, not a double-count.
- **"What about blocked/muted users appearing in a feed?"** — Not
  modeled; you'd add a `blockedIds`/`mutedIds` set to `User` and filter
  candidates in `getFeed` before handing them to the ranking strategy.
- **"Concurrent likes on a popular post?"** — `Post.like`/`unlike`/
  `addComment` are `synchronized` on that specific post, so concurrent
  likers of the same viral post are serialized correctly (and, since
  `likedByUserIds` is a `Set`, still can't double-count) while likes on a
  *different* post proceed without waiting. See the Concurrency section
  below.
- **"Concurrent follow/unfollow between the same two users?"** —
  `InMemoryUserService.follow`/`unfollow` acquire both users' locks in a
  fixed, globally-consistent order (lower user id first) before touching
  either side of the relationship, so `follow(A,B)` and `unfollow(A,B)`
  racing each other can't leave the graph half-updated (A following B but
  B not listing A as a follower, or vice versa) — and, just as
  importantly, two operations touching the same pair of users from
  opposite directions can't deadlock each other.

## Concurrency & thread-safety

- **Per-post monitor** (`Post`) — `like`, `unlike`, `addComment`, and the
  read methods are all `synchronized` on that post instance. Interacting
  with post A never blocks post B; only concurrent interactions with the
  *same* post serialize.
- **Per-user monitor with a fixed lock order for two-sided updates**
  (`User`, `InMemoryUserService.follow`/`unfollow`) — each individual
  mutator (`addFollowing`, `addFollower`, ...) is synchronized on that
  user, but updating *both* sides of a follow relationship consistently
  means holding both users' locks together. `withOrderedLocks` always
  acquires them in the same order (comparing user ids), which is the
  standard fix for the classic "two threads each hold one lock and wait
  for the other's" deadlock that an *inconsistent* lock order would
  produce.
- **Defensive copies on reads** — `User.getFollowingIds`/`getFollowerIds`
  and `Post.getComments` return a snapshot copy taken under the lock, not
  a live view over the mutable backing collection, so a caller iterating
  the result can't collide with a concurrent mutation or throw
  `ConcurrentModificationException`.
- **`ConcurrentHashMap` for the user and post stores** — safe for
  concurrent reads (feed assembly, profile views) alongside new
  users/posts being created.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Fixed lock ordering (by user id) for two-sided follow updates | Lock whichever user is the "follower" first, then the "followee" | An order that depends on the *role* (follower vs. followee) rather than a fixed property (id) means `follow(A,B)` locks A-then-B while `follow(B,A)` locks B-then-A — the classic setup for a deadlock if both run concurrently. Ordering by id is direction-independent. |
| Pull-based feed (compute at read time) | Fan-out-on-write (precomputed per-follower timelines) | Simpler to implement correctly in an interview timebox and easier to reason about; the fan-out alternative is the textbook "how would you actually scale this" answer, called out explicitly above rather than implemented. |
| One-directional follow, tracked as two sets (`followingIds` + `followerIds`) | A single global "follows" edge list, queried on demand | Twitter-style asymmetric follow is the more common interview framing than mutual friendship, and maintaining both directions at write time keeps both "who do I follow" and "who follows me" queries O(1). |
| Likes as a `Set<userId>`, comments as a `List<Comment>` | Both as simple integer counters | A set naturally answers "has this user already liked this" and prevents double-counting for free; comments need full content/authorship, so a counter alone wouldn't be enough to support "view comments," which the problem statement explicitly asks for. |
| `EngagementFeedStrategy` uses a transparent `likes + comments` score | A black-box ML ranking model | Matches the scope and testability expected in a coding round; explicitly named as a simplification versus what a real platform (which uses learned ranking models) would do. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — users/posts/graph reset on JVM exit.
- No fan-out-on-write / precomputed timelines (pull-only feed).
- No blocking/muting.
- No media attachments (images/video) on posts.
- Notifications are published via Observer but only printed — no
  delivery channels (see the Notification System LLD for that concern in
  isolation).
- No pagination — `getFeed` returns the full ranked list, not a page.
