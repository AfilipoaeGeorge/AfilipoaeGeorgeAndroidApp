package com.example.mindfocus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.navigation.MindFocusNavHost
import com.example.mindfocus.navigation.NavRoute
import com.example.mindfocus.ui.theme.MindFocusTheme

class MainActivity : FragmentActivity() {
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
    val biometricAuthManager = remember { com.example.mindfocus.core.auth.BiometricAuthManager(context) }
    
    var initialLoginState by remember { mutableStateOf<Boolean?>(null) }
    var hasReadInitialState by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!hasReadInitialState) {
            hasReadInitialState = true
            initialLoginState = authPreferencesManager.isLoggedIn.first()
        }
    }
    
    val startDestination = remember(initialLoginState) {
        when {
            initialLoginState == true && biometricAuthManager.isBiometricAvailable() -> NavRoute.BiometricAuth.route
            initialLoginState == true -> NavRoute.Home.route
            else -> NavRoute.Login.route
        }
    }
    
    var previousLoggedInState by remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(isLoggedIn) {
        val currentRoute = navController.currentDestination?.route
        
        if (previousLoggedInState == isLoggedIn) {
            return@LaunchedEffect
        }
        
        val wasLoggedIn = previousLoggedInState ?: false
        previousLoggedInState = isLoggedIn
        
        if (!isLoggedIn) {
            // user logged out - go to login
            if (currentRoute != NavRoute.Login.route && currentRoute != NavRoute.BiometricAuth.route) {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (!wasLoggedIn) {
            val validRoutes = setOf(
                NavRoute.Home.route,
                NavRoute.BiometricAuth.route,
                NavRoute.Calibration.route,
                NavRoute.Session.route,
                NavRoute.History.route,
                NavRoute.Profile.route,
                NavRoute.Settings.route,
                NavRoute.Tips.route
            )

            if (currentRoute != NavRoute.Login.route && currentRoute !in validRoutes) {
                navController.navigate(NavRoute.Home.route) {
                    popUpTo(0) { inclusive = true }
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