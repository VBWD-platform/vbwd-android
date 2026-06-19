package com.vbwd.core.plugins

import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.ApiJson
import com.vbwd.core.testutil.FakeApiClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginManifestTest {
    @Test
    fun `parses the plugins json shape and ignores unknown keys`() {
        val json = """
            {
              "_comment": "ignored",
              "plugins": {
                "example": { "enabled": true, "version": "1.0.0", "source": "local" },
                "off": { "enabled": false, "version": "2.0.0" }
              }
            }
        """.trimIndent()
        val manifest = ApiJson.instance.decodeFromString(PluginManifest.serializer(), json)
        assertTrue(manifest.isEnabled("example"))
        assertFalse(manifest.isEnabled("off"))
        assertEquals(setOf("example"), manifest.enabledNames)
    }

    @Test
    fun `in-memory loader returns its manifest`() = runTest {
        val manifest = PluginManifest(mapOf("a" to PluginManifest.Entry(enabled = true, version = "1.0.0")))
        assertEquals(manifest, InMemoryPluginManifestLoader(manifest).load())
    }

    @Test
    fun `remote loader falls back on error`() = runTest {
        val api = FakeApiClient(nextError = ApiError.Transport("offline"))
        val fallback = PluginManifest(mapOf("f" to PluginManifest.Entry(enabled = true, version = "1.0.0")))
        val loaded = RemotePluginManifestLoader(api, path = "/plugins", fallback = fallback).load()
        assertEquals(fallback, loaded)
    }
}
