package com.vbwd.core.notifications

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Core notifications seam (port of the iOS `NotificationsSDK`, S67.2). The host
 * forwards the FCM device token here; plugins register a [DeviceTokenSink] to
 * ship it to their own backend endpoint — core stays agnostic of any plugin wire
 * contract (core-agnosticism rule). The app-icon badge is a no-op by default on
 * Android (documented divergence: no app-icon badge API like iOS — surfaced via
 * notification channels at the call site).
 */
interface NotificationsSdk {
    /** Called when FCM returns a token; buffered + forwarded to every sink. */
    suspend fun didReceiveDeviceToken(tokenHex: String)

    /** Register a sink; a buffered token (if any) is replayed immediately. */
    suspend fun registerSink(sink: DeviceTokenSink)

    /** Drive the badge count (negative clamped to zero). */
    suspend fun setBadgeCount(count: Int)
}

/** Receives the device token on registration (if buffered) and on each refresh. */
interface DeviceTokenSink {
    suspend fun handleDeviceToken(tokenHex: String)
}

/**
 * Default [NotificationsSdk]: buffers the latest token, forwards it to every
 * registered sink, and replays it to sinks that register later (the token
 * usually arrives before any plugin — or before login). Port of the iOS
 * `DefaultNotificationsSDK` (actor → [Mutex]-guarded state).
 */
class DefaultNotificationsSdk(
    private val badgeSetter: suspend (Int) -> Unit = {},
) : NotificationsSdk {
    private val mutex = Mutex()
    private var latestTokenHex: String? = null
    private val sinks = mutableListOf<DeviceTokenSink>()

    override suspend fun didReceiveDeviceToken(tokenHex: String) {
        val current = mutex.withLock {
            latestTokenHex = tokenHex
            sinks.toList()
        }
        current.forEach { it.handleDeviceToken(tokenHex) }
    }

    override suspend fun registerSink(sink: DeviceTokenSink) {
        val buffered = mutex.withLock {
            sinks.add(sink)
            latestTokenHex
        }
        buffered?.let { sink.handleDeviceToken(it) }
    }

    override suspend fun setBadgeCount(count: Int) {
        badgeSetter(maxOf(0, count))
    }

    /** Latest token this process has seen (S68 same-device suppression). */
    suspend fun currentTokenHex(): String? = mutex.withLock { latestTokenHex }
}
