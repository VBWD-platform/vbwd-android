package com.vbwd.core.events

import com.vbwd.core.networking.ApiError
import com.vbwd.core.testutil.FakeApiClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventBusTest {
    private fun bus(api: FakeApiClient = FakeApiClient()) = DefaultEventBus(api)

    @Test
    fun `on delivers emitted payloads`() {
        val received = mutableListOf<Any?>()
        val eventBus = bus()
        eventBus.on("evt") { received.add(it) }
        eventBus.emit("evt", "payload")
        assertEquals(listOf<Any?>("payload"), received)
    }

    @Test
    fun `unsubscribe stops delivery`() {
        val received = mutableListOf<Any?>()
        val eventBus = bus()
        val unsubscribe = eventBus.on("evt") { received.add(it) }
        eventBus.emit("evt", 1)
        unsubscribe()
        eventBus.emit("evt", 2)
        assertEquals(listOf<Any?>(1), received)
    }

    @Test
    fun `once fires exactly once`() {
        var count = 0
        val eventBus = bus()
        eventBus.once("evt") { count++ }
        eventBus.emit("evt")
        eventBus.emit("evt")
        assertEquals(1, count)
    }

    @Test
    fun `off removes all listeners for a name`() {
        var count = 0
        val eventBus = bus()
        eventBus.on("evt") { count++ }
        eventBus.off("evt")
        eventBus.emit("evt")
        assertEquals(0, count)
        assertFalse(eventBus.hasListeners("evt"))
    }

    @Test
    fun `history caps at maxHistory and filters by name`() {
        val eventBus = DefaultEventBus(FakeApiClient(), maxHistory = 3)
        repeat(5) { eventBus.emit("a") }
        eventBus.emit("b")
        assertEquals(3, eventBus.history().size) // capped
        assertEquals(1, eventBus.history("b").size)
    }

    @Test
    fun `local-only events are never forwarded to the backend`() = runTest {
        val api = FakeApiClient()
        val eventBus = bus(api)
        val forwarded = eventBus.sendToBackend(AppEvents.MODAL_OPEN)
        assertFalse(forwarded)
        assertEquals(0, eventBus.pendingCount())
        assertEquals(0, api.requestCount)
    }

    @Test
    fun `failed backend send is retained and retried on the next flush`() = runTest {
        val api = FakeApiClient(nextError = ApiError.Transport("offline"))
        val eventBus = bus(api)

        assertFalse(eventBus.sendToBackend(AppEvents.PAYMENT_SUCCEEDED))
        assertEquals(1, eventBus.pendingCount())

        api.nextError = null
        assertTrue(eventBus.flushPending())
        assertEquals(0, eventBus.pendingCount())
    }
}
