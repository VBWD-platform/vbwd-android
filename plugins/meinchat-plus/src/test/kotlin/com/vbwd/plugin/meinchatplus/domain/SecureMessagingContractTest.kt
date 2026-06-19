package com.vbwd.plugin.meinchatplus.domain

import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.MeinChatSecureMessaging
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * One contract run against both [StubSecureMessaging] and [SignalSecureMessaging]
 * (Liskov). Both are currently fail-closed: never ready, every secure op raises,
 * peer-can-receive false — never a plaintext fallback.
 */
class SecureMessagingContractTest {
    private val conversation = Conversation(id = "c1", protocol = "e2e_v1")

    private suspend fun assertFailClosed(secure: MeinChatSecureMessaging) {
        assertFalse(secure.isReady())
        assertFalse(secure.peerCanReceiveE2E("u1"))
        assertNotNull(runCatching { secure.sendSecure("hi", conversation) }.exceptionOrNull())
        assertNotNull(runCatching { secure.decryptIncoming(ChatMessage(id = "m1")) }.exceptionOrNull())
    }

    @Test
    fun `stub honours the fail-closed contract`() = runTest { assertFailClosed(StubSecureMessaging()) }

    @Test
    fun `signal (pending libsignal) honours the fail-closed contract`() = runTest {
        assertFailClosed(SignalSecureMessaging())
    }
}
