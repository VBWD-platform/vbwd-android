package com.vbwd.plugin.meinchatplus.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.get
import com.vbwd.core.networking.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Signed + one-time prekey pools (S28.3b §2.2-2.5). Port of the iOS service. */
interface PrekeyService {
    suspend fun publishSigned(key: SignedPrekey)
    suspend fun publishOneTimeBatch(keys: List<OneTimePrekey>)
    suspend fun fetchStatus(): PrekeyStatus
    suspend fun needsRefill(): Boolean
}

class DefaultPrekeyService(
    private val api: ApiClient,
    private val fallbackLowWaterFraction: Double = DEFAULT_LOW_WATER_FRACTION,
) : PrekeyService {

    override suspend fun publishSigned(key: SignedPrekey) {
        api.post<SignedBody, EmptyResponse>(
            MeinChatPlusEndpoints.SIGNED_PREKEY,
            SignedBody(id = key.id, publicKey = key.publicKey, signature = key.signature),
        )
    }

    override suspend fun publishOneTimeBatch(keys: List<OneTimePrekey>) {
        if (keys.isEmpty()) return
        api.post<OneTimeBody, EmptyResponse>(MeinChatPlusEndpoints.ONE_TIME_PREKEYS, OneTimeBody(keys))
    }

    override suspend fun fetchStatus(): PrekeyStatus = api.get(MeinChatPlusEndpoints.PREKEY_STATUS)

    override suspend fun needsRefill(): Boolean {
        val status = fetchStatus()
        status.lowWaterMark?.let { return status.oneTimeRemaining <= it }
        val threshold = maxOf(1, (status.oneTimeCapacity * fallbackLowWaterFraction).toInt())
        return status.oneTimeRemaining <= threshold
    }

    @Serializable
    private data class SignedBody(val id: Int, @SerialName("public_key") val publicKey: String, val signature: String)

    @Serializable
    private data class OneTimeBody(val keys: List<OneTimePrekey>)

    private companion object {
        const val DEFAULT_LOW_WATER_FRACTION = 0.2
    }
}
