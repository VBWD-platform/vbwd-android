package com.vbwd.plugin.cms.ui

/**
 * Builds the auth-seed JS that seeds `localStorage` (`token`,
 * `isAuthenticated`, `user`) so a logged-in user shares the session inside the
 * embed WebView. Pure string-building (no WebKit) so it is unit-testable. Port
 * of the iOS `PostsBrowserAuthBootstrap`.
 */
object PostsBrowserAuthBootstrap {
    private const val FORM_FEED = 0x0C
    private const val LINE_SEPARATOR = 0x2028
    private const val PARAGRAPH_SEPARATOR = 0x2029

    /** JS source; empty (no-op) when both inputs are null/blank. */
    fun javaScriptSource(token: String?, userJson: String?): String {
        val statements = buildList {
            if (!token.isNullOrEmpty()) {
                add("localStorage.setItem('token', ${jsLiteral(token)});")
                add("localStorage.setItem('isAuthenticated', 'true');")
            }
            if (!userJson.isNullOrEmpty()) {
                add("localStorage.setItem('user', ${jsLiteral(userJson)});")
            }
        }
        if (statements.isEmpty()) return ""
        return "(function(){try{${statements.joinToString("")}}catch(_){}})();"
    }

    /** Single-quoted JS string literal, escaping the breaking characters. */
    fun jsLiteral(raw: String): String {
        val out = StringBuilder("'")
        for (ch in raw) {
            val escaped = when (ch) {
                '\\' -> "\\\\"
                '\'' -> "\\'"
                '\n' -> "\\n"
                '\r' -> "\\r"
                '\t' -> "\\t"
                '\b' -> "\\b"
                else -> escapeByCode(ch)
            }
            out.append(escaped)
        }
        out.append("'")
        return out.toString()
    }

    private fun escapeByCode(ch: Char): String = when (ch.code) {
        FORM_FEED -> "\\f"
        LINE_SEPARATOR -> "\\u2028"
        PARAGRAPH_SEPARATOR -> "\\u2029"
        else -> ch.toString()
    }
}
