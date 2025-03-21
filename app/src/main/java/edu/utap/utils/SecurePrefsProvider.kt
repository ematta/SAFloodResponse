package edu.utap.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface SecurePrefsProviderInterface {
    fun cacheUserData(userJson: String, loginTimeMillis: Long)
    fun getCachedUserData(): String?
    fun getInitialLoginTime(): Long
    fun clearAuthCache()
}

/**
 * Default implementation of [SecurePrefsProviderInterface] that uses EncryptedSharedPreferences
 * to securely store and retrieve user authentication data.
 *
 * This class provides methods to:
 * - Cache user data (serialized as JSON) along with the initial login time.
 * - Retrieve the cached user data.
 * - Retrieve the initial login time.
 * - Clear the entire authentication cache.
 *
 * The data is encrypted using AES256-GCM for values and AES256-SIV for keys,
 * ensuring confidentiality and integrity. The encryption keys are managed using
 * the AndroidX Security library's MasterKey.
 *
 * @property context The application context, required to initialize the EncryptedSharedPreferences.
 */
class DefaultSecurePrefsProvider(context: Context) : SecurePrefsProviderInterface {

    companion object {
        private const val PREF_FILE_NAME = "secure_auth_prefs"
        private const val KEY_CACHED_USER = "cachedUser"
        private const val KEY_INITIAL_LOGIN_TIME = "initialLoginTime"
    }

    private val prefs: SharedPreferences

    init {
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
     * Caches user data and the initial login time in shared preferences.
     *
     * This function stores the user's data as a JSON string and the timestamp of their
     * initial login in shared preferences. This data can be retrieved later to
     * maintain user state or perform other operations.
     *
     * @param userJson The user data serialized as a JSON string. This should contain
     *                 all relevant information about the user that needs to be
     *                 cached.
     * @param loginTimeMillis The timestamp (in milliseconds since the epoch)
     *                        representing the user's initial login time. This is
     *                        useful for tracking session duration or other time-based
     *                        events.
     *
     * @see [KEY_CACHED_USER]
     * @see [KEY_INITIAL_LOGIN_TIME]
     */
    override fun cacheUserData(userJson: String, loginTimeMillis: Long) {
        prefs.edit()
            .putString(KEY_CACHED_USER, userJson)
            .putLong(KEY_INITIAL_LOGIN_TIME, loginTimeMillis)
            .apply()
    }

    /**
     * Retrieves the cached user data from shared preferences.
     *
     * This function accesses the shared preferences to retrieve a string value
     * associated with the key `KEY_CACHED_USER`. This string typically represents
     * serialized user data that was previously stored for caching purposes.
     *
     * @return The cached user data as a string, or `null` if no data is found
     *         for the key `KEY_CACHED_USER` or if an error occurs during retrieval.
     *
     * Example Usage:
     * ```kotlin
     * val userData = getCachedUserData()
     * if (userData != null) {
     *     // Use the cached user data
     *     println("Cached user data: $userData")
     * } else {
     *     // No cached user data found
     *     println("No cached user data found.")
     * }
     * ```
     */
    override fun getCachedUserData(): String? = prefs.getString(KEY_CACHED_USER, null)

    /**
     * Retrieves the initial login time of the user.
     *
     * This function fetches the timestamp representing the first time the user logged in.
     * It retrieves the value stored in shared preferences under the key [KEY_INITIAL_LOGIN_TIME].
     * If no value is found for this key, it defaults to 0L, indicating that the user has likely not logged in before.
     *
     * @return A Long representing the initial login time as a Unix timestamp (milliseconds since epoch).
     *         Returns 0L if the initial login time has not been recorded.
     */
    override fun getInitialLoginTime(): Long = prefs.getLong(KEY_INITIAL_LOGIN_TIME, 0L)

    /**
     * Clears the authentication cache.
     *
     * This function removes all stored authentication-related data from the shared preferences.
     * This typically includes tokens, user IDs, and other sensitive information
     * that is used to maintain an authenticated session.
     *
     * Calling this method effectively logs the user out, as any subsequent requests
     * requiring authentication will fail until the user re-authenticates.
     *
     * After calling this function, the `SharedPreferences` used for storing authentication data will be empty.
     */
    override fun clearAuthCache() {
        prefs.edit().clear().apply()
    }
}
