# File Storage System LLD (Google Drive-style)

Files and folders in a shared hierarchy, per-item permissions
(viewer/editor/owner), file versioning, and a pluggable version-retention
policy. Two implementations live here:

- **Full version** (`model`, `service`, `strategy`, `driver` packages) —
  what you'd build across the full 60-minute coding window.
- **Compact version** (`interview/SimpleFileStorageInterview.java`) — a
  single ~90-line file covering upload/download with a permission check,
  versioning, and sharing, for when you only have 15-20 minutes or want a
  warm-up before the full design.

## Problem statement

> Design a file storage system (like Google Drive): users can create
> folders, upload files into them, download files, share files/folders
> with other users at different access levels, and files keep a version
> history.

## Clarifying questions to ask in the first 10 minutes

- Are files and folders fundamentally different things, or do they share
  most behavior (both live in a hierarchy, both can be shared)? (Modeled
  here: **a shared abstract base, `FileSystemItem`** — Composite-pattern
  shaped — with `Folder` and `StorageFile` as the two leaf/container
  types.)
- How many permission levels, and do higher levels imply lower ones (can
  an `EDITOR` also just view)? (Modeled here: **3 ordered levels** —
  `VIEWER < EDITOR < OWNER` — checked via `hasAtLeast`, so "requires
  EDITOR" also passes for `OWNER`.)
- Does sharing a folder cascade permissions to everything inside it, or
  is each item's ACL independent? (Modeled here: **independent per
  item** — sharing a folder does not automatically share its children;
  see Known Gaps for what cascading would require.)
- Keep every historical version forever, or prune old ones? (Modeled
  here: pluggable `VersionRetentionStrategy`, applied after every new
  version upload.)
- Who can create a new version — only the owner, or anyone with edit
  access? (Modeled here: **`EDITOR` or above** — matches how real
  collaborative editing works.)

## Class design

```
model/
  PermissionLevel    enum: VIEWER, EDITOR, OWNER (ordinal-comparable)
  FileVersion        versionNumber, content, uploadedBy, createdAt
  FileSystemItem     (abstract) id, name, ownerId, parentFolderId,
                       Map<userId, PermissionLevel>; hasAtLeast() checks access
  Folder extends FileSystemItem     List<childId>
  StorageFile extends FileSystemItem   List<FileVersion>

strategy/
  VersionRetentionStrategy   + KeepAllVersionsStrategy / KeepLastNVersionsStrategy

service/
  FileStorageService   + InMemoryFileStorageService   createFolder, uploadFile,
                                                          uploadNewVersion,
                                                          downloadFile, shareItem,
                                                          listChildren;
                                                          PermissionDeniedException

driver/
  FileStorageDriver   end-to-end demo
```

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Composite** | `FileSystemItem` (abstract) → `Folder` (container) / `StorageFile` (leaf) | Folders and files share identity, ownership, and an ACL, but only folders hold children and only files hold version content — the classic shape for "things that nest, but not everything nests the same way." |
| **Strategy** | `VersionRetentionStrategy` (`KeepAll`/`KeepLastN`) | "How much history do we keep" is a storage-cost policy decision, isolated so it can change (or vary per file/plan tier) without touching upload logic. |
| **Ordinal-comparable enum for a permission hierarchy** | `PermissionLevel` + `hasAtLeast` | `VIEWER < EDITOR < OWNER` as enum declaration order means "does this user have at least EDITOR" is one `ordinal()` comparison instead of a lookup table — same trick as `VehicleSize`/`SeatType` elsewhere in this repo. |
| **Repository-ish interface + impl** | `FileStorageService`/`InMemoryFileStorageService` | Same DIP shape as the rest of this repo's LLDs. |

## SOLID mapping

- **SRP** — `FileSystemItem` only tracks identity/ownership/ACL;
  `StorageFile` only adds version storage; `Folder` only adds child
  tracking; retention policy lives only in `VersionRetentionStrategy`
  impls.
- **OCP** — a new retention policy (e.g. "keep one version per day") or a
  new item type (e.g. a `Shortcut` pointing at another item) can be added
  without touching `InMemoryFileStorageService`'s core upload/download
  flow.
- **LSP** — `InMemoryFileStorageService` treats every `FileSystemItem`
  identically for permission checks (`hasAtLeast`), regardless of whether
  it's a `Folder` or a `StorageFile`.
- **ISP** — `FileStorageService` exposes six narrow operations instead of
  one catch-all `handleFileOperation(...)`.
- **DIP** — retention policy is injected per call
  (`uploadNewVersion(..., VersionRetentionStrategy)`), not hardcoded
  inside the service.

## Core algorithms

### Permission check (`FileSystemItem.hasAtLeast`)

`permissions.get(userId).ordinal() >= required.ordinal()` — O(1) lookup,
and because the enum is declared in increasing-access order, a single
comparison covers "is this user allowed to do X," for any X that requires
a minimum level (view, edit, or own/share).

### Upload flow (`InMemoryFileStorageService.uploadFile`)

1. Check the *target folder's* ACL for `EDITOR`+ access (you need edit
   rights on a folder to add something into it) — not the new file's ACL,
   since it doesn't exist yet.
2. Create the file with version 1, seed its own ACL with the uploader as
   `OWNER`, and link it into the parent folder's child list.

### Version retention (`uploadNewVersion`)

After appending the new version, the *entire* version list is passed
through the injected `VersionRetentionStrategy` and the result replaces
the stored list — so pruning happens as a post-processing step on every
upload, keeping the trimming logic completely decoupled from the upload
logic itself.

## How to run

```bash
cd /Users/akashpanigrahi/Documents/Interview/LLD
javac -d out $(find src/filestorage -name "*.java")

java -cp out filestorage.driver.FileStorageDriver
java -cp out filestorage.interview.SimpleFileStorageInterview
```

