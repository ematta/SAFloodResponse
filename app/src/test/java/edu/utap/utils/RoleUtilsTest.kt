package edu.utap.utils

import org.junit.Test
import org.junit.Assert.*

class RoleUtilsTest {

    @Test
    fun `getRoleDescriptions returns all roles`() {
        // Check if getRoleDescriptions returns a map containing all defined roles (regular, volunteer, admin).
        val roles = RoleUtils.roleDescriptions.keys
        val expectedRoles = setOf(
            RoleUtils.ROLE_REGULAR,
            RoleUtils.ROLE_VOLUNTEER,
            RoleUtils.ROLE_ADMIN
        )
        assertEquals(expectedRoles, roles)
    }

    @Test
    fun `getRoleDescriptions correct descriptions`() {
        // Verify that the descriptions for each role in getRoleDescriptions match the expected values.
        val descriptions = RoleUtils.roleDescriptions
        assertEquals("Can report floods, participate in discussions, and request/offer help", descriptions[RoleUtils.ROLE_REGULAR])
        assertEquals("Can coordinate rescue efforts and perform regular user actions", descriptions[RoleUtils.ROLE_VOLUNTEER])
        assertEquals("Can moderate discussions, verify reports, and perform all user actions", descriptions[RoleUtils.ROLE_ADMIN])
    }

    @Test
    fun `getRoleDescriptions empty check`() {
        // Test if getRoleDescriptions returns an empty map if no roles are defined. 
        // Although, in this case it should not happen because of the definition
        // This test is not applicable, but we can assert the map is not empty.
        assertFalse(RoleUtils.roleDescriptions.isEmpty())
    }

    @Test
    fun `hasPermission regular user regular role`() {
        // Verify that a regular user has regular permissions (should be true).
        assertTrue(RoleUtils.hasPermission(RoleUtils.ROLE_REGULAR, RoleUtils.ROLE_REGULAR))
    }

    @Test
    fun `hasPermission volunteer user regular role`() {
        // Verify that a volunteer user has regular permissions (should be true).
        assertTrue(RoleUtils.hasPermission(RoleUtils.ROLE_VOLUNTEER, RoleUtils.ROLE_REGULAR))
    }

    @Test
    fun `hasPermission admin user regular role`() {
        // Verify that an admin user has regular permissions (should be true).
        assertTrue(RoleUtils.hasPermission(RoleUtils.ROLE_ADMIN, RoleUtils.ROLE_REGULAR))
    }

    @Test
    fun `hasPermission regular user volunteer role`() {
        // Verify that a regular user does not have volunteer permissions (should be false).
        assertFalse(RoleUtils.hasPermission(RoleUtils.ROLE_REGULAR, RoleUtils.ROLE_VOLUNTEER))
    }

    @Test
    fun `hasPermission volunteer user volunteer role`() {
        // Verify that a volunteer user has volunteer permissions (should be true).
        assertTrue(RoleUtils.hasPermission(RoleUtils.ROLE_VOLUNTEER, RoleUtils.ROLE_VOLUNTEER))
    }

    @Test
    fun `hasPermission admin user volunteer role`() {
        // Verify that an admin user has volunteer permissions (should be true).
        assertTrue(RoleUtils.hasPermission(RoleUtils.ROLE_ADMIN, RoleUtils.ROLE_VOLUNTEER))
    }

    @Test
    fun `hasPermission regular user admin role`() {
        // Verify that a regular user does not have admin permissions (should be false).
        assertFalse(RoleUtils.hasPermission(RoleUtils.ROLE_REGULAR, RoleUtils.ROLE_ADMIN))
    }

    @Test
    fun `hasPermission volunteer user admin role`() {
        // Verify that a volunteer user does not have admin permissions (should be false).
        assertFalse(RoleUtils.hasPermission(RoleUtils.ROLE_VOLUNTEER, RoleUtils.ROLE_ADMIN))
    }

    @Test
    fun `hasPermission admin user admin role`() {
        // Verify that an admin user has admin permissions (should be true).
        assertTrue(RoleUtils.hasPermission(RoleUtils.ROLE_ADMIN, RoleUtils.ROLE_ADMIN))
    }

    @Test
    fun `hasPermission invalid user role`() {
        // Test the scenario where the provided userRole is not one of the defined roles (e.g., null, empty, unknown).
        assertFalse(RoleUtils.hasPermission("", RoleUtils.ROLE_REGULAR))
        assertFalse(RoleUtils.hasPermission("unknown", RoleUtils.ROLE_VOLUNTEER))
    }

    @Test
    fun `hasPermission invalid required role`() {
        // Test the scenario where the provided requiredRole is not one of the defined roles (e.g., null, empty, unknown). Should return false
        assertFalse(RoleUtils.hasPermission(RoleUtils.ROLE_REGULAR, ""))
        assertFalse(RoleUtils.hasPermission(RoleUtils.ROLE_ADMIN, "unknown"))
    }

    @Test
    fun `getAllRoles returns all roles`() {
        // Check if getAllRoles returns a list containing all defined roles (regular, volunteer, admin).
        val allRoles = RoleUtils.getAllRoles()
        val expectedAllRoles = listOf(
            RoleUtils.ROLE_REGULAR,
            RoleUtils.ROLE_VOLUNTEER,
            RoleUtils.ROLE_ADMIN
        )
        assertEquals(expectedAllRoles.toSet(), allRoles.toSet())
        assertEquals(expectedAllRoles.size, allRoles.size)
    }

    @Test
    fun `getAllRoles correct role order`() {
        // Check if the order of roles returned by getAllRoles is in the right order.
        val orderedRoles = RoleUtils.getAllRoles()
        assertEquals(listOf(
            RoleUtils.ROLE_REGULAR,
            RoleUtils.ROLE_VOLUNTEER,
            RoleUtils.ROLE_ADMIN
        ), orderedRoles)
    }

    @Test
    fun `getAllRoles no duplicated roles`() {
        // Check to make sure the getAllRoles method does not return any duplicated roles.
        val rolesList = RoleUtils.getAllRoles()
        assertEquals(rolesList.size, rolesList.toSet().size)
    }

    @Test
    fun `getAllRoles empty check`() {
        // Test if getAllRoles returns an empty list if no roles are defined. 
        // Although, in this case it should not happen because of the definition
        // This test is not applicable, but we can assert the list is not empty.
        assertFalse(RoleUtils.getAllRoles().isEmpty())
    }
}
