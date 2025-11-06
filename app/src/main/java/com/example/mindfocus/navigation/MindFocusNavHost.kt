package com.example.mindfocus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mindfocus.ui.feature.calibration.CalibrationScreen
import com.example.mindfocus.ui.feature.history.HistoryScreen
import com.example.mindfocus.ui.feature.home.HomeScreen
import com.example.mindfocus.ui.feature.login.LoginScreen
import com.example.mindfocus.ui.feature.session.SessionScreen
import com.example.mindfocus.ui.feature.settings.SettingsScreen

@Composable
fun MindFocusNavHost(
    navController: NavHostController,
    startDestination: String = NavRoute.Login.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = NavRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                },
                modifier = Modifier
            )
        }
        
        composable(route = NavRoute.Home.route) {
            HomeScreen(
                onCalibrationClick = {
                    navController.navigate(NavRoute.Calibration.route)
                },
                onStartSessionClick = {
                    navController.navigate(NavRoute.Session.route)
                },
                onHistoryClick = {
                    navController.navigate(NavRoute.History.route)
                },
                onSettingsClick = {
                    navController.navigate(NavRoute.Settings.route)
                },
                onLogoutClick = {
                    // This will be handled by MainActivity via auth state observation
                    // The actual logout is done in HomeScreen, navigation happens automatically
                },
                modifier = Modifier
            )
        }
        
        composable(route = NavRoute.Calibration.route) {
            CalibrationScreen(
                onStopClick = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Home.route) { inclusive = false }
                    }
                },
                onPauseResumeClick = {
                    // Pause/resume is handled by ViewModel
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCalibrationComplete = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Home.route) { inclusive = false }
                    }
                },
                modifier = Modifier
            )
        }
        
        composable(route = NavRoute.Session.route) {
            SessionScreen(
                onStopClick = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Home.route) { inclusive = false }
                    }
                },
                onPauseResumeClick = {
                    // Pause/resume is handled by ViewModel
                },
                onFlipCameraClick = {
                    // Camera flip logic will be handled by camera manager
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                modifier = Modifier
            )
        }
        
        composable(route = NavRoute.History.route) {
            HistoryScreen(
                sessions = null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                modifier = Modifier
            )
        }
        
        composable(route = NavRoute.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                modifier = Modifier
            )
        }
    }
}

