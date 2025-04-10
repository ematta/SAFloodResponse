package edu.utap.utils

import org.junit.Test

class LocationPermissionHandlerTest {

    @Test
    fun `onCreate lifecycle call`() {
        // Verify that onCreate correctly calls the super.onCreate and re-registers the locationPermissionRequest. 
        // This ensures that the activity result launcher is ready upon activity creation.
        // TODO implement test
    }

    @Test
    fun `onCreate register for permission multiple call`() {
        // Check that calling onCreate multiple times does not create multiple instances of the 
        // permission request launcher.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission granted`() {
        // Verify that if location permission is already granted, onGranted callback is executed immediately 
        // and permission is not requested again.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission denied`() {
        // Verify that if location permission is not granted, it requests the permissions using 
        // locationPermissionRequest.launch.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission fine location permission`() {
        // Verify that if only ACCESS_FINE_LOCATION is granted, the onGranted callback is executed.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission coarse location permission`() {
        // Verify that if only ACCESS_COARSE_LOCATION is granted, the onGranted callback is executed.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission no location permission`() {
        // Verify that if neither ACCESS_FINE_LOCATION nor ACCESS_COARSE_LOCATION is granted, 
        // the onDenied callback is executed.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission onGranted callback execution`() {
        // Verify that onGranted callback function in checkAndRequestLocationPermission is called when permissions are granted.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission onDenied callback execution`() {
        // Verify that onDenied callback function in checkAndRequestLocationPermission is called when 
        // permissions are denied.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission empty callbacks`() {
        // Verify that the function correctly handles empty lambdas when no callbacks are passed to 
        // checkAndRequestLocationPermission. The test should not crash or error if default callbacks are empty.
        // TODO implement test
    }

    @Test
    fun `hasLocationPermission fine location permission granted`() {
        // Verify that hasLocationPermission returns true when ACCESS_FINE_LOCATION is granted.
        // TODO implement test
    }

    @Test
    fun `hasLocationPermission coarse location permission granted`() {
        // Verify that hasLocationPermission returns true when ACCESS_COARSE_LOCATION is granted.
        // TODO implement test
    }

    @Test
    fun `hasLocationPermission no location permission granted`() {
        // Verify that hasLocationPermission returns false when neither ACCESS_FINE_LOCATION nor 
        // ACCESS_COARSE_LOCATION is granted.
        // TODO implement test
    }

    @Test
    fun `requestLocationPermission request type`() {
        // Check that the requestLocationPermission() is requesting for both fine and coarse location permissions
        // TODO implement test
    }

    @Test
    fun `requestLocationPermission launch parameter`() {
        // Ensure that the launch function is invoked with the correct parameter, which is an array containing 
        // ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION.
        // TODO implement test
    }

    @Test
    fun `checkAndRequestLocationPermission both permission granted`() {
        // Verify that if both location permissions are already granted, onGranted callback is executed immediately. 
        //and no request is triggered.
        // TODO implement test
    }

}
