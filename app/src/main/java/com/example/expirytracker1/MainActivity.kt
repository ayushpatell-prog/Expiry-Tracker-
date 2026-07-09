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
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.example.expirytracker1.screens.*
import com.example.expirytracker1.ui.theme.ExpiryTracker1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            ExpiryTracker1Theme(darkTheme = darkMode) {
                var currentScreen by remember { mutableStateOf("LOGIN") }

                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition",
                    transitionSpec = {
                        if (targetState == "HOME" && (initialState == "LOGIN" || initialState == "SIGNUP")) {
                            (fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f))
                                .togetherWith(fadeOut(animationSpec = tween(500)))
                        } else if (targetState == "SCANNER") {
                            (slideInVertically(animationSpec = tween(400)) { it } + fadeIn())
                                .togetherWith(fadeOut(animationSpec = tween(400)))
                        } else if (initialState == "SCANNER") {
                            (fadeIn(animationSpec = tween(400)))
                                .togetherWith(slideOutVertically(animationSpec = tween(400)) { it } + fadeOut())
                        } else {
                            fadeIn(animationSpec = tween(300))
                                .togetherWith(fadeOut(animationSpec = tween(300)))
                        }
                    }
                ) { screen ->
                    when (screen) {
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