package com.vbwd.plugin.cms.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `GET /cms/embed-manifest` (S91). The `ok` flag is the load gate:
 * false/missing → the not-available state. Port of the iOS `CmsManifest`.
 */
@Serializable
data class CmsManifest(
    val ok: Boolean,
    val type: String? = null,
    val category: CmsManifestCategory? = null,
    @SerialName("post_count") val postCount: Int? = null,
    @SerialName("archive_url") val archiveUrl: String? = null,
)

@Serializable
data class CmsManifestCategory(
    val slug: String,
    val name: String? = null,
)
