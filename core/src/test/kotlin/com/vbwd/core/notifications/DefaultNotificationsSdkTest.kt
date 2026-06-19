package com.vbwd.core.notifications

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class RecordingSink : DeviceTokenSink {
    val tokens = mutableListOf<String>()
    override suspend fun handleDeviceToken(tokenHex: String) {
        tokens.add(tokenHex)
    }
}

class DefaultNotificationsSdkTest {
    @Test
    fun `a sink registered before the token receives it on arrival`() = runTest {
        val sdk = DefaultNotificationsSdk()
        val sink = RecordingSink()
        sdk.registerSink(sink)
        sdk.didReceiveDeviceToken("TOK")
        assertEquals(listOf("TOK"), sink.tokens)
    }

    @Test
    fun `a buffered token is replayed to a sink that registers later`() = runTest {
        val sdk = DefaultNotificationsSdk()
        sdk.didReceiveDeviceToken("TOK")
        val sink = RecordingSink()
        sdk.registerSink(sink)
        assertEquals(listOf("TOK"), sink.tokens)
        assertEquals("TOK", sdk.currentTokenHex())
    }

    @Test
    fun `setBadgeCount clamps negative values to zero`() = runTest {
        var applied = -1
        val sdk = DefaultNotificationsSdk(badgeSetter = { applied = it })
        sdk.setBadgeCount(-5)
        assertEquals(0, applied)
    }
}
