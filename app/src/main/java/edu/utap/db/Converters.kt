package edu.utap.db

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database.
 *
 * This class provides methods to convert between complex data types and types that Room can persist.
 * Room uses these converters to store and retrieve data types that are not directly supported by SQLite.
 *
 * The converters in this class handle:
 * - Date objects (converting to/from Long timestamps)
 * - Boolean values (converting to/from Int values for SQLite compatibility)
 * - List<String> objects (converting to/from JSON strings)
 */
class Converters {
    /**
     * Converts a Long timestamp to a Date object.
     *
     * @param value The timestamp in milliseconds since epoch
     * @return A Date object representing the timestamp, or null if the input is null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    /**
     * Converts a Date object to a Long timestamp.
     *
     * @param date The Date object to convert
     * @return The timestamp in milliseconds since epoch, or null if the input is null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    /**
     * Converts a Boolean value to an Integer for SQLite storage.
     *
     * @param value The Boolean value to convert
     * @return 1 if true, 0 if false
     */
    @TypeConverter
    fun fromBoolean(value: Boolean): Int = if (value) 1 else 0

    /**
     * Converts an Integer from SQLite to a Boolean value.
     *
     * @param value The Integer value from SQLite
     * @return true if the value is 1, false otherwise
     */
    @TypeConverter
    fun toBoolean(value: Int): Boolean = value == 1
    
    /**
     * Converts a List of Strings to a JSON string for SQLite storage.
     *
     * @param list The List<String> to convert
     * @return A JSON string representation of the list
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString(",") ?: null
    }

    /**
     * Converts a JSON string from SQLite to a List of Strings.
     *
     * @param value The JSON string from SQLite
     * @return A List<String> parsed from the JSON string
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() } ?: null
    }
}
