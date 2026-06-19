package com.vbwd.core.plugins

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemanticVersionTest {
    @Test
    fun `parses a valid version`() {
        assertEquals(SemanticVersion(1, 2, 3), SemanticVersion.parse("1.2.3"))
    }

    @Test
    fun `rejects malformed versions`() {
        assertThrows(PluginError.InvalidVersion::class.java) { SemanticVersion.parse("1.2") }
        assertThrows(PluginError.InvalidVersion::class.java) { SemanticVersion.parse("a.b.c") }
        assertThrows(PluginError.InvalidVersion::class.java) { SemanticVersion.parse("1.2.-3") }
    }

    @Test
    fun `orders by major then minor then patch`() {
        assertTrue(SemanticVersion(1, 0, 0) < SemanticVersion(1, 0, 1))
        assertTrue(SemanticVersion(1, 2, 0) < SemanticVersion(1, 10, 0))
        assertTrue(SemanticVersion(2, 0, 0) > SemanticVersion(1, 9, 9))
    }

    @Test
    fun `wildcard and empty constraints accept anything`() {
        val v = SemanticVersion(3, 4, 5)
        assertTrue(VersionConstraint("*").isSatisfiedBy(v))
        assertTrue(VersionConstraint("").isSatisfiedBy(v))
        assertTrue(VersionConstraint("x").isSatisfiedBy(v))
        assertTrue(VersionConstraint("latest").isSatisfiedBy(v))
    }

    @Test
    fun `caret allows patch and minor within the major`() {
        val c = VersionConstraint("^1.2.3")
        assertTrue(c.isSatisfiedBy(SemanticVersion(1, 2, 3)))
        assertTrue(c.isSatisfiedBy(SemanticVersion(1, 9, 0)))
        assertFalse(c.isSatisfiedBy(SemanticVersion(2, 0, 0)))
        assertFalse(c.isSatisfiedBy(SemanticVersion(1, 2, 2)))
    }

    @Test
    fun `tilde allows patch within the minor`() {
        val c = VersionConstraint("~1.2.3")
        assertTrue(c.isSatisfiedBy(SemanticVersion(1, 2, 9)))
        assertFalse(c.isSatisfiedBy(SemanticVersion(1, 3, 0)))
    }

    @Test
    fun `comparator and exact constraints`() {
        assertTrue(VersionConstraint(">=1.0.0").isSatisfiedBy(SemanticVersion(1, 0, 0)))
        assertTrue(VersionConstraint(">1.0.0").isSatisfiedBy(SemanticVersion(1, 0, 1)))
        assertFalse(VersionConstraint(">1.0.0").isSatisfiedBy(SemanticVersion(1, 0, 0)))
        assertTrue(VersionConstraint("<=2.0.0").isSatisfiedBy(SemanticVersion(2, 0, 0)))
        assertTrue(VersionConstraint("1.2.3").isSatisfiedBy(SemanticVersion(1, 2, 3)))
        assertFalse(VersionConstraint("1.2.3").isSatisfiedBy(SemanticVersion(1, 2, 4)))
    }

    @Test
    fun `x-range matches the fixed prefix`() {
        assertTrue(VersionConstraint("1.x").isSatisfiedBy(SemanticVersion(1, 9, 9)))
        assertTrue(VersionConstraint("1.2.x").isSatisfiedBy(SemanticVersion(1, 2, 7)))
        assertFalse(VersionConstraint("1.2.x").isSatisfiedBy(SemanticVersion(1, 3, 0)))
        assertFalse(VersionConstraint("1.x").isSatisfiedBy(SemanticVersion(2, 0, 0)))
    }
}
