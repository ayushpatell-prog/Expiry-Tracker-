package com.example.expirytracker1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.expirytracker1.screens.AssistantScreen
import com.example.expirytracker1.screens.HomeScreen
import com.example.expirytracker1.screens.InventoryScreen
import com.example.expirytracker1.screens.LoginScreen
import com.example.expirytracker1.screens.ProfileScreen
import com.example.expirytracker1.screens.ScannerScreen
import com.example.expirytracker1.screens.SettingsScreen
import com.example.expirytracker1.screens.SignUpScreen
import com.example.expirytracker1.ui.theme.ExpiryTracker1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            ExpiryTracker1Theme(darkTheme = darkMode) {
                var currentScreen by remember { mutableStateOf("LOGIN") }
                
                when (currentScreen) {
                    "LOGIN" -> LoginScreen(
                        onSignUpClick = { currentScreen = "SIGNUP" },
                        onLoginSuccess = { currentScreen = "HOME" }
                    )
                    "SIGNUP" -> SignUpScreen(
                        onLoginClick = { currentScreen = "LOGIN" },
                        onSignUpSuccess = { currentScreen = "HOME" }
                    )
                    "HOME" -> HomeScreen(onNavigate = { currentScreen = it })
                    "INVENTORY" -> InventoryScreen(onNavigate = { currentScreen = it })
                    "ASSISTANT" -> AssistantScreen(onNavigate = { currentScreen = it })
                    "SETTINGS" -> SettingsScreen(onNavigate = { currentScreen = it })
                    "PROFILE" -> ProfileScreen(
                        darkMode = darkMode,
                        onDarkModeChange = { darkMode = it },
                        onNavigate = { currentScreen = it }
                    )
                    "SCANNER" -> ScannerScreen(onNavigateBack = { currentScreen = "HOME" })
                }
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
    ExpiryTracker1Theme {
        Greeting("Android")
    }
}