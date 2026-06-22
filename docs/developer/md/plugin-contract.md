---
title: The plugin contract
---

# The plugin contract

[← Back to index](index.md)

Everything a plugin can do is defined by two types in
`com.vbwd.core.plugins`: the **`Plugin`** interface you implement, and the
**`PlatformSdk`** facade you receive. A plugin depends on these — and nothing else
in core.

## The `Plugin` interface

```kotlin
interface Plugin {
    val metadata: PluginMetadata

    suspend fun install(sdk: PlatformSdk) {}   // register your seams here
    suspend fun activate() {}                   // becomes "live"
    suspend fun deactivate() {}                 // active-dependent guarded
    suspend fun uninstall() {}                  // tear down (unsubscribe, etc.)
}
```

All hooks are `suspend`; all but `install` default to no-op, so override only what
you need. Lifecycle: `register → install → activate → deactivate → uninstall`.

### `PluginMetadata`

```kotlin
data class PluginMetadata(
    val name: String,                       // unique id, e.g. "meinchat"
    val version: SemanticVersion,           // SemanticVersion(1, 1, 0)
    val description: String? = null,
    val author: String? = null,
    val homepage: String? = null,
    val keywords: List<String> = emptyList(),
    val dependencies: PluginDependencies = PluginDependencies.None,
    val translations: Map<String, Map<String, String>> = emptyMap(),
)
```

**Declaring a peer dependency** (the only way one plugin may use another):

```kotlin
dependencies = PluginDependencies.List(listOf("meinchat"))          // any version
dependencies = PluginDependencies.Constrained(mapOf("meinchat" to "^1.0.0"))
```

The registry installs in topological order and checks the semver constraint
before installing the dependent.

## The `PlatformSdk` facade

This is the **single seam** through which you extend the app. You get it in
`install(sdk)`.

### Injected collaborators

| Member | Type | Use |
|--------|------|-----|
| `sdk.api` | `ApiClient` | Call the backend (`get`/`post`/`put`/…, verb methods). |
| `sdk.apiConfig` | `ApiClientConfig` | `baseUrl` (e.g. to derive a media origin). |
| `sdk.events` | `EventBus` | Subscribe/emit app events (auth, checkout, …). |
| `sdk.notifications` | `NotificationsSdk` | Register a push-token sink / badge. |
| `sdk.cart` | `Cart` | The session-wide cart (add items, read contents). |
| `sdk.checkoutSources` | `CheckoutSourceRegistry` | Register a checkout source. |

### Extension methods

```kotlin
// Routes — screens reachable by path
fun addRoute(route: PluginRoute)

// Components — named Compose factories the shell discovers by prefix
fun addComponent(name: String, factory: @Composable () -> Unit)
fun removeComponent(name: String)

// Stores — share state/services across the plugin under a key
fun createStore(id: String, store: Any)

// i18n
fun addTranslations(locale: String, messages: Map<String, String>)

// Side menu
fun addMenuItem(item: MenuItem)
fun removeMenuItem(id: String)

// Payment + checkout (for payment-method / commerce plugins)
fun addPaymentAction(code: String, handler: PaymentActionHandler)
fun addCheckoutSource(source: CheckoutSource)
fun removeCheckoutSource(id: String)
```

## Seam reference

### Routes

```kotlin
PluginRoute(
    path = "/meinchat",
    name = "meinchat",
    requiresAuth = true,            // gate behind login
    requiredUserPermission = null,  // optional permission string
    matchPrefix = true,             // any path under /meinchat matches
) { MeinChatScreen(/* … */) }       // content: @Composable
```

### Components (the naming convention)

`addComponent(name) { … }` registers a Compose factory. The shell renders it by
**name prefix**:

| Prefix | Where it renders |
|--------|------------------|
| `Dashboard*` | a card on the Dashboard (e.g. `"DashboardExample"`) |
| `Profile*` | a section on the Profile screen (e.g. `"ProfileMeinChatNickname"`) |

```kotlin
sdk.addComponent("DashboardExample") { ExampleDashboardWidget(store) }
sdk.addComponent("ProfileExample")   { ExampleProfileSection() }
```

### Menu items

```kotlin
sdk.addMenuItem(
    MenuItem(
        id = "meinchat",
        icon = "chat",
        title = "MeinChat",
        routePath = "/meinchat",
        order = 40,
        section = "top",
    ),
)
```

### Events

```kotlin
val unsubscribe = sdk.events.on(AppEvents.AUTH_LOGIN) { /* payload */ }
sdk.events.emit(AppEvents.CHECKOUT_FAILED)
// in uninstall(): unsubscribe()
```

Common events: `AUTH_LOGIN`, `AUTH_LOGOUT`, `AUTH_SESSION_EXPIRED`,
`CHECKOUT_FAILED`, `PLUGIN_ERROR`.

### Payment actions

A payment-method plugin registers a handler keyed by a code. After checkout
creates an invoice, the handler decides what happens next:

```kotlin
sdk.addPaymentAction("stripe") { invoiceId ->
    val url = createStripeSession(invoiceId)
    PaymentAction.OpenUrl(url)            // open external payment page
}
// or PaymentAction.ShowConfirmation     // go straight to confirmation (e.g. invoice)
```

Registering **no** handler for a method makes generic checkout go straight to the
confirmation page (that is how the invoice plugin works).

## Error isolation contract

If any of your hooks throws, the registry marks your plugin
`PluginStatus.Error(reason)` and continues with the others — it never aborts the
shell. Cancellation always propagates (use
`currentCoroutineContext().ensureActive()` inside broad `catch` blocks rather than
swallowing it).

---

Next: [Writing a plugin →](writing-a-plugin.md)