`FileStorageDriver` demonstrates: Alice creates a folder and uploads a
file into it → Bob's read attempt fails (no access yet) → Alice shares
both the folder and file with Bob as `EDITOR` → Bob successfully reads
the file → Bob uploads 3 more versions with a "keep last 2" retention
policy, and only the most recent 2 are retained → Bob's attempt to share
the folder with a third user fails (`EDITOR` isn't enough to share,
only `OWNER` is) → list the folder's contents as Alice.

`SimpleFileStorageInterview` is the same golden path (upload → permission
check → share → versioned upload → sharing requires OWNER) in one file,
with no folder hierarchy or retention policy — useful as a live-coding
warm-up.

## Follow-up questions to expect (and how this design answers them)

- **"Does sharing a folder share everything inside it?"** — Not in this
  design; each `FileSystemItem` has its own independent ACL. A real
  system typically wants folder shares to cascade — you'd add an
  `effectivePermission(userId)` that walks up `parentFolderId` chains
  looking for the highest applicable grant, falling back to the item's
  own ACL, rather than requiring every file to be shared individually.
- **"How would you support nested folders and 'move file to another
  folder'?"** — Already structurally supported (`parentFolderId` +
  `Folder.childIds`); a `moveItem(itemId, newParentId)` method would just
  update both the item's `parentFolderId` and the old/new parent's child
  lists — no redesign needed.
- **"How do you prevent storage from growing unbounded with version
  history?"** — That's exactly what `VersionRetentionStrategy` is for;
  the follow-up worth naming is that pruning here is *synchronous* on
  upload (fine for a demo) — a real system would likely prune
  asynchronously via a background job so uploads aren't slowed by
  retention bookkeeping.
- **"Concurrent edits — what if two users upload a new version at the
  same time?"** — `InMemoryFileStorageService.uploadNewVersion`
  synchronizes on the specific `StorageFile` instance for the whole
  read-latest → add → prune sequence, so two concurrent uploads to the
  *same* file can't both compute the same "next version number"; uploads
  to *different* files never contend at all. See the Concurrency section
  below.
- **"How would you support large files (not fitting in memory as a
  `String`)?"** — `FileVersion.content` is a `String` here purely for
  demo simplicity; a real system stores content in a blob store (e.g. S3)
  and `FileVersion` would hold a reference/URL + checksum rather than the
  bytes themselves.

## Concurrency & thread-safety

- **Per-file lock for versioning** (`uploadNewVersion`) — the whole
  read-latest-version → add → prune sequence is wrapped in
  `synchronized (file)`, using the `StorageFile` instance itself as the
  monitor. `StorageFile`'s own `addVersion`/`getVersions`/
  `getLatestVersion`/`setVersions` methods are `synchronized` on that same
  instance, so the service's compound block composes atomically with them
  (Java monitors are reentrant) rather than needing a separate lock
  object. Uploading a new version to file A never blocks an upload to
  file B.
- **`ConcurrentHashMap` for permissions and the item store** —
  `FileSystemItem.permissions` and `InMemoryFileStorageService.items`
  are both `ConcurrentHashMap`s; `grantPermission`/`hasAtLeast` are each a
  single atomic map operation, so no extra locking is needed on top.
- **`CopyOnWriteArrayList` for `Folder.childIds`** — a folder's contents
  are listed far more often (every `listChildren` call) than a new item
  is uploaded into it, so optimizing for lock-free, safe-under-iteration
  reads is the right trade-off — the same choice made for the Library
  LLD's per-title copy lists.
- **`name`/`parentFolderId` are `volatile`** — `setName`/
  `setParentFolderId` can be called from a different thread than the one
  currently reading `getName`/`getParentFolderId` (e.g. a rename racing a
  listing); `volatile` guarantees the new value is visible immediately
  rather than only eventually.

## Tech decisions & trade-offs

| Decision | Alternative considered | Why this way |
|---|---|---|
| Per-file lock (`synchronized (file)`) for versioning | A whole-service lock on `uploadNewVersion` | A service-wide lock would serialize version uploads across every file in the system; locking on the specific `StorageFile` instance scopes contention to only the file two uploads are actually racing over. |
| `FileSystemItem` as a Composite base for `Folder`/`StorageFile` | Two entirely separate, unrelated classes | Sharing identity/ownership/ACL logic in one base class means a permission check, a rename, or a move works identically for files and folders — one code path, not two parallel ones. |
| Per-item ACL, no cascading from parent folders | Cascading folder-level permissions | Simpler to reason about and test for a timeboxed interview; explicitly named as the first thing a real system would add (see follow-ups) rather than silently assumed away. |
| `VersionRetentionStrategy` applied synchronously on every upload | A background sweep/cron job | Keeps the demo deterministic and immediately observable — you see the pruned list right after the upload call returns, no timing games needed to demonstrate it. |
| `PermissionLevel` as an ordinal-comparable enum | A `Set<Permission>` of fine-grained capabilities (READ, WRITE, DELETE, SHARE, ...) | Three ordered tiers cover "viewer/editor/owner" cleanly with O(1) comparisons; fine-grained capability sets are more flexible but add real complexity this problem's scope doesn't need. |

## Known gaps (deliberately out of scope for a timeboxed interview)

- No persistence — the entire hierarchy resets on JVM exit.
- No cascading permissions from parent folders to children.
- No move/rename/delete operations wired into the driver (the model
  supports rename via `setName`, but nothing exercises it).
- File content is an in-memory `String`, not a reference to real blob
  storage — unrealistic for large files.
- No search (by name, owner, or content).
- No trash/recycle-bin semantics for deleted items.
