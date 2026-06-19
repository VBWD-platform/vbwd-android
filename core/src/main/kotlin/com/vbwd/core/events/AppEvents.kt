package com.vbwd.core.events

/**
 * Event-name catalog. Port of the iOS `AppEvents` constants (same strings).
 * Plugins and core emit/subscribe by these names — neither imports the other
 * (decoupled via the [EventBus], OCP).
 */
object AppEvents {
    // Auth
    const val AUTH_LOGIN = "auth:login"
    const val AUTH_LOGOUT = "auth:logout"
    const val AUTH_TOKEN_REFRESHED = "auth:token-refreshed"
    const val AUTH_SESSION_EXPIRED = "auth:session-expired"

    // User
    const val USER_REGISTERED = "user:registered"
    const val USER_UPDATED = "user:updated"
    const val USER_DELETED = "user:deleted"

    // Subscription
    const val SUBSCRIPTION_CREATED = "subscription:created"
    const val SUBSCRIPTION_ACTIVATED = "subscription:activated"
    const val SUBSCRIPTION_UPGRADED = "subscription:upgraded"
    const val SUBSCRIPTION_DOWNGRADED = "subscription:downgraded"
    const val SUBSCRIPTION_CANCELLED = "subscription:cancelled"
    const val SUBSCRIPTION_EXPIRED = "subscription:expired"

    // Payment
    const val PAYMENT_INITIATED = "payment:initiated"
    const val PAYMENT_SUCCEEDED = "payment:succeeded"
    const val PAYMENT_FAILED = "payment:failed"
    const val PAYMENT_REFUNDED = "payment:refunded"

    // Checkout
    const val CHECKOUT_STARTED = "checkout:started"
    const val CHECKOUT_COMPLETED = "checkout:completed"
    const val CHECKOUT_FAILED = "checkout:failed"

    // Plugin lifecycle
    const val PLUGIN_REGISTERED = "plugin:registered"
    const val PLUGIN_INITIALIZED = "plugin:initialized"
    const val PLUGIN_ERROR = "plugin:error"
    const val PLUGIN_STOPPED = "plugin:stopped"

    // UI-local (never sent to the backend)
    const val NOTIFICATION_SHOW = "notification:show"
    const val NOTIFICATION_HIDE = "notification:hide"
    const val MODAL_OPEN = "modal:open"
    const val MODAL_CLOSE = "modal:close"
    const val LOADING_START = "loading:start"
    const val LOADING_END = "loading:end"

    // WebSocket
    const val WS_CONNECTED = "ws:connected"
    const val WS_DISCONNECTED = "ws:disconnected"
    const val WS_MESSAGE = "ws:message"
    const val WS_ERROR = "ws:error"

    // Meinchat — local-only nudge that drives inbox refresh on push arrival.
    const val MEINCHAT_MESSAGE_RECEIVED = "meinchat:message-received"

    /** Local-only events excluded from backend forwarding (web exclusion list). */
    val localOnly: Set<String> = setOf(
        NOTIFICATION_SHOW, NOTIFICATION_HIDE,
        MODAL_OPEN, MODAL_CLOSE,
        LOADING_START, LOADING_END,
        MEINCHAT_MESSAGE_RECEIVED,
    )
}
