package com.vbwd.plugin.meinchatplus

import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginDependencies
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.SemanticVersion
import com.vbwd.plugin.meinchat.domain.MeinChatSecureMessagingStoreId
import com.vbwd.plugin.meinchatplus.domain.DefaultDeviceRegistryService
import com.vbwd.plugin.meinchatplus.domain.DefaultPrekeyService
import com.vbwd.plugin.meinchatplus.domain.StubSecureMessaging

/**
 * E2E-encryption layer for `meinchat` — the canonical **declared plugin→plugin
 * dependency** (`dependencies = ["meinchat"]`): it consumes meinchat's
 * secure-messaging seam, registering a [com.vbwd.plugin.meinchat.domain.MeinChatSecureMessaging]
 * impl under [MeinChatSecureMessagingStoreId].
 *
 * Current state (port of the iOS plugin): the HTTP wire services
 * ([DefaultDeviceRegistryService], [DefaultPrekeyService]) + the pure-logic
 * `Padding`/`EnvelopePacker`/`DowngradeGuard` are usable today; the actual
 * Signal session is a **fail-closed [StubSecureMessaging]** until a maintained
 * Kotlin/JVM Signal library + Android-Keystore identity store are vendored
 * (mirrors the iOS `#if canImport(LibSignalClient)` gate).
 */
class MeinChatPlusPlugin : Plugin {
    override val metadata = PluginMetadata(
        name = "meinchat-plus",
        version = SemanticVersion(0, 2, 0),
        description = "End-to-end encrypted messaging — Signal ratchet over meinchat.",
        author = "VBWD",
        keywords = listOf("chat", "messaging", "e2e", "signal", "meinchat"),
        dependencies = PluginDependencies.List(listOf("meinchat")),
        translations = mapOf("en" to TRANSLATIONS),
    )

    override suspend fun install(sdk: PlatformSdk) {
        // HTTP wire services — usable today against the S28.3b backend.
        sdk.createStore("meinchatPlusDevices", DefaultDeviceRegistryService(sdk.api))
        sdk.createStore("meinchatPlusPrekeys", DefaultPrekeyService(sdk.api))

        // Cross-plugin secure-messaging seam. Fail-closed stub until libsignal
        // is vendored; swap for SignalSecureMessaging then (one-line change).
        sdk.createStore(MeinChatSecureMessagingStoreId, StubSecureMessaging())

        sdk.addTranslations("en", TRANSLATIONS)
    }

    private companion object {
        val TRANSLATIONS = mapOf(
            "meinchat_plus.title" to "Secure Messaging",
            "meinchat_plus.pair" to "Pair this device",
            "meinchat_plus.not_ready" to "Secure messaging not yet available on this build.",
        )
    }
}
