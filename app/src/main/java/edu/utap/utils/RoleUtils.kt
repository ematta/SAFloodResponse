package edu.utap.utils

/**
 * Utility class for managing user roles and permissions in the application.
 */
object RoleUtils {
    // Role constants
    const val ROLE_REGULAR = "regular"
    const val ROLE_VOLUNTEER = "volunteer"
    const val ROLE_ADMIN = "admin"

    // Role descriptions for UI display
    val roleDescriptions = mapOf(
        ROLE_REGULAR to "Can report floods, participate in discussions, and request/offer help",
        ROLE_VOLUNTEER to "Can coordinate rescue efforts and perform regular user actions",
        ROLE_ADMIN to "Can moderate discussions, verify reports, and perform all user actions"
    )

    /**
     * Checks if a user has at least the specified permission level.
     *
     * @param userRole The role of the user
     * @param requiredRole The minimum role required for the action
     * @return True if the user has sufficient permissions, false otherwise
     */
    fun hasPermission(userRole: String, requiredRole: String): Boolean {
        val validRoles = setOf(ROLE_REGULAR, ROLE_VOLUNTEER, ROLE_ADMIN)
        if (userRole !in validRoles) return false
        return when (requiredRole) {
            ROLE_REGULAR -> true // Everyone has regular permissions
            ROLE_VOLUNTEER -> userRole == ROLE_VOLUNTEER || userRole == ROLE_ADMIN
            ROLE_ADMIN -> userRole == ROLE_ADMIN
            else -> false
        }
    }

    /**
     * Returns a list of all available roles, suitable for UI selection.
     */
    fun getAllRoles(): List<String> = listOf(ROLE_REGULAR, ROLE_VOLUNTEER, ROLE_ADMIN)
}
