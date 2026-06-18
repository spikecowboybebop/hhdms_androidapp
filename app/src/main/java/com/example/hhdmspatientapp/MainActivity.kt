package com.example.hhdmspatientapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.hhdmspatientapp.ui.theme.HHDMSPatientAppTheme // Import your theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use your actual custom theme here
            HHDMSPatientAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFFF4F6F8)
                ) {
                    AuthScreen(onAuthSuccess = { verifiedEmail ->
                        Toast.makeText(this, "Logged in as $verifiedEmail", Toast.LENGTH_LONG).show()
                    })
                }
            }
        }
    }
}