# meinchat — Walkthrough Report (rooms, invites, security)

**Date:** 2026-06-22
**Build/run status:** ✅ **Tests run & green here.** Findings are code-grounded *and*
corroborated by (a) the meinchat Robolectric Compose UI tests, which I ran, and (b) the
**8 real screenshots committed in the repo** (`plugins/meinchat/docs/screenshots/`,
538×1200, captured from the running app against the `vbwd.cc` backend) — embedded below.

---

## 0. Toolchain note (correction)

An earlier draft claimed this box couldn't build (only JDK 26). **That was wrong.**
JDK 17 *is* installed (Homebrew `openjdk@17`, not registered with `java_home`), and
`gradle.properties` pins the Gradle daemon to it via `org.gradle.java.home`. Verified by
running:

```bash
./gradlew :meinchat:meinchat:test          # (composite build: NOT :plugins:meinchat — CLAUDE.md path is stale)
# BUILD SUCCESSFUL — ui.MeinChatScreenTest 3/3 pass; screenshot + domain tests green
./gradlew :meinchat:meinchat:recordRoborazziDebug -Proborazzi.test.record=true   # re-record screenshots
```

The screenshots below are **real captures already in the repo**, not mockups. For a
*live* rooms/invite flow you still need the Flask backend + an emulator (neither is in
this sandbox), but every screen relevant to the three tasks is covered by the committed
captures.

---

## 1. What the meinchat client actually exposes

The entire navigable surface is in `ui/MeinChat.kt`. Two tabs + a search:

| Surface | Source | Capability |
|---|---|---|
| **Chats** tab | `DirectInbox` | list 1:1 conversations · `+ New chat` button |
| **New chat** | `NewChatSearch` | search a nickname → open a 1:1 conversation |
| **Rooms** tab | `RoomsList` | **list** rooms · open a room · send text |
| Conversation / Room view | `ConversationView` / `RoomView` | read thread, send messages |

The backing service (`MeinChatService`) has exactly these verbs:
`fetchConversations · openConversation · fetchMessages · sendText · markRead ·
searchNickname · transferTokens · fetchMyNickname · setNickname · fetchRooms ·
fetchRoomMessages · sendRoomText · register/unregister device token`.

**That list is the whole story for your three tasks** — note what is *absent*.

---

## 2. Walkthrough (real captures)

Images are the committed repo screenshots
(`plugins/meinchat/docs/screenshots/`); paths are relative to this report.

### Step 1 — Chats inbox
![Chats inbox](../../../../plugins/meinchat/docs/screenshots/01-inbox-chats-tabs.png)

`Chats`/`Rooms` segmented toggle, conversation cards with colour-keyed avatars,
previews and unread badges (e.g. `testexample •2`). Note `joric → [token_transfer]`
and `alice → [demo-meinchat] hi bobby — sending you 25 to…`. (`+ New chat` lives in
`DirectInbox` above the list; this capture is scrolled to the conversations.)

### Step 2 — New chat / nickname search
![New chat search](../../../../plugins/meinchat/docs/screenshots/07-new-chat-search.png)

Typing `alice` returns `alice` and `alice26` as "Tap to start chatting" hits. The only
way to open a chat is to tap a returned hit → `openConversation(nickname)`. Min 2 chars
before it queries.

### Step 3 — Rooms tab
![Rooms list](../../../../plugins/meinchat/docs/screenshots/05-rooms-list.png)

Read-only list: `a1cd3163-d412-…` (a room whose `name` is unset, so the UI falls back
to `room.id` — `room.name ?: room.id`), `new room` (3 members→ here 2), `b`, with member
counts + unread badges. **There is no "create room" control anywhere on this surface** —
these rooms were created by another client/the backend.

Other committed captures: `02-conversation-bubbles`, `03-token-transfer-cards`,
`04-image-attachment`, `06-room-view`, `08-nickname-section`.

---

