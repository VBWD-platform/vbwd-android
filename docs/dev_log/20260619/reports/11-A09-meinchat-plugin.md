# Report 11 — A09 meinchat plugin (backbone)

**Date:** 2026-06-19 · **Sprint:** [A09](../sprints/A09-plugin-meinchat.md) ·
**Module:** `:plugins:meinchat` (→ `:core` only) · **Status:** on disk, **NOT
gate-checked** (JDK 26 / no Android SDK). The largest plugin (iOS: 5,369 LOC / 37
files); ported as a **tested backbone + lean UI** (scope note below).

## What landed (port of `vbwd-ios-plugin-meinchat`)
- **Domain models** — `Conversation`, `ChatMessage`/`MessageAttachment`, `Room`,
  nickname/token models, and the **bot `meta` vocab** (`MessageMeta` sealed type
  with a custom kind-dispatch serializer: `bot_choices`/`bot_action`/`bot_menu`/
  `bot_cart`, unknown ⇒ `Unknown` → plain-body fallback, Liskov).
- `MeinChatEndpoints`, `MeinChatService` (interface + impl: conversations,
  messages, sendText+meta, rooms, nickname, token transfer, device register).
- `MessagingLimits`/`MessagingCapabilities` + `MeinChatLimitsService` (TTL cache,
  404 → defaults, last-good preserved).
- `ClientRetentionResolver` (the testable retention math: `min(user, server)`,
  clamp) over a `RetentionStore` seam.
- `MessageCache` seam + `InMemoryMessageCache` (upsert/list/remove/**evict**).
- `MeinChatSecureMessaging` cross-plugin seam + `MeinChatSecureMessagingStoreId`
  (implemented by A10).
- `MeinChatTokenSink` (auth-aware FCM `DeviceTokenSink`: buffers until login).
- UI: `BotMetaContent` (native choices/menu/cart rendering; cart → core `Cart`),
  `MeinChatScreen` (inbox → conversation + composer), inbox/conversation VMs.
- `MeinChatPlugin` — `/meinchat` (prefix), profile section, menu item,
  translations, the limits/retention/cache stores, the token sink + auth events.
  Wired into the app + `plugins.json`.

## Tests (TDD)
`MeinChatModelsTest` (bot-meta parse: choices/cart/unknown), `MeinChatLimitsServiceTest`,
`ClientRetentionResolverTest` (min + clamp), `MessageCacheTest` (evict),
`MeinChatServiceTest` (MockK), `MeinChatTokenSinkTest` (buffer-until-login),
`MeinChatPluginContractTest` (route/section/menu/translations/stores).

## Scope note (deferred, documented)
The full SSE streaming (one `Flow` over OkHttp-SSE), the at-rest **encrypted**
cache (iOS CoreData + ChaChaPoly under a Keystore KEK → needs Room/SQLCipher +
Keystore, built with the SDK present), and the many sheet UIs (create-room,
invite, token-transfer, fullscreen image) are **deferred**. The in-memory cache
carries the tested semantics; the plugin degrades to the no-cache path. This
keeps A09 a real, tested backbone without fabricating un-runnable realtime/crypto
infrastructure.

## Validation
**Gate NOT run** (JDK 26, no Android SDK). `./gradlew :plugins:meinchat:check
dependencyBoundaryCheck :app:assembleDebug` in Studio/CI. Self-review: imports
ordered, lines ≤120, `ApiError`-specific catches, boundary = `:core` only.

## Next: A10 (meinchat-plus E2E).
