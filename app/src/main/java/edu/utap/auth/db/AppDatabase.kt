package edu.utap.auth.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database configuration for the Flood Response application.
 * 
 * This abstract class defines the database schema and provides access to the
 * Data Access Objects (DAOs) for interacting with the database tables.
 * It uses the singleton pattern to ensure only one instance of the database
 * exists throughout the application lifecycle.
 */
@Database(
    entities = [
        UserEntity::class,
        FloodReportEntity::class,
        DiscussionEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false  // Don't export the schema for version control
)
@TypeConverters(Converters::class)  // Register type converters for custom data types
abstract class AppDatabase : RoomDatabase() {
    
    // Abstract methods that return DAOs for database operations
    abstract fun userDao(): UserDao
    abstract fun floodReportDao(): FloodReportDao
    abstract fun discussionDao(): DiscussionDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile  // Ensures visibility of changes to INSTANCE across threads
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton database instance, creating it if it doesn't exist.
         * 
         * This method implements the double-checked locking pattern to ensure
         * thread safety when creating the database instance.
         * 
         * @param context Application context used to create the database
         * @return The singleton AppDatabase instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flood_response_database"
                )
                .createFromAsset("database/initial_database.db")  // Initialize from pre-populated database
                .fallbackToDestructiveMigration()  // Recreate database if migration path not found
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}