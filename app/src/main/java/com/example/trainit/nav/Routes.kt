package com.example.trainit.nav

sealed class Routes(val route: String) {

    // Auth / Onboarding
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object Register : Routes("register")
    data object Onboarding : Routes("onboarding")

    // Main app
    data object Home : Routes("home")
    data object Plan : Routes("plan")
    data object LogWorkout : Routes("log_workout")
    data object History : Routes("history")
    data object Profile : Routes("profile")
}
