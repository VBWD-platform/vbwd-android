package com.vbwd.plugin.meinchatplus.domain

/** API paths for the meinchat-plus wire contract (S28.3b §2). Port of the iOS enum. */
internal object MeinChatPlusEndpoints {
    const val MY_DEVICES = "/me/devices"
    fun device(id: String): String = "/me/devices/$id"
    fun bundle(userId: String, deviceId: String): String =
        "/messaging/users/$userId/devices/$deviceId/bundle"

    const val SIGNED_PREKEY = "/me/prekeys/signed"
    const val ONE_TIME_PREKEYS = "/me/prekeys/one-time"
    const val PREKEY_STATUS = "/me/prekeys/status"
}
