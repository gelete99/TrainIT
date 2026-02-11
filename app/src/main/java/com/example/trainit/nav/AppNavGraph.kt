package com.example.trainit.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.trainit.ui.theme.screens.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Login.route,
        modifier = modifier
    ) {

        // Auth
        composable(Routes.Login.route) { LoginScreen() }
        composable(Routes.Register.route) { RegisterScreen() }
        composable(Routes.Onboarding.route) { OnboardingScreen() }

        // Main
        composable(Routes.Home.route) { HomeScreen() }
        composable(Routes.Plan.route) { PlanScreen() }
        composable(Routes.LogWorkout.route) { LogWorkoutScreen() }
        composable(Routes.History.route) { HistoryScreen() }
        composable(Routes.Profile.route) { ProfileScreen() }
    }
}
