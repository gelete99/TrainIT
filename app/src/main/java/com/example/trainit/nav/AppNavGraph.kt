package com.example.trainit.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.UserRepository
import com.example.trainit.ui.theme.screens.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val authRepo = AuthRepository()
    val userRepo = UserRepository()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route,
        modifier = modifier
    ) {

        // Splash decide: Login vs Onboarding vs Home
        composable(Routes.Splash.route) {
            SplashScreen()

            LaunchedEffect(Unit) {
                val uid = authRepo.currentUid()

                val next = if (uid == null) {
                    Routes.Login.route
                } else {
                    val hasProfile = try {
                        userRepo.hasProfile(uid)
                    } catch (_: Exception) {
                        false
                    }

                    if (hasProfile) Routes.Home.route else Routes.Onboarding.route
                }

                navController.navigate(next) {
                    popUpTo(Routes.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable(Routes.Login.route) {
            LoginScreen(
                onGoToRegister = { navController.navigate(Routes.Register.route) },
                onLoginSuccess = {
                    // Ir a Splash para decidir Home u Onboarding
                    navController.navigate(Routes.Splash.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Register.route) {
            RegisterScreen(
                onGoToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Routes.Onboarding.route) {
                        popUpTo(Routes.Register.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Home.route) { HomeScreen() }
        composable(Routes.Plan.route) { PlanScreen() }


        composable(Routes.History.route) { HistoryScreen() }


        composable(Routes.LogWorkout.route) {
            LogWorkoutScreen(
                onSaved = {
                    navController.navigate(Routes.History.route) {
                        popUpTo(Routes.LogWorkout.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Profile.route) {
            ProfileScreen(
                onLogout = {
                    authRepo.logout()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Profile.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
