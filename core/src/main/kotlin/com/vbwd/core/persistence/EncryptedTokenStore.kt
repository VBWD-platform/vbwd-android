package com.vbwd.core.persistence

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * On-device, Keystore-backed store — the Android port of the iOS
 * `KeychainTokenStore`. Behaviourally substitutable for [InMemoryTokenStore]
 * (proven by the shared `TokenStore` contract; see the androidTest).
 *
 * The only `EncryptedSharedPreferences` site in the SDK. Values are encrypted
 * at rest with an AES-256 master key held in the Android Keystore.
 */
class EncryptedTokenStore(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
) : TokenStore {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        fileName,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun saveToken(token: String) = putString(KEY_TOKEN, token)
    override fun loadToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun saveRefreshToken(token: String) = putString(KEY_REFRESH, token)
    override fun loadRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override fun saveUser(data: ByteArray) =
        putString(KEY_USER, Base64.encodeToString(data, Base64.NO_WRAP))

    override fun loadUser(): ByteArray? =
        prefs.getString(KEY_USER, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "vbwd_auth"
        const val KEY_TOKEN = "token"
        const val KEY_REFRESH = "refresh"
        const val KEY_USER = "user"
    }
}
