package com.vbwd.core.plugins

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginContractTest {
    @Test
    fun `none resolves to no dependencies`() {
        assertTrue(PluginDependencies.None.resolved.isEmpty())
    }

    @Test
    fun `bare list resolves each name to the any-version constraint`() {
        val resolved = PluginDependencies.List(listOf("a", "b")).resolved
        assertEquals(listOf("a", "b"), resolved.map { it.first })
        assertTrue(resolved.all { it.second.isSatisfiedBy(SemanticVersion(9, 9, 9)) })
    }

    @Test
    fun `constrained resolves names to their version constraints`() {
        val resolved = PluginDependencies.Constrained(mapOf("a" to "^1.0.0")).resolved
        assertEquals("a", resolved.single().first)
        assertTrue(resolved.single().second.isSatisfiedBy(SemanticVersion(1, 5, 0)))
    }

    @Test
    fun `metadata defaults are empty`() {
        val meta = PluginMetadata(name = "x", version = SemanticVersion(1, 0, 0))
        assertEquals(PluginDependencies.None, meta.dependencies)
        assertTrue(meta.keywords.isEmpty())
        assertTrue(meta.translations.isEmpty())
        assertEquals(null, meta.description)
    }
}
