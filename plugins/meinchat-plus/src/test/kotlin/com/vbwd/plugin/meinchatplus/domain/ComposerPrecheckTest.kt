package com.vbwd.plugin.meinchatplus.domain

import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.MeinChatSecureMessaging
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class FakeSecure(
    private val ready: Boolean,
    private val peerOk: Boolean = false,
    private val throwOnPeer: Boolean = false,
) : MeinChatSecureMessaging {
    override suspend fun isReady(): Boolean = ready
    override suspend fun sendSecure(plaintext: String, conversation: Conversation): ChatMessage = error("unused")
    override suspend fun decryptIncoming(message: ChatMessage): String = error("unused")
    override suspend fun peerCanReceiveE2E(userId: String): Boolean {
        if (throwOnPeer) error("probe failed")
        return peerOk
    }
}

class ComposerPrecheckTest {
    @Test
    fun `local not paired disables the composer`() = runTest {
        assertEquals(
            ComposerPrecheckResult.LocalNotPaired,
            ComposerPrecheck(FakeSecure(ready = false)).check("u"),
        )
    }

    @Test
    fun `ready when paired and the peer can receive`() = runTest {
        assertEquals(
            ComposerPrecheckResult.Ready,
            ComposerPrecheck(FakeSecure(ready = true, peerOk = true)).check("u"),
        )
    }

    @Test
    fun `peer cannot receive disables the composer`() = runTest {
        assertEquals(
            ComposerPrecheckResult.PeerCannotReceive,
            ComposerPrecheck(FakeSecure(ready = true, peerOk = false)).check("u"),
        )
    }

    @Test
    fun `a probe failure enables optimistically`() = runTest {
        val result = ComposerPrecheck(FakeSecure(ready = true, throwOnPeer = true)).check("u")
        assertInstanceOf(ComposerPrecheckResult.ProbeFailedOptimistic::class.java, result)
        assertTrue(result.canCompose)
    }
}
