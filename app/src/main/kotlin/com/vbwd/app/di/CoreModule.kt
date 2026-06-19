package com.vbwd.app.di

import android.content.Context
import com.vbwd.core.cart.Cart
import com.vbwd.core.checkout.CheckoutSourceRegistry
import com.vbwd.core.config.AppConfig
import com.vbwd.core.domain.AuthService
import com.vbwd.core.domain.DefaultAuthService
import com.vbwd.core.domain.DefaultProfileService
import com.vbwd.core.domain.ProfileService
import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.events.EventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.OkHttpApiClient
import com.vbwd.core.notifications.DefaultNotificationsSdk
import com.vbwd.core.notifications.NotificationsSdk
import com.vbwd.core.persistence.EncryptedTokenStore
import com.vbwd.core.persistence.TokenStore
import com.vbwd.core.plugins.BundledPluginManifestLoader
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginHost
import com.vbwd.core.plugins.PluginManifestLoader
import com.vbwd.core.session.AuthSession
import com.vbwd.core.store.TokenBundleCheckoutSource
import com.vbwd.core.theme.SharedPrefsThemeStore
import com.vbwd.core.theme.ThemeManager
import com.vbwd.core.theme.ThemeRegistry
import com.vbwd.plugin.cms.CmsPlugin
import com.vbwd.plugin.example.ExamplePlugin
import com.vbwd.plugin.invoice.InvoicePaymentPlugin
import com.vbwd.plugin.meinchat.MeinChatPlugin
import com.vbwd.plugin.meinchatplus.MeinChatPlusPlugin
import com.vbwd.plugin.stripe.StripePaymentPlugin
import com.vbwd.plugin.subscription.SubscriptionPlugin
import com.vbwd.plugin.tarot.TarotPlugin
import com.vbwd.plugin.tokenpayment.TokenPaymentPlugin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The composition root — the Android port of the iOS `SDKContainer`. The **only**
 * place concrete adapters (`OkHttpApiClient`, `EncryptedTokenStore`) are named;
 * everything downstream depends on the interfaces and is constructor-injected by
 * Hilt (DIP / DI compliance). All singletons, so the whole app shares one
 * `ApiClient`/`TokenStore`/`AuthSession` (the same lifecycle the iOS container
 * gives its stored properties).
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideAppConfig(@ApplicationContext context: Context): AppConfig =
        AppConfig.load(context)

    @Provides
    @Singleton
    fun provideApiClientConfig(config: AppConfig): ApiClientConfig =
        ApiClientConfig(baseUrl = config.apiBaseUrl)

    @Provides
    @Singleton
    fun provideApiClient(config: ApiClientConfig): ApiClient =
        OkHttpApiClient(config)

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore =
        EncryptedTokenStore(context)

    @Provides
    @Singleton
    fun provideAuthService(client: ApiClient, store: TokenStore): AuthService =
        DefaultAuthService(client, store)

    @Provides
    @Singleton
    fun provideProfileService(client: ApiClient): ProfileService =
        DefaultProfileService(client)

    /** Shared event bus (A02.3) — AuthSession + plugins emit/subscribe here. */
    @Provides
    @Singleton
    fun provideEventBus(client: ApiClient): EventBus = DefaultEventBus(client)

    /** Theme registry (A03) — built-in themes; plugins may register more. */
    @Provides
    @Singleton
    fun provideThemeRegistry(): ThemeRegistry = ThemeRegistry()

    @Provides
    @Singleton
    fun provideThemeManager(
        @ApplicationContext context: Context,
        registry: ThemeRegistry,
    ): ThemeManager = ThemeManager(registry, SharedPrefsThemeStore(context))

    /** App-scoped supervisor scope for fire-and-forget work outside any screen. */
    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun provideAuthSession(
        service: AuthService,
        client: ApiClient,
        events: EventBus,
        scope: CoroutineScope,
    ): AuthSession {
        val session = AuthSession(service)
        // Auto sign-out on a 401 (expired / invalid token) — parity with the web
        // axios response interceptor that cleared the token. A02 also emits the
        // session-expired event on the shared bus (the 401 seam, now bus-backed).
        client.on(ApiEvent.TOKEN_EXPIRED) {
            events.emit(AppEvents.AUTH_SESSION_EXPIRED)
            scope.launch { if (session.isAuthenticated) session.signOut() }
        }
        return session
    }

    /** Session-scoped cart — single source of truth shared app-wide (A04). */
    @Provides
    @Singleton
    fun provideCart(): Cart = Cart()

    /** Checkout-source registry seeded with the built-in token-bundle source. */
    @Provides
    @Singleton
    fun provideCheckoutSourceRegistry(client: ApiClient, cart: Cart): CheckoutSourceRegistry =
        CheckoutSourceRegistry().apply { register(TokenBundleCheckoutSource(client, cart)) }

    /** Notifications seam (A04.5) — token relay + badge; plugins register sinks. */
    @Provides
    @Singleton
    fun provideNotificationsSdk(): NotificationsSdk = DefaultNotificationsSdk()

    /** Compiled-in plugins the host bundles (the available-plugins list). */
    @Provides
    @Singleton
    fun provideAvailablePlugins(appConfig: AppConfig, authSession: AuthSession): List<Plugin> = listOf(
        ExamplePlugin(),
        SubscriptionPlugin(),
        TokenPaymentPlugin(),
        StripePaymentPlugin(),
        InvoicePaymentPlugin(),
        CmsPlugin(
            postType = appConfig.rootPostTypeOnHost,
            category = appConfig.rootCategoryOnHost,
            webOrigin = appConfig.webOrigin,
            archiveUrl = appConfig.cmsArchiveUrl,
            tokenProvider = { authSession.accessToken },
        ),
        TarotPlugin(),
        MeinChatPlugin(),
        MeinChatPlusPlugin(),
    )

    /** Offline-first manifest loader (bundled `plugins.json`). */
    @Provides
    @Singleton
    fun providePluginManifestLoader(@ApplicationContext context: Context): PluginManifestLoader =
        BundledPluginManifestLoader(context)

    @Provides
    @Singleton
    fun providePluginHost(
        client: ApiClient,
        apiClientConfig: ApiClientConfig,
        manifestLoader: PluginManifestLoader,
        plugins: List<Plugin>,
        events: EventBus,
        cart: Cart,
        checkoutSources: CheckoutSourceRegistry,
        notifications: NotificationsSdk,
    ): PluginHost = PluginHost(
        api = client,
        apiConfig = apiClientConfig,
        manifestLoader = manifestLoader,
        plugins = plugins,
        events = events,
        cart = cart,
        checkoutSources = checkoutSources,
        notifications = notifications,
    )
}
