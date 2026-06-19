# Sprint A09 — `meinchat` plugin

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Module:** `:plugins:meinchat` (→ `:core` only) ·
**Depends on:** A04 (notifications + cart for bot-cart cards) ·
**Blocks:** A10 ·
**Source:** `vbwd-ios-plugin-meinchat` (5,369 LOC, 37 files) — the largest plugin.

> **Sub-divide:** this is the biggest port. Build it as ordered slices
> A09.1–A09.5; each slice is independently gate-green before the next. Do **not**
> attempt it as one commit.

## Slices

### A09.1 — Domain + networking + caching
**Port** `Domain/*`, `MeinChatEndpoints`, `Background/CacheEvictionTask`.
- `MeinChatModels` (`@Serializable`), `MeinChatEndpoints`, `MeinChatService`
  (interface + impl over `ApiClient`), `MeinChatLimitsService`,
  `MessageCache` + `CacheKey` + `ClientRetentionResolver` + `CacheEvictionTask`
  (port the eviction/retention logic; a WorkManager/coroutine task replaces the
  iOS background task).
- `MeinChatSecureMessaging` **port-only** here (the protocol/seam; the real
  crypto impl is `meinchat-plus`, A10) + `BotChatTheme`.
- **TDD:** service happy/error; cache put/evict/retention math; limits service.

### A09.2 — Rooms + SSE streaming
**Port** `Views/RoomViewModel`, `InboxViewModel`, `ConversationViewModel` (domain
parts) + the SSE subscription.
- One streaming connection (`subscribe_many`) over OkHttp SSE / a `Flow`;
  rooms + inbox state as `StateFlow`. Port the "messages only on refresh" lesson:
  ensure no proxy buffering assumption — stream is `Flow`-driven.
- **TDD** (Turbine): inbox updates on stream events; room message append; reconnect.

### A09.3 — Bot rich-choice rendering (S70 `meta` contract)
**Port** `Views/BotChoiceCards`, `BotMenuList`, `BotCartCard`,
`ChatBubbleReceipts`, `MessageBubble`, `MessageComposer`, `FullscreenImageView`.
- Native Compose cards/menu/cart rendering the `BotReply.meta` vocab
  (`bot_choices`/`bot_menu`/`bot_cart`) with a plain-`body` fallback (Liskov).
  `BotCartCard` adds to the A04 `Cart`.
- **TDD:** each `meta` shape renders its card; unknown/empty `meta` → plain body;
  cart card → cart add.

### A09.4 — Conversation / room / inbox UI + composer
**Port** `Views/ConversationView`, `RoomView`, `InboxView`, `RoomHeaderView`,
`RoomInboxRow`, `CreateRoomSheet`, `InviteRoomMemberSheet`, `FindUserSheet`,
`TokenTransferSheet`, `MeinChatRouter`, `MeinChatFloatingButton`,
`ProfileNicknameSection`, `MeinChatRetentionSection`.
- Compose screens + sheets; `ProfileNicknameSection` is a `Profile*` component;
  retention section in settings.
- **TDD:** Compose render + interaction tests for compose/send, create/invite,
  token transfer; router resolves room/conversation deep links.

### A09.5 — Notifications + plugin wiring
**Port** `Notifications/MeinChatNotificationPermissions`,
`MeinChatNotificationRouting`, `MeinChatTokenSink`, `MeinChatPlugin`.
- FCM token sink relays via core `NotificationsSdk` (A04); notification routing
  opens the right room; runtime-permission request (Android 13+).
- `MeinChatPlugin` registers routes (`/meinchat`, prefix-matched), the floating
  button, dashboard/profile components, menu items, translations, the secure-
  messaging seam, and event subscriptions.
- **TDD:** `MeinChatPluginContractTest` (full registration + `uninstall` cleanup);
  token sink relays; routing opens the target room.

## Exit Gate

- [ ] `./gradlew :plugins:meinchat:check` + whole-repo `check` green (all slices).
- [ ] `:dependencyBoundaryCheck`: `:plugins:meinchat → :core` only.
- [ ] In the app: inbox + room messaging live over SSE; bot rich choices render
      natively (choices/menu/cart) with plain fallback; FCM notification opens
      the room; token transfer works.

## Principle notes

- **Liskov:** rich-choice rendering degrades to plain `body` for unknown `meta`;
  the secure-messaging seam has a stub default (plain path) — base contract held.
- **DIP:** depends on `:core` (`PlatformSdk`, `NotificationsSdk`, `Cart`) only;
  the real E2E crypto arrives via the declared `meinchat-plus` dependency (A10),
  never imported directly here.
- **No overengineering:** SSE is one `Flow`; no bespoke realtime framework.
