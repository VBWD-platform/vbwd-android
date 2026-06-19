package com.vbwd.core.domain

import com.vbwd.core.networking.ApiJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Round-trip the domain models against the `Fixtures/*.json` copied from
 * `vbwd-ios-core/Fixtures/` — proves the `@Serializable` shapes match the
 * shared backend contract field-for-field.
 */
class DomainModelJsonTest {
    private val json = ApiJson.instance

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "missing fixture $name"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `auth_user fixture decodes with snake_case mapping`() {
        val user = json.decodeFromString(AuthUser.serializer(), fixture("auth_user.json"))
        assertEquals("31c0e59c-aae5-4eba-9328-98b99370bf32", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("John Bach", user.name)
        assertEquals("USER", user.role)
        assertFalse(user.isAdmin!!)
        assertEquals(emptyList<AccessLevel>(), user.accessLevels)
        assertEquals(1, user.userAccessLevels?.size)
        assertEquals(
            AccessLevel("d63cb59f-805e-47eb-aa86-1a413fb92834", "logged-in", "Logged In"),
            user.userAccessLevels!!.first(),
        )
        assertTrue(user.userPermissions!!.contains("user.profile.view"))
    }

    @Test
    fun `auth_user round-trips through encode and decode`() {
        val user = json.decodeFromString(AuthUser.serializer(), fixture("auth_user.json"))
        val reencoded = json.encodeToString(AuthUser.serializer(), user)
        assertEquals(user, json.decodeFromString(AuthUser.serializer(), reencoded))
    }

    @Test
    fun `login_success fixture decodes token and nested user`() {
        val response = json.decodeFromString(LoginResponse.serializer(), fixture("login_success.json"))
        assertEquals(true, response.success)
        assertEquals("TEST_TOKEN", response.token)
        assertEquals("31c0e59c-aae5-4eba-9328-98b99370bf32", response.userId)
        assertNull(response.error)
        assertEquals("test@example.com", response.user?.email)
    }

    @Test
    fun `login_error fixture decodes the failure shape`() {
        val response = json.decodeFromString(LoginResponse.serializer(), fixture("login_error.json"))
        assertEquals(false, response.success)
        assertNull(response.token)
        assertNull(response.user)
        assertEquals("Invalid credentials", response.error)
    }

    @Test
    fun `UserProfile coerces null detail fields to empty strings`() {
        val body = """{"first_name":"Ada","last_name":null,"tax_number":null}"""
        val profile = json.decodeFromString(UserProfile.serializer(), body)
        assertEquals("Ada", profile.firstName)
        assertEquals("", profile.lastName)
        assertEquals("", profile.taxNumber)
        assertEquals("", profile.country)
    }
}
