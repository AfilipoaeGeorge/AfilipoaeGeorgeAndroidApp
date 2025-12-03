package com.example.mindfocus.navigation

sealed class NavRoute(val route: String) {
    object Login : NavRoute("login")
    
    object Home : NavRoute("home")
    object Calibration : NavRoute("calibration")
    object Session : NavRoute("session")
    object History : NavRoute("history")
    object Profile : NavRoute("profile")
    object Settings : NavRoute("settings")
}

