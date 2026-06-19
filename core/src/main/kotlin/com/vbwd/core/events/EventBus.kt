package com.vbwd.core.events

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.post
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.serialization.Serializable

/** Unsubscribe handle returned by [EventBus.on]. */
typealias Unsubscribe = () -> Unit

/** A recorded emission (capped history). */
data class EventRecord(val name: String, val atMillis: Long)

/**
 * Pub/sub bus. Port of the iOS `EventBus` (on/emit/once/off, capped history,
 * backend batching with retry, local-only exclusion). Core ↔ plugin
 * communication goes through this only (OCP — neither imports the other).
 *
 * Faithful to the iOS source: `emit` notifies local listeners only (synchronous,
 * fire-and-forget); backend forwarding is explicit via [sendToBackend] and
 * excludes [AppEvents.localOnly].
 */
interface EventBus {
    fun emit(name: String, payload: Any? = null)
    fun on(name: String, callback: (Any?) -> Unit): Unsubscribe
    fun once(name: String, callback: (Any?) -> Unit)
    fun off(name: String)
    fun hasListeners(name: String): Boolean
    fun listenerCount(name: String): Int
    fun history(name: String? = null): List<EventRecord>
    fun clear()
    fun clearHistory()

    /** Queue an event for the backend and flush. Local-only events are skipped
     *  (returns false — web parity, not an error). Returns whether the batch
     *  reached the backend. */
    suspend fun sendToBackend(name: String): Boolean
    suspend fun flushPending(): Boolean
    fun pendingCount(): Int
}

/**
 * Default in-memory bus. The backend transport is the injected [ApiClient]
 * (DIP — no OkHttp here). State is guarded by a [ReentrantLock]; callbacks fire
 * outside the lock so a listener may safely (un)subscribe.
 */
class DefaultEventBus(
    private val api: ApiClient,
    private val endpoint: String = "/events",
    private val maxHistory: Int = DEFAULT_MAX_HISTORY,
    private val localOnly: Set<String> = AppEvents.localOnly,
) : EventBus {

    private class Listener(val id: Long, val callback: (Any?) -> Unit)

    private val lock = ReentrantLock()
    private val listeners = mutableMapOf<String, MutableList<Listener>>()
    private val records = ArrayDeque<EventRecord>()
    private val pending = mutableListOf<String>()
    private val nextId = AtomicLong(0)

    override fun emit(name: String, payload: Any?) {
        val snapshot = lock.withLock {
            records.addLast(EventRecord(name, System.currentTimeMillis()))
            while (records.size > maxHistory) records.removeFirst()
            listeners[name]?.toList()
        }
        snapshot?.forEach { it.callback(payload) }
    }

    override fun on(name: String, callback: (Any?) -> Unit): Unsubscribe {
        val listener = Listener(nextId.getAndIncrement(), callback)
        lock.withLock { listeners.getOrPut(name) { mutableListOf() }.add(listener) }
        return {
            lock.withLock {
                listeners[name]?.removeAll { it.id == listener.id }
            }
        }
    }

    override fun once(name: String, callback: (Any?) -> Unit) {
        lateinit var unsubscribe: Unsubscribe
        unsubscribe = on(name) { payload ->
            callback(payload)
            unsubscribe()
        }
    }

    override fun off(name: String) {
        lock.withLock { listeners.remove(name) }
    }

    override fun hasListeners(name: String): Boolean =
        lock.withLock { listeners[name]?.isNotEmpty() == true }

    override fun listenerCount(name: String): Int =
        lock.withLock { listeners[name]?.size ?: 0 }

    override fun history(name: String?): List<EventRecord> =
        lock.withLock {
            if (name == null) records.toList() else records.filter { it.name == name }
        }

    override fun clear() {
        lock.withLock {
            listeners.clear()
            records.clear()
            pending.clear()
        }
    }

    override fun clearHistory() {
        lock.withLock { records.clear() }
    }

    override suspend fun sendToBackend(name: String): Boolean {
        if (name in localOnly) return false
        lock.withLock { pending.add(name) }
        return flushPending()
    }

    // Broad catch is intentional: any transport failure must retain the batch
    // for the next flush (web-faithful retry); cancellation is re-thrown.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun flushPending(): Boolean {
        val batch = lock.withLock { pending.toList() }
        if (batch.isEmpty()) return true
        return try {
            api.post<Batch, EmptyResponse>(endpoint, Batch(batch.map { PendingEvent(it) }))
            lock.withLock { repeat(batch.size) { if (pending.isNotEmpty()) pending.removeAt(0) } }
            true
        } catch (error: Exception) {
            error.rethrowIfCancellation()
            false // keep pending; retried on next flush (web retry)
        }
    }

    override fun pendingCount(): Int = lock.withLock { pending.size }

    @Serializable
    private data class PendingEvent(val name: String)

    @Serializable
    private data class Batch(val events: List<PendingEvent>)

    private companion object {
        const val DEFAULT_MAX_HISTORY = 100
    }
}

private fun Throwable.rethrowIfCancellation() {
    if (this is kotlinx.coroutines.CancellationException) throw this
}
