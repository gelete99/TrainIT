package com.example.trainit.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.trainit.auth.AuthRepository
import com.example.trainit.data.UserRepository
import com.example.trainit.ui.theme.screens.HistoryScreen
import com.example.trainit.ui.theme.screens.HomeScreen
import com.example.trainit.ui.theme.screens.LogWorkoutScreen
import com.example.trainit.ui.theme.screens.LoginScreen
import com.example.trainit.ui.theme.screens.OnboardingScreen
import com.example.trainit.ui.theme.screens.PlanScreen
import com.example.trainit.ui.theme.screens.ProfileScreen
import com.example.trainit.ui.theme.screens.RegisterScreen
import com.example.trainit.ui.theme.screens.SplashScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showMessage: (String) -> Unit
) {
    val authRepo = AuthRepository()
    val userRepo = UserRepository()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route,
        modifier = modifier
    ) {
        composable(Routes.Splash.route) {
            SplashScreen()

            LaunchedEffect(Unit) {
                val uid = authRepo.currentUid()

                val next = if (uid == null) {
                    Routes.Login.route
                } else {
                    val completed = try {
                        userRepo.hasCompletedOnboarding(uid)
                    } catch (_: Exception) {
                        false
                    }
                    if (completed) Routes.Home.route else Routes.Onboarding.route
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
                    // ✅ sin snackbar (no lo quieres)
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
                    // ✅ sin snackbar (no lo quieres)
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
                    showMessage("Perfil guardado")
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Home.route) { HomeScreen() }
        composable(Routes.Plan.route) { PlanScreen() }

        composable(Routes.History.route) {
            HistoryScreen(showMessage = showMessage) // aquí se usa "Entrenamiento eliminado"
        }

        composable(Routes.LogWorkout.route) {
            LogWorkoutScreen(
                onSaved = {
                    showMessage("Entrenamiento guardado")
                    navController.navigate(Routes.History.route) {
                        popUpTo(Routes.LogWorkout.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.Profile.route) {
            ProfileScreen(
                showMessage = showMessage, // aquí se usa "Objetivo cambiado"
                onLogout = {
                    authRepo.logout()
                    // ✅ sin snackbar (no lo quieres)
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true } // limpia todo el stack
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
