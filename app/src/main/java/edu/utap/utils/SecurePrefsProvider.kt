package edu.utap.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Singleton object providing encrypted shared preferences for secure storage.
 *
 * Used to cache user authentication data securely on the device.
 */
object SecurePrefsProvider {
    private const val PREF_FILE_NAME = "secure_auth_prefs"
    private const val KEY_CACHED_USER = "cachedUser"
    private const val KEY_INITIAL_LOGIN_TIME = "initialLoginTime"

    private lateinit var prefs: SharedPreferences

    /**
     * Initializes the encrypted shared preferences.
     *
     * @param context The application context.
     */
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

    /**
     * Checks if the preferences have been initialized.
     *
     * @return True if initialized, false otherwise.
     */
    fun isInitialized(): Boolean = ::prefs.isInitialized

    /**
     * Caches user data and login time securely.
     *
     * @param userJson The user data as a JSON string.
     * @param loginTimeMillis The login timestamp in milliseconds.
     */
    fun cacheUserData(userJson: String, loginTimeMillis: Long) {
        prefs.edit()
            .putString(KEY_CACHED_USER, userJson)
            .putLong(KEY_INITIAL_LOGIN_TIME, loginTimeMillis)
            .apply()
    }

    /**
     * Retrieves cached user data JSON string.
     *
     * @return The cached user data or null if not set.
     */
    fun getCachedUserData(): String? =
        prefs.getString(KEY_CACHED_USER, null)

    /**
     * Retrieves the initial login timestamp.
     *
     * @return The login time in milliseconds.
     */
    fun getInitialLoginTime(): Long =
        prefs.getLong(KEY_INITIAL_LOGIN_TIME, 0L)

    /**
     * Clears all cached authentication data.
     */
    fun clearAuthCache() {
        prefs.edit().clear().apply()
    }
}
