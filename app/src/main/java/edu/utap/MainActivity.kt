package edu.utap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import edu.utap.auth.LoginScreen
import edu.utap.auth.RegisterScreen
import edu.utap.ui.theme.SAFloodResponseTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import edu.utap.auth.AuthViewModel

class MainActivity : ComponentActivity() {

    val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showRegisterScreen by remember { mutableStateOf(false) }
            var isAuthenticated by remember { mutableStateOf(false) }
            
            if (isAuthenticated) {
                SAFloodResponseTheme {
                    Greeting("SA Flood Response User")
                }
            } else if (showRegisterScreen) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { showRegisterScreen = false },
                    onRegisterSuccess = { isAuthenticated = true }
                )
            } else {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { showRegisterScreen = true },
                    onLoginSuccess = { isAuthenticated = true }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SAFloodResponseTheme {
        Greeting("Android")
    }
}