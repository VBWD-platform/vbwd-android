package com.vbwd.core.persistence

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Shared `TokenStore` contract. Every implementation must satisfy it — this is
 * the Liskov-substitutability proof that the in-memory twin and the Keystore
 * store are interchangeable (the iOS `TokenStore` design).
 *
 * The instrumented `EncryptedTokenStoreInstrumentedTest` (androidTest) asserts
 * the same behaviours against `EncryptedTokenStore` on a real Keystore.
 */
abstract class TokenStoreContractTest {
    abstract fun newStore(): TokenStore

    @Test
    fun `loads null before anything is saved`() {
        val store = newStore()
        assertNull(store.loadToken())
        assertNull(store.loadRefreshToken())
        assertNull(store.loadUser())
    }

    @Test
    fun `round-trips the access and refresh tokens`() {
        val store = newStore()
        store.saveToken("access-1")
        store.saveRefreshToken("refresh-1")
        assertEquals("access-1", store.loadToken())
        assertEquals("refresh-1", store.loadRefreshToken())
    }

    @Test
    fun `round-trips the user blob`() {
        val store = newStore()
        val blob = """{"id":"u1"}""".toByteArray()
        store.saveUser(blob)
        assertArrayEquals(blob, store.loadUser())
    }

    @Test
    fun `clear wipes everything`() {
        val store = newStore()
        store.saveToken("a")
        store.saveRefreshToken("b")
        store.saveUser("c".toByteArray())
        store.clear()
        assertNull(store.loadToken())
        assertNull(store.loadRefreshToken())
        assertNull(store.loadUser())
    }
}

class InMemoryTokenStoreTest : TokenStoreContractTest() {
    override fun newStore(): TokenStore = InMemoryTokenStore()
}
