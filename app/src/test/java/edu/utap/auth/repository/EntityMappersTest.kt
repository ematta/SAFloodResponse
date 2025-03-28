package edu.utap.auth.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import edu.utap.db.UserEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import io.mockk.every
import io.mockk.mockk

class EntityMappersTest {
    
    @Test
    fun `toUserEntity creates UserEntity from FirebaseUser`() {
        // Setup
        val mockUser = mockk<FirebaseUser>()
        val mockUri = mockk<Uri>()
        val userId = "test-uid"
        val displayName = "Test User"
        val email = "test@example.com"
        val photoUrl = "https://example.com/photo.jpg"
        
        every { mockUser.uid } returns userId
        every { mockUser.displayName } returns displayName
        every { mockUser.email } returns email
        every { mockUser.photoUrl } returns mockUri
        every { mockUri.toString() } returns photoUrl
        
        // Execute
        val result = mockUser.toUserEntity()
        
        // Verify
        assertEquals(userId, result.userId)
        assertEquals(displayName, result.name)
        assertEquals(email, result.email)
        assertEquals(photoUrl, result.profilePic)
        assertEquals("regular", result.role)
    }
    
    @Test
    fun `toUserEntity handles null values from FirebaseUser`() {
        // Setup
        val mockUser = mockk<FirebaseUser>()
        val userId = "test-uid"
        
        every { mockUser.uid } returns userId
        every { mockUser.displayName } returns null
        every { mockUser.email } returns null
        every { mockUser.photoUrl } returns null
        
        // Execute
        val result = mockUser.toUserEntity()
        
        // Verify
        assertEquals(userId, result.userId)
        assertEquals("", result.name)
        assertEquals("", result.email)
        assertEquals(null, result.profilePic)
        assertEquals("regular", result.role)
    }
    
    @Test
    fun `updateFromFirebaseUser updates UserEntity correctly`() {
        // Setup
        val mockUser = mockk<FirebaseUser>()
        val mockUri = mockk<Uri>()
        
        val userId = "test-uid"
        val oldName = "Old Name"
        val newName = "New Name"
        val oldEmail = "old@example.com"
        val newEmail = "new@example.com"
        val oldPhoto = "https://example.com/old.jpg"
        val newPhoto = "https://example.com/new.jpg"
        
        val existingEntity = UserEntity(
            userId = userId,
            name = oldName,
            email = oldEmail,
            profilePic = oldPhoto,
            role = "admin"
        )
        
        every { mockUser.displayName } returns newName
        every { mockUser.email } returns newEmail
        every { mockUser.photoUrl } returns mockUri
        every { mockUri.toString() } returns newPhoto
        
        // Execute
        val result = existingEntity.updateFromFirebaseUser(mockUser)
        
        // Verify
        assertEquals(userId, result.userId)
        assertEquals(newName, result.name)
        assertEquals(newEmail, result.email)
        assertEquals(newPhoto, result.profilePic)
        assertEquals("admin", result.role) // Role should not change
    }
    
    @Test
    fun `updateFromFirebaseUser preserves existing values when Firebase values are null`() {
        // Setup
        val mockUser = mockk<FirebaseUser>()
        
        val userId = "test-uid"
        val name = "Existing Name"
        val email = "existing@example.com"
        val photo = "https://example.com/existing.jpg"
        
        val existingEntity = UserEntity(
            userId = userId,
            name = name,
            email = email,
            profilePic = photo,
            role = "volunteer"
        )
        
        every { mockUser.displayName } returns null
        every { mockUser.email } returns null
        every { mockUser.photoUrl } returns null
        
        // Execute
        val result = existingEntity.updateFromFirebaseUser(mockUser)
        
        // Verify
        assertEquals(userId, result.userId)
        assertEquals(name, result.name) // Should preserve existing name
        assertEquals(email, result.email) // Should preserve existing email
        assertEquals(photo, result.profilePic) // Should preserve existing photo
        assertEquals("volunteer", result.role) // Role should not change
    }
} 