## 3. Your three tasks — results

### ❌ "Create new rooms"  → **not possible in the Android client**
The Rooms surface is **read-only**. `RoomsList` lists what `fetchRooms()` returns and
lets you open/send; there is **no create-room button, no create-room endpoint**.
`MeinChatEndpoints` has only `ROOMS` (GET list), `roomMessages`, `roomRead` — no POST to
create a room and no member endpoint. Rooms are provisioned **server-side / by another
client**; this port consumes them but cannot make them.

### ❌ "Invite assistant and consultant"  → **no invite flow, and no such concept**
- **No invite UI or endpoint** exists (grep for `invite`/`addMember`/`create_room` across
  `meinchat/src/main` → **NONE**). You cannot add members to a room from here.
- **"assistant"/"consultant" don't exist** as roles in the client (grep → NONE). The only
  bot concept is *rich-message rendering* (`BotChoice`/`BotCart`/`BotMenu` in
  `MessageMeta`) — i.e. the client renders bot replies; it has no notion of inviting a
  named assistant. If "assistant"/"consultant" are **backend bot accounts with
  nicknames**, the only way to reach them from Android is **New chat → search their
  nickname → open a 1:1** (Step 2). There is no group-invite path.

### ⚠️ "Can you add a person who doesn't exist?"  → **No — and it fails silently**
You *cannot* type a free-text name and add them. The only way to start a chat is to tap a
**search hit**, and `openConversation` is reachable **only** from a returned hit. So:

- Search a non-existent nickname → backend returns `[]` → `results = emptyList()` →
  **the list is simply empty.** No error, no "user not found" message.
- The code actively swallows failures: `searchNickname` → `getOrDefault(emptyList())`
  (`MeinChatNewChat.kt:57`) and `open` → `getOrNull()` (`:61`). So even a backend
  rejection degrades to "nothing appears / nothing happens."

