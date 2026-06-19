package com.vbwd.plugin.cms

import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.core.plugins.DefaultPlatformSdk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class FakeApi : ApiClient {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> request(
        method: HttpMethod,
        path: String,
        jsonBody: String?,
        deserializer: DeserializationStrategy<T>,
    ): T = EmptyResponse() as T
    override fun setToken(token: String?) = Unit
    override fun on(event: ApiEvent, handler: () -> Unit) = Unit
}

class CmsPluginContractTest {
    private fun sdk() = DefaultPlatformSdk(FakeApi(), ApiClientConfig("http://x"), DefaultEventBus(FakeApi()))

    @Test
    fun `install registers the posts route, menu item and translation when configured`() = runTest {
        val platform = sdk()
        CmsPlugin(postType = "post", category = "news", webOrigin = "https://x", archiveUrl = "https://x/a")
            .install(platform)

        assertEquals(listOf("/posts"), platform.getRoutes().map { it.path })
        assertEquals(listOf("cms-posts"), platform.getMenuItems().map { it.id })
        assertEquals("Posts", platform.getTranslations()["en"]?.get("cms.posts.title"))
    }

    @Test
    fun `install registers nothing when the CMS config is absent`() = runTest {
        val platform = sdk()
        CmsPlugin(postType = null, category = null, webOrigin = null, archiveUrl = null).install(platform)
        assertTrue(platform.getRoutes().isEmpty())
        assertTrue(platform.getMenuItems().isEmpty())
    }
}
