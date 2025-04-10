package edu.utap.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationPermissionHandlerTest {

    private lateinit var activity: ComponentActivity
    private lateinit var launcher: ActivityResultLauncher<Array<String>>
    private lateinit var handler: LocationPermissionHandler

    @Before
    fun setUp() {
        activity = mockk(relaxed = true)
        launcher = mockk(relaxed = true)

        mockkStatic(ContextCompat::class)

        every {
            activity.registerForActivityResult(
                any<ActivityResultContracts.RequestMultiplePermissions>(),
                capture(permissionCallbackSlot)
            )
        } returns launcher

        handler = LocationPermissionHandler(activity)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private val permissionCallbackSlot = slot<ActivityResultCallback<Map<String, Boolean>>>()

    @Test
    fun `checkAndRequestLocationPermission granted`() = runTest {
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        var grantedCalled = false
        var deniedCalled = false

        handler.checkAndRequestLocationPermission(
            onGranted = { grantedCalled = true },
            onDenied = { deniedCalled = true }
        )

        assertTrue(grantedCalled)
        assertFalse(deniedCalled)
        verify(exactly = 0) { launcher.launch(any()) }
    }

    @Test
    fun `checkAndRequestLocationPermission denied triggers launcher`() = runTest {
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        var grantedCalled = false
        var deniedCalled = false

        handler.checkAndRequestLocationPermission(
            onGranted = { grantedCalled = true },
            onDenied = { deniedCalled = true }
        )

        assertFalse(grantedCalled)
        assertFalse(deniedCalled)
        verify {
            launcher.launch(
                match {
                    it.toList().containsAll(
                        listOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
    }

    @Test
    fun `permission callback grants permission calls onGranted`() = runTest {
        var grantedCalled = false
        var deniedCalled = false

        handler.checkAndRequestLocationPermission(
            onGranted = { grantedCalled = true },
            onDenied = { deniedCalled = true }
        )

        permissionCallbackSlot.captured.onActivityResult(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to true,
                Manifest.permission.ACCESS_COARSE_LOCATION to false
            )
        )

        assertTrue(grantedCalled)
        assertFalse(deniedCalled)
    }

    @Test
    fun `permission callback denies permission calls onDenied`() = runTest {
        var grantedCalled = false
        var deniedCalled = false

        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        handler.checkAndRequestLocationPermission(
            onGranted = { grantedCalled = true },
            onDenied = { deniedCalled = true }
        )

        permissionCallbackSlot.captured.onActivityResult(
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to false,
                Manifest.permission.ACCESS_COARSE_LOCATION to false
            )
        )

        assertFalse(grantedCalled)
        assertTrue(deniedCalled)
    }

    @Test
    fun `checkAndRequestLocationPermission fine location permission granted`() = runTest {
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        var grantedCalled = false

        handler.checkAndRequestLocationPermission(
            onGranted = { grantedCalled = true }
        )

        assertTrue(grantedCalled)
        verify(exactly = 0) { launcher.launch(any()) }
    }

    @Test
    fun `checkAndRequestLocationPermission coarse location permission granted`() = runTest {
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        var grantedCalled = false

        handler.checkAndRequestLocationPermission(
            onGranted = { grantedCalled = true }
        )

        assertTrue(grantedCalled)
        verify(exactly = 0) { launcher.launch(any()) }
    }

    @Test
    fun `checkAndRequestLocationPermission no location permission triggers launcher`() = runTest {
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        handler.checkAndRequestLocationPermission()

        verify {
            launcher.launch(match {
                it.toList().containsAll(
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            })
        }
    }

    @Test
    fun `checkAndRequestLocationPermission empty callbacks do not crash`() = runTest {
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        handler.checkAndRequestLocationPermission()
    }
}
