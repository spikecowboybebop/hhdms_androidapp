package com.example.hhdmspatientapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.hhdmspatientapp.ui.theme.HHDMSPatientAppTheme

enum class AppScreen {
    AUTH, DASHBOARD
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚀 BABY STEP: Warm up the WebRTC hardware factory and connection pipelines instantly!
        CallSignalingManager.initialize(applicationContext)

        setContent {
            HHDMSPatientAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFFF4F6F8)
                ) {
                    // 1. Set up the local navigation state containers
                    var currentScreen by remember { mutableStateOf(AppScreen.AUTH) }
                    var loggedInUserEmail by remember { mutableStateOf("") }

                    // 2. Conditionally switch screens based on currentScreen value
                    when (currentScreen) {
                        AppScreen.AUTH -> {
                            AuthScreen(onAuthSuccess = { verifiedEmail ->
                                // Trigger the toast notification alert
                                Toast.makeText(this@MainActivity, "Logged in as $verifiedEmail", Toast.LENGTH_LONG).show()

                                // Save the email string and flip state to navigate forward
                                loggedInUserEmail = verifiedEmail
                                currentScreen = AppScreen.DASHBOARD
                            })
                        }

                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                userEmail = loggedInUserEmail,
                                onLogout = {
                                    // Reset state tracker flags to route them backward
                                    currentScreen = AppScreen.AUTH
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}