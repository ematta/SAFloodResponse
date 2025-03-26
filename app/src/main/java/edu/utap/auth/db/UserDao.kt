package edu.utap.auth.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for user operations.
 *
 * This interface defines the database operations that can be performed on users.
 * It provides methods to query, insert, update, and delete user data in the database.
 * The methods are implemented by Room at compile time.
 */
@Dao
interface UserDao {
    /**
     * Gets all users from the database as a Flow.
     *
     * Using Flow allows the UI to observe changes to the data and update automatically
     * when the underlying database changes.
     *
     * @return A Flow emitting the list of all users
     */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    /**
     * Gets a specific user by their ID.
     *
     * @param userId The unique identifier of the user to retrieve
     * @return The user with the specified ID, or null if not found
     */
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * Gets a user by their email address.
     *
     * This is useful for authentication flows where the email is known but the user ID is not.
     *
     * @param email The email address of the user to retrieve
     * @return The user with the specified email, or null if not found
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Inserts or updates a user in the database.
     *
     * If a user with the same ID already exists, it will be replaced.
     *
     * @param user The user to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Updates an existing user in the database.
     *
     * @param user The user to update
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Deletes a user from the database.
     *
     * @param user The user to delete
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)
}
