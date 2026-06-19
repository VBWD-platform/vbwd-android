package com.vbwd.plugin.cms

import java.net.URLEncoder

/** Backend endpoints the CMS plugin calls. Port of the iOS `CmsEndpoints`. */
internal object CmsEndpoints {
    /** `GET /cms/embed-manifest?type=&category=` — gates the browser launch. */
    fun embedManifest(type: String, category: String): String {
        val encodedType = URLEncoder.encode(type, Charsets.UTF_8.name())
        val encodedCategory = URLEncoder.encode(category, Charsets.UTF_8.name())
        return "/cms/embed-manifest?type=$encodedType&category=$encodedCategory"
    }
}
