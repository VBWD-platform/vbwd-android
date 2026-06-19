package com.vbwd.core.persistence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The `TokenStore` contract against [EncryptedTokenStore] on a real Android
 * Keystore. Instrumented (not Robolectric) because `EncryptedSharedPreferences`
 * needs a genuine Keystore — runs under `connectedCheck`, not the JVM `check`.
 *
 * Mirrors the assertions in the JVM `TokenStoreContractTest`; the two source
 * sets cannot share code, so the small overlap is intentional and documented.
 */
@RunWith(AndroidJUnit4::class)
class EncryptedTokenStoreInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fileName = "vbwd_auth_test"

    private fun newStore(): TokenStore = EncryptedTokenStore(context, fileName)

    @Before
    @After
    fun reset() {
        newStore().clear()
    }

    @Test
    fun loadsNullBeforeAnythingSaved() {
        val store = newStore()
        assertNull(store.loadToken())
        assertNull(store.loadRefreshToken())
        assertNull(store.loadUser())
    }

    @Test
    fun roundTripsTokensAndUserBlob() {
        val store = newStore()
        store.saveToken("access-1")
        store.saveRefreshToken("refresh-1")
        val blob = """{"id":"u1"}""".toByteArray()
        store.saveUser(blob)

        // A fresh instance reads the persisted, encrypted values back.
        val reopened = newStore()
        assertEquals("access-1", reopened.loadToken())
        assertEquals("refresh-1", reopened.loadRefreshToken())
        assertArrayEquals(blob, reopened.loadUser())
    }

    @Test
    fun clearWipesEverything() {
        val store = newStore()
        store.saveToken("a")
        store.saveUser("c".toByteArray())
        store.clear()
        assertNull(store.loadToken())
        assertNull(store.loadUser())
    }
}
