package com.example.trainit.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.trainit.auth.AuthRepository
import com.example.trainit.ui.theme.screens.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val repo = AuthRepository()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route,
        modifier = modifier
    ) {

        composable(Routes.Splash.route) {
            SplashScreen()

            // Redirección segura en el arranque
            LaunchedEffect(Unit) {
                val next = if (repo.isUserLoggedIn()) Routes.Home.route else Routes.Login.route
                navController.navigate(next) {
                    popUpTo(Routes.Splash.route) { inclusive = true }
                }
            }
        }

        // Auth
        composable(Routes.Login.route) {
            LoginScreen(
                onGoToRegister = { navController.navigate(Routes.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Register.route) {
            RegisterScreen(
                onGoToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Onboarding.route) { OnboardingScreen() }

        // Main
        composable(Routes.Home.route) { HomeScreen() }
        composable(Routes.Plan.route) { PlanScreen() }
        composable(Routes.LogWorkout.route) { LogWorkoutScreen() }
        composable(Routes.History.route) { HistoryScreen() }

        composable(Routes.Profile.route) {
            // si ya lo tienes con logout, mantenlo como lo tengas
            ProfileScreen(
                onLogout = {
                    repo.logout()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
