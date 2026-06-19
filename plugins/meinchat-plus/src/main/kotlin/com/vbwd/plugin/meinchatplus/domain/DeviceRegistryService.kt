package com.vbwd.plugin.meinchatplus.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.delete
import com.vbwd.core.networking.get

/** Device registry + prekey-bundle fetch (S28.3b §2.1). Port of the iOS service. */
interface DeviceRegistryService {
    suspend fun fetchMyDevices(): List<DeviceDescriptor>
    suspend fun fetchBundle(userId: String, deviceId: String): PrekeyBundle
    suspend fun revokeDevice(id: String)
}

class DefaultDeviceRegistryService(private val api: ApiClient) : DeviceRegistryService {
    override suspend fun fetchMyDevices(): List<DeviceDescriptor> =
        api.get<MyDevicesResponse>(MeinChatPlusEndpoints.MY_DEVICES).items ?: emptyList()

    override suspend fun fetchBundle(userId: String, deviceId: String): PrekeyBundle =
        api.get(MeinChatPlusEndpoints.bundle(userId, deviceId))

    override suspend fun revokeDevice(id: String) {
        api.delete<EmptyResponse>(MeinChatPlusEndpoints.device(id))
    }
}
