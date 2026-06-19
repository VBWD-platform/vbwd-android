package com.vbwd.core.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class PermissionEvaluatorTest {
    private val evaluator = PermissionEvaluator()

    @ParameterizedTest(name = "has({1}) in {0} == {2}")
    @MethodSource("hasCases")
    fun `has matches the iOS wildcard rules`(
        granted: List<String>,
        required: String,
        expected: Boolean,
    ) {
        assertEquals(expected, evaluator.has(required, granted))
    }

    @Test
    fun `hasAny is true when any required permission is granted`() {
        val granted = listOf("shop.read")
        assertTrue(evaluator.hasAny(listOf("user.edit", "shop.read"), granted))
        assertFalse(evaluator.hasAny(listOf("user.edit", "billing.view"), granted))
    }

    @Test
    fun `hasAny short-circuits on the superuser wildcard`() {
        assertTrue(evaluator.hasAny(listOf("anything"), listOf("*")))
    }

    @Test
    fun `hasAny on empty required list is false`() {
        assertFalse(evaluator.hasAny(emptyList(), listOf("shop.read")))
    }

    companion object {
        @JvmStatic
        fun hasCases(): List<Arguments> = listOf(
            // superuser wildcard grants everything
            Arguments.of(listOf("*"), "anything.at.all", true),
            // exact match
            Arguments.of(listOf("shop.read"), "shop.read", true),
            // prefix wildcard grants the namespace
            Arguments.of(listOf("shop.*"), "shop.read", true),
            Arguments.of(listOf("shop.*"), "shop.write.bulk", true),
            // prefix wildcard does NOT grant a different namespace
            Arguments.of(listOf("shop.*"), "billing.read", false),
            // the bare prefix without the dot does not match the wildcard
            Arguments.of(listOf("shop.*"), "shop", false),
            // deny: not present
            Arguments.of(listOf("user.view"), "user.edit", false),
            // empty grants deny everything
            Arguments.of(emptyList<String>(), "shop.read", false),
        )
    }
}
