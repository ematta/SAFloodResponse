package edu.utap.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefsProvider {
    private const val PREF_FILE_NAME = "secure_auth_prefs"
    private const val KEY_CACHED_USER = "cachedUser"
    private const val KEY_INITIAL_LOGIN_TIME = "initialLoginTime"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isInitialized(): Boolean = ::prefs.isInitialized

    fun cacheUserData(userJson: String, loginTimeMillis: Long) {
        prefs.edit()
            .putString(KEY_CACHED_USER, userJson)
            .putLong(KEY_INITIAL_LOGIN_TIME, loginTimeMillis)
            .apply()
    }

    fun getCachedUserData(): String? =
        prefs.getString(KEY_CACHED_USER, null)

    fun getInitialLoginTime(): Long =
        prefs.getLong(KEY_INITIAL_LOGIN_TIME, 0L)

    fun clearAuthCache() {
        prefs.edit().clear().apply()
    }
}
