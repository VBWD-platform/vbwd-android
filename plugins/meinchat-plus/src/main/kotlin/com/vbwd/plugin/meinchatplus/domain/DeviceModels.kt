package com.vbwd.plugin.meinchatplus.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A device registered to a user (S28.3b §2.1). Port of the iOS `DeviceDescriptor`. */
@Serializable
data class DeviceDescriptor(
    val id: String,
    @SerialName("user_id") val userId: String,
    val label: String? = null,
    @SerialName("identity_key") val identityKey: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class SignedPrekey(
    val id: Int,
    @SerialName("public_key") val publicKey: String,
    val signature: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class OneTimePrekey(
    val id: Int,
    @SerialName("public_key") val publicKey: String,
)

@Serializable
data class PrekeyBundle(
    @SerialName("device_id") val deviceId: String,
    @SerialName("identity_key") val identityKey: String,
    @SerialName("signed_prekey") val signedPrekey: SignedPrekey,
    @SerialName("one_time_prekey") val oneTimePrekey: OneTimePrekey? = null,
)

@Serializable
data class PrekeyStatus(
    @SerialName("one_time_remaining") val oneTimeRemaining: Int,
    @SerialName("one_time_capacity") val oneTimeCapacity: Int,
    @SerialName("signed_rotated_at") val signedRotatedAt: String? = null,
    @SerialName("low_water_mark") val lowWaterMark: Int? = null,
)

@Serializable
internal data class MyDevicesResponse(val items: List<DeviceDescriptor>? = null)
