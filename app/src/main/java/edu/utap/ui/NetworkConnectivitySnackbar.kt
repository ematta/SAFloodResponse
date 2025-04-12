package edu.utap.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import edu.utap.utils.NetworkMonitor
import edu.utap.utils.NetworkState

/**
 * A composable that displays a snackbar when network connectivity changes
 * @param modifier Modifier to apply to the snackbar host
 * @param networkMonitor NetworkMonitor to monitor network state
 * @param onNetworkAvailable Callback when network becomes available
 * @param onNetworkUnavailable Callback when network becomes unavailable
 */
@Composable
fun NetworkConnectivitySnackbar(
    modifier: Modifier = Modifier,
    networkMonitor: NetworkMonitor,
    onNetworkAvailable: () -> Unit = {},
    onNetworkUnavailable: () -> Unit = {}
) {
    val networkState by networkMonitor.networkState().collectAsState(
        initial = NetworkState.AVAILABLE
    )
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(networkState) {
        when (networkState) {
            NetworkState.AVAILABLE -> {
                // Clear any existing snackbar
                snackbarHostState.currentSnackbarData?.dismiss()
                onNetworkAvailable()
            }
            NetworkState.UNAVAILABLE, NetworkState.LOST -> {
                snackbarHostState.showSnackbar(
                    message = "No internet connection. Some features may be unavailable.",
                    duration = SnackbarDuration.Indefinite
                )
                onNetworkUnavailable()
            }
            NetworkState.LOSING -> {
                snackbarHostState.showSnackbar(
                    message = "Internet connection is unstable.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(16.dp),
        snackbar = { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = when (networkState) {
                    NetworkState.AVAILABLE -> Color.Green.copy(alpha = 0.8f)
                    NetworkState.UNAVAILABLE, NetworkState.LOST -> Color.Red.copy(alpha = 0.8f)
                    NetworkState.LOSING -> Color.Yellow.copy(alpha = 0.8f)
                }
            )
        }
    )
}
