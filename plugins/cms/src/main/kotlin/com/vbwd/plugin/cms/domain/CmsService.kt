package com.vbwd.plugin.cms.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.get
import com.vbwd.plugin.cms.CmsEndpoints

/** Plugin-specific errors that map cleanly to the not-configured UI. */
sealed class CmsServiceError(message: String) : Exception(message) {
    data class NotFound(val reason: String) : CmsServiceError("not found: $reason")
}

/** Narrow service contract (ISP/DIP) — one verb: fetch the embed manifest. */
interface CmsService {
    suspend fun fetchManifest(type: String, category: String): CmsManifest
}

/**
 * Default impl over the SDK's [ApiClient]. A 404 becomes an explicit
 * [CmsServiceError.NotFound] (fail-loud — the view shows "not configured",
 * never a silent blank). Port of the iOS `DefaultCmsService`.
 */
class DefaultCmsService(private val api: ApiClient) : CmsService {
    override suspend fun fetchManifest(type: String, category: String): CmsManifest =
        try {
            api.get(CmsEndpoints.embedManifest(type, category))
        } catch (error: ApiError) {
            if (error is ApiError.Http && error.status == HTTP_NOT_FOUND) {
                throw CmsServiceError.NotFound(error.message)
            }
            throw error
        }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
