package com.vbwd.plugin.meinchatplus.domain

/**
 * Fail-closed E2E errors (S28.7 §3.4). The secure send/read path throws rather
 * than ever transmitting plaintext for an `e2e_v1` conversation. Port of the iOS
 * `E2eGuardError`.
 */
sealed class E2eGuardError(message: String) : Exception(message) {
    data class ProtocolDowngrade(val serverProtocol: String) :
        E2eGuardError("server downgraded to '$serverProtocol' when e2e_v1 was required")
    data object NoPeerDeviceKeys : E2eGuardError("peer has no active device keys")
    data object NoSlotForThisDevice : E2eGuardError("no envelope slot for this device")
    data object ConversationIsNotE2e : E2eGuardError("conversation is not e2e_v1")
    data object NotAnE2eMessage : E2eGuardError("message is not an e2e_v1 row")
}

/** Single home for the demanded-e2e response check. Port of the iOS `DowngradeGuard`. */
object DowngradeGuard {
    /** Throws [E2eGuardError.ProtocolDowngrade] unless [responseProtocol] is `e2e_v1`. */
    fun assertE2e(responseProtocol: String) {
        if (responseProtocol != "e2e_v1") {
            throw E2eGuardError.ProtocolDowngrade(responseProtocol)
        }
    }
}
