package com.example.mindfocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.ui.feature.home.HomeScreen
import com.example.mindfocus.ui.feature.login.LoginScreen
import com.example.mindfocus.ui.theme.MindFocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindFocusTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val isLoggedIn by authPreferencesManager.isLoggedIn.collectAsState(initial = false)
    
    if (isLoggedIn) {
        HomeScreen(modifier = Modifier.fillMaxSize())
    } else {
        LoginScreen(
            onLoginSuccess = { /* State will automatically update via Flow */ },
            modifier = Modifier.fillMaxSize()
        )
    }
}