package com.vbwd.core.networking

import android.util.Log

/**
 * Structured HTTP traffic logger. Port of the iOS `APITrafficLogger`.
 *
 * Default: [Level.BODIES] in debug builds, [Level.OFF] in release. Bodies for
 * `/auth/` paths are redacted so passwords and bearer tokens never reach the
 * device log. The `Authorization` header is never logged.
 *
 * The output [sink] is injectable so tests can capture lines without Android's
 * [Log].
 */
class ApiTrafficLogger(
    val level: Level,
    private val bodyCap: Int = DEFAULT_BODY_CAP,
    private val sink: (String) -> Unit = { Log.d(TAG, it) },
) {
    enum class Level { OFF, SUMMARY, BODIES }

    fun logRequest(method: String, url: String, body: String?) {
        if (level == Level.OFF) return
        var line = "→ $method ${displayPath(url)}"
        if (level == Level.BODIES) {
            val preview = redactedBodyPreview(url, body)
            if (preview.isNotEmpty()) line += "\n   body: $preview"
        }
        sink(line)
    }

    fun logResponse(method: String, url: String, status: Int, body: String) {
        if (level == Level.OFF) return
        var line = "← $status $method ${displayPath(url)}"
        if (level == Level.BODIES) {
            val preview = redactedBodyPreview(url, body)
            if (preview.isNotEmpty()) line += "\n   body: $preview"
        }
        sink(line)
    }

    fun logTransportError(method: String, url: String, error: Throwable) {
        if (level == Level.OFF) return
        sink("✗ $method ${displayPath(url)} — $error")
    }

    private fun displayPath(url: String): String = url

    private fun redactedBodyPreview(url: String, body: String?): String {
        if (body.isNullOrEmpty()) return ""
        if (url.contains("/auth/")) return "<redacted: auth body>"
        return if (body.length > bodyCap) {
            "${body.take(bodyCap)}… (${body.length} chars total)"
        } else {
            body
        }
    }

    companion object {
        private const val TAG = "vbwd.api"
        private const val DEFAULT_BODY_CAP = 4096

        /** [Level.BODIES] in debug, [Level.OFF] in release. */
        fun default(): ApiTrafficLogger =
            ApiTrafficLogger(if (BuildConfigBridge.isDebug) Level.BODIES else Level.OFF)

        /** Convenience for tests / silent contexts. */
        fun off(): ApiTrafficLogger = ApiTrafficLogger(Level.OFF)
    }
}

/**
 * Tiny indirection so the logger default doesn't hard-depend on a generated
 * `BuildConfig` (which the library module may not expose). Defaults to debug
 * logging on; release builds flip it via Hilt config in A01.4.
 */
internal object BuildConfigBridge {
    var isDebug: Boolean = true
}
