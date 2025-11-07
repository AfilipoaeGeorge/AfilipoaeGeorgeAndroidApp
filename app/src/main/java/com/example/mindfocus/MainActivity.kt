package com.example.mindfocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.navigation.MindFocusNavHost
import com.example.mindfocus.navigation.NavRoute
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
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val isLoggedIn by authPreferencesManager.isLoggedIn.collectAsState(initial = false)
    
    val startDestination = if (isLoggedIn) {
        NavRoute.Home.route
    } else {
        NavRoute.Login.route
    }
    
    LaunchedEffect(isLoggedIn) {
        val currentRoute = navController.currentDestination?.route
        
        if (!isLoggedIn) {
            // user logged out - go to login
            if (currentRoute != NavRoute.Login.route) {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            // user logged in - only navigate if on login screen
            if (currentRoute == NavRoute.Login.route) {
                navController.navigate(NavRoute.Home.route) {
                    popUpTo(NavRoute.Login.route) { inclusive = true }
                }
            }
        }
    }
    
    MindFocusNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    )
}