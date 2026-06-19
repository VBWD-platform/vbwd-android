package com.vbwd.plugin.cms.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.HttpMethod
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class CmsServiceTest {
    private val client = mockk<ApiClient>(relaxed = true)
    private val service = DefaultCmsService(client)

    @Test
    fun `fetchManifest decodes the manifest`() = runTest {
        coEvery { client.request<CmsManifest>(HttpMethod.GET, any(), any(), any()) } returns
            CmsManifest(ok = true, archiveUrl = "/cms/embed/post/news")
        val manifest = service.fetchManifest("post", "news")
        assertEquals(true, manifest.ok)
        assertEquals("/cms/embed/post/news", manifest.archiveUrl)
    }

    @Test
    fun `a 404 becomes a fail-loud NotFound (no silent blank)`() = runTest {
        coEvery { client.request<CmsManifest>(HttpMethod.GET, any(), any(), any()) } throws
            ApiError.Http(404, "type not registered")
        val error = runCatching { service.fetchManifest("ghost", "news") }.exceptionOrNull()
        assertInstanceOf(CmsServiceError.NotFound::class.java, error)
    }
}
