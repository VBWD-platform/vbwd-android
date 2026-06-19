package com.vbwd.plugin.meinchatplus.domain

import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.MeinChatSecureMessaging

/** Raised by the fail-closed secure-messaging impls — never returns plaintext. */
class SecureMessagingNotReadyException :
    Exception("Secure messaging (Signal) is not yet available in this build")

/**
 * Liskov fake / disabled-path default for [MeinChatSecureMessaging]: every
 * secure op raises, peer-can-receive is false, never ready. Port of the iOS
 * `StubSecureMessaging`.
 */
class StubSecureMessaging : MeinChatSecureMessaging {
    override suspend fun isReady(): Boolean = false
    override suspend fun sendSecure(plaintext: String, conversation: Conversation): ChatMessage =
        throw SecureMessagingNotReadyException()
    override suspend fun decryptIncoming(message: ChatMessage): String =
        throw SecureMessagingNotReadyException()
    override suspend fun peerCanReceiveE2E(userId: String): Boolean = false
}

/**
 * The real Signal-protocol implementation seam. **Fail-closed stub pending a
 * maintained Kotlin/JVM Signal library (`libsignal`) + Android-Keystore identity
 * storage** — exactly as the iOS port keeps `SignalSecureMessaging` behind
 * `#if canImport(LibSignalClient)` and ships the stub until the library is
 * vendored. Until then it throws rather than ever transmitting plaintext.
 *
 * When the library lands, this class gains X3DH/Double-Ratchet over
 * [EnvelopePacker] + [Padding] and takes the [DeviceRegistryService] /
 * [PrekeyService] collaborators; the only wiring change is registering it
 * instead of [StubSecureMessaging] in the plugin.
 */
class SignalSecureMessaging : MeinChatSecureMessaging {
    override suspend fun isReady(): Boolean = false
    override suspend fun sendSecure(plaintext: String, conversation: Conversation): ChatMessage =
        throw SecureMessagingNotReadyException()
    override suspend fun decryptIncoming(message: ChatMessage): String =
        throw SecureMessagingNotReadyException()
    override suspend fun peerCanReceiveE2E(userId: String): Boolean = false
}
