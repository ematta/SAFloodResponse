package edu.utap.db

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class to convert the SQL script to an SQLite database file.
 * Room can then use this pre-populated database.
 */
object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"

    /**
     * Debugging function to log the SQL from the initial database script
     *
     * This synchronous version is kept for backward compatibility but should be avoided
     * in favor of the asynchronous version.
     *
     * @deprecated Use logDatabaseScriptAsync instead
     */
    fun logDatabaseScript(context: Context) {
        try {
            val inputStream = context.assets.open("database/initial_database.sql")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
                stringBuilder.append("\n")
            }

            Log.d(TAG, "Database script content:")
            Log.d(TAG, stringBuilder.toString())

            inputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading database script", e)
        }
    }

    /**
     * Asynchronous version of logDatabaseScript that runs in a background thread
     * using coroutines to avoid blocking the main thread.
     *
     * @param context The application context
     */
    suspend fun logDatabaseScriptAsync(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("database/initial_database.sql")
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                    stringBuilder.append("\n")
                }

                Log.d(TAG, "Database script content:")
                Log.d(TAG, stringBuilder.toString())

                inputStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading database script", e)
            }
        }
    }
}