**Verdict:** adding a ghost user is *structurally impossible* via the UI, which is good for
integrity — but the **silent empty state is a UX gap** (indistinguishable from "no
network" or "search too short"). Recommend an explicit "No users found" empty state.

---

## 4. How secure is it?

Honest, code-grounded assessment. Two layers: what ships today, and the cover channel
([design doc 01](01-meinchat-cover-channel-design.md), not yet built).

**Strengths (today)**
- **Opt-in E2E exists.** `protocol == "e2e_v1"` conversations route through
  `MeinChatSecureMessaging` (impl in `meinchat-plus`), **fail-closed** — the contract
  *must throw rather than transmit plaintext* for a secure row. The UI surfaces it
  (🔒 chip, "End-to-end encrypted" subtitle).
- **Transport** is the shared `:core` `ApiClient` (OkHttp) over the configured
  `api_base_url` — use HTTPS in prod (the emulator `10.0.2.2:5000` default is **dev-only,
  cleartext**).
- **Token store** is `EncryptedSharedPreferences` (`EncryptedTokenStore`) — keys at rest
  are OS-protected.
- **No ghost-user injection** (§3) — the open path is gated by server search results.

**Weaknesses / risks**
1. **E2E is per-conversation and opt-in, not default.** A plain conversation sends
   `body` in cleartext to the server — readable by the **DB/Redis admin**. This is the
   exact gap design-doc 01 targets, and today it is **open** for non-`e2e_v1` chats.
2. **Even with `e2e_v1`, traffic is *observable*.** The `envelope_b64` blob screams
   "encrypted payload" to a network monitor / DB admin. Content is hidden; the *fact of
   secret comms* is not. (→ the cover channel fixes this; **not yet implemented**.)
3. **Nickname enumeration.** `searchNickname` (min 2 chars) returns
   `NicknameSearchHit(nickname, userId)` — i.e. it leaks **user IDs** and allows
   prefix-enumeration of the user base. Privacy/abuse surface worth rate-limiting and
   considering whether `userId` should be returned at all.
4. **Rooms have no client-side E2E path.** Room sends (`sendRoomText`) go through the
   plain `body`; no `e2e_v1` equivalent for group rooms is wired here.
5. **Silent error swallowing** (§3) can mask security-relevant failures (auth expiry,
   tampering) as empty UI.

**Bottom line:** *transport + at-rest token security are sound; content confidentiality is
solid but opt-in (E2E) and limited to 1:1; unobservability against a monitoring admin is
the open gap* — which is precisely what the `cover_v1` design is for. Rooms are the
weakest surface (no E2E, read-only here).

---

## 5. Recommendations

1. Add an explicit **"No users found"** empty state to `NewChatSearch` (close the §3 UX gap).
2. **Rate-limit / minimise** `searchNickname` — reconsider returning `userId`.
3. Decide whether **E2E should be default** for 1:1, and whether **rooms** get an E2E path.
4. Prioritise the **`cover_v1`** determinism spike (design 01 §8/§11) — it's the only item
   that closes weakness #2.
5. If a **create-room / invite** capability is wanted on Android, it's a new feature:
   needs backend endpoints + new `MeinChatService` verbs + UI — none exist today.

---

## 6. Test & gate evidence (run 2026-06-22)

All run locally on the pinned JDK 17 (Gradle 8.13 / AGP 8.9.1 / Kotlin 2.0.21).

- **meinchat suite** — `./gradlew :meinchat:meinchat:test` → **BUILD SUCCESSFUL**.
  - `ui.MeinChatScreenTest` (Robolectric **Compose UI** test) — **3/3 pass**:
    *inbox shows the chats and rooms tabs plus the new-chat entry* · *tapping the rooms
    tab shows the rooms list* · *the nickname section shows the current nickname*.
  - `screenshot.MeinChatScreenshotTest` (Roborazzi) + all `domain.*` / contract /
    notifications unit tests — green.
- **Screenshots re-recorded** — `./gradlew :meinchat:meinchat:recordRoborazziDebug
  -Proborazzi.test.record=true` → BUILD SUCCESSFUL (report under
  `meinchat/build/reports/roborazzi/`).
- **Full CI gate** — `./gradlew check dependencyBoundaryCheck` across **all 10 included
  builds + :app** → **BUILD SUCCESSFUL** (ktlint + detekt + unit tests + Android lint +
  boundary rule all green).

### Build-infra finding → FIXED (2026-06-22)
Two coupled bugs in `:dependencyBoundaryCheck` (root `build.gradle.kts`), found and
fixed during this run:

1. **It enforced nothing.** Post-composite-refactor, plugins are `includeBuild(...)`s,
   not root `subprojects`, so the task's `subprojects.filter { ":plugins:" }` matched
   **zero** modules — it had been *vacuously green* (`OK (0 plugin module(s) checked)`).
2. **Config-cache incompatible.** It walked the project `configurations` model in
   `doLast`, which the configuration cache (`org.gradle.configuration-cache=true` in
   `gradle.properties`) forbids — so the gate errored at cache-store time unless run
   with `--no-configuration-cache`.

**Fix:** rewrote the task to *statically scan each plugin module's `build.gradle.kts`*
for substituted `com.vbwd:vbwd-android-<module>` coordinates (the form plugins now use),
checking them against the `declaredPeerDependencies` allow-map (mirrors the iOS
`boundary-lint.sh`). It touches only `File`s + plain data → config-cache safe.

Verified: now reports **`OK (9 plugin module(s) checked)`**; passes `check` **and**
`dependencyBoundaryCheck` with the config cache **enabled** (stored + reused, no
problems); and a negative test (dropping meinchat-plus's declared peer) correctly
**fails** with `:plugins:meinchat-plus -> :plugins:meinchat (allowed: [:core])`.

---

*Screens above are the committed real captures; re-record with the §6 Roborazzi command.*
