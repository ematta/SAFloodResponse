package edu.utap.utils

import org.junit.Test

class FirebaseStorageUtilTest {

    @Test
    fun `uploadProfileImage successful upload`() {
        // Test a successful image upload to Firebase Storage and 
        // verify the returned download URL.
        // TODO implement test
    }

    @Test
    fun `uploadProfileImage invalid URI`() {
        // Test with an invalid image URI to ensure the function 
        // handles invalid input correctly and returns an error.
        // TODO implement test
    }

    @Test
    fun `uploadProfileImage network error`() {
        // Simulate a network error during upload to ensure 
        // proper error handling.
        // TODO implement test
    }

    @Test
    fun `uploadProfileImage empty user ID`() {
        // Test with an empty user ID to verify how the 
        // function handles this edge case.
        // TODO implement test
    }

    @Test
    fun `uploadProfileImage null context`() {
        // Test with null context to check for null pointer exceptions 
        // and ensure function handles null context.
        // TODO implement test
    }

    @Test
    fun `uploadProfileImage non image file type`() {
        // Test with a non-image file type to ensure it's 
        // handled correctly. Check that the file extension is being used.
        // TODO implement test
    }

    @Test
    fun `uploadProfileImage context lacks permissions`() {
        // Test with a context that does not have access to read 
        // files and check if exception is thrown
        // TODO implement test
    }

    @Test
    fun `uploadProfileImage large file upload`() {
        // Upload a very large file to ensure that the function 
        // will return an error if file is to large.
        // TODO implement test
    }

    @Test
    fun `deleteProfileImage successful deletion`() {
        // Test a successful image deletion from Firebase Storage 
        // with a valid image URL.
        // TODO implement test
    }

    @Test
    fun `deleteProfileImage invalid URL`() {
        // Test with an invalid image URL to ensure the function 
        // handles invalid URLs and returns an error.
        // TODO implement test
    }

    @Test
    fun `deleteProfileImage non existent image`() {
        // Test deleting an image that does not exist to verify 
        // how the function handles non-existent images.
        // TODO implement test
    }

    @Test
    fun `deleteProfileImage network error`() {
        // Simulate a network error during deletion to 
        // ensure proper error handling.
        // TODO implement test
    }

    @Test
    fun `getFileExtension valid mime type`() {
        // Test the getFileExtension with a valid mime type in order to make sure 
        // the correct extension is being set
        // TODO implement test
    }

    @Test
    fun `getFileExtension invalid mime type`() {
        // Test getFileExtension function with an invalid mime type to make sure 
        // function handles it correctly and sets a default file extension.
        // TODO implement test
    }

    @Test
    fun `getFileExtension null mime type`() {
        // Test getFileExtension with a null mime type to ensure that it will 
        // still set a default file extension and that no exceptions are thrown
        // TODO implement test
    }

    @Test
    fun `getFileExtension null context`() {
        // Test getFileExtension with a null context to ensure that it throws an 
        // exception or handles null context.
        // TODO implement test
    }

    @Test
    fun `uploadProfileImage storage error`() {
        // test for errors that come directly from firebase storage such 
        // as storage quota errors
        // TODO implement test
    }

    @Test
    fun `deleteProfileImage storage error`() {
        // test for errors that come directly from firebase storage such as 
        // storage quota errors
        // TODO implement test
    }

}
