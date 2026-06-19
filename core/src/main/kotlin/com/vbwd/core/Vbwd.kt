package com.vbwd.core

/**
 * SDK-wide metadata namespace — the Android port of `VBWDCore.swift`.
 *
 * Intentionally logic-free. All behaviour lives in the networking / persistence
 * / domain / session / plugins / ui layers, introduced test-first sub-sprint by
 * sub-sprint (see `docs/dev_log`).
 */
object Vbwd {
    /** Target API contract version (parity with web `fe-core` and `VBWDCore`). */
    const val API_CONTRACT_VERSION: String = "v1"
}
