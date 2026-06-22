---
title: The host app
---

# The host app

[← Back to index](index.md)

The host app (`:app` / the `vbwd-android-app-example` repo) is thin: it names
concrete adapters, lists the plugins to bundle, and ships config. `vbwd-android-core`
provides everything else.

## The composition root

`app/src/main/kotlin/com/vbwd/app/di/CoreModule.kt` is a Hilt module — the **only**
place concrete adapters are constructed:

```kotlin
@Module @InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides @Singleton
    fun provideApiClient(config: ApiClientConfig): ApiClient = OkHttpApiClient(config)

    @Provides @Singleton
    fun provideTokenStore(@ApplicationContext c: Context): TokenStore = EncryptedTokenStore(c)

    /** The available-plugins list the host bundles. */
    @Provides @Singleton
    fun provideAvailablePlugins(/* deps */): List<Plugin> = listOf(
        ExamplePlugin(), SubscriptionPlugin(), MeinChatPlugin(), /* … */
    )

    /** Offline-first manifest (bundled plugins.json). */
    @Provides @Singleton
    fun providePluginManifestLoader(@ApplicationContext c: Context): PluginManifestLoader =
        BundledPluginManifestLoader(c)

    @Provides @Singleton
    fun providePluginHost(/* api, manifestLoader, plugins, events, cart, … */): PluginHost =
        PluginHost(/* … */)
}
```

`@JvmSuppressWildcards` is needed on the `List<Plugin>` parameter so Dagger matches
the covariant list — keep it when you add providers.

## Bootstrapping

`AppRoot` themes the app, boots the host, then drives the auth shell:

```kotlin
@Composable
fun AppRoot(session: AuthSession, pluginHost: PluginHost, themeManager: ThemeManager) {
    var booted by remember { mutableStateOf(false) }
    LaunchedEffect(pluginHost) { pluginHost.bootstrap(); booted = true }

    VbwdTheme(themeManager) {
        if (!booted) /* spinner */
        else RootView(
            session = session,
            loginContent = { LoginScreen(hiltViewModel()) },
            authenticatedContent = {
                AppShellView(
                    host = pluginHost,
                    session = session,
                    dashboardContent = { DashboardScreen(hiltViewModel()) },
                    profileContent = {
                        ProfileEditScreen(hiltViewModel(),
                            profileSections = pluginHost.components.profileComponents())
                    },
                    settingsContent = { SettingsScreen(themeManager) },
                )
            },
        )
    }
}
```

## Two config assets

### `plugins.json` — enable/disable manifest

```json
{
  "plugins": {
    "example":  { "enabled": true,  "version": "1.0.0", "source": "local" },
    "meinchat": { "enabled": true,  "version": "1.1.0", "source": "local" },
    "tarot":    { "enabled": false, "version": "0.1.0", "source": "local" }
  }
}
```

Only `enabled` plugins are installed. The bundled file is the offline default; the
backend's `/admin/frontend-plugins/<app>` endpoint is the runtime single-writer
(Android reads, never writes).

### `vbwd_config.json` — runtime config

```json
{
  "api_base_url": "http://10.0.2.2:5000/api/v1",
  "root_android_category_on_host": "news",
  "root_android_post_type_on_host": "post",
  "web_base_url": "http://10.0.2.2:8080"
}
```

- `10.0.2.2` is the emulator's alias for the host machine's `localhost`.
- The **media/web origin** is derived by stripping `/api/vN` from `api_base_url`
  (used e.g. to absolutise relative attachment URLs).

## Adding a plugin to the host — checklist

1. Add the dependency (`implementation("com.vbwd:vbwd-android-<name>:<ver>")`) or
   the module.
2. Add `MyPlugin()` to `provideAvailablePlugins`.
3. Add its entry to `plugins.json`.

---

Next: [Testing & the quality gate →](testing.md)
