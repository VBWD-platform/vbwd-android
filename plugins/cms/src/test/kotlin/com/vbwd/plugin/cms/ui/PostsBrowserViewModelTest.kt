package com.vbwd.plugin.cms.ui

import com.vbwd.plugin.cms.domain.CmsManifest
import com.vbwd.plugin.cms.domain.CmsService
import com.vbwd.plugin.cms.domain.CmsServiceError
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private class FakeCmsService(
    var manifest: CmsManifest = CmsManifest(ok = true),
    var error: Throwable? = null,
) : CmsService {
    override suspend fun fetchManifest(type: String, category: String): CmsManifest {
        error?.let { throw it }
        return manifest
    }
}

class PostsBrowserViewModelTest {
    private fun vm(
        type: String? = "post",
        category: String? = "news",
        fallback: String? = null,
        webOrigin: String? = "https://vbwd.cc",
        service: CmsService = FakeCmsService(),
    ) = PostsBrowserViewModel(type, category, fallback, webOrigin, service)

    @Test
    fun `missing config goes straight to NotConfigured`() = runTest {
        val viewModel = vm(type = null)
        viewModel.load()
        assertEquals(PostsBrowserViewModel.LoadState.NotConfigured, viewModel.state.value)
    }

    @Test
    fun `resolves a relative archive url against the web origin`() = runTest {
        val viewModel = vm(service = FakeCmsService(CmsManifest(ok = true, archiveUrl = "/cms/embed/post/news")))
        viewModel.load()
        assertEquals(
            PostsBrowserViewModel.LoadState.Ready("https://vbwd.cc/cms/embed/post/news"),
            viewModel.state.value,
        )
    }

    @Test
    fun `uses an absolute archive url verbatim`() = runTest {
        val url = "https://host/cms/embed/post/news"
        val viewModel = vm(service = FakeCmsService(CmsManifest(ok = true, archiveUrl = url)))
        viewModel.load()
        assertEquals(PostsBrowserViewModel.LoadState.Ready(url), viewModel.state.value)
    }

    @Test
    fun `falls back when the manifest has no archive url`() = runTest {
        val viewModel = vm(fallback = "https://fallback/x", service = FakeCmsService(CmsManifest(ok = true)))
        viewModel.load()
        assertEquals(PostsBrowserViewModel.LoadState.Ready("https://fallback/x"), viewModel.state.value)
    }

    @Test
    fun `a not-ok manifest is an error`() = runTest {
        val viewModel = vm(service = FakeCmsService(CmsManifest(ok = false)))
        viewModel.load()
        assertInstanceOf(PostsBrowserViewModel.LoadState.Error::class.java, viewModel.state.value)
    }

    @Test
    fun `a NotFound from the service maps to NotConfigured`() = runTest {
        val viewModel = vm(service = FakeCmsService(error = CmsServiceError.NotFound("nope")))
        viewModel.load()
        assertEquals(PostsBrowserViewModel.LoadState.NotConfigured, viewModel.state.value)
    }

    @Test
    fun `resolve handles absolute, relative and unusable inputs`() {
        assertEquals("https://x/a", PostsBrowserViewModel.resolve("https://x/a", null))
        assertEquals("https://b/a", PostsBrowserViewModel.resolve("/a", "https://b"))
        assertNull(PostsBrowserViewModel.resolve("/a", null))
        assertNull(PostsBrowserViewModel.resolve(null, "https://b"))
    }
}
