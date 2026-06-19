package com.vbwd.core.persistence

/**
 * Token persistence contract. Port of the iOS `TokenStore` — mirrors the three
 * web `localStorage` keys: access token, refresh token, encoded user blob.
 * Domain depends on this interface; the concrete store is injected (DIP).
 */
interface TokenStore {
    fun saveToken(token: String)
    fun loadToken(): String?
    fun saveRefreshToken(token: String)
    fun loadRefreshToken(): String?
    fun saveUser(data: ByteArray)
    fun loadUser(): ByteArray?
    fun clear()
}

/**
 * In-memory store. Production-usable (previews/tests) and the substitutable twin
 * proving Liskov against [com.vbwd.core.persistence.EncryptedTokenStore] via the
 * shared contract.
 */
class InMemoryTokenStore : TokenStore {
    private var token: String? = null
    private var refreshToken: String? = null
    private var user: ByteArray? = null

    override fun saveToken(token: String) {
        this.token = token
    }

    override fun loadToken(): String? = token

    override fun saveRefreshToken(token: String) {
        this.refreshToken = token
    }

    override fun loadRefreshToken(): String? = refreshToken

    override fun saveUser(data: ByteArray) {
        this.user = data.copyOf()
    }

    override fun loadUser(): ByteArray? = user?.copyOf()

    override fun clear() {
        token = null
        refreshToken = null
        user = null
    }
}
